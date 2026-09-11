package bd.fasol.diagnosis.controller;

import bd.fasol.diagnosis.dto.request.FeedbackRequest;
import bd.fasol.diagnosis.dto.response.DiagnosisResponse;
import bd.fasol.diagnosis.service.DiagnosisService;
import bd.fasol.model.User;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/diagnoses")
public class DiagnosisController {
    private final DiagnosisService service;

    public DiagnosisController(DiagnosisService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DIAGNOSES_READ')")
    public List<DiagnosisResponse> list(
            @RequestParam(required = false) Long farmerId, Authentication authentication) {
        return service.list((User) authentication.getPrincipal(), farmerId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DIAGNOSES_READ')")
    public DiagnosisResponse get(@PathVariable Long id, Authentication authentication) {
        return service.get((User) authentication.getPrincipal(), id);
    }

    @PostMapping("/{id}/feedback")
    @PreAuthorize("hasAuthority('DIAGNOSES_FEEDBACK')")
    public DiagnosisResponse feedback(
            @PathVariable Long id,
            @Valid @RequestBody FeedbackRequest request,
            Authentication authentication) {
        return service.feedback((User) authentication.getPrincipal(), id, request);
    }
}
