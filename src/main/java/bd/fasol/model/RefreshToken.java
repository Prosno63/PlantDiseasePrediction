package bd.fasol.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @jakarta.persistence.Column(name = "token_hash", unique = true, nullable = false)
    public String tokenHash;

    @ManyToOne(optional = false)
    public User user;

    public Instant expiresAt;
    public boolean revoked = false;
    public Instant createdAt = Instant.now();

    @Version
    public Long version;
}
