package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.finance.ContractSummary;
import fu.he.training.fall2026.dto.finance.HistoryEntry;
import fu.he.training.fall2026.dto.finance.PaymentResponse;
import fu.he.training.fall2026.dto.finance.ReceiptRequest;
import fu.he.training.fall2026.dto.finance.RefundRequest;
import fu.he.training.fall2026.dto.finance.RefundResponse;
import fu.he.training.fall2026.dto.finance.RuleCheck;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.PaymentStatus;
import fu.he.training.fall2026.entity.enums.RefundStatus;
import fu.he.training.fall2026.entity.payment.Payment;
import fu.he.training.fall2026.entity.payment.PaymentTransaction;
import fu.he.training.fall2026.entity.payment.Refund;
import fu.he.training.fall2026.entity.sales.SalesContract;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.PaymentRepository;
import fu.he.training.fall2026.repository.RefundRepository;
import fu.he.training.fall2026.repository.SaleLookupRepository;
import fu.he.training.fall2026.service.AuditService;
import fu.he.training.fall2026.service.PaymentService;
import fu.he.training.fall2026.service.SaleLedgerService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    static final String METHOD_PAYOS = "PAYOS";
    private static final String AUDIT_TARGET = "Payment";
    private static final List<ContractStatus> COLLECTABLE = List.of(ContractStatus.SIGNED, ContractStatus.ACTIVE);
    /** Refunds that still count against what a payment can give back. */
    private static final List<RefundStatus> COMMITTED_REFUNDS =
            List.of(RefundStatus.PENDING, RefundStatus.APPROVED, RefundStatus.PROCESSED);

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final SaleLookupRepository saleLookupRepository;
    private final SaleLedgerService saleLedgerService;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> search(String keyword, PaymentStatus status, int page, int size) {
        String pattern = keyword == null || keyword.isBlank()
                ? "%"
                : "%" + keyword.trim().toLowerCase() + "%";
        return paymentRepository.search(
                        pattern, status, PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(PaymentServiceImpl::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<PaymentStatus, Long> countByStatus() {
        Map<PaymentStatus, Long> counts = new EnumMap<>(PaymentStatus.class);
        for (PaymentStatus status : PaymentStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : paymentRepository.countByStatus()) {
            counts.put((PaymentStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse get(Long id) {
        return toDto(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ContractSummary contractOf(Long paymentId) {
        return saleLedgerService.summarize(load(paymentId).getSalesContract());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleCheck> checks(Long id) {
        Payment payment = load(id);
        ContractSummary contract = saleLedgerService.summarize(payment.getSalesContract());
        boolean settled = payment.getStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentStatus.REFUNDED;
        boolean withinContract = settled
                ? contract.getClearedAmount().compareTo(contract.getSalePrice()) <= 0
                : payment.getAmount().compareTo(contract.getOutstandingAmount()) <= 0;
        RuleCheck evidence = METHOD_PAYOS.equals(payment.getMethod())
                ? RuleCheck.of("Transaction payOS đã xác minh qua webhook", isVerified(payment.getPaymentTransaction()),
                        "Chưa có transaction PAID từ webhook đã xác minh; redirect của khách không chứng minh đã trả tiền.")
                : RuleCheck.of("Có bằng chứng khoản thu", hasText(payment.getEvidencePath()),
                        "Cần số hiệu/ảnh chứng từ trước khi đối soát.");
        return List.of(
                RuleCheck.of("Có mã tham chiếu duy nhất", hasText(payment.getReferenceCode()), "Thiếu mã tham chiếu."),
                evidence,
                RuleCheck.of("Không vượt giá trị hợp đồng", withinContract,
                        "Tổng tiền thu vượt phần còn phải thu của " + contract.getCode() + "; cần ngoại lệ được Manager duyệt."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> refunds(Long paymentId) {
        return refundRepository.findByPayment(paymentId).stream().map(PaymentServiceImpl::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoryEntry> history(Long id) {
        return auditService.history(AUDIT_TARGET, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractSummary> collectableContracts() {
        return saleLookupRepository.findOpenContracts(COLLECTABLE).stream()
                .map(saleLedgerService::summarize)
                .filter(c -> c.getOutstandingAmount().subtract(c.getPendingAmount()).signum() > 0)
                .toList();
    }

    @Override
    @Transactional
    public Long recordReceipt(ReceiptRequest form, Long actorUserId) {
        SalesContract contract = saleLookupRepository.findContract(form.getContractId())
                .orElseThrow(() -> new BusinessException("Hợp đồng bán không tồn tại."));
        if (!COLLECTABLE.contains(contract.getStatus()) || contract.isSaleClosed()) {
            throw new BusinessException(
                    "Hợp đồng " + contract.getCode() + " chưa ký hoặc đã đóng, không thể ghi nhận khoản thu.", HttpStatus.CONFLICT);
        }
        String reference = form.getReferenceCode().trim();
        if (paymentRepository.existsByReferenceCodeIgnoreCase(reference)) {
            throw new BusinessException(
                    "Mã tham chiếu " + reference + " đã được ghi nhận. Không ghi trùng khoản thu.", HttpStatus.CONFLICT);
        }
        ContractSummary summary = saleLedgerService.summarize(contract);
        BigDecimal collectable = summary.getOutstandingAmount().subtract(summary.getPendingAmount()).max(BigDecimal.ZERO);
        if (form.getAmount().compareTo(collectable) > 0) {
            throw new BusinessException("Số tiền vượt phần còn phải thu (" + money(collectable)
                    + ", đã trừ khoản đang chờ đối soát). Khoản vượt cần ngoại lệ được Manager duyệt.", HttpStatus.CONFLICT);
        }
        if (form.getPaidAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Thời gian thu không được ở tương lai.");
        }
        Payment payment = new Payment();
        payment.setCode("TMP-" + UUID.randomUUID().toString().substring(0, 12)); // replaced by TT-{id} below
        payment.setSalesContract(contract);
        payment.setAmount(form.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMethod(form.getMethod());
        payment.setReferenceCode(reference);
        payment.setPayerName(form.getPayerName().trim());
        payment.setPaidAt(form.getPaidAt().atZone(ZoneId.systemDefault()).toInstant());
        payment.setEvidencePath(blankToNull(form.getEvidencePath()));
        paymentRepository.save(payment);
        payment.setCode("TT-" + payment.getId());
        auditService.record(actorUserId, "RECEIPT_RECORDED", AUDIT_TARGET, payment.getId(),
                "Ghi nhận " + money(payment.getAmount()) + " cho " + contract.getCode() + " · ref " + reference);
        return payment.getId();
    }

    @Override
    @Transactional
    public void reconcile(Long id, Long actorUserId) {
        Payment payment = load(id);
        requireInFlight(payment);
        List<String> failed = checks(id).stream()
                .filter(check -> !check.isPassed())
                .map(check -> check.getLabel() + ": " + check.getDetail())
                .toList();
        if (!failed.isEmpty()) {
            throw new BusinessException("Chưa thể đối soát: " + String.join(" ", failed), HttpStatus.CONFLICT);
        }
        payment.setStatus(PaymentStatus.PAID);
        auditService.record(actorUserId, "RECONCILED", AUDIT_TARGET, id, "Đối soát khớp, chuyển PAID.");
    }

    @Override
    @Transactional
    public void reject(Long id, String reason, Long actorUserId) {
        if (!hasText(reason)) {
            throw new BusinessException("Vui lòng nhập lý do từ chối khoản thu.");
        }
        Payment payment = load(id);
        requireInFlight(payment);
        payment.setStatus(PaymentStatus.FAILED);
        auditService.record(actorUserId, "REJECTED", AUDIT_TARGET, id, reason.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> refundablePayments() {
        return paymentRepository.findByStatus(PaymentStatus.PAID).stream()
                .filter(p -> refundableAmount(p).signum() > 0)
                .map(PaymentServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional
    public Long recordRefund(RefundRequest form, Long actorUserId) {
        Payment payment = paymentRepository.findDetailById(form.getPaymentId())
                .orElseThrow(() -> new BusinessException("Khoản thu gốc không tồn tại."));
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new BusinessException("Chỉ hoàn tiền từ khoản thu đã đối soát (PAID).", HttpStatus.CONFLICT);
        }
        BigDecimal refundable = refundableAmount(payment);
        if (form.getAmount().compareTo(refundable) > 0) {
            throw new BusinessException("Số tiền hoàn vượt phần còn hoàn được của " + payment.getCode()
                    + " (" + money(refundable) + ").", HttpStatus.CONFLICT);
        }
        Refund refund = new Refund();
        refund.setCode("TMP-" + UUID.randomUUID().toString().substring(0, 12)); // replaced by HT-{id} below
        refund.setPayment(payment);
        refund.setReservation(form.isDepositRefund() ? payment.getSalesContract().getReservation() : null);
        refund.setAmount(form.getAmount());
        refund.setStatus(RefundStatus.PENDING);
        refund.setReason(form.getReason().trim());
        refund.setEvidencePath(blankToNull(form.getEvidencePath()));
        refundRepository.save(refund);
        refund.setCode("HT-" + refund.getId());
        auditService.record(actorUserId, "REFUND_REQUESTED", AUDIT_TARGET, payment.getId(),
                refund.getCode() + " · " + money(refund.getAmount()) + " · " + refund.getReason());
        return payment.getId();
    }

    @Override
    @Transactional
    public Long processRefund(Long refundId, String evidencePath, Long actorUserId) {
        Refund refund = loadRefund(refundId);
        if (refund.getStatus() != RefundStatus.PENDING && refund.getStatus() != RefundStatus.APPROVED) {
            throw new BusinessException("Khoản hoàn " + refund.getCode() + " đã được xử lý.", HttpStatus.CONFLICT);
        }
        if (hasText(evidencePath)) {
            refund.setEvidencePath(evidencePath.trim());
        }
        if (!hasText(refund.getEvidencePath())) {
            throw new BusinessException("Cần bằng chứng chuyển hoàn trước khi hoàn tất.");
        }
        Payment payment = refund.getPayment();
        // summed before the status change so the query's auto-flush does not count this refund twice
        BigDecimal processed = refundRepository.sumByPayment(payment.getId(), List.of(RefundStatus.PROCESSED))
                .add(refund.getAmount());
        refund.setStatus(RefundStatus.PROCESSED);
        refund.setProcessedAt(Instant.now());
        if (processed.compareTo(payment.getAmount()) >= 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
        }
        auditService.record(actorUserId, "REFUND_PROCESSED", AUDIT_TARGET, payment.getId(),
                refund.getCode() + " · " + money(refund.getAmount()) + " · bằng chứng " + refund.getEvidencePath());
        return payment.getId();
    }

    @Override
    @Transactional
    public Long rejectRefund(Long refundId, String reason, Long actorUserId) {
        if (!hasText(reason)) {
            throw new BusinessException("Vui lòng nhập lý do từ chối hoàn tiền.");
        }
        Refund refund = loadRefund(refundId);
        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BusinessException("Chỉ từ chối được khoản hoàn đang PENDING.", HttpStatus.CONFLICT);
        }
        refund.setStatus(RefundStatus.REJECTED);
        auditService.record(actorUserId, "REFUND_REJECTED", AUDIT_TARGET, refund.getPayment().getId(),
                refund.getCode() + " · " + reason.trim());
        return refund.getPayment().getId();
    }

    @Override
    @Transactional(readOnly = true)
    public long pendingRefundCount() {
        return refundRepository.countByStatus(RefundStatus.PENDING);
    }

    private BigDecimal refundableAmount(Payment payment) {
        return payment.getAmount().subtract(refundRepository.sumByPayment(payment.getId(), COMMITTED_REFUNDS));
    }

    private Payment load(Long id) {
        return paymentRepository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException("Khoản thu", id));
    }

    private Refund loadRefund(Long id) {
        return refundRepository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException("Khoản hoàn", id));
    }

    /** Only receipts waiting for reconciliation can change status; PAID money is never edited in place. */
    private static void requireInFlight(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new BusinessException("Khoản thu " + payment.getCode() + " đã ở trạng thái " + payment.getStatus()
                    + "; điều chỉnh phải đi qua hoàn tiền hoặc phê duyệt.", HttpStatus.CONFLICT);
        }
    }

    private static boolean isVerified(PaymentTransaction transaction) {
        return transaction != null && transaction.getStatus() == PaymentStatus.PAID;
    }

    static PaymentResponse toDto(Payment p) {
        PaymentTransaction txn = p.getPaymentTransaction();
        SalesContract contract = p.getSalesContract();
        return PaymentResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .contractId(contract.getId())
                .contractCode(contract.getCode())
                .buyerName(contract.getReservation().getBuyer().getFullName())
                .amount(p.getAmount())
                .status(p.getStatus())
                .payerName(p.getPayerName())
                .referenceCode(p.getReferenceCode())
                .method(p.getMethod())
                .paidAt(toLocal(p.getPaidAt()))
                .evidencePath(p.getEvidencePath())
                .createdAt(toLocal(p.getCreatedAt()))
                .providerTxnId(txn != null ? txn.getProviderTxnId() : null)
                .transactionStatus(txn != null ? txn.getStatus() : null)
                .build();
    }

    private static RefundResponse toDto(Refund r) {
        return RefundResponse.builder()
                .id(r.getId())
                .code(r.getCode())
                .paymentId(r.getPayment().getId())
                .reservationCode(r.getReservation() != null ? r.getReservation().getCode() : null)
                .amount(r.getAmount())
                .status(r.getStatus())
                .reason(r.getReason())
                .evidencePath(r.getEvidencePath())
                .processedAt(toLocal(r.getProcessedAt()))
                .createdAt(toLocal(r.getCreatedAt()))
                .build();
    }

    static LocalDateTime toLocal(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    static String money(BigDecimal amount) {
        return String.format(Locale.US, "%,.0f ₫", amount).replace(',', '.');
    }

    static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
