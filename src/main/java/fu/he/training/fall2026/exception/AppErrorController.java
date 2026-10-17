package fu.he.training.fall2026.exception;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Handles container-level errors (unknown URL 404, filter failures, …)
 * so they also render {@code error/problem} like {@link GlobalExceptionHandler}.
 */
@Controller
public class AppErrorController implements ErrorController {

    @RequestMapping("/error")
    public String error(HttpServletRequest request, Model model) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = statusAttr != null ? Integer.parseInt(statusAttr.toString()) : 500;
        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        Object messageAttr = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        String message = messageAttr != null && !messageAttr.toString().isBlank()
                ? messageAttr.toString()
                : defaultMessage(status);

        Object pathAttr = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String path = pathAttr != null ? pathAttr.toString() : request.getRequestURI();

        model.addAttribute("status", status.value());
        model.addAttribute("error", status.getReasonPhrase());
        model.addAttribute("message", message);
        model.addAttribute("path", path);
        model.addAttribute("referer", request.getHeader("Referer"));
        return "error/problem";
    }

    private String defaultMessage(HttpStatus status) {
        if (status == HttpStatus.NOT_FOUND) {
            return "Trang bạn đang tìm không tồn tại hoặc đã được di chuyển.";
        }
        if (status == HttpStatus.FORBIDDEN) {
            return "Bạn không có quyền truy cập trang này.";
        }
        return "Đã xảy ra lỗi hệ thống.";
    }
}
