package bd.fasol.repository;

import bd.fasol.model.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

public interface MessageRepository extends JpaRepository<Message, Long> {
  List<Message> findByConversationIdOrderByCreatedAt(Long id);
  List<Message> findByConversationIdOrderByCreatedAt(Long id, Pageable pageable);
}
