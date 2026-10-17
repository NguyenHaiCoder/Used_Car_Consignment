package fu.he.training.fall2026.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Catches exceptions thrown from {@code @Controller} methods and renders {@code error/problem}.
 * Unknown URLs / container errors are handled by {@link AppErrorController}.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(
            Exception ex,
            Model model,
            HttpServletRequest request) {
        String message = ex instanceof NoResourceFoundException
                ? "Trang bạn đang tìm không tồn tại hoặc đã được di chuyển."
                : ex.getMessage();
        return render(model, request, HttpStatus.NOT_FOUND, message);
    }

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(
            BusinessException ex,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_REQUEST;
        response.setStatus(status.value());
        return render(model, request, status, ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String handleResponseStatus(
            ResponseStatusException ex,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        response.setStatus(status.value());
        String message = ex.getReason() != null && !ex.getReason().isBlank()
                ? ex.getReason()
                : status.getReasonPhrase();
        return render(model, request, status, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpected(
            Exception ex,
            Model model,
            HttpServletRequest request) {
        return render(model, request, HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống.");
    }

    private String render(Model model, HttpServletRequest request, HttpStatus status, String message) {
        model.addAttribute("status", status.value());
        model.addAttribute("error", status.getReasonPhrase());
        model.addAttribute("message", message);
        model.addAttribute("path", request.getRequestURI());
        model.addAttribute("referer", request.getHeader("Referer"));
        return "error/problem";
    }
}
