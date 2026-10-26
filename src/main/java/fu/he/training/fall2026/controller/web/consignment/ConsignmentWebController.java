package fu.he.training.fall2026.controller.web.consignment;

import fu.he.training.fall2026.dto.consignment.ConsignmentForm;
import fu.he.training.fall2026.dto.consignment.ConsignmentResponse;
import fu.he.training.fall2026.dto.consignment.WithdrawForm;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.ConsignmentService;
import fu.he.training.fall2026.service.ConsignorService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 02.02 Consignment Request List and 02.03 Create / Edit Draft. */
@Controller
@RequestMapping("/consignment/consignments")
@RequiredArgsConstructor
public class ConsignmentWebController {

    private static final int PAGE_SIZE = 10;
    private static final String BASE = "/consignment/consignments";
    private static final String FORM_VIEW = "consignment/consignments/app-form";

    private final ConsignmentService consignmentService;
    private final ConsignorService consignorService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ConsignmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Page<ConsignmentResponse> result = consignmentService.search(q, status, page, PAGE_SIZE);
        model.addAttribute("page", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status);
        model.addAttribute("counts", consignmentService.countByStatus());
        return "consignment/consignments/app-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new ConsignmentForm());
        model.addAttribute("consignors", consignorService.listAll());
        model.addAttribute("vehicles", consignmentService.listVehicles());
        model.addAttribute("editing", false);
        model.addAttribute("formAction", BASE);
        model.addAttribute("cancelUrl", BASE);
        return FORM_VIEW;
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("form") ConsignmentForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                Long id = consignmentService.createDraft(form);
                flash.addFlashAttribute("flashMessage", "Đã tạo draft ký gửi.");
                return "redirect:" + BASE + "/" + id;
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("consignors", consignorService.listAll());
        model.addAttribute("vehicles", consignmentService.listVehicles());
        model.addAttribute("editing", false);
        model.addAttribute("formAction", BASE);
        model.addAttribute("cancelUrl", BASE);
        return FORM_VIEW;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("consignment", consignmentService.get(id));
        model.addAttribute("withdrawRequest", consignmentService.findWithdrawRequest(id).orElse(null));
        return "consignment/consignments/app-detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", consignmentService.formFor(id));
        model.addAttribute("consignors", consignorService.listAll());
        model.addAttribute("vehicles", consignmentService.listVehicles());
        model.addAttribute("editing", true);
        model.addAttribute("formAction", BASE + "/" + id);
        model.addAttribute("cancelUrl", BASE + "/" + id);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") ConsignmentForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                consignmentService.updateDraft(id, form);
                flash.addFlashAttribute("flashMessage", "Đã lưu draft.");
                return "redirect:" + BASE + "/" + id;
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("consignors", consignorService.listAll());
        model.addAttribute("vehicles", consignmentService.listVehicles());
        model.addAttribute("editing", true);
        model.addAttribute("formAction", BASE + "/" + id);
        model.addAttribute("cancelUrl", BASE + "/" + id);
        return FORM_VIEW;
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id, RedirectAttributes flash) {
        consignmentService.submit(id);
        flash.addFlashAttribute("flashMessage", "Đã gửi duyệt. Hồ sơ được khóa, không sửa trực tiếp nữa.");
        return "redirect:" + BASE + "/" + id;
    }

    @GetMapping("/{id}/withdraw")
    public String withdrawForm(@PathVariable Long id, Model model) {
        model.addAttribute("consignment", consignmentService.get(id));
        model.addAttribute("checks", consignmentService.withdrawChecks(id));
        model.addAttribute("form", new WithdrawForm());
        model.addAttribute("base", BASE);
        return "consignment/consignments/app-withdraw";
    }

    @PostMapping("/{id}/withdraw")
    public String withdraw(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") WithdrawForm form,
            BindingResult errors,
            HttpSession session,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                consignmentService.requestWithdraw(id, form, SessionAuthUtils.getUserId(session));
                flash.addFlashAttribute("flashMessage", "Đã gửi yêu cầu rút hồ sơ cho quản lý, chờ duyệt.");
                return "redirect:" + BASE + "/" + id;
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("consignment", consignmentService.get(id));
        model.addAttribute("checks", consignmentService.withdrawChecks(id));
        model.addAttribute("base", BASE);
        return "consignment/consignments/app-withdraw";
    }
}
