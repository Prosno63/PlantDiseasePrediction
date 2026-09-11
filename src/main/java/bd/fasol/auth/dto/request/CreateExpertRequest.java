package bd.fasol.auth.dto.request;

import bd.fasol.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExpertRequest(
        @NotBlank @Size(max = 20) String phoneNumber,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 100) String name,
        String profileImageUrl,
        String designation,
        String qualification,
        String specialization,
        Boolean available,
        Boolean online,
        String district,
        String upazila,
        Double latitude,
        Double longitude
) {}
