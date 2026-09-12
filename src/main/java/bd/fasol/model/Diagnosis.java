package bd.fasol.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

@Entity
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Diagnosis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne
    public User farmer;

    @ManyToOne
    public Crop crop;

    public String inputType;

    @Column(columnDefinition = "TEXT")
    public String inputText;
    public String imagePath;
    @Column(columnDefinition = "BYTEA")
    public byte[] imageData;
    public String imageContentType;

    @ManyToOne
    public Disease disease;

    public String diseaseNameRaw;
    public Double confidence;
    public boolean needsExpertReview;

    @Column(columnDefinition = "TEXT")
    public String aiMessage;

    @ManyToOne
    public Treatment treatment;

    public String outcomeFeedback;
    public Instant createdAt = Instant.now();
}
