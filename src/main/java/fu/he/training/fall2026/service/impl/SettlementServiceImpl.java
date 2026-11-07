package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import fu.he.training.fall2026.dto.finance.PayoutRequest;
import fu.he.training.fall2026.dto.finance.SettlementFilter;
import fu.he.training.fall2026.dto.finance.SettlementView;
import fu.he.training.fall2026.entity.auth.Approval;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.ConsignmentAgreement;
import fu.he.training.fall2026.entity.consignment.Consignor;
import fu.he.training.fall2026.entity.enums.AgreementStatus;
import fu.he.training.fall2026.entity.enums.ApprovalStatus;
import fu.he.training.fall2026.entity.enums.ApprovalType;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.entity.enums.PayoutStatus;
import fu.he.training.fall2026.entity.enums.SettlementStatus;
import fu.he.training.fall2026.entity.payment.Payout;
import fu.he.training.fall2026.entity.payment.Settlement;
import fu.he.training.fall2026.entity.sales.SalesContract;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.ApprovalRepository;
import fu.he.training.fall2026.repository.ConsignmentAgreementRepository;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import fu.he.training.fall2026.repository.PayoutRepository;
import fu.he.training.fall2026.repository.SettlementRepository;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.service.SettlementService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);
    private static final String TARGET_TYPE = "Settlement";

    private final SettlementRepository settlementRepository;
    private final PayoutRepository payoutRepository;
    private final ConsignmentRepository consignmentRepository;
    private final ConsignmentAgreementRepository agreementRepository;
    private final ApprovalRepository approvalRepository;
    private final UserRepository userRepository;

    @Override
    public Map<String, Map<String, String>> statusMeta() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put("DRAFT", meta("Nháp", "slate", "pencil", "draft"));
        map.putAll(reviewStatusMeta());
        return map;
    }

    @Override
    public Map<String, Map<String, String>> reviewStatusMeta() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put("PENDING_REVIEW", meta("Chờ duyệt", "amber", "hourglass", "pending"));
        map.put("APPROVED", meta("Đã duyệt", "blue", "shield-check", "signed"));
        map.put("PAID_OUT", meta("Đã chi", "green", "check", "listed"));
        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SettlementView> search(SettlementFilter filter, boolean excludeDraft, int page, Integer size) {
        SettlementFilter f = filter == null ? new SettlementFilter() : filter;
        String keyword = f.getQ();
        String pattern = keyword == null || keyword.isBlank()
                ? "%"
                : "%" + keyword.trim().toLowerCase() + "%";
        String staff = f.getStaff() == null || f.getStaff().isBlank() ? null : f.getStaff().trim();
        int pageSize = CatalogPageSupport.normalizeSize(size);
        return settlementRepository
                .search(
                        pattern,
                        parseStatus(f.getStatus()),
                        excludeDraft,
                        staff,
                        startOf(f.getFrom()),
                        startOf(f.getTo() == null ? null : f.getTo().plusDays(1)),
                        millions(f.getAmountMin()),
                        millions(f.getAmountMax()),
                        PageRequest.of(Math.max(page, 0), pageSize, Sort.by(Sort.Direction.DESC, "id")))
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByStatus(boolean excludeDraft) {
        Map<String, Long> counts = new LinkedHashMap<>();
        (excludeDraft ? reviewStatusMeta() : statusMeta()).keySet().forEach(k -> counts.put(k, 0L));
        for (Object[] row : settlementRepository.countByStatus()) {
            SettlementStatus st = (SettlementStatus) row[0];
            if (excludeDraft && st == SettlementStatus.DRAFT) {
                continue;
            }
            if (counts.containsKey(st.name())) {
                counts.put(st.name(), (Long) row[1]);
            }
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listStaffNames() {
        return settlementRepository.findDistinctStaffNames();
    }

    @Override
    @Transactional(readOnly = true)
    public SettlementView get(Long id) {
        return toDto(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public SettlementView getForReview(Long id) {
        Settlement settlement = load(id);
        if (settlement.getStatus() == SettlementStatus.DRAFT) {
            throw new ResourceNotFoundException("Quyết toán", id);
        }
        return toDto(settlement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> steps(Long id) {
        SettlementView row = get(id);
        String status = row.getStatus();
        boolean approved = "APPROVED".equals(status) || "PAID_OUT".equals(status);
        boolean sent = approved || "PENDING_REVIEW".equals(status);
        return List.of(
                step("Đóng sale", row.getContractCode(), true),
                step("Lập quyết toán", row.getCode(), true),
                step("Gửi Quản lý duyệt", sent ? "Đã gửi" : "Chưa gửi", sent),
                step("Quản lý duyệt", approved ? blank(row.getReviewedBy(), "Đã duyệt") : "Chưa duyệt", approved),
                step("Chi trả chủ xe", "PAID_OUT".equals(status) ? row.getPayoutCode() : "Chưa chi", "PAID_OUT".equals(status)));
    }

    @Override
    @Transactional
    public void openFor(SalesContract contract) {
        if (settlementRepository.existsBySalesContractId(contract.getId())) {
            return;
        }
        Consignment consignment = consignmentRepository
                .findByVehicleIdOrderByIdDesc(contract.getReservation().getVehicle().getId()).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("Không tìm thấy hồ sơ ký gửi của xe.", HttpStatus.CONFLICT));
        ConsignmentAgreement agreement = agreementRepository
                .findFirstByConsignmentIdOrderByVersionNoDesc(consignment.getId())
                .filter(a -> a.getStatus() == AgreementStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        "Chưa có hợp đồng ký gửi ACTIVE để tính hoa hồng quyết toán.", HttpStatus.CONFLICT));
        BigDecimal base = nz(contract.getSalePrice());
        BigDecimal rate = nz(agreement.getCommissionRate());
        BigDecimal commission = base.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal fee = nz(agreement.getFixedFee());
        Settlement settlement = new Settlement();
        settlement.setCode(temporaryCode());
        settlement.setSalesContract(contract);
        settlement.setConsignmentAgreement(agreement);
        settlement.setStatus(SettlementStatus.DRAFT);
        settlement.setBaseAmount(base);
        settlement.setCommissionAmount(commission);
        settlement.setFeeAmount(fee);
        settlement.setApprovedCostAmount(BigDecimal.ZERO);
        settlement.setRefundHeldAmount(BigDecimal.ZERO);
        settlement.setDepositHeldAmount(BigDecimal.ZERO);
        settlement.setNetPayoutAmount(base.subtract(commission).subtract(fee));
        settlement.setNotes("");
        settlementRepository.save(settlement);
        settlement.setCode(generateCode(settlement.getId()));
    }

    @Override
    @Transactional
    public void submit(Long id) {
        Settlement settlement = load(id);
        if (settlement.getStatus() != SettlementStatus.DRAFT) {
            return;
        }
        settlement.setStatus(SettlementStatus.PENDING_REVIEW);
        settlement.setNotes("");
    }

    @Override
    @Transactional
    public void decide(Long id, String decision, String reason, Long actorUserId) {
        Settlement settlement = load(id);
        if (settlement.getStatus() != SettlementStatus.PENDING_REVIEW) {
            throw new BusinessException("Quyết toán này đã được xử lý.", HttpStatus.CONFLICT);
        }
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", actorUserId));
        if ("APPROVED".equals(decision)) {
            settlement.setStatus(SettlementStatus.APPROVED);
            Approval approval = newApproval(settlement, actor, ApprovalStatus.APPROVED, null);
            approval.setApprovedBy(actor);
            approval.setDecidedAt(Instant.now());
            approvalRepository.save(approval);
            return;
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Từ chối thì phải nhập lý do.", HttpStatus.BAD_REQUEST);
        }
        settlement.setStatus(SettlementStatus.DRAFT);
        settlement.setNotes(reason.trim());
        Approval approval = newApproval(settlement, actor, ApprovalStatus.REJECTED, reason.trim());
        approval.setApprovedBy(actor);
        approval.setDecidedAt(Instant.now());
        approvalRepository.save(approval);
    }

    @Override
    @Transactional
    public void payout(Long id, PayoutRequest form) {
        Settlement settlement = load(id);
        if (settlement.getStatus() != SettlementStatus.APPROVED) {
            throw new BusinessException("Chỉ chi trả khi quyết toán đã được duyệt.", HttpStatus.CONFLICT);
        }
        String reference = form.getReference() == null ? "" : form.getReference().trim();
        if (reference.isEmpty()) {
            throw new BusinessException("Vui lòng nhập mã tham chiếu chi trả.", HttpStatus.BAD_REQUEST);
        }
        Consignor owner = settlement.getConsignmentAgreement().getConsignment().getConsignor();
        Payout payout = new Payout();
        payout.setCode(temporaryCode());
        payout.setSettlement(settlement);
        payout.setAmount(nz(settlement.getNetPayoutAmount()));
        payout.setStatus(PayoutStatus.COMPLETED);
        payout.setRecipientName(owner.getFullName());
        payout.setBankName(owner.getBank());
        payout.setAccountNumberMasked(mask(owner.getAccountNumber()));
        payout.setReferenceCode(reference);
        payout.setPaidAt(Instant.now());
        payoutRepository.save(payout);
        payout.setCode(generatePayoutCode(payout.getId()));
        settlement.setStatus(SettlementStatus.PAID_OUT);
        Consignment consignment = settlement.getConsignmentAgreement().getConsignment();
        consignment.setStatus(ConsignmentStatus.COMPLETED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> approvalQueueRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SettlementView s : search(new SettlementFilter(), true, 0, 500)) {
            if (!"PENDING_REVIEW".equals(s.getStatus()) && !"APPROVED".equals(s.getStatus())
                    && !"PAID_OUT".equals(s.getStatus())) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", s.getId());
            row.put("code", s.getCode());
            row.put("type", "SETTLEMENT");
            row.put("target", s.getContractCode());
            row.put("file", s.getCode());
            row.put("car", s.getCar());
            row.put("plate", s.getPlate());
            row.put("staff", "Tài chính");
            row.put("amount", s.getNet());
            row.put("created", s.getCreated());
            row.put("status", "PENDING_REVIEW".equals(s.getStatus()) ? "PENDING" : "APPROVED");
            row.put("url", "/manager/settlements/" + s.getId());
            rows.add(row);
        }
        return rows;
    }

    private SettlementView toDto(Settlement s) {
        SalesContract contract = s.getSalesContract();
        Consignment consignment = s.getConsignmentAgreement().getConsignment();
        Consignor owner = consignment.getConsignor();
        BigDecimal rate = nz(s.getConsignmentAgreement().getCommissionRate());
        String rateText = rate.stripTrailingZeros().scale() <= 0
                ? rate.toPlainString() + "%"
                : rate.setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + "%";
        Payout payout = payoutRepository.findFirstBySettlementIdOrderByIdDesc(s.getId()).orElse(null);
        String reviewedBy = approvalRepository
                .findDecisions(ApprovalType.SETTLEMENT, TARGET_TYPE, s.getId(), ApprovalStatus.APPROVED)
                .stream()
                .findFirst()
                .map(a -> a.getApprovedBy() != null ? a.getApprovedBy().getFullName() : "")
                .orElse("");
        return SettlementView.builder()
                .id(s.getId())
                .code(s.getCode())
                .contractCode(contract.getCode())
                .car(contract.getReservation().getVehicle().getDisplayLabel())
                .plate(nullToEmpty(contract.getReservation().getVehicle().getLicensePlate()))
                .file(consignment.getCode())
                .owner(owner.getFullName())
                .buyer(contract.getReservation().getBuyer().getFullName())
                .staff(contract.getReservation().getAssignedTo() != null
                        ? contract.getReservation().getAssignedTo().getFullName() : "")
                .salePrice(money(s.getBaseAmount()))
                .commissionRate(rateText)
                .commission(money(s.getCommissionAmount()))
                .fee(money(s.getFeeAmount()))
                .net(money(s.getNetPayoutAmount()))
                .status(s.getStatus().name())
                .created(iso(s.getCreatedAt()))
                .bank(nullToEmpty(owner.getBank()))
                .account(mask(owner.getAccountNumber()))
                .rejectReason(s.getStatus() == SettlementStatus.DRAFT ? nullToEmpty(s.getNotes()) : "")
                .reviewedBy(reviewedBy)
                .payoutCode(payout != null ? payout.getCode() : generatePayoutCode(s.getId()))
                .payoutReference(payout != null ? nullToEmpty(payout.getReferenceCode()) : "")
                .paidOn(payout != null ? day(payout.getPaidAt()) : "")
                .build();
    }

    private Approval newApproval(Settlement settlement, User requester, ApprovalStatus status, String reason) {
        Approval approval = new Approval();
        approval.setCode(temporaryCode());
        approval.setType(ApprovalType.SETTLEMENT);
        approval.setStatus(status);
        approval.setTargetType(TARGET_TYPE);
        approval.setTargetId(settlement.getId());
        approval.setReason(reason);
        approval.setRequestedBy(requester);
        approvalRepository.save(approval);
        approval.setCode(generateApprovalCode(approval.getId()));
        return approval;
    }

    private Settlement load(Long id) {
        return settlementRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quyết toán", id));
    }

    private static String generateCode(Long id) {
        return String.format("QT-%06d", id);
    }

    private static String generatePayoutCode(Long id) {
        return String.format("PC-%06d", id);
    }

    private static String generateApprovalCode(Long id) {
        return String.format("PD-%06d", id);
    }

    private static String temporaryCode() {
        return "TMP-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private static SettlementStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return SettlementStatus.valueOf(raw.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Instant startOf(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZONE).toInstant();
    }

    private static BigDecimal millions(Integer value) {
        return value == null ? null : MILLION.multiply(BigDecimal.valueOf(value.longValue()));
    }

    private static Map<String, String> meta(String label, String tone, String icon, String pill) {
        return Map.of("label", label, "tone", tone, "icon", icon, "pill", pill);
    }

    private static Map<String, String> step(String label, String note, boolean passed) {
        return Map.of("label", label, "note", note, "passed", String.valueOf(passed));
    }

    private static String mask(String account) {
        if (account == null || account.isBlank()) {
            return "****";
        }
        String digits = account.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return "****" + digits;
        }
        return "****" + digits.substring(digits.length() - 4);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static long money(BigDecimal value) {
        return nz(value).longValue();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String day(Instant instant) {
        return instant == null ? "" : LocalDate.ofInstant(instant, ZONE).toString();
    }

    private static String iso(Instant instant) {
        return instant == null ? "" : LocalDateTime.ofInstant(instant, ZONE).withNano(0).toString();
    }
}
