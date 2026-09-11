package bd.fasol.repository;

import bd.fasol.model.Role;
import bd.fasol.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String phoneNumber);
    List<User> findByRole(Role role);
    List<User> findByRole(Role role, Pageable pageable);
}
