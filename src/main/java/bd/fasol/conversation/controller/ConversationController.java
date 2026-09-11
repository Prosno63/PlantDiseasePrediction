package bd.fasol.conversation.controller;

import bd.fasol.conversation.dto.request.UpdateConversationRequest;
import bd.fasol.conversation.dto.response.ConversationResponse;
import bd.fasol.conversation.dto.response.MessageResponse;
import bd.fasol.conversation.service.ConversationService;
import bd.fasol.model.User;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('CONVERSATIONS_READ')")
    public List<ConversationResponse> list(Authentication authentication) {
        return service.list((User) authentication.getPrincipal());
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize("hasAuthority('CONVERSATIONS_READ')")
    public List<MessageResponse> messages(@PathVariable Long id, Authentication authentication) {
        return service.messages((User) authentication.getPrincipal(), id);
    }

    @PostMapping(value = "/{id}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CONVERSATIONS_MESSAGE')")
    public MessageResponse send(
            @PathVariable Long id,
            @RequestParam(required = false) String body,
            @RequestParam(required = false) MultipartFile image,
            Authentication authentication) {
        return service.send((User) authentication.getPrincipal(), id, body, image);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('CONVERSATIONS_MANAGE')")
    public ConversationResponse update(
            @PathVariable Long id,
            @RequestBody UpdateConversationRequest request,
            Authentication authentication) {
        return service.update((User) authentication.getPrincipal(), id, request);
    }
}
