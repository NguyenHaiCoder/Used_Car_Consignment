package fu.he.training.fall2026.config;

import fu.he.training.fall2026.entity.auth.Approval;
import fu.he.training.fall2026.entity.auth.Permission;
import fu.he.training.fall2026.entity.auth.Role;
import fu.he.training.fall2026.entity.auth.RolePermission;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.enums.ApprovalStatus;
import fu.he.training.fall2026.entity.enums.ApprovalType;
import fu.he.training.fall2026.entity.enums.RoleCode;
import fu.he.training.fall2026.repository.ApprovalRepository;
import fu.he.training.fall2026.repository.PermissionRepository;
import fu.he.training.fall2026.repository.RolePermissionRepository;
import fu.he.training.fall2026.repository.RoleRepository;
import fu.he.training.fall2026.repository.UserRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Seeds roles, permissions, demo users (Hải = ADMIN), sample approvals. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthDataLoader implements ApplicationRunner {

    private static final String OTP_PASSWORD_PLACEHOLDER = "{OTP_LOGIN}";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final ApprovalRepository approvalRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<RoleCode, Role> roles = ensureRoles();
        Map<String, Permission> permissions = ensurePermissions();
        ensureDefaultGrants(roles, permissions);

        User hai = ensureUser("hai@garagepro.example", "Nguyễn Việt Hải", "0901000001", roles.get(RoleCode.ADMIN));
        User manager = ensureUser("manager@garagepro.example", "Demo Manager", "0901000002", roles.get(RoleCode.MANAGER));
        ensureUser("consign@garagepro.example", "Demo Consignment", "0901000003", roles.get(RoleCode.CONSIGNMENT));
        ensureUser("inspect@garagepro.example", "Demo Inspector", "0901000004", roles.get(RoleCode.INSPECTOR));
        ensureUser("sales@garagepro.example", "Demo Sales", "0901000005", roles.get(RoleCode.SALES));
        ensureUser("finance@garagepro.example", "Demo Finance", "0901000006", roles.get(RoleCode.FINANCE));
        ensureUser("handover@garagepro.example", "Demo Handover", "0901000007", roles.get(RoleCode.HANDOVER));
        ensureUser("docs@garagepro.example", "Demo Documents", "0901000008", roles.get(RoleCode.DOCUMENTS));

        ensureSampleApprovals(manager != null ? manager : hai);
        log.info("[AUTH] Seed ready — Hải ADMIN: hai@garagepro.example");
    }

    private Map<RoleCode, Role> ensureRoles() {
        Map<RoleCode, Role> map = new EnumMap<>(RoleCode.class);
        for (RoleCode code : RoleCode.values()) {
            Role role = roleRepository.findByCode(code).orElseGet(() -> {
                Role created = new Role();
                created.setCode(code);
                created.setName(displayName(code));
                created.setDescription("Primary role " + code.name());
                return roleRepository.save(created);
            });
            map.put(code, role);
        }
        return map;
    }

    private Map<String, Permission> ensurePermissions() {
        List<String[]> defs = List.of(
                new String[]{"VIEW_WORK", "Xem công việc", "Xem danh sách và chi tiết module"},
                new String[]{"HANDLE_MODULE", "Xử lý module", "Tạo/sửa hồ sơ trong module được gán"},
                new String[]{"APPROVE", "Phê duyệt", "Duyệt yêu cầu / ngoại lệ"},
                new String[]{"MANAGE_PERMISSIONS", "Quản trị quyền", "Quản lý tài khoản và ma trận quyền"});
        Map<String, Permission> map = new java.util.LinkedHashMap<>();
        for (String[] def : defs) {
            Permission p = permissionRepository.findByCode(def[0]).orElseGet(() -> {
                Permission created = new Permission();
                created.setCode(def[0]);
                created.setName(def[1]);
                created.setDescription(def[2]);
                return permissionRepository.save(created);
            });
            map.put(def[0], p);
        }
        return map;
    }

    private void ensureDefaultGrants(Map<RoleCode, Role> roles, Map<String, Permission> permissions) {
        if (!rolePermissionRepository.findAll().isEmpty()) {
            return;
        }
        // ADMIN: all
        grantAll(roles.get(RoleCode.ADMIN), permissions);
        // MANAGER: view, handle, approve
        grant(roles.get(RoleCode.MANAGER), permissions, "VIEW_WORK", "HANDLE_MODULE", "APPROVE");
        // Others: view + handle
        for (RoleCode code : List.of(RoleCode.CONSIGNMENT, RoleCode.INSPECTOR, RoleCode.SALES,
                RoleCode.FINANCE, RoleCode.HANDOVER, RoleCode.DOCUMENTS)) {
            grant(roles.get(code), permissions, "VIEW_WORK", "HANDLE_MODULE");
        }
    }

    private void grantAll(Role role, Map<String, Permission> permissions) {
        permissions.values().forEach(p -> saveGrant(role, p));
    }

    private void grant(Role role, Map<String, Permission> permissions, String... codes) {
        for (String code : codes) {
            saveGrant(role, permissions.get(code));
        }
    }

    private void saveGrant(Role role, Permission permission) {
        if (role == null || permission == null) {
            return;
        }
        if (rolePermissionRepository.existsByRoleIdAndPermissionId(role.getId(), permission.getId())) {
            return;
        }
        RolePermission rp = new RolePermission();
        rp.setRole(role);
        rp.setPermission(permission);
        rolePermissionRepository.save(rp);
    }

    private User ensureUser(String email, String fullName, String phone, Role role) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setFullName(fullName);
            user.setPhone(phone);
            user.setPasswordHash(OTP_PASSWORD_PLACEHOLDER);
            user.setEmailVerified(false);
            user.setActive(true);
            user.setRole(role);
            return userRepository.save(user);
        });
    }

    private void ensureSampleApprovals(User requester) {
        if (approvalRepository.count() > 0 || requester == null) {
            return;
        }
        createApproval("PD-101", ApprovalType.LISTING_ELIGIBILITY, "Listing", 101L, requester,
                "Kiểm tra eligibility trước publish tin đăng.");
        createApproval("PD-102", ApprovalType.BELOW_FLOOR_PRICE, "Valuation", 102L, requester,
                "Giá đề xuất dưới sàn — cần Manager xem.");
        createApproval("PD-103", ApprovalType.WITHDRAWAL, "Consignment", 103L, requester,
                "Chủ xe rút ký gửi khi còn reservation.");
    }

    private void createApproval(
            String code, ApprovalType type, String targetType, Long targetId, User requester, String reason) {
        Approval a = new Approval();
        a.setCode(code);
        a.setType(type);
        a.setStatus(ApprovalStatus.PENDING);
        a.setTargetType(targetType);
        a.setTargetId(targetId);
        a.setReason(reason);
        a.setRequestedBy(requester);
        approvalRepository.save(a);
    }

    private static String displayName(RoleCode code) {
        return switch (code) {
            case ADMIN -> "Quản trị viên";
            case MANAGER -> "Quản lý";
            case CONSIGNMENT -> "Nhân viên ký gửi";
            case INSPECTOR -> "Kiểm định viên";
            case SALES -> "Nhân viên kinh doanh";
            case FINANCE -> "Nhân viên tài chính";
            case HANDOVER -> "Nhân viên bàn giao";
            case DOCUMENTS -> "Nhân viên hồ sơ";
        };
    }
}
