package fu.he.training.fall2026.controller.web.consignment;

import fu.he.training.fall2026.dto.consignment.AgreementForm;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.ConsignmentAgreementService;
import fu.he.training.fall2026.service.ConsignmentService;
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

/** 02.04 Consignment Agreement Detail — draft, submit and amend the agreement of one consignment. */
@Controller
@RequestMapping("/consignment/consignments/{consignmentId}/agreement")
@RequiredArgsConstructor
public class AgreementWebController {

    private static final String BASE = "/consignment/consignments/";
    private static final String FORM_VIEW = "consignment/agreements/app-form";

    private final ConsignmentAgreementService agreementService;
    private final ConsignmentService consignmentService;

    @GetMapping
    public String detail(@PathVariable Long consignmentId, Model model) {
        model.addAttribute("consignment", consignmentService.get(consignmentId));
        model.addAttribute("agreement", agreementService.findLatest(consignmentId).orElse(null));
        model.addAttribute("versions", agreementService.versions(consignmentId));
        return "consignment/agreements/app-detail";
    }

    @GetMapping("/new")
    public String createForm(@PathVariable Long consignmentId, Model model) {
        model.addAttribute("consignment", consignmentService.get(consignmentId));
        model.addAttribute("form", new AgreementForm());
        model.addAttribute("formAction", BASE + consignmentId + "/agreement");
        return FORM_VIEW;
    }

    @PostMapping
    public String create(
            @PathVariable Long consignmentId,
            @Valid @ModelAttribute("form") AgreementForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                agreementService.createDraft(consignmentId, form);
                flash.addFlashAttribute("flashMessage", "Đã tạo hợp đồng (bản nháp).");
                return "redirect:" + BASE + consignmentId + "/agreement";
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("consignment", consignmentService.get(consignmentId));
        model.addAttribute("formAction", BASE + consignmentId + "/agreement");
        return FORM_VIEW;
    }

    @GetMapping("/edit")
    public String editForm(@PathVariable Long consignmentId, Model model) {
        model.addAttribute("consignment", consignmentService.get(consignmentId));
        model.addAttribute("form", agreementService.formFor(consignmentId));
        model.addAttribute("formAction", BASE + consignmentId + "/agreement/update");
        return FORM_VIEW;
    }

    @PostMapping("/update")
    public String update(
            @PathVariable Long consignmentId,
            @Valid @ModelAttribute("form") AgreementForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                agreementService.updateDraft(consignmentId, form);
                flash.addFlashAttribute("flashMessage", "Đã lưu hợp đồng (bản nháp).");
                return "redirect:" + BASE + consignmentId + "/agreement";
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("consignment", consignmentService.get(consignmentId));
        model.addAttribute("formAction", BASE + consignmentId + "/agreement/update");
        return FORM_VIEW;
    }

    @PostMapping("/submit")
    public String submit(@PathVariable Long consignmentId, RedirectAttributes flash) {
        agreementService.submit(consignmentId);
        flash.addFlashAttribute("flashMessage", "Đã gửi duyệt hợp đồng. Nội dung được khóa, chờ ký.");
        return "redirect:" + BASE + consignmentId + "/agreement";
    }

    @PostMapping("/amend")
    public String amend(@PathVariable Long consignmentId, RedirectAttributes flash) {
        agreementService.createAmendment(consignmentId);
        flash.addFlashAttribute("flashMessage", "Đã tạo phiên bản sửa đổi (bản nháp). Hợp đồng đang ACTIVE giữ nguyên.");
        return "redirect:" + BASE + consignmentId + "/agreement/edit";
    }
}
