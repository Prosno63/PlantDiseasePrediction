package bd.fasol.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity(name = "users")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true, nullable = false)
    public String phoneNumber;

    @JsonIgnore
    public String passwordHash;
    public String name;
    public String profileImageUrl;
    public String designation;
    public String qualification;
    public String specialization;
    public boolean available = true;
    public boolean online = false;
    public boolean isActive = true;
    public String district;
    public String upazila;
    public Double latitude;
    public Double longitude;

    @Enumerated(EnumType.STRING)
    public Role role = Role.FARMER;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_crop_ids", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "crop_id")
    public Set<Long> cropIds = new LinkedHashSet<>();

    public boolean acceptingConsultations = true;
    public String availabilityStatus = "AVAILABLE";
    public Integer displayOrder = 0;
    public Instant updatedAt = Instant.now();

    public Instant createdAt = Instant.now();
}
