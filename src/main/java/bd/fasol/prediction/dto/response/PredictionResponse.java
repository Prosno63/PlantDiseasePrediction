package bd.fasol.prediction.dto.response;

public record PredictionResponse(
    Long diagnosisId,
    String disease,
    String confidence,
    boolean needsExpertReview,
    String message,
    TreatmentResponse treatment) {
}
