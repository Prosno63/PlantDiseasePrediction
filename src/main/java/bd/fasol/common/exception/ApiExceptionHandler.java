package bd.fasol.common.exception;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.bind.MethodArgumentNotValidException; import java.util.*;
@RestControllerAdvice
public class ApiExceptionHandler {
  record ErrorResponse(String detail) {}
  @ExceptionHandler(ApiException.class) ResponseEntity<ErrorResponse> api(ApiException e) { return ResponseEntity.status(e.status).body(new ErrorResponse(e.getMessage())); }
  @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException e) { return ResponseEntity.unprocessableEntity().body(new ErrorResponse(e.getBindingResult().getAllErrors().get(0).getDefaultMessage())); }
  @ExceptionHandler(ConstraintViolationException.class) ResponseEntity<ErrorResponse> invalid(ConstraintViolationException e) { return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage())); }
}
