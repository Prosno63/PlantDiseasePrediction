package bd.fasol.conversation.dto.response;

import bd.fasol.model.Conversation;
import java.time.Instant;

public record ConversationResponse(Long id, Long farmerId, Long expertId, Long diagnosisId, boolean resolved, Instant createdAt) {
    public static ConversationResponse from(Conversation c) {
        return new ConversationResponse(c.id, c.farmer.id, c.expert == null ? null : c.expert.id, c.diagnosis == null ? null : c.diagnosis.id, c.resolved, c.createdAt);
    }
}
