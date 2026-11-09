package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.documents.TransferCheckResponse;
import fu.he.training.fall2026.dto.documents.TransferResponse;
import fu.he.training.fall2026.dto.storage.StoredFile;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.TransferCase;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.HandoverStatus;
import fu.he.training.fall2026.entity.enums.InquiryStatus;
import fu.he.training.fall2026.entity.enums.PaymentStatus;
import fu.he.training.fall2026.entity.enums.ReservationStatus;
import fu.he.training.fall2026.entity.enums.TransferStatus;
import fu.he.training.fall2026.entity.sales.Inquiry;
import fu.he.training.fall2026.entity.sales.Reservation;
import fu.he.training.fall2026.entity.sales.SalesContract;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import fu.he.training.fall2026.repository.HandoverRepository;
import fu.he.training.fall2026.repository.PaymentRepository;
import fu.he.training.fall2026.repository.SalesContractRepository;
import fu.he.training.fall2026.repository.TransferCaseRepository;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.service.FileStorageService;
import fu.he.training.fall2026.service.TransferService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
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
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private static final int DEFAULT_DEADLINE_DAYS = 30;
    private static final List<TransferStatus> FLOW = List.of(
            TransferStatus.PENDING, TransferStatus.IN_PROGRESS, TransferStatus.COMPLETED);

    private final TransferCaseRepository transferCaseRepository;
    private final SalesContractRepository salesContractRepository;
    private final ConsignmentRepository consignmentRepository;
    private final PaymentRepository paymentRepository;
    private final HandoverRepository handoverRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Override
    public Map<String, Map<String, String>> statusMeta() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put(TransferStatus.PENDING.name(), meta("Chờ xử lý", "slate", "hourglass", "draft"));
        map.put(TransferStatus.IN_PROGRESS.name(), meta("Đang xử lý", "blue", "refresh", "signed"));
        map.put(TransferStatus.COMPLETED.name(), meta("Hoàn tất", "green", "check", "listed"));
        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransferResponse> search(
            String keyword,
            TransferStatus status,
            String staff,
            LocalDate from,
            LocalDate to,
            Long amountMinMillion,
            Long amountMaxMillion,
            int page,
            int size) {
        String pattern = keyword == null || keyword.isBlank()
                ? "%"
                : "%" + keyword.trim().toLowerCase() + "%";
        String staffFilter = staff == null || staff.isBlank() ? null : staff.trim();
        BigDecimal min = amountMinMillion == null ? null : BigDecimal.valueOf(amountMinMillion * 1_000_000L);
        BigDecimal max = amountMaxMillion == null ? null : BigDecimal.valueOf(amountMaxMillion * 1_000_000L);
        return transferCaseRepository
                .search(
                        pattern,
                        status,
                        staffFilter,
                        from,
                        to,
                        min,
                        max,
                        PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "id")))
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<TransferStatus, Long> countByStatus() {
        Map<TransferStatus, Long> counts = new EnumMap<>(TransferStatus.class);
        for (TransferStatus value : FLOW) {
            counts.put(value, 0L);
        }
        for (Object[] row : transferCaseRepository.countByStatus()) {
            TransferStatus st = (TransferStatus) row[0];
            if (FLOW.contains(st)) {
                counts.put(st, (Long) row[1]);
            }
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listStaffNames() {
        return transferCaseRepository.findDistinctStaffNames();
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse get(Long id) {
        return toDto(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferResponse> candidates() {
        return salesContractRepository.findSignedWithoutTransfer(ContractStatus.CANCELLED).stream()
                .filter(this::isTransferReady)
                .map(sc -> toPreviewDto(sc, null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse preview(Long salesContractId) {
        SalesContract contract = loadContract(salesContractId);
        if (!isTransferReady(contract)) {
            throw new BusinessException(
                    "Hợp đồng " + contract.getCode() + " chưa đủ điều kiện tạo hồ sơ sang tên.",
                    HttpStatus.CONFLICT);
        }
        if (transferCaseRepository.existsBySalesContractId(salesContractId)) {
            throw new BusinessException("Hợp đồng này đã có hồ sơ sang tên.", HttpStatus.CONFLICT);
        }
        return toPreviewDto(contract, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferCheckResponse> transferChecks(Long salesContractId) {
        return buildTransferChecks(loadContract(salesContractId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferCheckResponse> saleMilestones(Long salesContractId) {
        boolean paid = paymentRepository.existsBySalesContractIdAndStatus(salesContractId, PaymentStatus.PAID);
        boolean transferred = transferCaseRepository
                .findBySalesContractId(salesContractId)
                .map(t -> t.getStatus() == TransferStatus.COMPLETED)
                .orElse(false);
        boolean handedOver = handoverRepository.existsBySalesContractIdAndStatusIn(
                salesContractId, List.of(HandoverStatus.COMPLETED, HandoverStatus.FINALIZED));
        return List.of(
                check("Thanh toán", paid ? "Đã thanh toán đủ" : "Chưa thanh toán đủ", paid),
                check("Sang tên", transferred ? "Đã sang tên" : "Chưa sang tên", transferred),
                check("Bàn giao xe", handedOver ? "Đã bàn giao" : "Chưa bàn giao", handedOver));
    }

    @Override
    public List<Map<String, String>> steps(TransferStatus status) {
        int current = FLOW.indexOf(status);
        boolean completed = status == TransferStatus.COMPLETED;
        List<Map<String, String>> steps = new ArrayList<>();
        Map<String, Map<String, String>> meta = statusMeta();
        for (int i = 0; i < FLOW.size(); i++) {
            String state = completed || i < current ? "done" : i == current ? "current" : "";
            steps.add(Map.of(
                    "label", meta.get(FLOW.get(i).name()).get("label"),
                    "state", state,
                    "number", String.valueOf(i + 1)));
        }
        return steps;
    }

    @Override
    @Transactional
    public Long create(Long salesContractId) {
        SalesContract contract = loadContract(salesContractId);
        if (transferCaseRepository.existsBySalesContractId(salesContractId)) {
            throw new BusinessException("Hợp đồng này đã có hồ sơ sang tên.", HttpStatus.CONFLICT);
        }
        if (!isTransferReady(contract)) {
            throw new BusinessException(
                    "Hợp đồng " + contract.getCode() + " chưa đủ điều kiện tạo hồ sơ sang tên.",
                    HttpStatus.CONFLICT);
        }
        if (contract.getSignedOn() == null) {
            throw new BusinessException("Hợp đồng chưa có ngày ký.", HttpStatus.CONFLICT);
        }
        TransferCase row = new TransferCase();
        row.setCode(temporaryCode());
        row.setSalesContract(contract);
        row.setStatus(TransferStatus.PENDING);
        row.setDeadline(contract.getSignedOn().plusDays(DEFAULT_DEADLINE_DAYS));
        row.setNotes("");
        transferCaseRepository.save(row);
        row.setCode(generateCode(contract.getId()));
        return row.getId();
    }

    @Override
    @Transactional
    public void start(Long id, Long actorUserId) {
        TransferCase row = load(id);
        requireStatus(row, TransferStatus.PENDING);
        if (actorUserId == null) {
            throw new BusinessException("Cần đăng nhập để nhận xử lý hồ sơ.", HttpStatus.UNAUTHORIZED);
        }
        User user = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", actorUserId));
        row.setStatus(TransferStatus.IN_PROGRESS);
        row.setResponsibleUser(user);
    }

    @Override
    @Transactional
    public void complete(Long id, String notes, MultipartFile evidence) {
        TransferCase row = load(id);
        requireStatus(row, TransferStatus.IN_PROGRESS);
        if (evidence == null || evidence.isEmpty()) {
            throw new BusinessException("Cần tải lên tệp bằng chứng trước khi hoàn tất sang tên.");
        }
        StoredFile stored = fileStorageService.uploadDocument(evidence, "transfers");
        row.setEvidencePath(stored.getPath());
        row.setEvidenceFileName(stored.getFileName());
        row.setNotes(notes == null || notes.isBlank() ? null : notes.trim());
        row.setStatus(TransferStatus.COMPLETED);
        row.setCompletedOn(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public String evidenceUrl(Long id) {
        TransferCase row = load(id);
        if (row.getEvidencePath() == null || row.getEvidencePath().isBlank()) {
            throw new ResourceNotFoundException("Tệp bằng chứng", id);
        }
        return fileStorageService.getUrl(row.getEvidencePath());
    }

    /** Mã hiển thị: ST-{salesContractId 6 số}. */
    private static String generateCode(Long salesContractId) {
        return String.format("ST-%06d", salesContractId);
    }

    private static String temporaryCode() {
        return "TMP-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private TransferCase load(Long id) {
        return transferCaseRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ sang tên", id));
    }

    private SalesContract loadContract(Long id) {
        return salesContractRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hợp đồng bán xe", id));
    }

    private static void requireStatus(TransferCase row, TransferStatus expected) {
        if (row.getStatus() != expected) {
            throw new BusinessException("Trạng thái hồ sơ không cho phép thao tác này.", HttpStatus.CONFLICT);
        }
    }

    private boolean isTransferReady(SalesContract contract) {
        return buildTransferChecks(contract).stream().allMatch(c -> "true".equals(c.getPassed()));
    }

    private List<TransferCheckResponse> buildTransferChecks(SalesContract contract) {
        Reservation reservation = contract.getReservation();
        Inquiry inquiry = reservation.getInquiry();
        Consignment consignment = latestConsignment(reservation.getVehicle().getId());
        boolean hasBuyer = inquiry.getStatus() == InquiryStatus.CONVERTED;
        boolean deposited = reservation.getStatus() == ReservationStatus.CONVERTED;
        boolean signed = contract.getSignedOn() != null && contract.getStatus() != ContractStatus.CANCELLED;
        boolean consignmentDeposited = false;
        if (consignment != null) {
            ConsignmentStatus st = consignment.getStatus();
            consignmentDeposited = st == ConsignmentStatus.RESERVED
                    || st == ConsignmentStatus.SOLD
                    || st == ConsignmentStatus.COMPLETED
                    || st == ConsignmentStatus.CLOSED;
        }
        String contactName = inquiry.getFullName() == null ? "" : inquiry.getFullName();
        String inquiryNote = inquiry.getCode() + " · " + contactName;
        String depositNote = reservation.getCode() + " · "
                + (reservation.getDepositAmount() == null
                        ? "0"
                        : reservation.getDepositAmount().toBigInteger() + " ₫");
        String consignmentNote = consignment == null
                ? "Chưa có hồ sơ ký gửi"
                : consignment.getCode() + " · " + consignment.getStatus().name();
        return List.of(
                check("Có khách mua xe", inquiryNote, hasBuyer),
                check("Đã đặt cọc giữ xe", depositNote, deposited),
                check("Hợp đồng bán đã ký", contract.getCode(), signed),
                check("Hồ sơ ký gửi đã đặt cọc", consignmentNote, consignmentDeposited));
    }

    private Consignment latestConsignment(Long vehicleId) {
        List<Consignment> list = consignmentRepository.findByVehicleIdOrderByIdDesc(vehicleId);
        return list.isEmpty() ? null : list.get(0);
    }

    private TransferResponse toDto(TransferCase t) {
        return toPreviewDto(t.getSalesContract(), t);
    }

    private TransferResponse toPreviewDto(SalesContract contract, TransferCase transfer) {
        Reservation reservation = contract.getReservation();
        Vehicle vehicle = reservation.getVehicle();
        Inquiry inquiry = reservation.getInquiry();
        Consignment consignment = latestConsignment(vehicle.getId());
        String salesStaff = "";
        if (inquiry.getAssignedTo() != null) {
            salesStaff = inquiry.getAssignedTo().getFullName();
        } else if (reservation.getAssignedTo() != null) {
            salesStaff = reservation.getAssignedTo().getFullName();
        }
        String staff = "";
        LocalDate deadline = contract.getSignedOn() == null
                ? LocalDate.now().plusDays(DEFAULT_DEADLINE_DAYS)
                : contract.getSignedOn().plusDays(DEFAULT_DEADLINE_DAYS);
        TransferStatus status = TransferStatus.PENDING;
        String code = generateCode(contract.getId());
        Long id = contract.getId();
        String notes = "";
        String evidence = "";
        String evidencePath = null;
        LocalDate completedOn = null;
        boolean hasEvidence = false;
        if (transfer != null) {
            id = transfer.getId();
            code = transfer.getCode();
            status = transfer.getStatus();
            deadline = transfer.getDeadline() != null ? transfer.getDeadline() : deadline;
            if (transfer.getResponsibleUser() != null) {
                staff = transfer.getResponsibleUser().getFullName();
            }
            notes = transfer.getNotes() == null ? "" : transfer.getNotes();
            evidence = transfer.getEvidenceFileName() == null ? "" : transfer.getEvidenceFileName();
            evidencePath = transfer.getEvidencePath();
            completedOn = transfer.getCompletedOn();
            hasEvidence = evidencePath != null && !evidencePath.isBlank();
        }
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
        long salePrice = contract.getSalePrice() == null ? 0L : contract.getSalePrice().longValue();
        String carLabel = vehicleLabel(vehicle);
        return TransferResponse.builder()
                .id(id)
                .code(code)
                .salesContractId(contract.getId())
                .contractCode(contract.getCode())
                .salesStaff(salesStaff)
                .signedOn(contract.getSignedOn())
                .salePrice(salePrice)
                .buyer(blankToEmpty(inquiry.getFullName()))
                .phone(blankToEmpty(inquiry.getPhone()))
                .buyerIdNumber(blankToEmpty(inquiry.getIdNumber()))
                .buyerAddress(blankToEmpty(inquiry.getAddress()))
                .car(carLabel)
                .file(consignment == null ? "" : consignment.getCode())
                .plate(blankToEmpty(vehicle.getLicensePlate()))
                .vin(vehicle.getVin())
                .owner(consignment == null ? "" : consignment.getConsignor().getFullName())
                .status(status.name())
                .staff(staff)
                .deadline(deadline)
                .daysLeft(daysLeft)
                .notes(notes)
                .evidence(evidence)
                .evidencePath(evidencePath)
                .completedOn(completedOn)
                .hasEvidenceFile(hasEvidence)
                .build();
    }

    private static String vehicleLabel(Vehicle v) {
        StringBuilder sb = new StringBuilder(v.getDisplayLabel());
        if (v.getYear() != null) {
            sb.append(' ').append(v.getYear());
        }
        return sb.toString().trim();
    }

    private static TransferCheckResponse check(String label, String note, boolean passed) {
        return TransferCheckResponse.builder()
                .label(label)
                .note(note)
                .passed(String.valueOf(passed))
                .build();
    }

    private static Map<String, String> meta(String label, String tone, String icon, String pill) {
        return Map.of("label", label, "tone", tone, "icon", icon, "pill", pill);
    }

    private static String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
