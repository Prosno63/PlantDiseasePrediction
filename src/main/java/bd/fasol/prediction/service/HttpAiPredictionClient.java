package bd.fasol.prediction.service;

import bd.fasol.common.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class HttpAiPredictionClient {
    private static final Logger log = LoggerFactory.getLogger(HttpAiPredictionClient.class);
    private final RestClient client;
    private final ObjectMapper json;
    private final String baseUrl, key;

    public HttpAiPredictionClient(RestClient.Builder builder, ObjectMapper json,
            @Value("${ai-service.base-url}") String baseUrl,
            @Value("${ai-service.api-key:}") String key) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        this.client = builder.requestFactory(requestFactory).build();
        this.json = json;
        this.baseUrl = baseUrl;
        this.key = key;
    }

    public record AiResult(String disease, Double confidence, boolean needsExpertReview, String message) {}

    public AiResult text(String text, String crop) {
        return call(client.post().uri(baseUrl + "/predict/" + crop + "/text")
                .header("X-API-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("text", text)));
    }

    public AiResult image(MultipartFile image, String crop) {
        try {
            var body = new LinkedMultiValueMap<String, Object>();
            body.add("image", new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            });
            return call(client.post().uri(baseUrl + "/predict/" + crop + "/image")
                    .header("X-API-Key", key)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot read image");
        }
    }

    private AiResult call(RestClient.RequestHeadersSpec<?> request) {
        try {
            AiResult raw = json.readValue(request.retrieve().body(String.class), AiResult.class);
            Double confidence = raw.confidence() != null && raw.confidence() > 1 ? raw.confidence() / 100 : raw.confidence();
            return new AiResult(raw.disease(), confidence, raw.needsExpertReview(), raw.message());
        } catch (RestClientResponseException e) {
            String detail = e.getResponseBodyAsString();
            try {
                detail = json.readTree(detail).path("detail").asText(detail);
            } catch (Exception ignored) {
            }
            log.warn("AI request failed with HTTP {}: {}", e.getStatusCode().value(), detail);
            throw new ApiException(e.getStatusCode().is4xxClientError() ? HttpStatus.valueOf(e.getStatusCode().value()) : HttpStatus.BAD_GATEWAY, detail);
        } catch (RestClientException e) {
            log.warn("AI request failed: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI service is unavailable");
        } catch (Exception e) {
            log.warn("AI response could not be processed: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI returned an invalid prediction response");
        }
    }
}
