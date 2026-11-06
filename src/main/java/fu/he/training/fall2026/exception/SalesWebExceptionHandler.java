package fu.he.training.fall2026.exception;

import fu.he.training.fall2026.controller.web.sales.BuyerWebController;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice(basePackageClasses = BuyerWebController.class)
public class SalesWebExceptionHandler {
    private static final String ERROR_VIEW = "sales/scheduling/error";

    @ExceptionHandler(BusinessException.class)
    public String businessError(BusinessException exception, Model model, HttpServletResponse response) {
        response.setStatus(exception.getStatus().value());
        model.addAttribute("error", exception.getMessage());
        return ERROR_VIEW;
    }

    @ExceptionHandler(BuyerException.class)
    public String buyerError(BuyerException exception, Model model, HttpServletResponse response) {
        HttpStatus status = switch (exception.getReason()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case VALIDATION -> HttpStatus.BAD_REQUEST;
        };
        response.setStatus(status.value());
        model.addAttribute("error", exception.getMessage());
        return ERROR_VIEW;
    }

    @ExceptionHandler(SalesSchedulingException.class)
    public String schedulingError(SalesSchedulingException exception, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("error", exception.getMessage());
        return ERROR_VIEW;
    }

    @ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, IllegalArgumentException.class})
    public String invalidInput(Exception exception, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        model.addAttribute("error", "Dữ liệu không hợp lệ. Vui lòng kiểm tra thông tin đã nhập.");
        return ERROR_VIEW;
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
    public String conflictingChange(Exception exception, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.CONFLICT.value());
        model.addAttribute("error", "Dữ liệu đang được cập nhật. Vui lòng tải lại trang và thử lại.");
        return ERROR_VIEW;
    }
}
