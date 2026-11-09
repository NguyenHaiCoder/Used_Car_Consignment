package fu.he.training.fall2026.dto.admin;

import fu.he.training.fall2026.entity.enums.RoleCode;
import java.util.List;

/** Nguyễn Việt Hải: dữ liệu màn phân quyền, mỗi role một tab, trong tab chia theo nhóm chức năng. */
public final class PermissionMatrix {

    private PermissionMatrix() {
    }

    public record RoleTab(RoleCode code, String name, int enabled, int total, List<ModuleGroup> modules) {

        public int disabled() {
            return total - enabled;
        }
    }

    public record ModuleGroup(String name, List<Item> items) {
    }

    /** grantKey là giá trị checkbox gửi lên: ROLE|MÃ_CHỨC_NĂNG. */
    public record Item(String code, String label, String description, boolean enabled, boolean locked, String grantKey) {
    }
}
