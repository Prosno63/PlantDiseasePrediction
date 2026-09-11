package bd.fasol.prediction.controller;

import bd.fasol.model.User;
import bd.fasol.prediction.dto.request.TextPredictionRequest;
import bd.fasol.prediction.dto.response.PredictionResponse;
import bd.fasol.prediction.service.PredictionService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/predictions")
public class PredictionController {
    private final PredictionService service;

    public PredictionController(PredictionService service) {
        this.service = service;
    }

    @PostMapping("/text")
    @PreAuthorize("hasAuthority('PREDICTIONS_CREATE')")
    public PredictionResponse text(
            @Valid @RequestBody TextPredictionRequest request, Authentication authentication) {
        return service.predictText((User) authentication.getPrincipal(), request);
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PREDICTIONS_CREATE')")
    public PredictionResponse image(
            @RequestParam MultipartFile image, Authentication authentication) {
        return service.predictImage((User) authentication.getPrincipal(), image);
    }
}
