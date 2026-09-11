package bd.fasol.repository;

import bd.fasol.model.Conversation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByFarmerId(Long id);
    List<Conversation> findByFarmerId(Long id, Pageable pageable);

    List<Conversation> findByExpertIdOrExpertIsNullAndResolvedFalse(Long id);
    List<Conversation> findByExpertIdOrExpertIsNullAndResolvedFalse(Long id, Pageable pageable);
}
