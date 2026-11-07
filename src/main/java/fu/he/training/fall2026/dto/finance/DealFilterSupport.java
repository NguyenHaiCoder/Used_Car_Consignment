package fu.he.training.fall2026.dto.finance;

import fu.he.training.fall2026.dto.handover.HandoverFilter;
import java.time.LocalDate;
import org.springframework.ui.Model;

/** Vũ Mạnh Cường: gắn filter list ra model phẳng cho sales/deal-parts.html. */
public final class DealFilterSupport {

    private DealFilterSupport() {
    }

    public static void bindListModel(Model model, String base, PaymentFilter filter, int pageSize) {
        PaymentFilter f = filter == null ? new PaymentFilter() : filter;
        bind(model, base, f.getQ(), f.getStatus(), f.getStaff(),
                f.getFrom(), f.getTo(), f.getAmountMin(), f.getAmountMax(), pageSize);
    }

    public static void bindListModel(Model model, String base, SettlementFilter filter, int pageSize) {
        SettlementFilter f = filter == null ? new SettlementFilter() : filter;
        bind(model, base, f.getQ(), f.getStatus(), f.getStaff(),
                f.getFrom(), f.getTo(), f.getAmountMin(), f.getAmountMax(), pageSize);
    }

    public static void bindListModel(Model model, String base, HandoverFilter filter, int pageSize) {
        HandoverFilter f = filter == null ? new HandoverFilter() : filter;
        bind(model, base, f.getQ(), f.getStatus(), f.getStaff(),
                f.getFrom(), f.getTo(), f.getAmountMin(), f.getAmountMax(), pageSize);
    }

    public static void bind(
            Model model,
            String base,
            String q,
            String status,
            String staff,
            LocalDate from,
            LocalDate to,
            Integer amountMin,
            Integer amountMax,
            int pageSize) {
        model.addAttribute("base", base);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status == null ? "" : status);
        model.addAttribute("staff", staff == null ? "" : staff);
        model.addAttribute("dateFrom", from == null ? "" : from.toString());
        model.addAttribute("dateTo", to == null ? "" : to.toString());
        model.addAttribute("amountMin", amountMin == null ? "" : amountMin.toString());
        model.addAttribute("amountMax", amountMax == null ? "" : amountMax.toString());
        model.addAttribute("size", pageSize);
    }
}
