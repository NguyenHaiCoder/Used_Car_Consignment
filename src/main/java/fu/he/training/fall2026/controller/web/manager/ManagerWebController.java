package fu.he.training.fall2026.controller.web.manager;

import fu.he.training.fall2026.dto.manager.DecideApprovalRequest;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.model.ui.UiModels.Role;
import fu.he.training.fall2026.model.ui.AppRoles;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.service.ManagerApprovalService;
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

/**
 * Manager UI — chỉ tài khoản role MANAGER (vd. manager@garagepro.example).
 * ADMIN không vào được /manager/** (RoleAuthInterceptor + SecurityConfig).
 */
@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerWebController {

    private final AdminUiModelService uiModelService;
    private final ManagerApprovalService approvalService;
    private final UserRepository userRepository;

    @GetMapping({"", "/"})
    public String root() {
        return "redirect:/manager/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        return uiModelService.renderManagerPage("dashboard-manager", model, sessionRole(session), null);
    }

    @GetMapping("/approvals")
    public String approvals(HttpSession session, Model model) {
        return uiModelService.renderManagerPage("approvals", model, sessionRole(session), null);
    }

    @GetMapping("/approvals/{id}")
    public String approvalDetail(@PathVariable Long id, HttpSession session, Model model) {
        String notice = (String) model.asMap().getOrDefault("notice", null);
        return uiModelService.renderManagerPage("approvals/" + id, model, sessionRole(session), notice);
    }

    @PostMapping("/approvals/{id}/decide")
    public String decide(
            @PathVariable Long id,
            @Valid @ModelAttribute DecideApprovalRequest request,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("notice", "Vui lòng chọn quyết định và nhập lý do.");
            return "redirect:/manager/approvals/" + id;
        }
        // Map Vietnamese select labels if needed
        normalizeDecision(request);
        try {
            approvalService.decide(id, request, currentUser(session));
            redirectAttributes.addFlashAttribute("notice", "Đã ghi nhận quyết định phê duyệt.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("notice", ex.getMessage());
        }
        return "redirect:/manager/approvals/" + id;
    }

    @GetMapping("/audit")
    public String audit(HttpSession session, Model model) {
        return uiModelService.renderManagerPage("audit", model, sessionRole(session), null);
    }

    @GetMapping("/reports")
    public String reports(HttpSession session, Model model) {
        return uiModelService.renderManagerPage("reports", model, sessionRole(session), null);
    }

    private void normalizeDecision(DecideApprovalRequest request) {
        if (request.getDecision() == null) {
            return;
        }
        String d = request.getDecision().trim();
        if (d.equalsIgnoreCase("Phê duyệt") || d.equalsIgnoreCase("APPROVED")) {
            request.setDecision("APPROVED");
        } else if (d.equalsIgnoreCase("Từ chối") || d.equalsIgnoreCase("REJECTED")) {
            request.setDecision("REJECTED");
        } else if (d.contains("bổ sung") || d.equalsIgnoreCase("NEED_MORE_INFO")) {
            request.setDecision("NEED_MORE_INFO");
        }
    }

    private User currentUser(HttpSession session) {
        Long id = SessionAuthUtils.getUserId(session);
        return userRepository.findByIdWithRole(id)
                .orElseThrow(() -> new BusinessException("Phiên đăng nhập không hợp lệ."));
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
