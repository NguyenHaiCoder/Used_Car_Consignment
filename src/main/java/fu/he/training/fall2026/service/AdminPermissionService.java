package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.admin.PermissionMatrix;
import fu.he.training.fall2026.dto.admin.SavePermissionsRequest;
import fu.he.training.fall2026.entity.auth.User;
import java.util.List;

/** Nguyễn Việt Hải: bật/tắt chức năng của từng role. Không cấp chéo chức năng sang role khác. */
public interface AdminPermissionService {

    List<PermissionMatrix.RoleTab> matrix();

    /** Lưu theo diff so với DB, trả câu tóm tắt để báo cho Admin. */
    String saveGrants(SavePermissionsRequest request, User actor);
}
