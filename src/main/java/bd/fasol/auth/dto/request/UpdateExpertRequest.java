package bd.fasol.auth.dto.request;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateExpertRequest(
        @Size(max = 100) String name,
        String profileImageUrl,
        String designation,
        String qualification,
        String specialization,
        String visitAddress,
        String expertType,
        Boolean available,
        Boolean online,
        String district,
        String upazila,
        Double latitude,
        Double longitude,
        Boolean isActive,
        List<Long> cropIds,
        Boolean acceptingConsultations,
        String availabilityStatus,
        List<Double> location,
        Integer displayOrder
) {}
