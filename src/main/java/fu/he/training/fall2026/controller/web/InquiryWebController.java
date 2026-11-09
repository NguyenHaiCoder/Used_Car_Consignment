package fu.he.training.fall2026.controller.web;

import fu.he.training.fall2026.dto.sales.InquiryRequest;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.InquiryService;
import fu.he.training.fall2026.service.PublicSaleNewsService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
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
 * Form liên hệ tư vấn xe — mẫu cô giáo:
 * GET viewForm (ModelAttribute) → POST create (@Valid + BindingResult).
 */
@Controller
@RequestMapping("/tin-ban-xe")
@RequiredArgsConstructor
public class InquiryWebController {

    private static final String FORM_VIEW = "public/inquiry-contact";

    private final InquiryService inquiryService;
    private final PublicSaleNewsService publicSaleNewsService;

    /** Không chọn xe → quay danh sách (tránh 500 khi mở /tin-ban-xe/lien-he). */
    @GetMapping("/lien-he")
    public String contactWithoutListing(RedirectAttributes flash) {
        flash.addFlashAttribute("flashError", "Vui lòng chọn xe cần tư vấn.");
        return "redirect:/tin-ban-xe";
    }

    /** Mở form tạo Inquiry gắn tin LISTED (giống /departments/detail). */
    @GetMapping("/{listingId}/lien-he")
    public String viewForm(@PathVariable Long listingId, HttpSession session, Model model) {
        InquiryRequest inquiry = new InquiryRequest();
        inquiry.setListingId(listingId);
        inquiry.setChannel("WEBSITE");
        inquiry.setNeed("PRICE");

        model.addAttribute("inquiry", inquiry);
        fillFormModel(session, model, listingId);
        return FORM_VIEW;
    }

    /** Lưu Inquiry; lỗi validation trả lại cùng view (không redirect). */
    @PostMapping("/lien-he")
    public String create(
            @ModelAttribute("inquiry") @Valid InquiryRequest inquiry,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes flash) {
        if (bindingResult.hasErrors()) {
            if (inquiry.getListingId() == null) {
                flash.addFlashAttribute("flashError",
                        bindingResult.getAllErrors().get(0).getDefaultMessage());
                return "redirect:/tin-ban-xe";
            }
            fillFormModel(session, model, inquiry.getListingId());
            return FORM_VIEW;
        }

        try {
            String code = inquiryService.createFromListing(inquiry);
            flash.addFlashAttribute("flashMessage",
                    "Đã ghi nhận yêu cầu " + code + ". Nhân viên tư vấn sẽ liên hệ sớm.");
            return "redirect:/tin-ban-xe";
        } catch (BusinessException ex) {
            if (inquiry.getListingId() == null) {
                flash.addFlashAttribute("flashError", ex.getMessage());
                return "redirect:/tin-ban-xe";
            }
            String message = ex.getMessage();
            if (message != null && message.toLowerCase().contains("điện thoại")) {
                bindingResult.rejectValue("phone", "phone.duplicate", message);
            } else {
                model.addAttribute("errorMessage", message);
            }
            fillFormModel(session, model, inquiry.getListingId());
            return FORM_VIEW;
        }
    }

    private void fillFormModel(HttpSession session, Model model, Long listingId) {
        populateSessionModel(session, model);
        model.addAttribute("listing", publicSaleNewsService.getListed(listingId));
        model.addAttribute("needs", needOptions());
    }

    private static void populateSessionModel(HttpSession session, Model model) {
        boolean loggedIn = SessionAuthUtils.isLoggedIn(session);
        model.addAttribute("loggedIn", loggedIn);
        if (loggedIn) {
            model.addAttribute("fullName", SessionAuthUtils.getFullName(session));
            model.addAttribute("roleCode", SessionAuthUtils.getRoleCode(session));
            model.addAttribute("roleName", SessionAuthUtils.getRoleName(session));
            model.addAttribute("homePath",
                    SessionAuthUtils.homePathForRole(SessionAuthUtils.getRoleCode(session)));
        }
    }

    private static Map<String, String> needOptions() {
        Map<String, String> needs = new LinkedHashMap<>();
        needs.put("PRICE", "Hỏi giá");
        needs.put("VIEWING", "Xem xe");
        needs.put("TEST_DRIVE", "Lái thử");
        return needs;
    }
}
