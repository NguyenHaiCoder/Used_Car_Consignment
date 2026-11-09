package fu.he.training.fall2026.config;

import fu.he.training.fall2026.entity.auth.Approval;
import fu.he.training.fall2026.entity.auth.Role;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.enums.ApprovalStatus;
import fu.he.training.fall2026.entity.enums.ApprovalType;
import fu.he.training.fall2026.entity.enums.RoleCode;
import fu.he.training.fall2026.repository.ApprovalRepository;
import fu.he.training.fall2026.repository.RoleRepository;
import fu.he.training.fall2026.repository.UserRepository;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Seeds roles, tài khoản nhóm, và vài approval cho Manager. Quyền: xem PermissionCatalogSync. */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class AuthDataLoader implements ApplicationRunner {

    private static final String OTP_PASSWORD_PLACEHOLDER = "{OTP_LOGIN}";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ApprovalRepository approvalRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<RoleCode, Role> roles = ensureRoles();
        // Nguyễn Việt Hải: danh mục quyền và grant mặc định do PermissionCatalogSync nạp.

        ensureUser("admin@gmail.com", "Nguyễn Việt Hải", "0901000001", roles.get(RoleCode.ADMIN));
        User manager = ensureUser("hainv@gmail.com", "Nguyễn Việt Hải", "0901000002", roles.get(RoleCode.MANAGER));
        ensureUser("quyenhl@gmail.com", "Hà Lệ Quyên", "0901000003", roles.get(RoleCode.CONSIGNMENT));
        ensureUser("annvt@gmail.com", "Nguyễn Vũ Trường An", "0901000004", roles.get(RoleCode.INSPECTOR));
        ensureUser("lanh@gmail.com", "Đỗ Lan Anh", "0901000005", roles.get(RoleCode.SALES));
        ensureUser("cuongvm@gmail.com", "Vũ Mạnh Cường", "0901000006", roles.get(RoleCode.FINANCE));
        ensureUser("cuongvmhandover@gmail.com", "Vũ Mạnh Cường", "0901000007", roles.get(RoleCode.HANDOVER));
        ensureUser("khangnp@gmail.com", "Nguyễn Phúc Khang", "0901000008", roles.get(RoleCode.DOCUMENTS));

        ensureSampleApprovals(manager);
        log.info("[AUTH] Tài khoản nhóm đã sẵn sàng. Admin: admin@gmail.com");
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
