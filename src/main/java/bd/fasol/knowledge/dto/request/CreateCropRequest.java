package bd.fasol.knowledge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCropRequest(
        @NotBlank @Size(max = 100) String nameBn,
        @NotBlank @Size(max = 100) String nameEn,
        String imageUrl,
        Boolean selectable,
        Integer displayOrder
) {}