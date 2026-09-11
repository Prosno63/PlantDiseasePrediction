package bd.fasol.auth.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateExpertRequest(
        @Size(max = 100) String name,
        String profileImageUrl,
        String designation,
        String qualification,
        String specialization,
        Boolean available,
        Boolean online,
        String district,
        String upazila,
        Double latitude,
        Double longitude,
        Boolean isActive
) {}
