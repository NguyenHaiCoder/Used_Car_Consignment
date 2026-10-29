package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.finance.ContractSummary;
import fu.he.training.fall2026.dto.finance.HistoryEntry;
import fu.he.training.fall2026.dto.finance.PaymentResponse;
import fu.he.training.fall2026.dto.finance.ReceiptRequest;
import fu.he.training.fall2026.dto.finance.RefundRequest;
import fu.he.training.fall2026.dto.finance.RefundResponse;
import fu.he.training.fall2026.dto.finance.RuleCheck;
import fu.he.training.fall2026.entity.enums.PaymentStatus;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

/** 05.01 Payment Transaction List and 05.02 Record Receipt / Refund. */
public interface PaymentService {

    Page<PaymentResponse> search(String keyword, PaymentStatus status, int page, int size);

    /** Number of payments per status (every status is present, 0 when empty). */
    Map<PaymentStatus, Long> countByStatus();

    PaymentResponse get(Long id);

    ContractSummary contractOf(Long paymentId);

    List<RuleCheck> checks(Long id);

    List<RefundResponse> refunds(Long paymentId);

    List<HistoryEntry> history(Long id);

    /** Signed, not closed contracts that still have money to collect (choices on the receipt form). */
    List<ContractSummary> collectableContracts();

    /**
     * Records a buyer receipt as PENDING until reconciled.
     *
     * @throws fu.he.training.fall2026.exception.BusinessException on a duplicate reference, an overpayment,
     *         or a contract that is not signed / already closed
     */
    Long recordReceipt(ReceiptRequest form, Long actorUserId);

    /**
     * PENDING → PAID. Manual receipts need evidence; payOS receipts need a verified provider transaction
     * (the buyer's redirect is never proof of payment).
     */
    void reconcile(Long id, Long actorUserId);

    /** PENDING → FAILED with a reason; the record is kept, never deleted. */
    void reject(Long id, String reason, Long actorUserId);

    /** PAID payments that can still be refunded. */
    List<PaymentResponse> refundablePayments();

    /**
     * Records a PENDING refund against a PAID payment; returns the payment id.
     *
     * @throws fu.he.training.fall2026.exception.BusinessException when the refunds would exceed the payment
     */
    Long recordRefund(RefundRequest form, Long actorUserId);

    /** PENDING → PROCESSED once the money was sent back; returns the payment id. */
    Long processRefund(Long refundId, String evidencePath, Long actorUserId);

    /** PENDING → REJECTED with a reason; returns the payment id. */
    Long rejectRefund(Long refundId, String reason, Long actorUserId);

    long pendingRefundCount();
}
