package fu.he.training.fall2026.controller.web;

import fu.he.training.fall2026.dto.auth.LoginOtpRequest;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Public landing pages (Thymeleaf). */
@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home(HttpSession session, Model model) {
        boolean loggedIn = SessionAuthUtils.isLoggedIn(session);
        model.addAttribute("loggedIn", loggedIn);
        if (loggedIn) {
            model.addAttribute("fullName", SessionAuthUtils.getFullName(session));
            model.addAttribute("roleCode", SessionAuthUtils.getRoleCode(session));
            model.addAttribute("roleName", SessionAuthUtils.getRoleName(session));
            model.addAttribute("homePath",
                    SessionAuthUtils.homePathForRole(SessionAuthUtils.getRoleCode(session)));
        }
        if (!model.containsAttribute("loginOtpRequest")) {
            model.addAttribute("loginOtpRequest", new LoginOtpRequest());
        }
        return "public/home";
    }

    /** Legacy login routes → home (login lives in home modal). */
    @GetMapping({"/login", "/auth/sign-in"})
    public String loginRedirect() {
        return "redirect:/?openLogin=1";
    }
}
