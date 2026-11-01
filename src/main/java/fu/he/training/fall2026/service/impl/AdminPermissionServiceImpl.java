package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.admin.SavePermissionsRequest;
import fu.he.training.fall2026.entity.auth.Permission;
import fu.he.training.fall2026.entity.auth.Role;
import fu.he.training.fall2026.entity.auth.RolePermission;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.enums.RoleCode;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.repository.PermissionRepository;
import fu.he.training.fall2026.repository.RolePermissionRepository;
import fu.he.training.fall2026.repository.RoleRepository;
import fu.he.training.fall2026.service.AdminPermissionService;
import fu.he.training.fall2026.service.AuditService;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPermissionServiceImpl implements AdminPermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<Permission> listPermissions() {
        return permissionRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> listRoles() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> currentGrantKeys() {
        return rolePermissionRepository.findAllFetched().stream()
                .map(rp -> grantKey(rp.getRole().getCode().name(), rp.getPermission().getCode()))
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    @Transactional
    public void saveGrants(SavePermissionsRequest request, User actor) {
        Map<String, Role> roles = roleRepository.findAll().stream()
                .collect(Collectors.toMap(r -> r.getCode().name(), Function.identity()));
        Map<String, Permission> permissions = permissionRepository.findAll().stream()
                .collect(Collectors.toMap(Permission::getCode, Function.identity()));

        for (RoleCode code : RoleCode.values()) {
            Role role = roles.get(code.name());
            if (role != null) {
                rolePermissionRepository.deleteByRoleId(role.getId());
            }
        }
        rolePermissionRepository.flush();

        List<String> grants = request.getGrants() == null ? List.of() : request.getGrants();
        for (String raw : grants) {
            if (raw == null || !raw.contains("|")) {
                continue;
            }
            String[] parts = raw.split("\\|", 2);
            String roleCode = parts[0].trim().toUpperCase(Locale.ROOT);
            String permCode = parts[1].trim().toUpperCase(Locale.ROOT);
            Role role = roles.get(roleCode);
            Permission permission = permissions.get(permCode);
            if (role == null || permission == null) {
                throw new BusinessException("Grant không hợp lệ: " + raw);
            }
            RolePermission rp = new RolePermission();
            rp.setRole(role);
            rp.setPermission(permission);
            rolePermissionRepository.save(rp);
        }

        auditService.record(actor, "SAVE_PERMISSIONS", "RolePermission", null,
                "Cập nhật ma trận quyền (" + grants.size() + " grant)");
    }

    public static String grantKey(String roleCode, String permissionCode) {
        return roleCode + "|" + permissionCode;
    }
}
