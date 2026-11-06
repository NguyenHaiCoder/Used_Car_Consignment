package fu.he.training.fall2026.controller.web.admin;

import fu.he.training.fall2026.dto.admin.CreateEmployeeRequest;
import fu.he.training.fall2026.dto.admin.SavePermissionsRequest;
import fu.he.training.fall2026.dto.admin.UpdateEmployeeRequest;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.model.ui.UiModels.Role;
import fu.he.training.fall2026.model.ui.AppRoles;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.service.AdminEmployeeService;
import fu.he.training.fall2026.service.AdminPermissionService;
import fu.he.training.fall2026.service.admin.AdminUiModelService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Authenticated Admin UI for Nguyễn Việt Hải — data from SQL Server. */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminWebController {

    private final AdminUiModelService uiModelService;
    private final AdminEmployeeService employeeService;
    private final AdminPermissionService permissionService;
    private final UserRepository userRepository;

    @GetMapping({"", "/"})
    public String root() {
        return "redirect:/admin/employees";
    }

    /** Legacy bookmark — Admin home is Nhân sự & tài khoản. */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/admin/employees";
    }

    @GetMapping("/employees")
    public String employees(HttpSession session, Model model) {
        String notice = (String) model.asMap().getOrDefault("notice", null);
        return uiModelService.renderAdminPage("employees", model, sessionRole(session), notice);
    }

    @GetMapping("/employees/new")
    public String createForm(HttpSession session, Model model) {
        String notice = (String) model.asMap().getOrDefault("notice", null);
        return uiModelService.renderAdminPage("employees/new", model, sessionRole(session), notice);
    }

    @PostMapping("/employees")
    public String createEmployee(
            @Valid @ModelAttribute CreateEmployeeRequest request,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("notice",
                    bindingResult.getFieldError() != null
                            ? bindingResult.getFieldError().getDefaultMessage()
                            : "Dữ liệu không hợp lệ.");
            return "redirect:/admin/employees/new";
        }
        try {
            User actor = currentUser(session);
            User created = employeeService.createEmployee(request, actor);
            redirectAttributes.addFlashAttribute("notice", "Đã tạo nhân viên " + created.getEmail());
            return "redirect:/admin/employees/" + created.getId();
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("notice", ex.getMessage());
            return "redirect:/admin/employees/new";
        }
    }

    @GetMapping("/employees/{id}")
    public String employeeDetail(@PathVariable Long id, HttpSession session, Model model) {
        String notice = (String) model.asMap().getOrDefault("notice", null);
        return uiModelService.renderAdminPage("employees/" + id, model, sessionRole(session), notice);
    }

    @GetMapping("/employees/{id}/edit")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        String notice = (String) model.asMap().getOrDefault("notice", null);
        return uiModelService.renderAdminPage("employees/" + id + "/edit", model, sessionRole(session), notice);
    }

    @PostMapping("/employees/{id}")
    public String updateEmployee(
            @PathVariable Long id,
            @Valid @ModelAttribute UpdateEmployeeRequest request,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("notice",
                    bindingResult.getFieldError() != null
                            ? bindingResult.getFieldError().getDefaultMessage()
                            : "Dữ liệu không hợp lệ.");
            return "redirect:/admin/employees/" + id + "/edit";
        }
        try {
            User updated = employeeService.updateEmployee(id, request, currentUser(session));
            redirectAttributes.addFlashAttribute("notice", "Đã cập nhật " + updated.getFullName());
            return "redirect:/admin/employees/" + id;
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("notice", ex.getMessage());
            return "redirect:/admin/employees/" + id + "/edit";
        }
    }

    @PostMapping("/employees/{id}/toggle-active")
    public String toggleActivePost(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        return toggleActive(id, session, redirectAttributes);
    }

    @GetMapping("/employees/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            User updated = employeeService.toggleActive(id, currentUser(session));
            redirectAttributes.addFlashAttribute("notice",
                    updated.isActive() ? "Đã mở khóa tài khoản." : "Đã khóa tài khoản (inactive).");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("notice", ex.getMessage());
        }
        return "redirect:/admin/employees";
    }

    @PostMapping("/employees/{id}/delete")
    public String deleteEmployee(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            employeeService.deleteEmployee(id, currentUser(session));
            redirectAttributes.addFlashAttribute("notice", "Đã xóa vĩnh viễn tài khoản.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("notice", ex.getMessage());
        }
        return "redirect:/admin/employees";
    }

    @GetMapping("/permissions")
    public String permissions(HttpSession session, Model model) {
        String notice = (String) model.asMap().getOrDefault("notice", null);
        return uiModelService.renderAdminPage("permissions", model, sessionRole(session), notice);
    }

    @PostMapping("/permissions")
    public String savePermissions(
            @ModelAttribute SavePermissionsRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            permissionService.saveGrants(request, currentUser(session));
            redirectAttributes.addFlashAttribute("notice", "Đã lưu ma trận quyền vào database.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("notice", ex.getMessage());
        }
        return "redirect:/admin/permissions";
    }

    @GetMapping("/sessions")
    public String sessions(HttpSession session, Model model) {
        return uiModelService.renderAdminPage("sessions", model, sessionRole(session), null);
    }

    @GetMapping("/audit")
    public String audit(HttpSession session, Model model) {
        return uiModelService.renderAdminPage("audit", model, sessionRole(session), null);
    }

    @GetMapping("/reports")
    public String reports(HttpSession session, Model model) {
        return uiModelService.renderAdminPage("reports", model, sessionRole(session), null);
    }

    @GetMapping("/{*page}")
    public String fallback(@PathVariable("page") String page, HttpSession session, Model model) {
        String key = page == null ? "" : page.replaceAll("^/+", "").replaceAll("/+$", "");
        if (key.isBlank() || key.startsWith("dashboard")) {
            return "redirect:/admin/employees";
        }
        if ("sign-in".equals(key)) {
            return "redirect:/";
        }
        try {
            return uiModelService.renderAdminPage(key, model, sessionRole(session), null);
        } catch (Exception ex) {
            return "redirect:/admin/employees";
        }
    }

    private User currentUser(HttpSession session) {
        Long id = SessionAuthUtils.getUserId(session);
        return userRepository.findByIdWithRole(id)
                .orElseThrow(() -> new BusinessException("Phiên đăng nhập không hợp lệ."));
    }

    private static Role sessionRole(HttpSession session) {
        Role catalog = AppRoles.of("admin");
        String fullName = SessionAuthUtils.getFullName(session);
        if (fullName == null || fullName.isBlank()) {
            return catalog;
        }
        String[] parts = fullName.trim().split("\\s+");
        String initials = parts.length == 1
                ? parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase()
                : (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        return new Role(catalog.key(), catalog.label(), fullName, initials);
    }
}
