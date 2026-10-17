package fu.he.training.fall2026.config;

import fu.he.training.fall2026.entity.auth.Role;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.enums.RoleCode;
import fu.he.training.fall2026.repository.RoleRepository;
import fu.he.training.fall2026.repository.UserRepository;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Seeds roles + demo users (Hải = ADMIN). OTP login — passwordHash is placeholder. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthDataLoader implements ApplicationRunner {

    private static final String OTP_PASSWORD_PLACEHOLDER = "{OTP_LOGIN}";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<RoleCode, Role> roles = ensureRoles();
        ensureUser("hai@garagepro.example", "Nguyễn Việt Hải", "0901000001", roles.get(RoleCode.ADMIN));
        ensureUser("manager@garagepro.example", "Demo Manager", "0901000002", roles.get(RoleCode.MANAGER));
        ensureUser("consign@garagepro.example", "Demo Consignment", "0901000003", roles.get(RoleCode.CONSIGNMENT));
        ensureUser("inspect@garagepro.example", "Demo Inspector", "0901000004", roles.get(RoleCode.INSPECTOR));
        ensureUser("sales@garagepro.example", "Demo Sales", "0901000005", roles.get(RoleCode.SALES));
        ensureUser("finance@garagepro.example", "Demo Finance", "0901000006", roles.get(RoleCode.FINANCE));
        ensureUser("handover@garagepro.example", "Demo Handover", "0901000007", roles.get(RoleCode.HANDOVER));
        ensureUser("docs@garagepro.example", "Demo Documents", "0901000008", roles.get(RoleCode.DOCUMENTS));
        log.info("[AUTH] Seed ready — Hải ADMIN: hai@garagepro.example (OTP master 123456)");
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

    private void ensureUser(String email, String fullName, String phone, Role role) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setPasswordHash(OTP_PASSWORD_PLACEHOLDER);
        user.setEmailVerified(false);
        user.setActive(true);
        user.setRole(role);
        userRepository.save(user);
    }

    private static String displayName(RoleCode code) {
        return switch (code) {
            case ADMIN -> "Quản trị viên";
            case MANAGER -> "Quản lý";
            case CONSIGNMENT -> "Nhân viên ký gửi";
            case INSPECTOR -> "Kiểm định viên";
            case SALES -> "Nhân viên bán hàng";
            case FINANCE -> "Tài chính";
            case HANDOVER -> "Bàn giao";
            case DOCUMENTS -> "Hồ sơ giấy tờ";
        };
    }
}
