package bd.fasol.diagnosis.service;

import bd.fasol.common.exception.ApiException;
import bd.fasol.diagnosis.dto.request.FeedbackRequest;
import bd.fasol.diagnosis.dto.response.DiagnosisResponse;
import bd.fasol.model.Diagnosis;
import bd.fasol.model.Role;
import bd.fasol.model.User;
import bd.fasol.repository.DiagnosisRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class DiagnosisService {
    private final DiagnosisRepository diagnoses;

    public DiagnosisService(DiagnosisRepository diagnoses) {
        this.diagnoses = diagnoses;
    }

    @Transactional(readOnly = true)
    public List<DiagnosisResponse> list(User actor, Long farmerId) {
        Long id = actor.role == Role.FARMER ? actor.id : farmerId;
        return (id == null ? diagnoses.findAll() : diagnoses.findByFarmerIdOrderByCreatedAtDesc(id)).stream()
                .map(DiagnosisResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DiagnosisResponse get(User actor, Long id) {
        return DiagnosisResponse.from(visible(actor, id));
    }

    @Transactional
    public DiagnosisResponse feedback(User actor, Long id, FeedbackRequest request) {
        Diagnosis diagnosis = visible(actor, id);
        String outcome = request.outcome().toLowerCase();
        if (!outcome.equals("yes") && !outcome.equals("no") && !outcome.equals("somewhat")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Outcome must be yes, no, or somewhat");
        }
        diagnosis.outcomeFeedback = outcome;
        return DiagnosisResponse.from(diagnoses.save(diagnosis));
    }

    private Diagnosis visible(User actor, Long id) {
        Diagnosis d = diagnoses.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Diagnosis not found"));
        if (actor.role == Role.FARMER && !d.farmer.id.equals(actor.id))
            throw new ApiException(HttpStatus.NOT_FOUND, "Diagnosis not found");
        return d;
    }
}
