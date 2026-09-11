package bd.fasol.auth.dto.request;

import bd.fasol.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank String phoneNumber,
    @NotBlank @Size(min = 8) String password,
    @NotBlank String name,
    Role role,
    String profileImageUrl,
    String designation,
    String qualification,
    String specialization,
    Boolean available,
    Boolean online) {
}
