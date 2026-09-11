package bd.fasol.knowledge.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateCropRequest(
        @Size(max = 100) String nameBn,
        @Size(max = 100) String nameEn,
        Boolean isActive,
        String imageUrl,
        Boolean selectable
) {}