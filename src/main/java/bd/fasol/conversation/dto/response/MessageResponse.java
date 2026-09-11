package bd.fasol.conversation.dto.response; import bd.fasol.model.Message; import java.time.Instant;
public record MessageResponse(Long id,Long senderId,String body,String imagePath,boolean read,Instant createdAt){public static MessageResponse from(Message m){return new MessageResponse(m.id,m.sender.id,m.body,m.imagePath,m.isRead,m.createdAt);}}
