package fu.he.training.fall2026.controller.web;

import fu.he.training.fall2026.dto.auth.AuthSessionResponse;
import fu.he.training.fall2026.service.AuthService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Landing stubs for non-admin roles (admin uses {@link fu.he.training.fall2026.controller.web.admin.AdminWebController}).
 */
@Controller
@RequiredArgsConstructor
public class RoleDashboardController {

    private final AuthService authService;

    @GetMapping("/manager/dashboard")
    public String managerDashboard(HttpSession session, Model model) {
        return render(session, model, "manager");
    }

    @GetMapping("/inspector/dashboard")
    public String inspectorDashboard(HttpSession session, Model model) {
        return render(session, model, "inspector");
    }

    @GetMapping("/sales/dashboard")
    public String salesDashboard(HttpSession session, Model model) {
        return render(session, model, "sales");
    }

    @GetMapping("/finance/dashboard")
    public String financeDashboard(HttpSession session, Model model) {
        return render(session, model, "finance");
    }

    @GetMapping("/handover/dashboard")
    public String handoverDashboard(HttpSession session, Model model) {
        return render(session, model, "handover");
    }

    @GetMapping("/documents/dashboard")
    public String documentsDashboard(HttpSession session, Model model) {
        return render(session, model, "documents");
    }

    private String render(HttpSession session, Model model, String roleKey) {
        AuthSessionResponse auth = authService.currentSession(session);
        if (auth == null) {
            return "redirect:/?loginRequired=1";
        }
        model.addAttribute("auth", auth);
        model.addAttribute("fullName", auth.getFullName());
        model.addAttribute("email", auth.getEmail());
        model.addAttribute("roleCode", auth.getRoleCode().name());
        model.addAttribute("roleName", auth.getRoleName());
        model.addAttribute("roleKey", roleKey);
        model.addAttribute("homePath", SessionAuthUtils.homePathForRole(auth.getRoleCode()));
        return "app/role-dashboard";
    }
}
