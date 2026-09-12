package bd.fasol.repository;

import bd.fasol.model.Crop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CropRepository extends JpaRepository<Crop, Long> {
    List<Crop> findByIsActiveTrue();

    Optional<Crop> findByNameEnIgnoreCaseAndIsActiveTrue(String nameEn);
}
