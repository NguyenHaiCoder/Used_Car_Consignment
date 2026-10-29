package fu.he.training.fall2026.controller.web.handover;

import fu.he.training.fall2026.dto.handover.HandoverChecklistRequest;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.HandoverService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 05.04 Vehicle Handover Queue and 05.05 Handover Checklist, Receipt and Close sale. */
@Controller
@RequestMapping("/handover/handovers")
@RequiredArgsConstructor
public class HandoverWebController {

    private static final String BASE = "/handover/handovers";
    private static final String DETAIL_VIEW = "handover/handovers/app-detail";

    private final HandoverService handoverService;

    @GetMapping
    public String queue(Model model) {
        model.addAttribute("queue", handoverService.queue());
        return "handover/handovers/app-list";
    }

    @PostMapping
    public String open(@RequestParam Long contractId, HttpSession session, RedirectAttributes flash) {
        try {
            Long id = handoverService.open(contractId, SessionAuthUtils.getUserId(session));
            flash.addFlashAttribute("flashMessage", "Đã mở biên bản bàn giao.");
            return "redirect:" + BASE + "/" + id;
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:" + BASE;
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("form", handoverService.formFor(id));
        addDetail(id, model);
        return DETAIL_VIEW;
    }

    @PostMapping("/checklist")
    public String saveChecklist(
            @Valid @ModelAttribute("form") HandoverChecklistRequest form,
            BindingResult errors,
            HttpSession session,
            Model model,
            RedirectAttributes flash) {
        if (form.getId() == null) {
            return "redirect:" + BASE;
        }
        if (!errors.hasErrors()) {
            try {
                handoverService.saveChecklist(form, SessionAuthUtils.getUserId(session));
                flash.addFlashAttribute("flashMessage", "Đã lưu checklist bàn giao.");
                return "redirect:" + BASE + "/" + form.getId();
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        addDetail(form.getId(), model);
        return DETAIL_VIEW;
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id, HttpSession session, RedirectAttributes flash) {
        try {
            handoverService.complete(id, SessionAuthUtils.getUserId(session));
            flash.addFlashAttribute("flashMessage", "Đã hoàn tất bàn giao và phát hành biên nhận.");
            return "redirect:" + BASE + "/" + id + "/receipt";
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:" + BASE + "/" + id;
        }
    }

    @GetMapping("/{id}/receipt")
    public String receipt(@PathVariable Long id, Model model) {
        model.addAttribute("handover", handoverService.get(id));
        return "handover/handovers/app-receipt";
    }

    @GetMapping("/{id}/close")
    public String closeForm(@PathVariable Long id, Model model) {
        model.addAttribute("handover", handoverService.get(id));
        model.addAttribute("checks", handoverService.closeChecks(id));
        return "handover/handovers/app-close";
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, @RequestParam(required = false) String note,
                        HttpSession session, RedirectAttributes flash) {
        try {
            handoverService.closeSale(id, note, SessionAuthUtils.getUserId(session));
            flash.addFlashAttribute("flashMessage", "Đã đóng sale; hợp đồng chuyển CLOSED.");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE + "/" + id + "/close";
    }

    private void addDetail(Long id, Model model) {
        model.addAttribute("handover", handoverService.get(id));
        model.addAttribute("checks", handoverService.completionChecks(id));
        model.addAttribute("history", handoverService.history(id));
    }
}
