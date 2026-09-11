package bd.fasol.knowledge.service;

import bd.fasol.knowledge.dto.request.CreateCropRequest;
import bd.fasol.knowledge.dto.request.UpdateCropRequest;
import bd.fasol.knowledge.dto.response.CropResponse;
import bd.fasol.knowledge.dto.response.DiseaseResponse;
import bd.fasol.model.Crop;
import bd.fasol.prediction.dto.response.TreatmentResponse;
import bd.fasol.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class KnowledgeService {
    private final CropRepository crops;
    private final DiseaseRepository diseases;
    private final TreatmentRepository treatments;

    public KnowledgeService(CropRepository c, DiseaseRepository d, TreatmentRepository t) {
        crops = c;
        diseases = d;
        treatments = t;
    }

    @Transactional(readOnly = true)
    public List<CropResponse> crops(Boolean activeOnly) {
        List<Crop> list = Boolean.TRUE.equals(activeOnly) ? crops.findByIsActiveTrue() : crops.findAll();
        return list.stream().map(CropResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DiseaseResponse> diseases(Long cropId) {
        return (cropId == null ? diseases.findAll() : diseases.findByCropId(cropId)).stream()
                .map(DiseaseResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TreatmentResponse> treatments(Long diseaseId) {
        return (diseaseId == null ? treatments.findAll() : treatments.findByDiseaseId(diseaseId)).stream()
                .map(TreatmentResponse::from).toList();
    }

    @Transactional
    public CropResponse createCrop(CreateCropRequest request) {
        Crop crop = new Crop();
        crop.nameBn = request.nameBn();
        crop.nameEn = request.nameEn();
        crop.imageUrl = request.imageUrl();
        crop.selectable = request.selectable() != null ? request.selectable() : true;
        crop.isActive = true;
        return CropResponse.from(crops.save(crop));
    }

    @Transactional
    public CropResponse updateCrop(Long id, UpdateCropRequest request) {
        Crop crop = crops.findById(id).orElseThrow(() -> new IllegalArgumentException("Crop not found"));
        if (request.nameBn() != null) crop.nameBn = request.nameBn();
        if (request.nameEn() != null) crop.nameEn = request.nameEn();
        if (request.isActive() != null) crop.isActive = request.isActive();
        if (request.imageUrl() != null) crop.imageUrl = request.imageUrl();
        if (request.selectable() != null) crop.selectable = request.selectable();
        return CropResponse.from(crops.save(crop));
    }

    @Transactional
    public void deleteCrop(Long id) {
        crops.deleteById(id);
    }
}
