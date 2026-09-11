package bd.fasol.repository;

import bd.fasol.model.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
  List<Message> findByConversationIdOrderByCreatedAt(Long id);
}
