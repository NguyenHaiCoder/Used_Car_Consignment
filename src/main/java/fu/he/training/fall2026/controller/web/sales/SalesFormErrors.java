package fu.he.training.fall2026.controller.web.sales;

import fu.he.training.fall2026.exception.BusinessException;
import org.springframework.validation.BindingResult;

/** Maps expected service failures to form errors without exposing database details. */
final class SalesFormErrors {
    private SalesFormErrors() {
    }

    static void reject(BindingResult binding, RuntimeException exception) {
        if (exception instanceof BusinessException) {
            binding.reject("sales.invalid", exception.getMessage());
        } else {
            rejectConcurrentChange(binding);
        }
    }

    static void rejectConcurrentChange(BindingResult binding) {
        binding.reject("sales.busy", "Dữ liệu đang được cập nhật. Vui lòng thử lại.");
    }
}
