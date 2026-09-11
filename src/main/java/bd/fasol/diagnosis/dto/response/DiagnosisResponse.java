package bd.fasol.diagnosis.dto.response;

import bd.fasol.model.Diagnosis;
import bd.fasol.prediction.dto.response.TreatmentResponse;
import java.time.Instant;

public record DiagnosisResponse(Long id, Long farmerId, String diseaseName, Double confidence, boolean needsExpertReview, String message, TreatmentResponse treatment, String outcome, Instant createdAt) {
    public static DiagnosisResponse from(Diagnosis d) {
        return new DiagnosisResponse(d.id, d.farmer.id, d.diseaseNameRaw, d.confidence, d.needsExpertReview, d.aiMessage, TreatmentResponse.from(d.treatment), d.outcomeFeedback, d.createdAt);
    }
}
