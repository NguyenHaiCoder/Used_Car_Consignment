package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.admin.CreateEmployeeRequest;
import fu.he.training.fall2026.dto.admin.UpdateEmployeeRequest;
import fu.he.training.fall2026.entity.auth.Role;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.auth.UserToken;
import fu.he.training.fall2026.entity.enums.RoleCode;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.repository.ApprovalRepository;
import fu.he.training.fall2026.repository.AuditEventRepository;
import fu.he.training.fall2026.repository.OtpVerificationRepository;
import fu.he.training.fall2026.repository.RoleRepository;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.repository.UserTokenRepository;
import fu.he.training.fall2026.service.AdminEmployeeService;
import fu.he.training.fall2026.service.AuditService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminEmployeeServiceImpl implements AdminEmployeeService {

    private static final String OTP_PASSWORD_PLACEHOLDER = "{OTP_LOGIN}";

    private static final Map<String, RoleCode> ROLE_LABELS = Map.ofEntries(
            Map.entry("Quản trị viên", RoleCode.ADMIN),
            Map.entry("Quản lý", RoleCode.MANAGER),
            Map.entry("Nhân viên ký gửi", RoleCode.CONSIGNMENT),
            Map.entry("Kiểm định viên", RoleCode.INSPECTOR),
            Map.entry("Nhân viên kinh doanh", RoleCode.SALES),
            Map.entry("Nhân viên tài chính", RoleCode.FINANCE),
            Map.entry("Nhân viên bàn giao", RoleCode.HANDOVER),
            Map.entry("Nhân viên hồ sơ", RoleCode.DOCUMENTS));

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;
    private final UserTokenRepository userTokenRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final AuditEventRepository auditEventRepository;
    private final ApprovalRepository approvalRepository;

    @Override
    @Transactional(readOnly = true)
    public List<User> listEmployees() {
        return userRepository.findAllWithRole();
    }

    @Override
    @Transactional(readOnly = true)
    public User getEmployee(Long id) {
        return userRepository.findByIdWithRole(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên.", HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional
    public User createEmployee(CreateEmployeeRequest request, User actor) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email đã tồn tại trong hệ thống.");
        }
        RoleCode roleCode = resolveRoleCode(request.getRole(), request.getRoleLabel());
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new BusinessException("Vai trò không hợp lệ."));

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhone(request.getPhone().trim());
        user.setPasswordHash(OTP_PASSWORD_PLACEHOLDER);
        user.setEmailVerified(false);
        user.setActive(true);
        user.setRole(role);
        userRepository.save(user);

        auditService.record(actor, "CREATE_EMPLOYEE", "User", user.getId(),
                "Tạo tài khoản " + email + " · role " + roleCode.name());
        return userRepository.findByIdWithRole(user.getId()).orElse(user);
    }

    @Override
    @Transactional
    public User updateEmployee(Long id, UpdateEmployeeRequest request, User actor) {
        User user = getEmployee(id);
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email đã tồn tại trong hệ thống.");
        }
        RoleCode roleCode = resolveRoleCode(request.getRole(), null);
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new BusinessException("Vai trò không hợp lệ."));

        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhone(request.getPhone().trim());
        user.setRole(role);
        userRepository.save(user);

        auditService.record(actor, "UPDATE_EMPLOYEE", "User", user.getId(),
                "Cập nhật hồ sơ " + email + " · role " + roleCode.name());
        return userRepository.findByIdWithRole(user.getId()).orElse(user);
    }

    @Override
    @Transactional
    public User toggleActive(Long id, User actor) {
        if (Objects.equals(actor.getId(), id)) {
            throw new BusinessException("Không thể khóa / mở khóa chính tài khoản đang đăng nhập.");
        }
        User user = getEmployee(id);
        boolean locking = user.isActive();
        user.setActive(!user.isActive());
        userRepository.save(user);

        if (locking) {
            revokeAllSessions(user.getId());
        }

        auditService.record(actor, locking ? "LOCK_EMPLOYEE" : "UNLOCK_EMPLOYEE",
                "User", user.getId(),
                (locking ? "Khóa " : "Mở khóa ") + user.getEmail());
        return user;
    }

    @Override
    @Transactional
    public void deleteEmployee(Long id, User actor) {
        if (Objects.equals(actor.getId(), id)) {
            throw new BusinessException("Không thể xóa chính tài khoản đang đăng nhập.");
        }
        User user = getEmployee(id);
        if (approvalRepository.countByRequestedBy_Id(id) > 0
                || approvalRepository.countByApprovedBy_Id(id) > 0) {
            throw new BusinessException(
                    "Không thể xóa vĩnh viễn vì còn phê duyệt liên quan. Hãy khóa tài khoản thay vì xóa.");
        }

        String email = user.getEmail();
        otpVerificationRepository.deleteByUserId(id);
        userTokenRepository.deleteByUserId(id);
        auditEventRepository.clearActorByUserId(id);
        userRepository.delete(user);

        auditService.record(actor, "DELETE_EMPLOYEE", "User", id, "Xóa vĩnh viễn tài khoản " + email);
    }

    private void revokeAllSessions(Long userId) {
        Instant now = Instant.now();
        List<UserToken> tokens = userTokenRepository.findByUserIdAndRevokedAtIsNull(userId);
        for (UserToken token : tokens) {
            token.setRevokedAt(now);
        }
        if (!tokens.isEmpty()) {
            userTokenRepository.saveAll(tokens);
        }
    }

    private RoleCode resolveRoleCode(String role, String roleLabel) {
        if (role != null && !role.isBlank()) {
            String raw = role.trim();
            try {
                return RoleCode.valueOf(raw.toUpperCase(Locale.ROOT).replace(' ', '_'));
            } catch (IllegalArgumentException ignored) {
                RoleCode fromLabel = ROLE_LABELS.get(raw);
                if (fromLabel != null) {
                    return fromLabel;
                }
            }
        }
        if (roleLabel != null) {
            RoleCode fromLabel = ROLE_LABELS.get(roleLabel.trim());
            if (fromLabel != null) {
                return fromLabel;
            }
        }
        throw new BusinessException("Vai trò chính không hợp lệ.");
    }
}
