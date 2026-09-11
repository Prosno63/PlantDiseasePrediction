package bd.fasol.common.security;

import bd.fasol.model.Permission;
import bd.fasol.model.Role;
import bd.fasol.model.RolePermission;
import bd.fasol.repository.PermissionRepository;
import bd.fasol.repository.RolePermissionRepository;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class PermissionSeeder implements CommandLineRunner {
    private final PermissionRepository permissions;
    private final RolePermissionRepository rolePermissions;

    public PermissionSeeder(PermissionRepository permissions, RolePermissionRepository rolePermissions) {
        this.permissions = permissions;
        this.rolePermissions = rolePermissions;
    }

    @Override
    public void run(String... args) {
        Map<String, String> catalog = Map.ofEntries(
                Map.entry("PREDICTIONS_CREATE", "Run text and image predictions"),
                Map.entry("DIAGNOSES_READ", "Read diagnosis history"),
                Map.entry("DIAGNOSES_FEEDBACK", "Submit diagnosis feedback"),
                Map.entry("KNOWLEDGE_READ", "Read the knowledge base"),
                Map.entry("KNOWLEDGE_WRITE", "Manage crops, diseases, and treatments"),
                Map.entry("CONVERSATIONS_READ", "Read conversations and messages"),
                Map.entry("CONVERSATIONS_MESSAGE", "Send conversation messages"),
                Map.entry("CONVERSATIONS_MANAGE", "Claim and resolve conversations"),
                Map.entry("ADMIN_ACCESS", "Manage users, experts, and administration"));

        catalog.forEach((code, description) -> {
            Permission permission = permissions.findByCode(code).orElseGet(() -> {
                Permission created = new Permission();
                created.code = code;
                created.description = description;
                return permissions.save(created);
            });
            for (Role role : defaultRoles(code)) {
                if (!rolePermissions.existsByRoleAndPermissionCode(role, permission.code)) {
                    RolePermission grant = new RolePermission();
                    grant.role = role;
                    grant.permission = permission;
                    rolePermissions.save(grant);
                }
            }
        });
    }

    private Role[] defaultRoles(String code) {
        return switch (code) {
            case "PREDICTIONS_CREATE", "DIAGNOSES_FEEDBACK" -> new Role[]{Role.FARMER, Role.ADMIN};
            case "DIAGNOSES_READ", "KNOWLEDGE_READ", "CONVERSATIONS_READ", "CONVERSATIONS_MESSAGE" ->
                    new Role[]{Role.FARMER, Role.EXPERT, Role.FIELD_WORKER, Role.ADMIN};
            case "CONVERSATIONS_MANAGE" -> new Role[]{Role.EXPERT, Role.ADMIN};
            case "KNOWLEDGE_WRITE", "ADMIN_ACCESS" -> new Role[]{Role.ADMIN};
            default -> new Role[]{Role.ADMIN};
        };
    }
}
