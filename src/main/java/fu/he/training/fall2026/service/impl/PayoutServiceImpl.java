package fu.he.training.fall2026.service.impl;

import static fu.he.training.fall2026.service.impl.PaymentServiceImpl.hasText;
import static fu.he.training.fall2026.service.impl.PaymentServiceImpl.money;
import static fu.he.training.fall2026.service.impl.PaymentServiceImpl.toLocal;

import fu.he.training.fall2026.dto.finance.HistoryEntry;
import fu.he.training.fall2026.dto.finance.PayoutCompleteRequest;
import fu.he.training.fall2026.dto.finance.PayoutResponse;
import fu.he.training.fall2026.entity.enums.PayoutStatus;
import fu.he.training.fall2026.entity.enums.SettlementStatus;
import fu.he.training.fall2026.entity.payment.Payout;
import fu.he.training.fall2026.entity.payment.Settlement;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.PayoutRepository;
import fu.he.training.fall2026.service.AuditService;
import fu.he.training.fall2026.service.PayoutService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {

    private static final String AUDIT_TARGET = "Payout";
    private static final List<PayoutStatus> OPEN = List.of(PayoutStatus.PENDING, PayoutStatus.PROCESSING);

    private final PayoutRepository payoutRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public Page<PayoutResponse> search(PayoutStatus status, int page, int size) {
        return payoutRepository.search(
                        status, PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(PayoutServiceImpl::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PayoutResponse get(Long id) {
        return toDto(load(id));
    }

    @Override
    @Transactional
    public void complete(PayoutCompleteRequest form, Long actorUserId) {
        Payout payout = load(form.getId());
        requireOpen(payout);
        Settlement settlement = payout.getSettlement();
        if (settlement.getStatus() != SettlementStatus.APPROVED) {
            throw new BusinessException("Quyết toán " + settlement.getCode() + " không còn ở trạng thái đã duyệt.", HttpStatus.CONFLICT);
        }
        payout.setReferenceCode(form.getReferenceCode().trim());
        payout.setEvidencePath(form.getEvidencePath().trim());
        payout.setPaidAt(Instant.now());
        payout.setStatus(PayoutStatus.COMPLETED);
        settlement.setStatus(SettlementStatus.PAID_OUT);
        auditService.record(actorUserId, "PAID", AUDIT_TARGET, payout.getId(),
                money(payout.getAmount()) + " · ref " + payout.getReferenceCode());
        auditService.record(actorUserId, "PAID_OUT", SettlementServiceImpl.AUDIT_TARGET, settlement.getId(),
                "Đã chi " + payout.getCode() + " · ref " + payout.getReferenceCode());
    }

    @Override
    @Transactional
    public void fail(Long id, String reason, Long actorUserId) {
        if (!hasText(reason)) {
            throw new BusinessException("Vui lòng nhập lý do lệnh chi thất bại.");
        }
        Payout payout = load(id);
        requireOpen(payout);
        payout.setStatus(PayoutStatus.FAILED);
        auditService.record(actorUserId, "FAILED", AUDIT_TARGET, id, reason.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public long openCount() {
        return payoutRepository.countByStatusIn(OPEN);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoryEntry> history(Long id) {
        return auditService.history(AUDIT_TARGET, id);
    }

    private Payout load(Long id) {
        return payoutRepository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException("Lệnh chi", id));
    }

    private static void requireOpen(Payout payout) {
        if (!OPEN.contains(payout.getStatus())) {
            throw new BusinessException("Lệnh chi " + payout.getCode() + " đã " + payout.getStatus() + ".", HttpStatus.CONFLICT);
        }
    }

    static PayoutResponse toDto(Payout p) {
        Settlement s = p.getSettlement();
        return PayoutResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .settlementId(s.getId())
                .settlementCode(s.getCode())
                .contractCode(s.getSalesContract().getCode())
                .amount(p.getAmount())
                .status(p.getStatus())
                .recipientName(p.getRecipientName())
                .bankName(p.getBankName())
                .accountNumberMasked(p.getAccountNumberMasked())
                .referenceCode(p.getReferenceCode())
                .evidencePath(p.getEvidencePath())
                .paidAt(toLocal(p.getPaidAt()))
                .createdAt(toLocal(p.getCreatedAt()))
                .build();
    }
}
