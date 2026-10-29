package fu.he.training.fall2026.controller.web.finance;

import fu.he.training.fall2026.model.ui.UiModels.Link;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.List;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Sidebar menu of the Finance and Handover roles. The shared sidebar reads {@code navigation} and
 * {@code activeNav} for every role that has no hard-coded menu.
 */
@ControllerAdvice(basePackages = {
        "fu.he.training.fall2026.controller.web.finance",
        "fu.he.training.fall2026.controller.web.handover"})
public class Wp05NavigationAdvice {

    private static final List<Link> FINANCE = List.of(
            new Link("Tổng quan", "/finance/dashboard", "Tài chính"),
            new Link("Giao dịch thanh toán", "/finance/payments", "Tài chính"),
            new Link("Hoa hồng & quyết toán", "/finance/settlements", "Tài chính"),
            new Link("Chi trả chủ xe", "/finance/payouts", "Tài chính"),
            new Link("Hàng đợi bàn giao", "/finance/handovers", "Bàn giao"));

    private static final List<Link> HANDOVER = List.of(
            new Link("Tổng quan", "/handover/dashboard", "Bàn giao"),
            new Link("Hàng đợi bàn giao", "/handover/handovers", "Bàn giao"));

    @ModelAttribute
    public void navigation(HttpServletRequest request, Model model) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        // receipt and refund forms belong to the payment list
        String match = path.startsWith("/finance/receipts") || path.startsWith("/finance/refunds") ? "/finance/payments" : path;
        List<Link> links = path.startsWith("/handover") ? HANDOVER : FINANCE;
        model.addAttribute("navigation", links);
        model.addAttribute("activeNav", links.stream()
                .map(Link::url)
                .filter(match::startsWith)
                .max(Comparator.comparingInt(String::length))
                .orElse(null));
    }
}
