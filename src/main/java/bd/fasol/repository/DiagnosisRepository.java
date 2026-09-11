package bd.fasol.repository;

import bd.fasol.model.Diagnosis;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {
  List<Diagnosis> findByFarmerIdOrderByCreatedAtDesc(Long id);
  List<Diagnosis> findByFarmerIdOrderByCreatedAtDesc(Long id, Pageable pageable);
}
