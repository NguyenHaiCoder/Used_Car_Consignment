package fu.he.training.fall2026.controller.web;

import fu.he.training.fall2026.dto.auth.LoginOtpRequest;
import fu.he.training.fall2026.dto.listing.PublicListingFilter;
import fu.he.training.fall2026.service.PublicSaleNewsService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Public landing pages (Thymeleaf). */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final PublicSaleNewsService publicSaleNewsService;

    @GetMapping({"/", "/home"})
    public String home(HttpSession session, Model model) {
        populateSessionModel(session, model);
        if (!model.containsAttribute("loginOtpRequest")) {
            model.addAttribute("loginOtpRequest", new LoginOtpRequest());
        }
        model.addAttribute("featuredCars", publicSaleNewsService.featured());
        return "public/home";
    }

    /** Tin bán xe: only LISTED listings; the filter form is a plain GET. */
    @GetMapping("/tin-ban-xe")
    public String saleNews(
            @ModelAttribute("filter") PublicListingFilter filter,
            BindingResult filterErrors,
            HttpSession session,
            Model model) {
        populateSessionModel(session, model);
        model.addAttribute("page", publicSaleNewsService.search(filter));
        model.addAttribute("brands", publicSaleNewsService.listBrands());
        return "public/sale-news";
    }

    private void populateSessionModel(HttpSession session, Model model) {
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

    /** Legacy login routes → home (login lives in home modal). */
    @GetMapping({"/login", "/auth/sign-in"})
    public String loginRedirect() {
        return "redirect:/?openLogin=1";
    }
}
