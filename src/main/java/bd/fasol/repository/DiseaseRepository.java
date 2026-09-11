package bd.fasol.repository;

import bd.fasol.model.Disease;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiseaseRepository extends JpaRepository<Disease, Long> {
  Optional<Disease> findByModelClassLabelAndIsActiveTrue(String label);

  List<Disease> findByCropId(Long id);
}
