package fu.he.training.fall2026.controller.web.admin;

import fu.he.training.fall2026.controller.preview.UiPageRenderer;
import fu.he.training.fall2026.model.ui.UiModels.Role;
import fu.he.training.fall2026.model.ui.UiPreviewCatalog;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Authenticated Admin UI for Nguyễn Việt Hải — reuses preview templates under templates/admin/**
 * (Screen Tracking: dashboard, employees, permissions, sessions, audit, reports).
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminWebController {

    private static final String ROLE = "admin";

    private final UiPageRenderer uiPageRenderer;

    @GetMapping({"", "/"})
    public String root() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        return uiPageRenderer.renderApp(ROLE, "dashboard-admin", model, sessionRole(session));
    }

    /**
     * Catch-all for Hải admin pages: employees, employees/new, employees/101, permissions, …
     */
    @GetMapping("/{*page}")
    public String page(@PathVariable("page") String page, HttpSession session, Model model) {
        String key = normalizePage(page);
        if (key.isBlank() || "dashboard".equals(key) || "dashboard-admin".equals(key)) {
            return "redirect:/admin/dashboard";
        }
        if ("sign-in".equals(key)) {
            return "redirect:/";
        }
        if (UiPreviewCatalog.find(ROLE, key) == null) {
            return "redirect:/admin/dashboard";
        }
        return uiPageRenderer.renderApp(ROLE, key, model, sessionRole(session));
    }

    private static String normalizePage(String page) {
        if (page == null) {
            return "";
        }
        String key = page.trim();
        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        while (key.endsWith("/") && key.length() > 1) {
            key = key.substring(0, key.length() - 1);
        }
        return key;
    }

    private static Role sessionRole(HttpSession session) {
        Role catalog = UiPreviewCatalog.role(ROLE);
        String fullName = SessionAuthUtils.getFullName(session);
        if (fullName == null || fullName.isBlank()) {
            return catalog;
        }
        String initials = initialsOf(fullName);
        return new Role(catalog.key(), catalog.label(), fullName, initials);
    }

    private static String initialsOf(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        String a = parts[0].substring(0, 1);
        String b = parts[parts.length - 1].substring(0, 1);
        return (a + b).toUpperCase();
    }
}
