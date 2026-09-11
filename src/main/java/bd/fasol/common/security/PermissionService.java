package bd.fasol.common.security;

import bd.fasol.common.exception.ApiException;
import bd.fasol.model.Permission;
import bd.fasol.model.Role;
import bd.fasol.model.RolePermission;
import bd.fasol.model.User;
import bd.fasol.model.UserPermission;
import bd.fasol.repository.PermissionRepository;
import bd.fasol.repository.RolePermissionRepository;
import bd.fasol.repository.UserPermissionRepository;
import bd.fasol.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionService {
    private final PermissionRepository permissions;
    private final RolePermissionRepository rolePermissions;
    private final UserPermissionRepository userPermissions;
    private final UserRepository users;

    public PermissionService(PermissionRepository permissions, RolePermissionRepository rolePermissions,
            UserPermissionRepository userPermissions, UserRepository users) {
        this.permissions = permissions;
        this.rolePermissions = rolePermissions;
        this.userPermissions = userPermissions;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Set<SimpleGrantedAuthority> authorities(User user) {
        Set<String> codes = new HashSet<>();
        for (RolePermission grant : rolePermissions.findByRole(user.role)) {
            codes.add(grant.permission.code);
        }
        for (UserPermission grant : userPermissions.findByUserId(user.id)) {
            codes.add(grant.permission.code);
        }
        return codes.stream().map(SimpleGrantedAuthority::new).collect(java.util.stream.Collectors.toSet());
    }

    @Transactional
    public void grant(Long userId, String code) {
        User user = users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Permission permission = permissions.findByCode(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Permission not found"));
        if (userPermissions.findByUserIdAndPermissionCode(user.id, code).isEmpty()) {
            UserPermission grant = new UserPermission();
            grant.user = user;
            grant.permission = permission;
            userPermissions.save(grant);
        }
    }

    @Transactional
    public void revoke(Long userId, String code) {
        User user = users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        userPermissions.findByUserIdAndPermissionCode(user.id, code).ifPresent(userPermissions::delete);
    }

    @Transactional(readOnly = true)
    public List<Permission> list() {
        return permissions.findAll();
    }
}
