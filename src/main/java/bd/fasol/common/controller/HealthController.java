package bd.fasol.common.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import javax.sql.DataSource;

@RestController
public class HealthController {
  private final DataSource dataSource;

  public HealthController(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    try (var connection = dataSource.getConnection()) {
      if (!connection.isValid(2)) throw new IllegalStateException("Database is not ready");
      return ResponseEntity.ok(Map.of("status", "ok", "service", "Fasol Doctor Backend (Spring Boot)"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("status", "unavailable", "service", "Fasol Doctor Backend (Spring Boot)"));
    }
  }
}
