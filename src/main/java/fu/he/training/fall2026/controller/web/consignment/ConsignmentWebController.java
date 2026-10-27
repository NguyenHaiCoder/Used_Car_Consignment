package fu.he.training.fall2026.controller.web.consignment;

import fu.he.training.fall2026.dto.consignment.ConsignmentRequest;
import fu.he.training.fall2026.dto.consignment.ConsignmentResponse;
import fu.he.training.fall2026.dto.consignment.WithdrawRequest;
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

/** 02.02 Consignment Request List, 02.03 Create / Edit Draft and 02.05 Withdraw request. */
@Controller
@RequestMapping("/consignment/consignments")
@RequiredArgsConstructor
public class ConsignmentWebController {

    private static final int PAGE_SIZE = 10;
    private static final String BASE = "/consignment/consignments";
    private static final String FORM_VIEW = "consignment/consignments/app-form";
    private static final String WITHDRAW_VIEW = "consignment/consignments/app-withdraw";

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
        model.addAttribute("form", new ConsignmentRequest());
        model.addAttribute("consignors", consignorService.listAll());
        model.addAttribute("vehicles", consignmentService.listVehicles());
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
        return FORM_VIEW;
    }

    /** Creates the draft when the hidden id is empty, otherwise updates that draft. */
    @PostMapping
    public String save(
            @Valid @ModelAttribute("form") ConsignmentRequest form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                if (form.getId() == null) {
                    Long id = consignmentService.createDraft(form);
                    flash.addFlashAttribute("flashMessage", "Đã tạo draft ký gửi.");
                    return "redirect:" + BASE + "/" + id;
                }
                consignmentService.updateDraft(form.getId(), form);
                flash.addFlashAttribute("flashMessage", "Đã lưu draft.");
                return "redirect:" + BASE + "/" + form.getId();
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("consignors", consignorService.listAll());
        model.addAttribute("vehicles", consignmentService.listVehicles());
        return FORM_VIEW;
    }

    @PostMapping("/submit")
    public String submit(@RequestParam Long id, RedirectAttributes flash) {
        consignmentService.submit(id);
        flash.addFlashAttribute("flashMessage", "Đã gửi duyệt. Hồ sơ được khóa, không sửa trực tiếp nữa.");
        return "redirect:" + BASE + "/" + id;
    }

    @GetMapping("/{id}/withdraw")
    public String withdrawForm(@PathVariable Long id, Model model) {
        WithdrawRequest form = new WithdrawRequest();
        form.setId(id);
        model.addAttribute("form", form);
        model.addAttribute("consignment", consignmentService.get(id));
        model.addAttribute("checks", consignmentService.withdrawChecks(id));
        model.addAttribute("base", BASE);
        return WITHDRAW_VIEW;
    }

    @PostMapping("/withdraw")
    public String withdraw(
            @Valid @ModelAttribute("form") WithdrawRequest form,
            BindingResult errors,
            HttpSession session,
            Model model,
            RedirectAttributes flash) {
        if (form.getId() == null) {
            return "redirect:" + BASE;
        }
        if (!errors.hasErrors()) {
            try {
                consignmentService.requestWithdraw(form.getId(), form, SessionAuthUtils.getUserId(session));
                flash.addFlashAttribute("flashMessage", "Đã gửi yêu cầu rút hồ sơ cho quản lý, chờ duyệt.");
                return "redirect:" + BASE + "/" + form.getId();
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("consignment", consignmentService.get(form.getId()));
        model.addAttribute("checks", consignmentService.withdrawChecks(form.getId()));
        model.addAttribute("base", BASE);
        return WITHDRAW_VIEW;
    }
}
