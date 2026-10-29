package fu.he.training.fall2026.service.impl;

import static fu.he.training.fall2026.service.impl.PaymentServiceImpl.blankToNull;
import static fu.he.training.fall2026.service.impl.PaymentServiceImpl.money;
import static fu.he.training.fall2026.service.impl.SaleLedgerServiceImpl.nz;

import fu.he.training.fall2026.dto.finance.ContractSummary;
import fu.he.training.fall2026.dto.finance.HistoryEntry;
import fu.he.training.fall2026.dto.finance.PayoutResponse;
import fu.he.training.fall2026.dto.finance.RuleCheck;
import fu.he.training.fall2026.dto.finance.SettlementAdjustRequest;
import fu.he.training.fall2026.dto.finance.SettlementDecisionRequest;
import fu.he.training.fall2026.dto.finance.SettlementResponse;
import fu.he.training.fall2026.entity.auth.Approval;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.ConsignmentAgreement;
import fu.he.training.fall2026.entity.consignment.Consignor;
import fu.he.training.fall2026.entity.enums.AgreementStatus;
import fu.he.training.fall2026.entity.enums.ApprovalStatus;
import fu.he.training.fall2026.entity.enums.ApprovalType;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.PayoutStatus;
import fu.he.training.fall2026.entity.enums.RefundStatus;
import fu.he.training.fall2026.entity.enums.SettlementStatus;
import fu.he.training.fall2026.entity.payment.Payout;
import fu.he.training.fall2026.entity.payment.Settlement;
import fu.he.training.fall2026.entity.sales.SalesContract;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.ApprovalRepository;
import fu.he.training.fall2026.repository.PayoutRepository;
import fu.he.training.fall2026.repository.RefundRepository;
import fu.he.training.fall2026.repository.SaleLookupRepository;
import fu.he.training.fall2026.repository.SettlementRepository;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.service.AuditService;
import fu.he.training.fall2026.service.SaleLedgerService;
import fu.he.training.fall2026.service.SettlementService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
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
public class SettlementServiceImpl implements SettlementService {

    static final String AUDIT_TARGET = "Settlement";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final List<ContractStatus> SETTLEABLE =
            List.of(ContractStatus.SIGNED, ContractStatus.ACTIVE, ContractStatus.CLOSED);
    private static final List<PayoutStatus> OPEN_PAYOUTS =
            List.of(PayoutStatus.PENDING, PayoutStatus.PROCESSING, PayoutStatus.COMPLETED);

    private final SettlementRepository settlementRepository;
    private final PayoutRepository payoutRepository;
    private final RefundRepository refundRepository;
    private final ApprovalRepository approvalRepository;
    private final UserRepository userRepository;
    private final SaleLookupRepository saleLookupRepository;
    private final SaleLedgerService saleLedgerService;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public Page<SettlementResponse> search(SettlementStatus status, int page, int size) {
        return settlementRepository.search(
                        status, PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(SettlementServiceImpl::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<SettlementStatus, Long> countByStatus() {
        Map<SettlementStatus, Long> counts = new EnumMap<>(SettlementStatus.class);
        for (SettlementStatus status : SettlementStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : settlementRepository.countByStatus()) {
            counts.put((SettlementStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractSummary> contractsWithoutSettlement() {
        return saleLookupRepository.findContractsWithoutSettlement(SETTLEABLE).stream()
                .map(saleLedgerService::summarize)
                .toList();
    }

    @Override
    @Transactional
    public Long create(Long contractId, Long actorUserId) {
        SalesContract contract = saleLookupRepository.findContract(contractId)
                .orElseThrow(() -> new BusinessException("Hợp đồng bán không tồn tại."));
        if (!SETTLEABLE.contains(contract.getStatus())) {
            throw new BusinessException("Hợp đồng " + contract.getCode() + " chưa ký hoặc đã hủy.", HttpStatus.CONFLICT);
        }
        if (settlementRepository.findBySalesContractId(contractId).isPresent()) {
            throw new BusinessException("Hợp đồng " + contract.getCode() + " đã có quyết toán.", HttpStatus.CONFLICT);
        }
        Consignment consignment = saleLedgerService.consignmentOf(contract)
                .orElseThrow(() -> new BusinessException("Không tìm thấy hồ sơ ký gửi của xe trong hợp đồng.", HttpStatus.CONFLICT));
        ConsignmentAgreement agreement = saleLookupRepository.findAgreements(consignment.getId(), AgreementStatus.ACTIVE)
                .stream().findFirst()
                .orElseThrow(() -> new BusinessException("Hồ sơ " + consignment.getCode()
                        + " chưa có agreement ACTIVE; không thể tính hoa hồng.", HttpStatus.CONFLICT));
        Settlement settlement = new Settlement();
        settlement.setCode("TMP-" + UUID.randomUUID().toString().substring(0, 12)); // replaced by QT-{id} below
        settlement.setSalesContract(contract);
        settlement.setConsignmentAgreement(agreement);
        settlement.setStatus(SettlementStatus.DRAFT);
        settlement.setFeeAmount(BigDecimal.ZERO);
        settlement.setApprovedCostAmount(BigDecimal.ZERO);
        settlement.setDepositHeldAmount(BigDecimal.ZERO);
        recalculate(settlement);
        settlementRepository.save(settlement);
        settlement.setCode("QT-" + settlement.getId());
        auditService.record(actorUserId, "CREATED", AUDIT_TARGET, settlement.getId(),
                "Lập quyết toán từ " + contract.getCode() + " theo " + agreement.getCode() + " v" + agreement.getVersionNo());
        return settlement.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public SettlementResponse get(Long id) {
        return toDto(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ContractSummary contractOf(Long id) {
        return saleLedgerService.summarize(load(id).getSalesContract());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleCheck> checks(Long id) {
        Settlement s = load(id);
        ConsignmentAgreement agreement = s.getConsignmentAgreement();
        ContractSummary contract = saleLedgerService.summarize(s.getSalesContract());
        BigDecimal floor = nz(agreement.getPriceFloor());
        return List.of(
                RuleCheck.of("Agreement áp dụng đang ACTIVE", agreement.getStatus() == AgreementStatus.ACTIVE,
                        agreement.getCode() + " v" + agreement.getVersionNo() + " đang " + agreement.getStatus()
                                + "; lập lại quyết toán theo phiên bản đang hiệu lực."),
                RuleCheck.of("Giá bán không thấp hơn giá sàn", nz(s.getBaseAmount()).compareTo(floor) >= 0,
                        "Giá bán " + money(nz(s.getBaseAmount())) + " thấp hơn giá sàn " + money(floor)
                                + "; cần phê duyệt BELOW_FLOOR_PRICE."),
                RuleCheck.of("Số tiền ròng cho chủ xe không âm", nz(s.getNetPayoutAmount()).signum() >= 0,
                        "Hoa hồng, phí và chi phí đang lớn hơn giá bán."),
                RuleCheck.of("Hợp đồng đã thu đủ và đối soát", contract.isFullyPaid(),
                        "Còn thiếu " + money(contract.getOutstandingAmount()) + " tiền đã đối soát."));
    }

    @Override
    @Transactional(readOnly = true)
    public SettlementAdjustRequest formFor(Long id) {
        Settlement s = load(id);
        SettlementAdjustRequest form = new SettlementAdjustRequest();
        form.setId(s.getId());
        form.setFeeAmount(nz(s.getFeeAmount()));
        form.setApprovedCostAmount(nz(s.getApprovedCostAmount()));
        form.setDepositHeldAmount(nz(s.getDepositHeldAmount()));
        form.setNotes(s.getNotes());
        return form;
    }

    @Override
    @Transactional
    public void adjust(SettlementAdjustRequest form, Long actorUserId) {
        Settlement s = load(form.getId());
        requireEditable(s);
        s.setFeeAmount(form.getFeeAmount());
        s.setApprovedCostAmount(form.getApprovedCostAmount());
        s.setDepositHeldAmount(form.getDepositHeldAmount());
        s.setNotes(blankToNull(form.getNotes()));
        recalculate(s);
        auditService.record(actorUserId, "ADJUSTED", AUDIT_TARGET, s.getId(),
                "Phí " + money(s.getFeeAmount()) + ", chi phí " + money(s.getApprovedCostAmount())
                        + ", cọc giữ " + money(s.getDepositHeldAmount()) + " → ròng " + money(s.getNetPayoutAmount()));
    }

    @Override
    @Transactional
    public void submit(Long id, Long actorUserId) {
        Settlement s = load(id);
        requireEditable(s);
        recalculate(s); // refunds posted since the last save must appear in the formula (BR-07)
        List<String> failed = checks(id).stream()
                .filter(check -> !check.isPassed())
                .map(check -> check.getLabel() + ": " + check.getDetail())
                .toList();
        if (!failed.isEmpty()) {
            throw new BusinessException("Chưa thể gửi duyệt: " + String.join(" ", failed), HttpStatus.CONFLICT);
        }
        s.setStatus(SettlementStatus.PENDING_REVIEW);
        Approval request = new Approval();
        request.setCode("ST-" + id + "-" + System.currentTimeMillis());
        request.setType(ApprovalType.SETTLEMENT);
        request.setStatus(ApprovalStatus.PENDING);
        request.setTargetType(AUDIT_TARGET);
        request.setTargetId(id);
        request.setReason("Quyết toán " + s.getCode() + " · ròng " + money(s.getNetPayoutAmount()));
        request.setRequestedBy(userRepository.getReferenceById(actorUserId));
        approvalRepository.save(request);
        auditService.record(actorUserId, "SUBMITTED", AUDIT_TARGET, id, "Gửi Manager duyệt.");
    }

    @Override
    @Transactional
    public void decide(SettlementDecisionRequest form, Long actorUserId) {
        Settlement s = load(form.getId());
        if (s.getStatus() != SettlementStatus.PENDING_REVIEW) {
            throw new BusinessException("Quyết toán " + s.getCode() + " không chờ duyệt.", HttpStatus.CONFLICT);
        }
        SettlementStatus next = switch (form.getDecision()) {
            case APPROVED -> SettlementStatus.APPROVED;
            case REJECTED -> SettlementStatus.REJECTED;
            case NEED_MORE_INFO -> SettlementStatus.DRAFT;
            case PENDING -> throw new BusinessException("Vui lòng chọn Phê duyệt, Từ chối hoặc Yêu cầu bổ sung.");
        };
        approvalRepository.findRequests(ApprovalType.SETTLEMENT, AUDIT_TARGET, s.getId()).stream()
                .filter(a -> a.getStatus() == ApprovalStatus.PENDING)
                .forEach(a -> {
                    a.setStatus(form.getDecision());
                    a.setApprovedBy(userRepository.getReferenceById(actorUserId));
                    a.setDecidedAt(Instant.now());
                });
        s.setStatus(next);
        auditService.record(actorUserId, "DECISION_" + form.getDecision(), AUDIT_TARGET, s.getId(), form.getReason().trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayoutResponse> payouts(Long id) {
        return payoutRepository.findBySettlement(id).stream().map(PayoutServiceImpl::toDto).toList();
    }

    @Override
    @Transactional
    public Long createPayout(Long id, Long actorUserId) {
        Settlement s = load(id);
        if (s.getStatus() != SettlementStatus.APPROVED) {
            throw new BusinessException("Chỉ lập lệnh chi cho quyết toán đã được Manager duyệt.", HttpStatus.CONFLICT);
        }
        if (payoutRepository.existsBySettlementIdAndStatusIn(id, OPEN_PAYOUTS)) {
            throw new BusinessException("Quyết toán " + s.getCode() + " đã có lệnh chi đang xử lý hoặc đã chi.", HttpStatus.CONFLICT);
        }
        ContractSummary contract = saleLedgerService.summarize(s.getSalesContract());
        if (!contract.isFullyPaid() || contract.getClearedAmount().compareTo(nz(s.getNetPayoutAmount())) < 0) {
            throw new BusinessException("Tiền đã đối soát của " + contract.getCode() + " chưa đủ ("
                    + money(contract.getClearedAmount()) + "); không chi từ tiền chưa về.", HttpStatus.CONFLICT);
        }
        Consignor owner = s.getConsignmentAgreement().getConsignment().getConsignor();
        Payout payout = new Payout();
        payout.setCode("TMP-" + UUID.randomUUID().toString().substring(0, 12)); // replaced by PC-{id} below
        payout.setSettlement(s);
        payout.setAmount(s.getNetPayoutAmount());
        payout.setStatus(PayoutStatus.PENDING);
        payout.setRecipientName(owner.getAccountName());
        payout.setBankName(owner.getBank());
        payout.setAccountNumberMasked(mask(owner.getAccountNumber()));
        payoutRepository.save(payout);
        payout.setCode("PC-" + payout.getId());
        auditService.record(actorUserId, "PAYOUT_CREATED", AUDIT_TARGET, id,
                payout.getCode() + " · " + money(payout.getAmount()) + " cho " + owner.getAccountName());
        return payout.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoryEntry> history(Long id) {
        return auditService.history(AUDIT_TARGET, id);
    }

    /**
     * net = base − commission − fees − approved costs − refunds posted + forfeited deposit kept for the owner.
     * Commission is the agreement's fixed fee when set, otherwise base × rate%.
     */
    private void recalculate(Settlement s) {
        ConsignmentAgreement agreement = s.getConsignmentAgreement();
        BigDecimal base = nz(s.getSalesContract().getSalePrice());
        BigDecimal commission = nz(agreement.getFixedFee()).signum() > 0
                ? agreement.getFixedFee()
                : base.multiply(nz(agreement.getCommissionRate())).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal refunds = refundRepository.sumByContract(s.getSalesContract().getId(), List.of(RefundStatus.PROCESSED));
        s.setBaseAmount(base);
        s.setCommissionAmount(commission);
        s.setRefundHeldAmount(refunds);
        s.setNetPayoutAmount(base
                .subtract(commission)
                .subtract(nz(s.getFeeAmount()))
                .subtract(nz(s.getApprovedCostAmount()))
                .subtract(refunds)
                .add(nz(s.getDepositHeldAmount())));
    }

    private Settlement load(Long id) {
        return settlementRepository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException("Quyết toán", id));
    }

    private static void requireEditable(Settlement s) {
        if (s.getStatus() != SettlementStatus.DRAFT && s.getStatus() != SettlementStatus.REJECTED) {
            throw new BusinessException("Quyết toán " + s.getCode() + " đang " + s.getStatus()
                    + "; chỉ DRAFT hoặc REJECTED mới được sửa.", HttpStatus.CONFLICT);
        }
    }

    static String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }
        return "•••• " + accountNumber.substring(accountNumber.length() - 4);
    }

    static SettlementResponse toDto(Settlement s) {
        ConsignmentAgreement a = s.getConsignmentAgreement();
        Consignment c = a.getConsignment();
        Consignor owner = c.getConsignor();
        SalesContract contract = s.getSalesContract();
        return SettlementResponse.builder()
                .id(s.getId())
                .code(s.getCode())
                .status(s.getStatus())
                .contractId(contract.getId())
                .contractCode(contract.getCode())
                .vehicleLabel(contract.getReservation().getVehicle().getDisplayLabel())
                .consignmentId(c.getId())
                .consignmentCode(c.getCode())
                .consignorName(owner.getFullName())
                .agreementCode(a.getCode())
                .agreementVersion(a.getVersionNo())
                .commissionRate(a.getCommissionRate())
                .fixedFee(a.getFixedFee())
                .priceFloor(a.getPriceFloor())
                .payoutBank(owner.getBank())
                .payoutAccountName(owner.getAccountName())
                .payoutAccountMasked(mask(owner.getAccountNumber()))
                .baseAmount(nz(s.getBaseAmount()))
                .commissionAmount(nz(s.getCommissionAmount()))
                .feeAmount(nz(s.getFeeAmount()))
                .approvedCostAmount(nz(s.getApprovedCostAmount()))
                .refundHeldAmount(nz(s.getRefundHeldAmount()))
                .depositHeldAmount(nz(s.getDepositHeldAmount()))
                .netPayoutAmount(nz(s.getNetPayoutAmount()))
                .notes(s.getNotes())
                .build();
    }
}
