package bd.fasol.repository;

import bd.fasol.model.Role;
import bd.fasol.model.RolePermission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRole(Role role);
    boolean existsByRoleAndPermissionCode(Role role, String code);
}
