package fu.he.training.fall2026.controller.web.consignment;

import fu.he.training.fall2026.dto.consignment.AgreementRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 02.04 Consignment Agreement Detail — draft, submit and amend the agreement of one consignment. */
@Controller
@RequestMapping("/consignment")
@RequiredArgsConstructor
public class AgreementWebController {

    private static final String BASE = "/consignment/consignments/";
    private static final String FORM_VIEW = "consignment/agreements/app-form";

    private final ConsignmentAgreementService agreementService;
    private final ConsignmentService consignmentService;

    @GetMapping("/consignments/{consignmentId}/agreement")
    public String detail(@PathVariable Long consignmentId, Model model) {
        model.addAttribute("consignment", consignmentService.get(consignmentId));
        model.addAttribute("agreement", agreementService.findLatest(consignmentId).orElse(null));
        model.addAttribute("versions", agreementService.versions(consignmentId));
        return "consignment/agreements/app-detail";
    }

    @GetMapping("/consignments/{consignmentId}/agreement/new")
    public String createForm(@PathVariable Long consignmentId, Model model) {
        AgreementRequest form = new AgreementRequest();
        form.setConsignmentId(consignmentId);
        model.addAttribute("consignment", consignmentService.get(consignmentId));
        model.addAttribute("form", form);
        return FORM_VIEW;
    }

    @GetMapping("/consignments/{consignmentId}/agreement/edit")
    public String editForm(@PathVariable Long consignmentId, Model model) {
        model.addAttribute("consignment", consignmentService.get(consignmentId));
        model.addAttribute("form", agreementService.formFor(consignmentId));
        return FORM_VIEW;
    }

    /** Creates the draft when the hidden agreement id is empty, otherwise updates the newest draft. */
    @PostMapping("/agreements")
    public String save(
            @Valid @ModelAttribute("form") AgreementRequest form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (form.getConsignmentId() == null) {
            return "redirect:/consignment/consignments";
        }
        Long consignmentId = form.getConsignmentId();
        if (!errors.hasErrors()) {
            try {
                if (form.getId() == null) {
                    agreementService.createDraft(consignmentId, form);
                    flash.addFlashAttribute("flashMessage", "Đã tạo hợp đồng (bản nháp).");
                } else {
                    agreementService.updateDraft(consignmentId, form);
                    flash.addFlashAttribute("flashMessage", "Đã lưu hợp đồng (bản nháp).");
                }
                return "redirect:" + BASE + consignmentId + "/agreement";
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("consignment", consignmentService.get(consignmentId));
        return FORM_VIEW;
    }

    @PostMapping("/agreements/submit")
    public String submit(@RequestParam Long consignmentId, RedirectAttributes flash) {
        agreementService.submit(consignmentId);
        flash.addFlashAttribute("flashMessage", "Đã gửi duyệt hợp đồng. Nội dung được khóa, chờ ký.");
        return "redirect:" + BASE + consignmentId + "/agreement";
    }

    @PostMapping("/agreements/amend")
    public String amend(@RequestParam Long consignmentId, RedirectAttributes flash) {
        agreementService.createAmendment(consignmentId);
        flash.addFlashAttribute("flashMessage", "Đã tạo phiên bản sửa đổi (bản nháp). Hợp đồng đang ACTIVE giữ nguyên.");
        return "redirect:" + BASE + consignmentId + "/agreement/edit";
    }
}
