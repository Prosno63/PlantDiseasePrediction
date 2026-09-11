package bd.fasol.conversation.service;

import bd.fasol.common.exception.ApiException;
import bd.fasol.common.util.ImageStorage;
import bd.fasol.conversation.dto.request.UpdateConversationRequest;
import bd.fasol.conversation.dto.response.*;
import bd.fasol.model.*;
import bd.fasol.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import org.springframework.data.domain.PageRequest;

@Service
public class ConversationService {
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final UserRepository users;
    private final ImageStorage imageStorage;

    public ConversationService(ConversationRepository c, MessageRepository m, UserRepository u, ImageStorage s) {
        conversations = c;
        messages = m;
        users = u;
        imageStorage = s;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(User actor, int limit, int offset) {
        if (actor.role == Role.FIELD_WORKER) return List.of();
        limit = Math.max(1, Math.min(limit, 100));
        var page = PageRequest.of(Math.max(0, offset) / limit, limit);
        List<Conversation> list = actor.role == Role.FARMER ? conversations.findByFarmerId(actor.id, page)
                : actor.role == Role.EXPERT ? conversations.findByExpertIdOrExpertIsNullAndResolvedFalse(actor.id, page)
                        : conversations.findAll(page).getContent();
        return list.stream().map(ConversationResponse::from).toList();
    }

    @Transactional
    public List<MessageResponse> messages(User actor, Long id, int limit, int offset) {
        visible(actor, id);
        limit = Math.max(1, Math.min(limit, 100));
        List<Message> list = messages.findByConversationIdOrderByCreatedAt(id,
                PageRequest.of(Math.max(0, offset) / limit, limit));
        list.stream().filter(m -> !m.sender.id.equals(actor.id)).forEach(m -> m.isRead = true);
        messages.saveAll(list);
        return list.stream().map(MessageResponse::from).toList();
    }

    @Transactional
    public MessageResponse send(User actor, Long id, String body, MultipartFile image) {
        if (!StringUtils.hasText(body) && (image == null || image.isEmpty()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Body or image is required");
        Message message = new Message();
        message.conversation = visible(actor, id);
        message.sender = actor;
        message.body = body;
        message.imagePath = image == null ? null : imageStorage.store(image);
        return MessageResponse.from(messages.save(message));
    }

    @Transactional
    public ConversationResponse update(User actor, Long id, UpdateConversationRequest request) {
        if (actor.role == Role.FARMER)
            throw new ApiException(HttpStatus.FORBIDDEN, "Expert or admin required");
        Conversation c = conversations.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found"));
        if (request.expertId() != null)
            c.expert = users.findById(request.expertId())
                    .filter(user -> user.role == Role.EXPERT)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expert not found"));
        else if (actor.role == Role.EXPERT)
            c.expert = actor;
        if ("resolved".equalsIgnoreCase(request.status()))
            c.resolved = true;
        return ConversationResponse.from(conversations.save(c));
    }

    private Conversation visible(User actor, Long id) {
        if (actor.role == Role.FIELD_WORKER)
            throw new ApiException(HttpStatus.FORBIDDEN, "Field workers cannot access conversations");
        Conversation c = conversations.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found"));
        if (actor.role == Role.FARMER && !c.farmer.id.equals(actor.id))
            throw new ApiException(HttpStatus.NOT_FOUND, "Conversation not found");
        if (actor.role == Role.EXPERT && c.expert != null && !c.expert.id.equals(actor.id))
            throw new ApiException(HttpStatus.NOT_FOUND, "Conversation not found");
        return c;
    }
}
