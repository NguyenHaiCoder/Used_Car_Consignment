package fu.he.training.fall2026.controller.web.auth;

import fu.he.training.fall2026.dto.auth.AuthSessionResponse;
import fu.he.training.fall2026.dto.auth.LoginOtpRequest;
import fu.he.training.fall2026.dto.auth.OtpIssuedResponse;
import fu.he.training.fall2026.dto.auth.VerifyOtpRequest;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.AuthService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Authentication pages + OTP flow (Thymeleaf + session + DTO). */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/verify-otp")
    public String verifyOtpPage(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "mode", required = false, defaultValue = "login") String mode,
            Model model,
            HttpSession session) {
        if (SessionAuthUtils.isLoggedIn(session)) {
            return "redirect:" + SessionAuthUtils.homePathForRole(SessionAuthUtils.getRoleCode(session));
        }
        VerifyOtpRequest form = new VerifyOtpRequest();
        form.setEmail(email);
        model.addAttribute("verifyOtpRequest", form);
        model.addAttribute("email", email);
        model.addAttribute("mode", mode);
        return "auth/verify-otp";
    }

    @PostMapping("/auth/login")
    public String requestOtp(
            @Valid @ModelAttribute LoginOtpRequest loginOtpRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("authError",
                    bindingResult.getFieldError() != null
                            ? bindingResult.getFieldError().getDefaultMessage()
                            : "Email không hợp lệ.");
            return "redirect:/?openLogin=1";
        }
        try {
            OtpIssuedResponse issued = authService.requestOtp(loginOtpRequest);
            redirectAttributes.addFlashAttribute("authInfo", issued.getMessage());
            return "redirect:/verify-otp?mode=login&email=" + encode(issued.getEmail());
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("authError", ex.getMessage());
            return "redirect:/?openLogin=1";
        }
    }

    @PostMapping("/auth/verify-otp")
    public String verifyOtp(
            @Valid @ModelAttribute("verifyOtpRequest") VerifyOtpRequest verifyOtpRequest,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("otpError",
                    bindingResult.getFieldError() != null
                            ? bindingResult.getFieldError().getDefaultMessage()
                            : "OTP không hợp lệ.");
            return "redirect:/verify-otp?mode=login&email=" + encode(verifyOtpRequest.getEmail());
        }
        try {
            AuthSessionResponse auth = authService.verifyOtpAndLogin(verifyOtpRequest, session);
            return "redirect:" + auth.getHomePath();
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("otpError", ex.getMessage());
            return "redirect:/verify-otp?mode=login&email=" + encode(verifyOtpRequest.getEmail());
        }
    }

    @PostMapping("/auth/resend-otp")
    public String resendOtp(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {
        LoginOtpRequest request = new LoginOtpRequest();
        request.setEmail(email);
        try {
            OtpIssuedResponse issued = authService.requestOtp(request);
            redirectAttributes.addFlashAttribute("authInfo", "Đã gửi lại OTP.");
            return "redirect:/verify-otp?mode=login&email=" + encode(issued.getEmail());
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("otpError", ex.getMessage());
            return "redirect:/verify-otp?mode=login&email=" + encode(email);
        }
    }

    @RequestMapping(value = "/auth/logout", method = {RequestMethod.GET, RequestMethod.POST})
    public String logout(HttpSession session) {
        authService.logout(session);
        return "redirect:/";
    }

    private static String encode(String email) {
        if (email == null) {
            return "";
        }
        return URLEncoder.encode(email, StandardCharsets.UTF_8);
    }
}
