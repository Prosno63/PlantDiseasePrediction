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
    private final DiseaseRepository diseases;
    private final TreatmentRepository treatments;
    private final DiagnosisRepository diagnoses;
    private final ConversationRepository conversations;
    private final ImageStorage imageStorage;

    public PredictionService(HttpAiPredictionClient ai, DiseaseRepository diseases, TreatmentRepository treatments,
            DiagnosisRepository diagnoses, ConversationRepository conversations,
            ImageStorage imageStorage) {
        this.ai = ai;
        this.diseases = diseases;
        this.treatments = treatments;
        this.diagnoses = diagnoses;
        this.conversations = conversations;
        this.imageStorage = imageStorage;
    }

    public PredictionResponse predictText(User farmer, TextPredictionRequest request) {
        return record(farmer, request.inputType() != null && request.inputType().equalsIgnoreCase("voice") ? "voice" : "text", request.text(), null, ai.text(request.text(), sessionId(farmer)));
    }

    public PredictionResponse predictImage(User farmer, MultipartFile image) {
        imageStorage.validate(image);
        HttpAiPredictionClient.AiResult result = ai.image(image, sessionId(farmer));
        String imagePath = imageStorage.store(image);
        try {
            return record(farmer, "image", null, imagePath, result);
        } catch (RuntimeException e) {
            imageStorage.delete(imagePath);
            throw e;
        }
    }

    private PredictionResponse record(User farmer, String inputType, String text, String imagePath, HttpAiPredictionClient.AiResult result) {
        Disease disease = diseases.findByModelClassLabelAndIsActiveTrue(result.disease()).orElse(null);
        Treatment treatment = disease == null ? null : treatments.findFirstByDiseaseIdAndIsActiveTrue(disease.id).orElse(null);
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.farmer = farmer;
        diagnosis.crop = null;
        diagnosis.inputType = inputType;
        diagnosis.inputText = text;
        diagnosis.imagePath = imagePath;
        diagnosis.disease = disease;
        diagnosis.diseaseNameRaw = result.disease();
        diagnosis.confidence = result.confidence();
        diagnosis.needsExpertReview = result.needsExpertReview();
        diagnosis.aiMessage = result.message();
        diagnosis.treatment = treatment;
        diagnoses.save(diagnosis);
        if (result.needsExpertReview()) {
            Conversation conversation = new Conversation();
            conversation.farmer = farmer;
            conversation.diagnosis = diagnosis;
            conversations.save(conversation);
        }
        return new PredictionResponse(diagnosis.id, result.disease(), result.confidence(), result.needsExpertReview(), result.message(), TreatmentResponse.from(treatment));
    }

    private String sessionId(User farmer) {
        return "fasol-farmer-" + farmer.id;
    }
}
