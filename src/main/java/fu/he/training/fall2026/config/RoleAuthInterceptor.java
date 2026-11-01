package fu.he.training.fall2026.config;

import fu.he.training.fall2026.dto.auth.SessionUser;
import fu.he.training.fall2026.entity.enums.RoleCode;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Chặn nhầm role theo prefix URL — cùng ý tưởng {@code AuthInterceptor} của FUCarRentingSystem.
 * <ul>
 *   <li>Chưa login → redirect {@code /?loginRequired=1}</li>
 *   <li>Sai role (vd. ADMIN vào /manager) → redirect về home của role đó + {@code accessDenied=1}</li>
 * </ul>
 */
@Component
public class RoleAuthInterceptor implements HandlerInterceptor {

    /** Longer prefixes first so /documents matches before accidental overlaps. */
    private static final Map<String, RoleCode> PREFIX_ROLES = new LinkedHashMap<>();

    static {
        PREFIX_ROLES.put("/admin", RoleCode.ADMIN);
        PREFIX_ROLES.put("/manager", RoleCode.MANAGER);
        PREFIX_ROLES.put("/consignment", RoleCode.CONSIGNMENT);
        PREFIX_ROLES.put("/inspector", RoleCode.INSPECTOR);
        PREFIX_ROLES.put("/sales", RoleCode.SALES);
        PREFIX_ROLES.put("/finance", RoleCode.FINANCE);
        PREFIX_ROLES.put("/handover", RoleCode.HANDOVER);
        PREFIX_ROLES.put("/documents", RoleCode.DOCUMENTS);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        RoleCode required = requiredRoleFor(path);
        if (required == null) {
            return true;
        }

        HttpSession session = request.getSession(false);
        SessionUser loginUser = session == null
                ? null
                : (SessionUser) session.getAttribute(SessionConstants.LOGIN_USER);

        // Fallback nếu chỉ còn attribute cũ
        if (loginUser == null && SessionAuthUtils.isLoggedIn(session)) {
            loginUser = SessionUser.builder()
                    .userId(SessionAuthUtils.getUserId(session))
                    .email(SessionAuthUtils.getEmail(session))
                    .fullName(SessionAuthUtils.getFullName(session))
                    .roleCode(SessionAuthUtils.getRoleCode(session))
                    .roleName(SessionAuthUtils.getRoleName(session))
                    .build();
        }

        if (loginUser == null || loginUser.getRoleCode() == null) {
            response.sendRedirect(request.getContextPath() + "/?loginRequired=1");
            return false;
        }

        if (loginUser.getRoleCode() != required) {
            String home = SessionAuthUtils.homePathForRole(loginUser.getRoleCode());
            response.sendRedirect(request.getContextPath() + home + "?accessDenied=1");
            return false;
        }
        return true;
    }

    static RoleCode requiredRoleFor(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        for (Map.Entry<String, RoleCode> entry : PREFIX_ROLES.entrySet()) {
            String prefix = entry.getKey();
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return entry.getValue();
            }
        }
        return null;
    }
}
