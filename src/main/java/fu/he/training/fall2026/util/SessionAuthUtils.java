package fu.he.training.fall2026.util;

import fu.he.training.fall2026.config.SessionConstants;
import fu.he.training.fall2026.dto.auth.SessionUser;
import fu.he.training.fall2026.entity.auth.Role;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.enums.RoleCode;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Gắn / xóa session login + đồng bộ SecurityContext.
 * Object chính trong session: {@link SessionConstants#LOGIN_USER} ({@link SessionUser}).
 */
public final class SessionAuthUtils {

    public static final String USER_ID = "AUTH_USER_ID";
    public static final String EMAIL = "AUTH_EMAIL";
    public static final String FULL_NAME = "AUTH_FULL_NAME";
    public static final String ROLE_CODE = "AUTH_ROLE_CODE";
    public static final String ROLE_NAME = "AUTH_ROLE_NAME";

    private SessionAuthUtils() {
    }

    public static void bindLogin(HttpSession session, User user) {
        Role role = user.getRole();
        RoleCode roleCode = role.getCode();

        SessionUser sessionUser = SessionUser.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roleCode(roleCode)
                .roleName(role.getName())
                .build();
        session.setAttribute(SessionConstants.LOGIN_USER, sessionUser);

        session.setAttribute(USER_ID, user.getId());
        session.setAttribute(EMAIL, user.getEmail());
        session.setAttribute(FULL_NAME, user.getFullName());
        session.setAttribute(ROLE_CODE, roleCode.name());
        session.setAttribute(ROLE_NAME, role.getName());
        applySecurityContext(user.getEmail(), roleCode);
    }

    public static void clearLogin(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(SessionConstants.LOGIN_USER);
        session.removeAttribute(USER_ID);
        session.removeAttribute(EMAIL);
        session.removeAttribute(FULL_NAME);
        session.removeAttribute(ROLE_CODE);
        session.removeAttribute(ROLE_NAME);
        SecurityContextHolder.clearContext();
        session.invalidate();
    }

    public static SessionUser getLoginUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SessionConstants.LOGIN_USER);
        return value instanceof SessionUser user ? user : null;
    }

    public static boolean isLoggedIn(HttpSession session) {
        return getLoginUser(session) != null || (session != null && session.getAttribute(USER_ID) != null);
    }

    public static Long getUserId(HttpSession session) {
        SessionUser user = getLoginUser(session);
        if (user != null) {
            return user.getUserId();
        }
        Object value = session != null ? session.getAttribute(USER_ID) : null;
        return value instanceof Long id ? id : null;
    }

    public static String getEmail(HttpSession session) {
        SessionUser user = getLoginUser(session);
        if (user != null) {
            return user.getEmail();
        }
        Object value = session != null ? session.getAttribute(EMAIL) : null;
        return value != null ? value.toString() : null;
    }

    public static String getFullName(HttpSession session) {
        SessionUser user = getLoginUser(session);
        if (user != null) {
            return user.getFullName();
        }
        Object value = session != null ? session.getAttribute(FULL_NAME) : null;
        return value != null ? value.toString() : null;
    }

    public static RoleCode getRoleCode(HttpSession session) {
        SessionUser user = getLoginUser(session);
        if (user != null) {
            return user.getRoleCode();
        }
        Object value = session != null ? session.getAttribute(ROLE_CODE) : null;
        if (value == null) {
            return null;
        }
        return RoleCode.valueOf(value.toString());
    }

    public static String getRoleName(HttpSession session) {
        SessionUser user = getLoginUser(session);
        if (user != null) {
            return user.getRoleName();
        }
        Object value = session != null ? session.getAttribute(ROLE_NAME) : null;
        return value != null ? value.toString() : null;
    }

    public static String homePathForRole(RoleCode roleCode) {
        if (roleCode == null) {
            return "/";
        }
        return switch (roleCode) {
            case ADMIN -> "/admin/employees";
            case MANAGER -> "/manager/dashboard";
            case CONSIGNMENT -> "/consignments";
            case INSPECTOR -> "/inspector/vehicles";
            case SALES -> "/sales/inquiries";
            case FINANCE -> "/finance/payments";
            case HANDOVER -> "/handover/handovers";
            case DOCUMENTS -> "/documents/transfers";
        };
    }

    public static String authorityOf(RoleCode roleCode) {
        return "ROLE_" + roleCode.name();
    }

    public static void applySecurityContext(String email, RoleCode roleCode) {
        var auth = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority(authorityOf(roleCode))));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public static void restoreSecurityContext(HttpSession session) {
        if (!isLoggedIn(session)) {
            return;
        }
        String email = getEmail(session);
        RoleCode roleCode = getRoleCode(session);
        if (email != null && roleCode != null) {
            applySecurityContext(email, roleCode);
        }
    }
}
