package bd.fasol.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

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
    public String district;
    public String upazila;
    public Double latitude;
    public Double longitude;

    @Enumerated(EnumType.STRING)
    public Role role = Role.FARMER;

    public Instant createdAt = Instant.now();
}
