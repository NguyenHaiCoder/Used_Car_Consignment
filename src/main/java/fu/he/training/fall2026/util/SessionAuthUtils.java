package fu.he.training.fall2026.util;

import fu.he.training.fall2026.entity.auth.Role;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.enums.RoleCode;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reusable helpers to bind / clear authenticated user data on {@link HttpSession}
 * and sync Spring Security context for role-based routing.
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
        session.setAttribute(USER_ID, user.getId());
        session.setAttribute(EMAIL, user.getEmail());
        session.setAttribute(FULL_NAME, user.getFullName());
        session.setAttribute(ROLE_CODE, roleCode.name());
        session.setAttribute(ROLE_NAME, role.getName());
        applySecurityContext(user.getEmail(), roleCode);
    }

    public static void clearLogin(HttpSession session) {
        session.removeAttribute(USER_ID);
        session.removeAttribute(EMAIL);
        session.removeAttribute(FULL_NAME);
        session.removeAttribute(ROLE_CODE);
        session.removeAttribute(ROLE_NAME);
        SecurityContextHolder.clearContext();
        session.invalidate();
    }

    public static boolean isLoggedIn(HttpSession session) {
        return session != null && session.getAttribute(USER_ID) != null;
    }

    public static Long getUserId(HttpSession session) {
        Object value = session.getAttribute(USER_ID);
        return value instanceof Long id ? id : null;
    }

    public static String getEmail(HttpSession session) {
        Object value = session.getAttribute(EMAIL);
        return value != null ? value.toString() : null;
    }

    public static String getFullName(HttpSession session) {
        Object value = session.getAttribute(FULL_NAME);
        return value != null ? value.toString() : null;
    }

    public static RoleCode getRoleCode(HttpSession session) {
        Object value = session.getAttribute(ROLE_CODE);
        if (value == null) {
            return null;
        }
        return RoleCode.valueOf(value.toString());
    }

    public static String getRoleName(HttpSession session) {
        Object value = session.getAttribute(ROLE_NAME);
        return value != null ? value.toString() : null;
    }

    /**
     * Home URL after successful login — one landing page per primary role (Hải = ADMIN).
     */
    public static String homePathForRole(RoleCode roleCode) {
        if (roleCode == null) {
            return "/";
        }
        return switch (roleCode) {
            case ADMIN -> "/admin/dashboard";
            case MANAGER -> "/manager/dashboard";
            case CONSIGNMENT -> "/consignment/dashboard";
            case INSPECTOR -> "/inspector/dashboard";
            case SALES -> "/sales/dashboard";
            case FINANCE -> "/finance/dashboard";
            case HANDOVER -> "/handover/dashboard";
            case DOCUMENTS -> "/documents/dashboard";
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

    /** Restore SecurityContext from an existing HTTP session (filter / interceptor). */
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
