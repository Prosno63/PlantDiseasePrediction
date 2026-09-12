package bd.fasol.prediction.service;

import bd.fasol.common.exception.ApiException;
import bd.fasol.common.util.ImageStorage;
import bd.fasol.model.*;
import bd.fasol.prediction.dto.request.TextPredictionRequest;
import bd.fasol.prediction.dto.response.*;
import bd.fasol.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Service
public class PredictionService {
    private final HttpAiPredictionClient ai;
    private final CropRepository crops;
    private final DiagnosisRepository diagnoses;
    private final ConversationRepository conversations;
    private final ImageStorage imageStorage;

    public PredictionService(HttpAiPredictionClient ai, CropRepository crops,
            DiagnosisRepository diagnoses, ConversationRepository conversations,
            ImageStorage imageStorage) {
        this.ai = ai;
        this.crops = crops;
        this.diagnoses = diagnoses;
        this.conversations = conversations;
        this.imageStorage = imageStorage;
    }

    public PredictionResponse predictText(User farmer, String cropName, TextPredictionRequest request) {
        Crop crop = resolveCrop(cropName);
        return record(farmer, crop, request.inputType() != null && request.inputType().equalsIgnoreCase("voice") ? "voice" : "text", request.text(), null, null, ai.text(request.text(), cropName));
    }

    public PredictionResponse predictImage(User farmer, String cropName, MultipartFile image) {
        Crop crop = resolveCrop(cropName);
        imageStorage.validate(image);
        HttpAiPredictionClient.AiResult result = ai.image(image, cropName);
        return record(farmer, crop, "image", null, imageStorage.store(image), image.getContentType(), result);
    }

    private Crop resolveCrop(String cropName) {
        return crops.findByNameEnIgnoreCaseAndIsActiveTrue(cropName)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown crop: " + cropName));
    }

    private PredictionResponse record(User farmer, Crop crop, String inputType, String text, byte[] imageData, String imageContentType, HttpAiPredictionClient.AiResult result) {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.farmer = farmer;
        diagnosis.crop = crop;
        diagnosis.inputType = inputType;
        diagnosis.inputText = text;
        diagnosis.imageData = imageData;
        diagnosis.imageContentType = imageContentType;
        diagnosis.diseaseNameRaw = result.disease();
        diagnosis.confidence = result.confidence();
        diagnosis.needsExpertReview = result.needsExpertReview();
        diagnosis.aiMessage = result.message();
        diagnosis.treatment = null;
        diagnoses.save(diagnosis);
        if (result.needsExpertReview()) {
            Conversation conversation = new Conversation();
            conversation.farmer = farmer;
            conversation.diagnosis = diagnosis;
            conversations.save(conversation);
        }
        return new PredictionResponse(diagnosis.id, result.disease(), result.confidence() == null ? null : Math.round(result.confidence()) + "%", result.needsExpertReview(), result.message(), TreatmentResponse.from(null));
    }
}
