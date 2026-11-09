package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import fu.he.training.fall2026.dto.finance.PaymentDealRow;
import fu.he.training.fall2026.dto.finance.PaymentFilter;
import fu.he.training.fall2026.dto.finance.ReceiptRequest;
import fu.he.training.fall2026.dto.finance.ReceiptView;
import fu.he.training.fall2026.dto.finance.RefundRequest;
import fu.he.training.fall2026.dto.finance.RefundView;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.HandoverStatus;
import fu.he.training.fall2026.entity.enums.PaymentStatus;
import fu.he.training.fall2026.entity.enums.RefundStatus;
import fu.he.training.fall2026.entity.enums.TransferStatus;
import fu.he.training.fall2026.entity.payment.Payment;
import fu.he.training.fall2026.entity.payment.PaymentRequest;
import fu.he.training.fall2026.entity.payment.Refund;
import fu.he.training.fall2026.entity.sales.Reservation;
import fu.he.training.fall2026.entity.sales.SalesContract;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import fu.he.training.fall2026.repository.HandoverRepository;
import fu.he.training.fall2026.repository.PaymentRepository;
import fu.he.training.fall2026.repository.PaymentRequestRepository;
import fu.he.training.fall2026.repository.RefundRepository;
import fu.he.training.fall2026.repository.ReservationRepository;
import fu.he.training.fall2026.repository.SalesContractRepository;
import fu.he.training.fall2026.repository.TransferCaseRepository;
import fu.he.training.fall2026.service.PaymentService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PaymentRepository paymentRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final RefundRepository refundRepository;
    private final SalesContractRepository salesContractRepository;
    private final ReservationRepository reservationRepository;
    private final ConsignmentRepository consignmentRepository;
    private final TransferCaseRepository transferCaseRepository;
    private final HandoverRepository handoverRepository;

    @Override
    public Map<String, Map<String, String>> paymentStatusMeta() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put("PARTIAL", meta("Còn phải thu", "amber", "coins", "pending"));
        map.put("PAID", meta("Đã thu đủ", "green", "check", "listed"));
        map.put("REFUND_PENDING", meta("Cần hoàn cọc", "red", "undo", "sold"));
        map.put("REFUNDED", meta("Đã hoàn cọc", "slate", "history", "closed"));
        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDealRow> searchDeals(PaymentFilter filter, int page, Integer size) {
        PaymentFilter f = filter == null ? new PaymentFilter() : filter;
        Long amountMin = f.getAmountMin() == null ? null : f.getAmountMin().longValue() * 1_000_000L;
        Long amountMax = f.getAmountMax() == null ? null : f.getAmountMax().longValue() * 1_000_000L;
        String from = f.getFrom() == null ? "" : f.getFrom().toString();
        String to = f.getTo() == null ? "" : f.getTo().toString();
        List<PaymentDealRow> rows = allDealRows().stream()
                .filter(r -> match(r, f.getQ(), f.getStatus(), f.getStaff(), from, to, amountMin, amountMax))
                .toList();
        return CatalogPageSupport.pageOf(rows, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countDealStatuses() {
        Map<String, Long> counts = new LinkedHashMap<>();
        paymentStatusMeta().keySet().forEach(k -> counts.put(k, 0L));
        for (PaymentDealRow row : allDealRows()) {
            counts.merge(row.getStatus(), 1L, Long::sum);
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listStaffNames() {
        return allDealRows().stream().map(PaymentDealRow::getStaff).filter(s -> s != null && !s.isBlank())
                .distinct().sorted().toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDealRow getPaymentDeal(Long contractId) {
        SalesContract contract = loadContract(contractId);
        if (!paymentRequestRepository.existsBySalesContractId(contractId)) {
            throw new ResourceNotFoundException("Giao dịch thanh toán", contractId);
        }
        return toDto(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptView> receiptsOf(Long contractId) {
        SalesContract contract = loadContract(contractId);
        List<ReceiptView> rows = new ArrayList<>();
        BigDecimal deposit = nz(contract.getDepositAmount());
        Reservation reservation = contract.getReservation();
        rows.add(ReceiptView.builder()
                .type("Tiền cọc")
                .amount(money(deposit))
                .method("Đặt cọc")
                .reference(reservation.getCode())
                .paidOn(day(contract.getSignedOn() != null ? contract.getSignedOn().atStartOfDay(ZONE).toInstant()
                        : contract.getCreatedAt()))
                .staff(staffName(reservation))
                .build());
        for (Payment payment : paymentRepository.findBySalesContractIdOrderByPaidAtAscIdAsc(contractId)) {
            if ("Đặt cọc".equals(payment.getMethod())) {
                continue;
            }
            rows.add(ReceiptView.builder()
                    .type("Thanh toán")
                    .amount(money(payment.getAmount()))
                    .method(nullToEmpty(payment.getMethod()))
                    .reference(nullToEmpty(payment.getReferenceCode()))
                    .paidOn(day(payment.getPaidAt()))
                    .staff(nullToEmpty(payment.getRecordedByName()))
                    .build());
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> saleMilestones(Long contractId) {
        boolean paid = isPaid(contractId);
        boolean transferred = transferCaseRepository.findBySalesContractId(contractId)
                .map(t -> t.getStatus() == TransferStatus.COMPLETED)
                .orElse(false);
        boolean handed = handoverRepository.existsBySalesContractIdAndStatusIn(
                contractId, List.of(HandoverStatus.COMPLETED, HandoverStatus.FINALIZED));
        return List.of(
                step("Thanh toán", paid ? "Đã thanh toán đủ" : "Chưa thanh toán đủ", paid),
                step("Sang tên", transferred ? "Đã sang tên" : "Chưa sang tên", transferred),
                step("Bàn giao xe", handed ? "Đã bàn giao" : "Chưa bàn giao", handed));
    }

    @Override
    @Transactional
    public void createRequest(Long contractId) {
        SalesContract contract = loadContract(contractId);
        if (contract.getStatus() != ContractStatus.SIGNED) {
            throw new BusinessException("Chỉ gửi yêu cầu thu khi hợp đồng đã ký.", HttpStatus.CONFLICT);
        }
        if (paymentRequestRepository.existsBySalesContractId(contractId)) {
            return;
        }
        ensureDepositPayment(contract);
        PaymentRequest request = new PaymentRequest();
        request.setCode(temporaryCode());
        request.setSalesContract(contract);
        request.setAmount(remaining(contract));
        request.setPurpose("Thanh toán hợp đồng");
        request.setStatus(PaymentStatus.PARTIAL);
        paymentRequestRepository.save(request);
        request.setCode(generatePaymentRequestCode(request.getId()));
        contract.setStatus(ContractStatus.ACTIVE);
    }

    @Override
    @Transactional
    public void recordReceipt(Long contractId, ReceiptRequest form, String staffName) {
        SalesContract contract = loadContract(contractId);
        if (!paymentRequestRepository.existsBySalesContractId(contractId)) {
            throw new ResourceNotFoundException("Giao dịch thanh toán", contractId);
        }
        BigDecimal amount = form.amountValue();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Vui lòng nhập số tiền thu.", HttpStatus.BAD_REQUEST);
        }
        BigDecimal left = remaining(contract);
        if (amount.compareTo(left) > 0) {
            throw new BusinessException("Số tiền thu không được lớn hơn số còn lại.", HttpStatus.CONFLICT);
        }
        String method = form.getMethod();
        if (!METHODS.contains(method)) {
            throw new BusinessException("Vui lòng chọn phương thức.", HttpStatus.BAD_REQUEST);
        }
        String ref = form.getReference() == null ? "" : form.getReference().trim();
        if (ref.isEmpty()) {
            throw new BusinessException("Vui lòng nhập mã tham chiếu.", HttpStatus.BAD_REQUEST);
        }
        if (referenceUsed(ref)) {
            throw new BusinessException("Mã tham chiếu " + ref + " đã được dùng.", HttpStatus.CONFLICT);
        }
        Payment payment = new Payment();
        payment.setCode(temporaryCode());
        payment.setSalesContract(contract);
        payment.setReservation(contract.getReservation());
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPayerName(contract.getReservation().getInquiry().getFullName());
        payment.setReferenceCode(ref);
        payment.setMethod(method);
        payment.setPaidAt(Instant.now());
        payment.setRecordedByName(staffName);
        paymentRepository.save(payment);
        payment.setCode(generatePaymentCode(payment.getId()));

        if (remaining(contract).compareTo(BigDecimal.ZERO) == 0) {
            paymentRequestRepository.findBySalesContractId(contractId).ifPresent(pr -> pr.setStatus(PaymentStatus.PAID));
            contract.setRemainingAmount(BigDecimal.ZERO);
        } else {
            contract.setRemainingAmount(remaining(contract));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RefundView getRefund(Long reservationId) {
        Reservation reservation = reservationRepository.findDetailById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Khoản hoàn cọc", reservationId));
        if (!"REFUND".equalsIgnoreCase(nullToEmpty(reservation.getDepositDecision()))) {
            throw new ResourceNotFoundException("Khoản hoàn cọc", reservationId);
        }
        return toRefundDto(reservation);
    }

    @Override
    @Transactional
    public void processRefund(Long reservationId, RefundRequest form, String staffName) {
        Reservation reservation = reservationRepository.findDetailById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Khoản hoàn cọc", reservationId));
        if (!"REFUND".equalsIgnoreCase(nullToEmpty(reservation.getDepositDecision()))) {
            throw new ResourceNotFoundException("Khoản hoàn cọc", reservationId);
        }
        if (refundRepository.findByReservationId(reservationId).filter(r -> r.getStatus() == RefundStatus.PROCESSED).isPresent()) {
            throw new BusinessException("Phiếu này đã hoàn cọc.", HttpStatus.CONFLICT);
        }
        String method = form.getMethod();
        if (!METHODS.contains(method)) {
            throw new BusinessException("Vui lòng chọn phương thức.", HttpStatus.BAD_REQUEST);
        }
        String ref = form.getReference() == null ? "" : form.getReference().trim();
        if (ref.isEmpty()) {
            throw new BusinessException("Vui lòng nhập mã tham chiếu.", HttpStatus.BAD_REQUEST);
        }
        if (referenceUsed(ref)) {
            throw new BusinessException("Mã tham chiếu " + ref + " đã được dùng.", HttpStatus.CONFLICT);
        }
        Payment deposit = paymentRepository.findByReservationIdOrderByPaidAtAscIdAsc(reservationId).stream()
                .findFirst()
                .orElseGet(() -> createStandaloneDeposit(reservation));
        Refund refund = refundRepository.findByReservationId(reservationId).orElseGet(Refund::new);
        if (refund.getId() == null) {
            refund.setCode(temporaryCode());
            refund.setPayment(deposit);
            refund.setReservation(reservation);
            refund.setAmount(nz(reservation.getDepositAmount()));
        }
        refund.setStatus(RefundStatus.PROCESSED);
        refund.setReason(blankTo(form.getNote(), reservation.getCancelReason()));
        refund.setMethod(method);
        refund.setReferenceCode(ref);
        refund.setProcessedByName(staffName);
        refund.setProcessedAt(Instant.now());
        refundRepository.save(refund);
        if (refund.getCode().startsWith("TMP-")) {
            refund.setCode(generateRefundCode(refund.getId()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPaid(Long contractId) {
        SalesContract contract = loadContract(contractId);
        return sumCollected(contractId).compareTo(nz(contract.getSalePrice())) >= 0;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumCollected(Long contractId) {
        SalesContract contract = loadContract(contractId);
        BigDecimal fromPayments = paymentRepository.sumCollected(contractId);
        // Tiền cọc trên HĐ luôn tính vào đã thu (kể cả khi Payment cọc gắn reservation).
        boolean depositPaymentExists = paymentRepository.findBySalesContractIdOrderByPaidAtAscIdAsc(contractId).stream()
                .anyMatch(p -> "Đặt cọc".equals(p.getMethod()));
        if (!depositPaymentExists) {
            fromPayments = fromPayments.add(nz(contract.getDepositAmount()));
        }
        return fromPayments;
    }

    private List<PaymentDealRow> allDealRows() {
        List<PaymentDealRow> rows = new ArrayList<>();
        salesContractRepository.findAll().stream()
                .filter(c -> paymentRequestRepository.existsBySalesContractId(c.getId()))
                .map(c -> salesContractRepository.findDetailById(c.getId()).orElse(c))
                .sorted(Comparator.comparing(SalesContract::getId).reversed())
                .forEach(c -> rows.add(toDto(c)));
        reservationRepository.findRefundable().forEach(r -> rows.add(toDealDto(toRefundDto(r))));
        return rows;
    }

    private PaymentDealRow toDto(SalesContract contract) {
        long sale = money(contract.getSalePrice());
        long collected = money(sumCollected(contract.getId()));
        long remaining = Math.max(sale - collected, 0);
        return PaymentDealRow.builder()
                .id(contract.getId())
                .kind("payment")
                .code(generatePaymentCode(contract.getId()))
                .reference(contract.getCode())
                .contractCode(contract.getCode())
                .buyer(contract.getReservation().getInquiry().getFullName())
                .phone(contract.getReservation().getInquiry().getPhone())
                .car(contract.getReservation().getVehicle().getDisplayLabel())
                .plate(nullToEmpty(contract.getReservation().getVehicle().getLicensePlate()))
                .file(fileCode(contract))
                .staff(staffName(contract.getReservation()))
                .owner(ownerName(contract))
                .amount(sale)
                .collected(collected)
                .remaining(remaining)
                .status(remaining == 0 ? "PAID" : "PARTIAL")
                .created(iso(contract.getCreatedAt()))
                .reason("")
                .build();
    }

    private PaymentDealRow toDealDto(RefundView view) {
        return PaymentDealRow.builder()
                .id(view.getId())
                .kind("refund")
                .code(view.getCode())
                .reference(view.getReference())
                .contractCode(view.getContractCode())
                .buyer(view.getBuyer())
                .phone(view.getPhone())
                .car(view.getCar())
                .plate(view.getPlate())
                .file(view.getFile())
                .staff(view.getStaff())
                .owner(view.getOwner())
                .amount(view.getAmount())
                .collected(0)
                .remaining(0)
                .status(view.getStatus())
                .created(view.getCreated())
                .reason(view.getReason())
                .build();
    }

    private RefundView toRefundDto(Reservation reservation) {
        Refund done = refundRepository.findByReservationId(reservation.getId()).orElse(null);
        boolean processed = done != null && done.getStatus() == RefundStatus.PROCESSED;
        Map<String, Object> doneMap = null;
        if (processed) {
            doneMap = new LinkedHashMap<>();
            doneMap.put("method", done.getMethod());
            doneMap.put("reference", done.getReferenceCode());
            doneMap.put("note", nullToEmpty(done.getReason()));
            doneMap.put("paidOn", day(done.getProcessedAt()));
            doneMap.put("staff", nullToEmpty(done.getProcessedByName()));
        }
        return RefundView.builder()
                .id(reservation.getId())
                .kind("refund")
                .code(generateRefundCode(reservation.getId()))
                .reference(reservation.getCode())
                .contractCode(reservation.getCode())
                .buyer(reservation.getInquiry().getFullName())
                .phone(reservation.getInquiry().getPhone())
                .car(reservation.getVehicle().getDisplayLabel())
                .plate(nullToEmpty(reservation.getVehicle().getLicensePlate()))
                .file("")
                .staff(staffName(reservation))
                .owner("")
                .amount(money(reservation.getDepositAmount()))
                .collected(0)
                .remaining(0)
                .status(processed ? "REFUNDED" : "REFUND_PENDING")
                .created(iso(reservation.getCreatedAt()))
                .reason(blankTo(reservation.getCancelReason(), "Hết hạn giữ xe"))
                .done(doneMap)
                .build();
    }

    private void ensureDepositPayment(SalesContract contract) {
        Long reservationId = contract.getReservation().getId();
        boolean exists = paymentRepository.findByReservationIdOrderByPaidAtAscIdAsc(reservationId).stream()
                .anyMatch(p -> "Đặt cọc".equals(p.getMethod()));
        if (exists) {
            paymentRepository.findByReservationIdOrderByPaidAtAscIdAsc(reservationId).forEach(p -> {
                if (p.getSalesContract() == null) {
                    p.setSalesContract(contract);
                }
            });
            return;
        }
        Payment deposit = new Payment();
        deposit.setCode(temporaryCode());
        deposit.setSalesContract(contract);
        deposit.setReservation(contract.getReservation());
        deposit.setAmount(nz(contract.getDepositAmount()));
        deposit.setStatus(PaymentStatus.PAID);
        deposit.setPayerName(contract.getReservation().getInquiry().getFullName());
        deposit.setReferenceCode(contract.getReservation().getCode());
        deposit.setMethod("Đặt cọc");
        deposit.setPaidAt(contract.getCreatedAt());
        deposit.setRecordedByName(staffName(contract.getReservation()));
        paymentRepository.save(deposit);
        deposit.setCode(generatePaymentCode(deposit.getId()));
    }

    private Payment createStandaloneDeposit(Reservation reservation) {
        Payment deposit = new Payment();
        deposit.setCode(temporaryCode());
        deposit.setReservation(reservation);
        deposit.setAmount(nz(reservation.getDepositAmount()));
        deposit.setStatus(PaymentStatus.PAID);
        deposit.setPayerName(reservation.getInquiry().getFullName());
        deposit.setReferenceCode(reservation.getCode());
        deposit.setMethod("Đặt cọc");
        deposit.setPaidAt(reservation.getCreatedAt());
        deposit.setRecordedByName(staffName(reservation));
        paymentRepository.save(deposit);
        deposit.setCode(generatePaymentCode(deposit.getId()));
        return deposit;
    }

    private BigDecimal remaining(SalesContract contract) {
        return nz(contract.getSalePrice()).subtract(sumCollected(contract.getId())).max(BigDecimal.ZERO);
    }

    private boolean referenceUsed(String reference) {
        return paymentRepository.existsByReferenceCodeIgnoreCase(reference)
                || refundRepository.existsByReferenceCodeIgnoreCase(reference);
    }

    private SalesContract loadContract(Long id) {
        return salesContractRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hợp đồng bán xe", id));
    }

    private String fileCode(SalesContract contract) {
        return consignmentRepository.findByVehicleIdOrderByIdDesc(contract.getReservation().getVehicle().getId()).stream()
                .findFirst()
                .map(Consignment::getCode)
                .orElse("");
    }

    private String ownerName(SalesContract contract) {
        return consignmentRepository.findByVehicleIdOrderByIdDesc(contract.getReservation().getVehicle().getId()).stream()
                .findFirst()
                .map(c -> c.getConsignor().getFullName())
                .orElse("");
    }

    private static boolean match(
            PaymentDealRow r, String q, String status, String staff, String from, String to,
            Long amountMin, Long amountMax) {
        String lower = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        if (!lower.isEmpty()) {
            String hay = Stream.of(r.getCode(), r.getReference(), r.getContractCode(), r.getFile(), r.getCar(),
                            r.getPlate(), r.getBuyer(), r.getPhone())
                    .filter(Objects::nonNull).map(String::valueOf).reduce((a, b) -> a + " " + b).orElse("")
                    .toLowerCase(Locale.ROOT);
            if (!hay.contains(lower)) {
                return false;
            }
        }
        if (status != null && !status.isBlank() && !status.equals(r.getStatus())) {
            return false;
        }
        if (staff != null && !staff.isBlank() && !staff.equals(r.getStaff())) {
            return false;
        }
        String day = r.getCreated() == null || r.getCreated().length() < 10 ? "" : r.getCreated().substring(0, 10);
        if (from != null && !from.isBlank() && day.compareTo(from) < 0) {
            return false;
        }
        if (to != null && !to.isBlank() && day.compareTo(to) > 0) {
            return false;
        }
        if (amountMin != null && r.getAmount() < amountMin) {
            return false;
        }
        return amountMax == null || r.getAmount() <= amountMax;
    }

    private static Map<String, String> meta(String label, String tone, String icon, String pill) {
        return Map.of("label", label, "tone", tone, "icon", icon, "pill", pill);
    }

    private static Map<String, String> step(String label, String note, boolean passed) {
        return Map.of("label", label, "note", note, "passed", String.valueOf(passed));
    }

    private static String staffName(Reservation reservation) {
        return reservation.getAssignedTo() != null ? reservation.getAssignedTo().getFullName() : "";
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static long money(BigDecimal value) {
        return nz(value).longValue();
    }

    private static String day(Instant instant) {
        return instant == null ? "" : LocalDate.ofInstant(instant, ZONE).toString();
    }

    private static String iso(Instant instant) {
        return instant == null ? "" : LocalDateTime.ofInstant(instant, ZONE).withNano(0).toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String generatePaymentRequestCode(Long id) {
        return String.format("PR-%06d", id);
    }

    private static String generatePaymentCode(Long id) {
        return String.format("TT-%06d", id);
    }

    private static String generateRefundCode(Long id) {
        return String.format("HC-%06d", id);
    }

    private static String temporaryCode() {
        return "TMP-" + UUID.randomUUID().toString().substring(0, 12);
    }
}