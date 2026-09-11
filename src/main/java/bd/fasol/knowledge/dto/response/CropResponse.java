package bd.fasol.knowledge.dto.response;

import bd.fasol.model.Crop;

public record CropResponse(Long id, String nameBn, String nameEn, boolean active, String imageUrl, boolean selectable) {
    public static CropResponse from(Crop x) {
        return new CropResponse(x.id, x.nameBn, x.nameEn, x.isActive, x.imageUrl, x.selectable);
    }
}
