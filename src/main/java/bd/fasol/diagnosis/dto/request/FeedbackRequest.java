package bd.fasol.diagnosis.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FeedbackRequest(@NotBlank String outcome) {
}
