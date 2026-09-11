package bd.fasol.conversation.controller;

import bd.fasol.conversation.dto.request.UpdateConversationRequest;
import bd.fasol.conversation.dto.response.ConversationResponse;
import bd.fasol.conversation.dto.response.MessageResponse;
import bd.fasol.conversation.service.ConversationService;
import bd.fasol.model.User;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {
    private final ConversationService service;

    public ConversationController(ConversationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ConversationResponse> list(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {
        return service.list((User) authentication.getPrincipal(), limit, offset);
    }

    @GetMapping("/{id}/messages")
    public List<MessageResponse> messages(@PathVariable Long id,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {
        return service.messages((User) authentication.getPrincipal(), id, limit, offset);
    }

    @PostMapping(value = "/{id}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MessageResponse send(
            @PathVariable Long id,
            @RequestParam(required = false) String body,
            @RequestParam(required = false) MultipartFile image,
            Authentication authentication) {
        return service.send((User) authentication.getPrincipal(), id, body, image);
    }

    @PatchMapping("/{id}")
    public ConversationResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateConversationRequest request,
            Authentication authentication) {
        return service.update((User) authentication.getPrincipal(), id, request);
    }
}
