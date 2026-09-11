package bd.fasol.repository;

import bd.fasol.model.Treatment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
  Optional<Treatment> findFirstByDiseaseIdAndIsActiveTrue(Long id);

  List<Treatment> findByDiseaseId(Long id);
}
