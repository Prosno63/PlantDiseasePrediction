package bd.fasol.auth.dto.request;

import bd.fasol.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

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
        Double longitude,
        List<Long> cropIds,
        Boolean acceptingConsultations,
        String availabilityStatus,
        List<Double> location,
        Integer displayOrder
) {}
