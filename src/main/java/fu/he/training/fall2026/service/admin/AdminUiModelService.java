package fu.he.training.fall2026.service.admin;

import static fu.he.training.fall2026.model.ui.UiModels.Action;
import static fu.he.training.fall2026.model.ui.UiModels.Activity;
import static fu.he.training.fall2026.model.ui.UiModels.Fact;
import static fu.he.training.fall2026.model.ui.UiModels.Field;
import static fu.he.training.fall2026.model.ui.UiModels.Link;
import static fu.he.training.fall2026.model.ui.UiModels.Metric;
import static fu.he.training.fall2026.model.ui.UiModels.Row;
import static fu.he.training.fall2026.model.ui.UiModels.Screen;

import fu.he.training.fall2026.entity.auth.AuditEvent;
import fu.he.training.fall2026.entity.auth.Permission;
import fu.he.training.fall2026.entity.auth.Role;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.auth.UserToken;
import fu.he.training.fall2026.entity.enums.ApprovalStatus;
import fu.he.training.fall2026.entity.enums.RoleCode;
import fu.he.training.fall2026.model.ui.UiModels;
import fu.he.training.fall2026.model.ui.AppRoles;
import fu.he.training.fall2026.repository.ApprovalRepository;
import fu.he.training.fall2026.repository.AuditEventRepository;
import fu.he.training.fall2026.repository.RoleRepository;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.repository.UserTokenRepository;
import fu.he.training.fall2026.service.AdminEmployeeService;
import fu.he.training.fall2026.service.AdminPermissionService;
import fu.he.training.fall2026.service.impl.AdminPermissionServiceImpl;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

/**
 * Builds admin/manager app screens from SQL Server data into existing Thymeleaf UI models.
 */
@Service
@RequiredArgsConstructor
public class AdminUiModelService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private static final List<String> ROLE_OPTIONS = List.of(
            "Quản trị viên", "Quản lý", "Nhân viên ký gửi", "Kiểm định viên",
            "Nhân viên kinh doanh", "Nhân viên tài chính", "Nhân viên bàn giao", "Nhân viên hồ sơ");

    private final AdminEmployeeService employeeService;
    private final AdminPermissionService permissionService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserTokenRepository userTokenRepository;
    private final AuditEventRepository auditEventRepository;
    private final ApprovalRepository approvalRepository;

    @Transactional(readOnly = true)
    public String renderAdminPage(String pageKey, Model model, UiModels.Role sessionRole, String notice) {
        Screen screen = switch (normalize(pageKey)) {
            case "dashboard", "dashboard-admin" -> dashboardScreen();
            case "employees" -> employeesListScreen(notice);
            case "employees/new" -> employeesCreateScreen(notice);
            case "permissions" -> permissionsScreen(notice);
            case "sessions" -> sessionsScreen(notice);
            case "audit" -> auditScreen();
            case "reports" -> reportsScreen();
            default -> {
                String key = normalize(pageKey);
                if (key.matches("employees/\\d+/edit")) {
                    Long id = Long.parseLong(key.split("/")[1]);
                    yield employeesEditScreen(id, notice);
                }
                if (key.matches("employees/\\d+")) {
                    Long id = Long.parseLong(key.substring("employees/".length()));
                    yield employeeDetailScreen(id, notice);
                }
                throw new IllegalArgumentException("Unknown admin page: " + pageKey);
            }
        };

        String normalized = normalize(pageKey);
        String template = switch (normalized) {
            case "dashboard", "dashboard-admin" -> "admin/dashboard";
            case "employees" -> "admin/employees/list";
            case "employees/new" -> "admin/employees/create";
            case "permissions" -> "admin/permissions/list";
            case "sessions" -> "admin/sessions/list";
            case "audit" -> "admin/audit/list";
            case "reports" -> "admin/reports/list";
            default -> normalized.matches("employees/\\d+/edit")
                    ? "admin/employees/edit"
                    : "admin/employees/detail";
        };

        return putAppModel(model, "admin", pageKey, screen, sessionRole, template, serverFormFor(pageKey));
    }

    @Transactional(readOnly = true)
    public String renderManagerPage(String pageKey, Model model, UiModels.Role sessionRole, String notice) {
        Screen screen = switch (normalize(pageKey)) {
            case "dashboard", "dashboard-manager" -> managerDashboardScreen();
            case "approvals" -> approvalsListScreen();
            case "audit" -> managerAuditScreen();
            case "reports" -> managerReportsScreen();
            default -> {
                if (pageKey.startsWith("approvals/")) {
                    Long id = Long.parseLong(pageKey.substring("approvals/".length()));
                    yield approvalDetailScreen(id, notice);
                }
                throw new IllegalArgumentException("Unknown manager page: " + pageKey);
            }
        };

        String template = switch (normalize(pageKey)) {
            case "dashboard", "dashboard-manager" -> "manager/dashboard";
            case "approvals" -> "manager/approvals/list";
            case "audit" -> "manager/audit/list";
            case "reports" -> "manager/reports/list";
            default -> "manager/approvals/detail";
        };

        String serverForm = pageKey.startsWith("approvals/") ? "/manager/approvals/" + pageKey.substring("approvals/".length()) + "/decide" : null;
        return putAppModel(model, "manager", pageKey, screen, sessionRole, template, serverForm);
    }

    private String putAppModel(
            Model model,
            String roleKey,
            String pageKey,
            Screen screen,
            UiModels.Role sessionRole,
            String template,
            String serverFormAction) {
        String route = appRoute(roleKey, pageKey.startsWith("dashboard") ? "dashboard-" + roleKey : pageKey);
        List<Link> navigation = appNavigation(roleKey);
        model.addAttribute("screen", screen);
        model.addAttribute("role", sessionRole != null ? sessionRole : AppRoles.of(roleKey));
        model.addAttribute("navigation", navigation);
        model.addAttribute("activeNav", resolveActiveNav(route, navigation));
        model.addAttribute("roles", AppRoles.all());
        model.addAttribute("route", route);
        model.addAttribute("appShell", true);
        model.addAttribute("roleHome", "admin".equals(roleKey) ? "/admin/employees" : "/" + roleKey + "/dashboard");
        model.addAttribute("serverFormAction", serverFormAction);
        if ("permissions".equals(normalize(pageKey))) {
            model.addAttribute("permissionGrants", permissionService.currentGrantKeys());
            model.addAttribute("permissionRows", permissionService.listPermissions());
            model.addAttribute("permissionRoles", permissionService.listRoles());
        }
        return template;
    }

    private String serverFormFor(String pageKey) {
        String key = normalize(pageKey);
        if ("employees/new".equals(key)) {
            return "/admin/employees";
        }
        if ("permissions".equals(key)) {
            return "/admin/permissions";
        }
        if (key.matches("employees/\\d+/edit")) {
            return "/admin/employees/" + key.split("/")[1];
        }
        return null;
    }

    private Screen dashboardScreen() {
        List<User> users = userRepository.findAllWithRole();
        List<Row> rows = users.stream().limit(8)
                .map(u -> new Row(u.getId(),
                        List.of(u.getFullName(), u.getEmail(), u.getRole().getName()),
                        u.isActive() ? "ACTIVE" : "LOCKED",
                        "/admin/employees/" + u.getId()))
                .toList();
        long activeSessions = userTokenRepository.findAll().stream()
                .filter(t -> t.getRevokedAt() == null && t.getExpiresAt().isAfter(java.time.Instant.now()))
                .count();
        return new Screen("dashboard-admin", "Tổng quan", "Quản trị",
                "Bảng điều khiển nhanh: số liệu hệ thống và lối tắt sang các màn quản trị.",
                "dashboard", "", "", List.of(),
                List.of("Nhân viên", "Email", "Vai trò"), rows, List.of(), List.of(),
                List.of(new Action("Mở danh sách nhân sự", "/admin/employees", "link", true, "")),
                List.of(),
                List.of(
                        new Metric("Nhân viên", String.valueOf(users.size()), "Tài khoản trong hệ thống"),
                        new Metric("Vai trò", String.valueOf(RoleCode.values().length), "RoleCode MVP"),
                        new Metric("Phiên truy cập", String.valueOf(activeSessions), "Session còn hiệu lực")),
                List.of());
    }

    private Screen employeesListScreen(String notice) {
        List<User> users = employeeService.listEmployees();
        List<Row> rows = users.stream()
                .map(u -> new Row(u.getId(),
                        List.of(u.getFullName(), u.getEmail(), u.getRole().getName()),
                        u.isActive() ? "ACTIVE" : "LOCKED",
                        "/admin/employees/" + u.getId()))
                .toList();
        long activeSessions = userTokenRepository.findAll().stream()
                .filter(t -> t.getRevokedAt() == null && t.getExpiresAt().isAfter(java.time.Instant.now()))
                .count();
        return new Screen("employees", "Nhân sự & tài khoản", "Quản trị",
                "Tạo, khóa/mở khóa và gán vai trò chính cho từng tài khoản đăng nhập.",
                "list", "", notice == null ? "" : notice, List.of(),
                List.of("Nhân viên", "Email", "Vai trò"), rows, List.of(), List.of(),
                List.of(new Action("Thêm nhân viên", "/admin/employees/new", "link", true, "")),
                List.of(),
                List.of(
                        new Metric("Nhân viên", String.valueOf(users.size()), "Tài khoản trong hệ thống"),
                        new Metric("Vai trò", String.valueOf(RoleCode.values().length), "RoleCode MVP"),
                        new Metric("Phiên truy cập", String.valueOf(activeSessions), "Session còn hiệu lực")),
                List.of());
    }

    private Screen employeesCreateScreen(String notice) {
        return new Screen("employees/new", "Tạo tài khoản nhân viên", "Quản trị",
                "Gán một vai trò chính trong MVP; đăng nhập bằng email OTP.",
                "form", "", notice == null ? "" : notice,
                List.of(
                        new Field("fullName", "Họ tên", "text", true, "", List.of(), ""),
                        new Field("email", "Email công việc", "email", true, "", List.of(), ""),
                        new Field("phone", "Số điện thoại", "tel", true, "", List.of(), ""),
                        new Field("role", "Vai trò chính", "select", true, "", ROLE_OPTIONS, "")),
                List.of(), List.of(), List.of(), List.of(),
                List.of(new Action("Về danh sách", "/admin/employees", "link", true, "")),
                List.of(), List.of(), List.of());
    }

    private Screen employeesEditScreen(Long id, String notice) {
        User u = employeeService.getEmployee(id);
        return new Screen("employees/" + id + "/edit", "Sửa " + u.getFullName(), "Quản trị",
                "Cập nhật thông tin và vai trò chính của tài khoản.",
                "form", u.isActive() ? "ACTIVE" : "LOCKED", notice == null ? "" : notice,
                List.of(
                        new Field("fullName", "Họ tên", "text", true, u.getFullName(), List.of(), ""),
                        new Field("email", "Email công việc", "email", true, u.getEmail(), List.of(), ""),
                        new Field("phone", "Số điện thoại", "tel", true,
                                u.getPhone() == null ? "" : u.getPhone(), List.of(), ""),
                        new Field("role", "Vai trò chính", "select", true, u.getRole().getName(), ROLE_OPTIONS, "")),
                List.of(), List.of(), List.of(), List.of(),
                List.of(
                        new Action("Về hồ sơ", "/admin/employees/" + id, "link", true, ""),
                        new Action("Danh sách", "/admin/employees", "link", true, "")),
                List.of(), List.of(), List.of());
    }

    private Screen employeeDetailScreen(Long id, String notice) {
        User u = employeeService.getEmployee(id);
        long sessions = userTokenRepository.findByUserIdAndRevokedAtIsNull(id).size();
        List<Fact> facts = List.of(
                new Fact("Họ tên", u.getFullName()),
                new Fact("Email", u.getEmail()),
                new Fact("Vai trò", u.getRole().getName()),
                new Fact("Xác minh email", u.isEmailVerified() ? "Đã xác minh" : "Chưa xác minh"),
                new Fact("Trạng thái", u.isActive() ? "ACTIVE" : "LOCKED"),
                new Fact("Phiên đang hoạt động", sessions + " phiên"));
        List<Action> actions = new ArrayList<>();
        actions.add(new Action("Sửa hồ sơ", "/admin/employees/" + id + "/edit", "link", true, ""));
        actions.add(new Action("Xem phiên đăng nhập", "/admin/sessions", "link", true, ""));
        actions.add(new Action(u.isActive() ? "Khóa tài khoản" : "Mở khóa tài khoản",
                "/admin/employees/" + id + "/toggle-active", "link", true, ""));
        return new Screen("employees/" + id, "Hồ sơ " + u.getFullName(), "Quản trị",
                "Xem vai trò, trạng thái xác minh và phiên truy cập của nhân viên.",
                "detail", u.isActive() ? "ACTIVE" : "LOCKED", notice == null ? "" : notice,
                List.of(), List.of(), List.of(), facts, List.of(), actions, List.of(), List.of(), List.of());
    }

    private Screen permissionsScreen(String notice) {
        // Dedicated template section via model attrs; save happens inside the matrix form.
        return new Screen("permissions", "Vai trò & quyền truy cập", "Quản trị",
                "Gán quyền nghiệp vụ theo từng vai trò. Lưu sẽ ghi RolePermission vào SQL Server.",
                "permissions", "", notice == null ? "" : notice,
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(),
                List.of(), List.of(), List.of());
    }

    private Screen sessionsScreen(String notice) {
        List<Row> rows = userTokenRepository.findAllWithUserOrderByCreatedAtDesc().stream()
                .map(t -> {
                    String device = abbreviate(t.getUserAgent(), 40);
                    String exp = FMT.format(t.getExpiresAt());
                    String status = t.getRevokedAt() != null ? "REVOKED"
                            : (t.getExpiresAt().isBefore(java.time.Instant.now()) ? "EXPIRED" : "ACTIVE");
                    return new Row(t.getId(),
                            List.of(t.getUser().getFullName(), device == null ? "—" : device, exp),
                            status,
                            "/admin/employees/" + t.getUser().getId());
                })
                .toList();
        return new Screen("sessions", "Phiên đăng nhập & xác minh", "Quản trị",
                "Theo dõi vòng đời phiên đăng nhập; không hiển thị mật khẩu.",
                "list", "", notice == null ? "" : notice, List.of(),
                List.of("Nhân viên", "Thiết bị", "Hết hạn"), rows, List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());
    }

    private Screen auditScreen() {
        List<Row> rows = auditEventRepository.findAllWithActorOrderByOccurredAtDesc().stream()
                .limit(50)
                .map(e -> new Row(e.getId(),
                        List.of(FMT.format(e.getOccurredAt()),
                                e.getActor() != null ? e.getActor().getFullName() : "Hệ thống",
                                e.getAction()),
                        "RECORDED",
                        e.getEntityType() != null && "User".equals(e.getEntityType()) && e.getEntityId() != null
                                ? "/admin/employees/" + e.getEntityId()
                                : "/admin/audit"))
                .toList();
        return new Screen("audit", "Nhật ký hoạt động", "Quản trị",
                "Lịch sử thao tác quản trị: tạo nhân viên, khóa tài khoản, quyền, phê duyệt.",
                "list", "", "", List.of(),
                List.of("Thời gian", "Người thực hiện", "Thao tác"), rows, List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());
    }

    private Screen reportsScreen() {
        long users = userRepository.count();
        long active = userRepository.countByActiveTrue();
        long pending = approvalRepository.countByStatus(ApprovalStatus.PENDING);
        long audits = auditEventRepository.count();
        return new Screen("reports", "Báo cáo vận hành", "Điều hành",
                "Chỉ số quản trị hiện tại từ SQL Server (MVP).",
                "report", "", "", List.of(),
                List.of(), List.of(), List.of(), List.of(),
                List.of(new Action("Xuất CSV mẫu", "/admin/employees", "link", true, "")),
                List.of(),
                List.of(
                        new Metric("Tài khoản", String.valueOf(users), "Tổng user"),
                        new Metric("Đang hoạt động", String.valueOf(active), "active=true"),
                        new Metric("Chờ duyệt", String.valueOf(pending), "Approval PENDING"),
                        new Metric("Sự kiện audit", String.valueOf(audits), "AuditEvent")),
                List.of());
    }

    private Screen managerDashboardScreen() {
        long pending = approvalRepository.countByStatus(ApprovalStatus.PENDING);
        List<Row> rows = approvalRepository.findAllFetched().stream()
                .filter(a -> a.getStatus() == ApprovalStatus.PENDING)
                .limit(10)
                .map(a -> new Row(a.getId(),
                        List.of(a.getCode(), a.getType().name(), a.getRequestedBy().getFullName()),
                        a.getStatus().name(),
                        "/manager/approvals/" + a.getId()))
                .toList();
        return new Screen("dashboard-manager", "Tổng quan", "Điều hành",
                "Hàng đợi phê duyệt và công việc điều hành hôm nay.",
                "dashboard", "", "", List.of(),
                List.of("Mã", "Loại", "Người lập"), rows, List.of(), List.of(),
                List.of(new Action("Mở hàng đợi", "/manager/approvals", "link", true, "")),
                List.of(),
                List.of(new Metric("Chờ duyệt", String.valueOf(pending), "PENDING")),
                List.of());
    }

    private Screen approvalsListScreen() {
        List<Row> rows = approvalRepository.findAllFetched().stream()
                .map(a -> new Row(a.getId(),
                        List.of(a.getCode(), a.getType().name(), a.getRequestedBy().getFullName()),
                        a.getStatus().name(),
                        "/manager/approvals/" + a.getId()))
                .toList();
        return new Screen("approvals", "Hàng đợi phê duyệt", "Điều hành",
                "Kiểm tra bằng chứng, blocker và lý do trước mỗi quyết định.",
                "list", "", "", List.of(),
                List.of("Mã", "Loại", "Người lập"), rows, List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());
    }

    private Screen approvalDetailScreen(Long id, String notice) {
        var a = approvalRepository.findByIdFetched(id).orElseThrow();
        List<Fact> facts = List.of(
                new Fact("Mã", a.getCode()),
                new Fact("Loại", a.getType().name()),
                new Fact("Trạng thái", a.getStatus().name()),
                new Fact("Đối tượng", a.getTargetType() + " #" + a.getTargetId()),
                new Fact("Người lập", a.getRequestedBy().getFullName()),
                new Fact("Lý do hiện tại", a.getReason() != null ? a.getReason() : "—"));
        boolean canDecide = a.getStatus() == ApprovalStatus.PENDING
                || a.getStatus() == ApprovalStatus.NEED_MORE_INFO;
        List<Field> fields = canDecide ? List.of(
                new Field("decision", "Quyết định", "select", true, "",
                        List.of("APPROVED", "REJECTED", "NEED_MORE_INFO"),
                        "APPROVED=Phê duyệt · REJECTED=Từ chối · NEED_MORE_INFO=Yêu cầu bổ sung"),
                new Field("reason", "Lý do quyết định", "textarea", true, "", List.of(), ""))
                : List.of();
        return new Screen("approvals/" + id, "Đánh giá yêu cầu " + a.getCode(), "Điều hành",
                "Quyết định cần lý do; blocker phải được giải quyết trước khi duyệt nghiệp vụ liên quan.",
                canDecide ? "decision" : "detail", a.getStatus().name(), notice == null ? "" : notice,
                fields, List.of(), List.of(), facts, List.of(),
                List.of(new Action("Về hàng đợi", "/manager/approvals", "link", true, "")),
                List.of(), List.of(), List.of());
    }

    private Screen managerAuditScreen() {
        return auditScreen();
    }

    private Screen managerReportsScreen() {
        return reportsScreen();
    }

    private List<Link> appNavigation(String roleKey) {
        if ("admin".equals(roleKey)) {
            return List.of(
                    new Link("Nhân sự & tài khoản", "/admin/employees", "Quản trị"),
                    new Link("Vai trò & quyền truy cập", "/admin/permissions", "Quản trị"),
                    new Link("Phiên đăng nhập & xác minh", "/admin/sessions", "Quản trị"),
                    new Link("Nhật ký hoạt động", "/admin/audit", "Quản trị"),
                    new Link("Báo cáo vận hành", "/admin/reports", "Điều hành"));
        }
        return List.of(
                new Link("Tổng quan", "/manager/dashboard", "Điều hành"),
                new Link("Hàng đợi phê duyệt", "/manager/approvals", "Điều hành"),
                new Link("Duyệt quyết toán", "/manager/settlements", "Điều hành"),
                new Link("Nhật ký hoạt động", "/manager/audit", "Quản trị"),
                new Link("Báo cáo vận hành", "/manager/reports", "Điều hành"));
    }

    private static String appRoute(String roleKey, String pageKey) {
        if (("dashboard-" + roleKey).equals(pageKey) || "dashboard".equals(pageKey)) {
            return "/" + roleKey + "/dashboard";
        }
        if ("sign-in".equals(pageKey)) {
            return "/";
        }
        return "/" + roleKey + "/" + pageKey;
    }

    private String resolveActiveNav(String currentRoute, List<Link> navigation) {
        return navigation.stream()
                .filter(link -> currentRoute.equals(link.url()) || currentRoute.startsWith(link.url() + "/"))
                .max(Comparator.comparingInt(link -> link.url().length()))
                .map(Link::url)
                .orElse("");
    }

    private static String normalize(String pageKey) {
        if (pageKey == null) {
            return "";
        }
        String key = pageKey;
        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        return key;
    }

    private static String abbreviate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
