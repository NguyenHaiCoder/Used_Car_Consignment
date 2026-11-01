package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.admin.SavePermissionsRequest;
import fu.he.training.fall2026.entity.auth.Permission;
import fu.he.training.fall2026.entity.auth.Role;
import fu.he.training.fall2026.entity.auth.User;
import java.util.List;
import java.util.Set;

public interface AdminPermissionService {

    List<Permission> listPermissions();

    List<Role> listRoles();

    Set<String> currentGrantKeys();

    void saveGrants(SavePermissionsRequest request, User actor);
}
