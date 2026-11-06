package fu.he.training.fall2026.model.ui;

import static fu.he.training.fall2026.model.ui.UiModels.Role;

import java.util.List;

/** Role labels used by authenticated Admin/Manager shells. */
public final class AppRoles {

    private static final List<Role> ROLES = List.of(
            new Role("admin", "Quản trị viên", "Nguyễn Việt Hải", "VH"),
            new Role("manager", "Quản lý", "Nguyễn Văn Khoa", "VK"),
            new Role("consignment", "Nhân viên ký gửi", "Hà Lệ Quyên", "LQ"),
            new Role("inspector", "Kiểm định viên", "Trường An", "TA"),
            new Role("sales", "Nhân viên kinh doanh", "Đỗ Lan Anh", "LA"),
            new Role("finance", "Nhân viên tài chính", "Vũ Mạnh Cường", "MC"),
            new Role("handover", "Nhân viên bàn giao", "Lê Minh Tuấn", "MT"),
            new Role("documents", "Nhân viên hồ sơ", "Nguyễn Phúc Khang", "PK"));

    private AppRoles() { }

    public static List<Role> all() {
        return ROLES;
    }

    public static Role of(String key) {
        return ROLES.stream()
                .filter(role -> role.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + key));
    }
}
