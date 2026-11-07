package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.finance.PaymentDealRow;
import fu.he.training.fall2026.dto.finance.PaymentFilter;
import fu.he.training.fall2026.dto.finance.ReceiptRequest;
import fu.he.training.fall2026.dto.finance.ReceiptView;
import fu.he.training.fall2026.dto.finance.RefundRequest;
import fu.he.training.fall2026.dto.finance.RefundView;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

/** Vũ Mạnh Cường: thu tiền hợp đồng và hoàn cọc. */
public interface PaymentService {

    List<String> METHODS = List.of("Chuyển khoản", "Tiền mặt", "Quẹt thẻ");
    String DEPOSIT_METHOD = "Đặt cọc";

    Map<String, Map<String, String>> paymentStatusMeta();

    Page<PaymentDealRow> searchDeals(PaymentFilter filter, int page, Integer size);

    Map<String, Long> countDealStatuses();

    List<String> listStaffNames();

    PaymentDealRow getPaymentDeal(Long contractId);

    List<ReceiptView> receiptsOf(Long contractId);

    List<Map<String, String>> saleMilestones(Long contractId);

    void createRequest(Long contractId);

    void recordReceipt(Long contractId, ReceiptRequest form, String staffName);

    RefundView getRefund(Long reservationId);

    void processRefund(Long reservationId, RefundRequest form, String staffName);

    boolean isPaid(Long contractId);

    BigDecimal sumCollected(Long contractId);
}
