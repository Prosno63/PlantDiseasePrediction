package bd.fasol.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Crop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String nameBn;
    public String nameEn;
    public boolean isActive = true;
    public String imageUrl;
    public boolean selectable = true;
    public Integer displayOrder = 0;
    public Instant updatedAt = Instant.now();
}
