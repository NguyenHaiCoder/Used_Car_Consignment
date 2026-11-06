package fu.he.training.fall2026.controller.web.inspector;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import fu.he.training.fall2026.dto.listing.ListingVehicleResponse;
import fu.he.training.fall2026.dto.listing.SaleNewsFilter;
import fu.he.training.fall2026.dto.listing.SaleNewsRequest;
import fu.he.training.fall2026.dto.listing.SaleNewsResponse;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.SaleNewsService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
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
 * Nguyễn Vũ Trường An: Quản lý tin đăng bán xe.
 * Đăng tin chỉ cho hồ sơ Đã kiểm định; người đăng là người phụ trách tin.
 */
@Controller
@RequestMapping("/inspector/listings")
@RequiredArgsConstructor
public class InspectorListingsWebController {

    private static final String BASE = "/inspector/listings";
    private static final String LIST_VIEW = "inspector/news/list";
    private static final String CREATE_VIEW = "inspector/news/create";
    private static final String EDIT_VIEW = "inspector/news/edit";

    private final SaleNewsService saleNewsService;

    @GetMapping
    public String list(
            @ModelAttribute("filter") SaleNewsFilter filter,
            BindingResult filterErrors,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        int pageSize = CatalogPageSupport.normalizeSize(size);
        Page<SaleNewsResponse> result = CatalogPageSupport.lastPageIfBeyond(
                saleNewsService.search(filter, page, pageSize),
                last -> saleNewsService.search(filter, last, pageSize));
        Map<String, Long> counts = new LinkedHashMap<>();
        saleNewsService.countByStatus().forEach((status, count) -> counts.put(status.name(), count));
        model.addAttribute("page", result);
        model.addAttribute("counts", counts);
        model.addAttribute("size", pageSize);
        model.addAttribute("staffs", saleNewsService.listStaffs());
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String createForm(
            @RequestParam(required = false) Long consignmentId,
            HttpSession session,
            Model model) {
        ListingVehicleResponse picked = saleNewsService.findReadyVehicle(consignmentId).orElse(null);
        model.addAttribute("form", picked == null ? new SaleNewsRequest() : saleNewsService.newForm(picked));
        addCreateOptions(model, picked, session);
        return CREATE_VIEW;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        SaleNewsRequest form = saleNewsService.formFor(id);
        model.addAttribute("form", form);
        addEditOptions(model, form);
        return EDIT_VIEW;
    }

    /** Creates the listing when the hidden id is empty, otherwise updates it. */
    @PostMapping
    public String save(
            @Valid @ModelAttribute("form") SaleNewsRequest form,
            BindingResult errors,
            HttpSession session,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                if (form.getId() == null) {
                    Long id = saleNewsService.create(form, SessionAuthUtils.getUserId(session));
                    flash.addFlashAttribute("flashMessage", "Đã đăng tin " + saleNewsService.get(id).getCode() + ".");
                    return "redirect:" + BASE;
                }
                saleNewsService.update(form.getId(), form);
                flash.addFlashAttribute("flashMessage", "Đã cập nhật tin " + saleNewsService.get(form.getId()).getCode() + ".");
                return "redirect:" + BASE;
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        if (form.getId() == null) {
            addCreateOptions(model, saleNewsService.findReadyVehicle(form.getConsignmentId()).orElse(null), session);
            return CREATE_VIEW;
        }
        addEditOptions(model, form);
        return EDIT_VIEW;
    }

    @PostMapping("/{id}/pause")
    public String pause(@PathVariable Long id, RedirectAttributes flash) {
        try {
            flash.addFlashAttribute("flashMessage", "Đã tạm ẩn tin " + saleNewsService.pause(id).getCode() + ".");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE;
    }

    @PostMapping("/{id}/resume")
    public String resume(@PathVariable Long id, RedirectAttributes flash) {
        try {
            flash.addFlashAttribute("flashMessage", "Đã hiển thị lại tin " + saleNewsService.resume(id).getCode() + ".");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE;
    }

    private void addCreateOptions(Model model, ListingVehicleResponse picked, HttpSession session) {
        model.addAttribute("ready", saleNewsService.listReadyVehicles());
        model.addAttribute("picked", picked);
        model.addAttribute("ownerName", SessionAuthUtils.getFullName(session));
    }

    private void addEditOptions(Model model, SaleNewsRequest form) {
        SaleNewsResponse news = saleNewsService.get(form.getId());
        model.addAttribute("news", news);
        model.addAttribute("picked", saleNewsService.getVehicle(saleNewsService.formFor(form.getId()).getConsignmentId()));
        model.addAttribute("ownerName", news.getStaffName());
        model.addAttribute("imageUrls", saleNewsService.imageUrls(form.getId()));
    }
}
