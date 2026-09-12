package bd.fasol.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

@Entity(name = "messages")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Message {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne
  public Conversation conversation;

  @ManyToOne
  public User sender;

  @Column(columnDefinition = "TEXT")
  public String body;
  public String imagePath;
  @Column(columnDefinition = "BYTEA")
  public byte[] imageData;
  public String imageContentType;
  public boolean isRead;
  public Instant createdAt = Instant.now();
}
