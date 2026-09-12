package bd.fasol.diagnosis.dto.response;

import bd.fasol.model.Diagnosis;
import bd.fasol.prediction.dto.response.TreatmentResponse;
import java.time.Instant;

public record DiagnosisResponse(Long id, Long farmerId, String diseaseName, String confidence, boolean needsExpertReview, String message, TreatmentResponse treatment, String outcome, Instant createdAt) {
    public static DiagnosisResponse from(Diagnosis d) {
        String confidence = d.confidence == null ? null : Math.round(d.confidence <= 1 ? d.confidence * 100 : d.confidence) + "%";
        return new DiagnosisResponse(d.id, d.farmer.id, d.diseaseNameRaw, confidence, d.needsExpertReview, d.aiMessage, TreatmentResponse.from(d.treatment), d.outcomeFeedback, d.createdAt);
    }
}
