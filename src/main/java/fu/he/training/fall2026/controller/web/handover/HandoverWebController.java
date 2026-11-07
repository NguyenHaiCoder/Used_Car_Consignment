package fu.he.training.fall2026.controller.web.handover;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import fu.he.training.fall2026.dto.finance.DealFilterSupport;
import fu.he.training.fall2026.dto.handover.HandoverConfirmRequest;
import fu.he.training.fall2026.dto.handover.HandoverFilter;
import fu.he.training.fall2026.dto.handover.HandoverView;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.HandoverService;
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

/**
 * Vũ Mạnh Cường: Bàn giao xe và hoàn tất giao dịch — Controller → HandoverService → DB.
 */
@Controller
@RequestMapping("/handover/handovers")
@RequiredArgsConstructor
public class HandoverWebController {

    private static final String BASE = "/handover/handovers";
    private static final String LIST_VIEW = "handover/handovers/list";
    private static final String DETAIL_VIEW = "handover/handovers/detail";
    private static final String RECEIPT_VIEW = "handover/handovers/receipt";

    private final HandoverService handoverService;

    @ModelAttribute
    public void dictionaries(Model model) {
        model.addAttribute("statuses", handoverService.statusMeta());
    }

    @GetMapping
    public String list(
            @ModelAttribute("filter") HandoverFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        int pageSize = CatalogPageSupport.normalizeSize(size);
        Page<HandoverView> result = CatalogPageSupport.lastPageIfBeyond(
                handoverService.search(filter, page, pageSize),
                last -> handoverService.search(filter, last, pageSize));
        DealFilterSupport.bindListModel(model, BASE, filter, pageSize);
        model.addAttribute("page", result);
        model.addAttribute("counts", handoverService.countByStatus());
        model.addAttribute("staffs", handoverService.listStaffNames());
        return LIST_VIEW;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("item", handoverService.getByContractId(id));
        model.addAttribute("milestones", handoverService.saleMilestones(id));
        model.addAttribute("handoverItems", HandoverService.ITEMS);
        model.addAttribute("form", new HandoverConfirmRequest());
        return DETAIL_VIEW;
    }

    @PostMapping("/{id}/confirm")
    public String confirm(
            @PathVariable long id,
            @Valid @ModelAttribute("form") HandoverConfirmRequest form,
            BindingResult errors,
            HttpSession session,
            RedirectAttributes flash) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("flashError", errors.getAllErrors().get(0).getDefaultMessage());
            return "redirect:" + BASE + "/" + id;
        }
        try {
            handoverService.confirm(
                    id, form, SessionAuthUtils.getUserId(session), SessionAuthUtils.getFullName(session));
            flash.addFlashAttribute("flashMessage", "Đã bàn giao xe cho khách.");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE + "/" + id;
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable long id, RedirectAttributes flash) {
        try {
            handoverService.closeSale(id);
            flash.addFlashAttribute("flashMessage", "Đã hoàn tất giao dịch, quyết toán đã mở bên Tài chính.");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE + "/" + id;
    }

    @GetMapping("/{id}/receipt")
    public String receipt(@PathVariable long id, Model model, RedirectAttributes flash) {
        try {
            model.addAttribute("item", handoverService.receipt(id));
            return RECEIPT_VIEW;
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:" + BASE + "/" + id;
        }
    }
}
