package bd.fasol.prediction.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TextPredictionRequest(@NotBlank String text, String inputType) {
}
