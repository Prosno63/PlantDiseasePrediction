package bd.fasol.repository;

import bd.fasol.model.Conversation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByFarmerId(Long id);

    List<Conversation> findByExpertIdOrExpertIsNullAndResolvedFalse(Long id);
}
