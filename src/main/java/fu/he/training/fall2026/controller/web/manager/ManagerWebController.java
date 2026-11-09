package fu.he.training.fall2026.controller.web.manager;

import fu.he.training.fall2026.model.ui.UiModels.Role;
import fu.he.training.fall2026.model.ui.AppRoles;
import fu.he.training.fall2026.service.admin.AdminUiModelService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import fu.he.training.fall2026.security.Perm;
import fu.he.training.fall2026.security.RequirePermission;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Manager UI — chỉ tài khoản role MANAGER (vd. manager@garagepro.example).
 * ADMIN không vào được /manager/** (RoleAuthInterceptor + SecurityConfig).
 * Hàng đợi phê duyệt nằm ở {@link ManagerApprovalWebController}.
 */
@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerWebController {

    private final AdminUiModelService uiModelService;

    @GetMapping({"", "/"})
    public String root() {
        return "redirect:/manager/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        return uiModelService.renderManagerPage("dashboard-manager", model, sessionRole(session), null);
    }

    @RequirePermission(Perm.MANAGER_AUDIT_VIEW)
    @GetMapping("/audit")
    public String audit(HttpSession session, Model model) {
        return uiModelService.renderManagerPage("audit", model, sessionRole(session), null);
    }

    @RequirePermission(Perm.MANAGER_REPORT_VIEW)
    @GetMapping("/reports")
    public String reports(HttpSession session, Model model) {
        return uiModelService.renderManagerPage("reports", model, sessionRole(session), null);
    }

    private static Role sessionRole(HttpSession session) {
        Role catalog = AppRoles.of("manager");
        String fullName = SessionAuthUtils.getFullName(session);
        if (fullName == null || fullName.isBlank()) {
            return catalog;
        }
        String[] parts = fullName.trim().split("\\s+");
        String initials = parts.length == 1
                ? parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase()
                : (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        // Keep ADMIN label if Hải
        String label = SessionAuthUtils.getRoleName(session) != null
                ? SessionAuthUtils.getRoleName(session)
                : catalog.label();
        return new Role(catalog.key(), label, fullName, initials);
    }
}
