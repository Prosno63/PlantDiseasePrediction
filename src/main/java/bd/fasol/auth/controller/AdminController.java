package bd.fasol.auth.controller;

import bd.fasol.auth.dto.request.CreateExpertRequest;
import bd.fasol.auth.dto.request.RegisterRequest;
import bd.fasol.auth.dto.request.UpdateExpertRequest;
import bd.fasol.auth.dto.response.ExpertResponse;
import bd.fasol.auth.dto.response.UserResponse;
import bd.fasol.auth.service.AuthService;
import bd.fasol.common.exception.ApiException;
import bd.fasol.model.Role;
import bd.fasol.model.User;
import jakarta.validation.Valid;
import bd.fasol.repository.UserRepository;
import bd.fasol.repository.RefreshTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AuthService authService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AdminController(AuthService authService, UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @GetMapping("/users")
    public List<UserResponse> listUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.max(1, Math.min(size, 100));
        page = Math.max(0, page);
        var pageable = PageRequest.of(page, size);
        if (role != null) {
            return userRepository.findByRole(role, pageable).stream()
                    .map(UserResponse::from)
                    .toList();
        }
        return userRepository.findAll(pageable).stream()
                .map(UserResponse::from)
                .toList();
    }

    @GetMapping("/users/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return UserResponse.from(user);
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody RegisterRequest request, Authentication authentication) {
        User actor = (User) authentication.getPrincipal();
        if (request.role() != null && request.role() != Role.FARMER) {
            if (actor.role != Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        User created = authService.createUser(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(created));
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        User actor = (User) authentication.getPrincipal();
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        
        if (target.id.equals(actor.id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        if (target.role == Role.ADMIN && actor.role != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        refreshTokenRepository.deleteByUserId(id);
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/experts")
    public List<ExpertResponse> listExperts() {
        return userRepository.findByRole(Role.EXPERT).stream()
                .map(ExpertResponse::from)
                .toList();
    }

    @GetMapping("/experts/{id}")
    public ExpertResponse getExpert(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expert not found"));
        if (user.role != Role.EXPERT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "User is not an expert");
        }
        return ExpertResponse.from(user);
    }

    @PostMapping("/experts")
    public ResponseEntity<ExpertResponse> createExpert(@Valid @RequestBody CreateExpertRequest request) {
        if (userRepository.findByPhoneNumber(request.phoneNumber()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        User expert = new User();
        expert.phoneNumber = request.phoneNumber();
        expert.passwordHash = authService.getPasswordEncoder().encode(request.password());
        expert.name = request.name();
        expert.profileImageUrl = request.profileImageUrl();
        expert.designation = request.designation();
        expert.qualification = request.qualification();
        expert.specialization = request.specialization();
        if (request.available() != null) expert.available = request.available();
        if (request.online() != null) expert.online = request.online();
        expert.district = request.district();
        expert.upazila = request.upazila();
        expert.latitude = request.latitude();
        expert.longitude = request.longitude();
        expert.role = Role.EXPERT;
        User saved = userRepository.save(expert);
        return ResponseEntity.status(HttpStatus.CREATED).body(ExpertResponse.from(saved));
    }

    @PutMapping("/experts/{id}")
    public ExpertResponse updateExpert(@PathVariable Long id, @Valid @RequestBody UpdateExpertRequest request) {
        User expert = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expert not found"));
        if (expert.role != Role.EXPERT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "User is not an expert");
        }
        if (request.name() != null) expert.name = request.name();
        if (request.profileImageUrl() != null) expert.profileImageUrl = request.profileImageUrl();
        if (request.designation() != null) expert.designation = request.designation();
        if (request.qualification() != null) expert.qualification = request.qualification();
        if (request.specialization() != null) expert.specialization = request.specialization();
        if (request.available() != null) expert.available = request.available();
        if (request.online() != null) expert.online = request.online();
        if (request.district() != null) expert.district = request.district();
        if (request.upazila() != null) expert.upazila = request.upazila();
        if (request.latitude() != null) expert.latitude = request.latitude();
        if (request.longitude() != null) expert.longitude = request.longitude();
        return ExpertResponse.from(userRepository.save(expert));
    }

    @DeleteMapping("/experts/{id}")
    @Transactional
    public ResponseEntity<Void> deleteExpert(@PathVariable Long id) {
        User expert = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expert not found"));
        if (expert.role != Role.EXPERT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "User is not an expert");
        }
        refreshTokenRepository.deleteByUserId(id);
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
