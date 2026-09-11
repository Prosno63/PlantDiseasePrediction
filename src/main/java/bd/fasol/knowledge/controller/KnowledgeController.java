package bd.fasol.knowledge.controller;

import bd.fasol.knowledge.dto.request.CreateCropRequest;
import bd.fasol.knowledge.dto.request.UpdateCropRequest;
import bd.fasol.knowledge.dto.response.CropResponse;
import bd.fasol.knowledge.dto.response.DiseaseResponse;
import bd.fasol.knowledge.service.KnowledgeService;
import bd.fasol.prediction.dto.response.TreatmentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class KnowledgeController {
    private final KnowledgeService service;

    public KnowledgeController(KnowledgeService service) {
        this.service = service;
    }

    @GetMapping("/crops")
    @PreAuthorize("hasAuthority('KNOWLEDGE_READ')")
    public List<CropResponse> crops(@RequestParam(required = false) Boolean activeOnly) {
        return service.crops(activeOnly);
    }

    @GetMapping("/diseases")
    @PreAuthorize("hasAuthority('KNOWLEDGE_READ')")
    public List<DiseaseResponse> diseases(@RequestParam(required = false) Long cropId) {
        return service.diseases(cropId);
    }

    @GetMapping("/treatments")
    @PreAuthorize("hasAuthority('KNOWLEDGE_READ')")
    public List<TreatmentResponse> treatments(@RequestParam(required = false) Long diseaseId) {
        return service.treatments(diseaseId);
    }

    @PostMapping("/admin/crops")
    @PreAuthorize("hasAuthority('KNOWLEDGE_WRITE')")
    public ResponseEntity<CropResponse> createCrop(@Valid @RequestBody CreateCropRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCrop(request));
    }

    @PutMapping("/admin/crops/{id}")
    @PreAuthorize("hasAuthority('KNOWLEDGE_WRITE')")
    public CropResponse updateCrop(@PathVariable Long id, @Valid @RequestBody UpdateCropRequest request) {
        return service.updateCrop(id, request);
    }

    @DeleteMapping("/admin/crops/{id}")
    @PreAuthorize("hasAuthority('KNOWLEDGE_WRITE')")
    public ResponseEntity<Void> deleteCrop(@PathVariable Long id) {
        service.deleteCrop(id);
        return ResponseEntity.noContent().build();
    }
}
