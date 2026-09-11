package bd.fasol.prediction.service;

import bd.fasol.common.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class HttpAiPredictionClient {
    private static final Logger log = LoggerFactory.getLogger(HttpAiPredictionClient.class);
    private final RestClient client;
    private final ObjectMapper json;
    private final String provider, baseUrl, key, model, imageModel;

    public HttpAiPredictionClient(RestClient.Builder builder, ObjectMapper json,
            @Value("${ai-service.provider:custom}") String provider,
            @Value("${ai-service.base-url}") String baseUrl,
            @Value("${ai-service.api-key:}") String key,
            @Value("${ai-service.model:}") String model,
            @Value("${ai-service.image-model:}") String imageModel) {
        this.client = builder.build();
        this.json = json;
        this.provider = provider;
        this.baseUrl = baseUrl;
        this.key = key;
        this.model = model.isBlank() ? defaultModel(provider) : model;
        this.imageModel = imageModel.isBlank() ? this.model : imageModel;
    }

    public record AiResult(String disease, Double confidence, boolean needsExpertReview, String message) {}

    public AiResult text(String text) {
        if (usesOpenAiCompatibleApi()) return openAiText(text);
        return call(client.post().uri(baseUrl + "/predict/text").header("X-API-Key", key).contentType(MediaType.APPLICATION_JSON).body(Map.of("text", text)));
    }

    public AiResult image(MultipartFile image) {
        if (usesOpenAiCompatibleApi()) return openAiImage(image);
        try {
            var body = new LinkedMultiValueMap<String, Object>();
            body.add("image", new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            });
            return call(client.post().uri(baseUrl + "/predict/image").header("X-API-Key", key).contentType(MediaType.MULTIPART_FORM_DATA).body(body));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot read image");
        }
    }

    private boolean usesOpenAiCompatibleApi() {
        return "openai".equalsIgnoreCase(provider)
                || "openrouter".equalsIgnoreCase(provider)
                || "opencode".equalsIgnoreCase(provider);
    }

    private String defaultModel(String provider) {
        if ("opencode".equalsIgnoreCase(provider)) return "deepseek-v4-flash-vision-exp";
        return "openrouter".equalsIgnoreCase(provider) ? "openrouter/auto" : "gpt-4o-mini";
    }

    private AiResult openAiText(String text) {
        return openAi(List.of(Map.of("type", "text", "text", text)));
    }

    private AiResult openAiImage(MultipartFile image) {
        try {
            String dataUrl = "data:" + image.getContentType() + ";base64,"
                    + Base64.getEncoder().encodeToString(image.getBytes());
        return openAi(imageModel, List.of(
                Map.of("type", "text", "text", "Analyze this crop image."),
                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot read image");
        }
    }

    private AiResult openAi(List<Map<String, Object>> content) {
        return openAi(model, content);
    }

    private AiResult openAi(String requestModel, List<Map<String, Object>> content) {
        try {
            if (key.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "AI provider API key is not configured");
            }
            String instructions = "Return only JSON with exactly these fields: "
                    + "disease (one of ব্লাস্ট, ব্রাউন স্পট, সুস্থ, লিফ স্মাট, রাইস টুংরো, শীথ ব্লাইট), "
                    + "confidence (number from 0 to 100), needsExpertReview (boolean), message (Bangla string). "
                    + "Do not add markdown or extra fields.";
            Map<String, Object> body = Map.of(
                    "model", requestModel,
                    "temperature", 0,
                    "messages", List.of(
                            Map.of("role", "system", "content", instructions),
                            Map.of("role", "user", "content", content)));
            String response = client.post()
                    .uri(baseUrl + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return normalize(response);
        } catch (ApiException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("OpenAI request failed with HTTP status {}", e.getStatusCode().value());
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "OpenAI request failed with status " + e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("LLM response could not be processed: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM returned an invalid prediction response");
        }
    }

    private AiResult normalize(String response) throws IOException {
        JsonNode root = json.readTree(response);
        String content = root.path("choices").path(0).path("message").path("content").asText(null);
        if (content == null) content = root.path("output_text").asText(null);
        if (content == null) content = root.path("content").path(0).path("text").asText(null);
        if (content == null) content = root.path("output").path(0).path("content").path(0).path("text").asText(null);
        if (content == null && root.has("disease")) content = root.toString();
        if (content == null) content = response;

        String candidate = content.trim();
        if (candidate.startsWith("```")) {
            candidate = candidate.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        int start = candidate.indexOf('{');
        int end = candidate.lastIndexOf('}');
        if (start >= 0 && end > start) candidate = candidate.substring(start, end + 1);

        try {
            AiResult result = json.readValue(candidate, AiResult.class);
            return new AiResult(
                    result.disease() == null ? "অজানা" : result.disease(),
                    result.confidence() == null ? 0.0 : result.confidence(),
                    result.needsExpertReview(),
                    result.message() == null ? content : result.message());
        } catch (Exception ignored) {
            return new AiResult("অজানা", 0.0, true, content);
        }
    }

    private AiResult call(RestClient.RequestHeadersSpec<?> request) {
        try {
            return normalize(request.retrieve().body(String.class));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI service is unavailable");
        }
    }
}
