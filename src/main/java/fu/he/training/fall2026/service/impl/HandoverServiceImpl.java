package fu.he.training.fall2026.service.impl;

import static fu.he.training.fall2026.service.impl.PaymentServiceImpl.blankToNull;
import static fu.he.training.fall2026.service.impl.PaymentServiceImpl.hasText;
import static fu.he.training.fall2026.service.impl.PaymentServiceImpl.money;
import static fu.he.training.fall2026.service.impl.PaymentServiceImpl.toLocal;

import fu.he.training.fall2026.dto.finance.ContractSummary;
import fu.he.training.fall2026.dto.finance.HistoryEntry;
import fu.he.training.fall2026.dto.finance.RuleCheck;
import fu.he.training.fall2026.dto.handover.HandoverChecklistRequest;
import fu.he.training.fall2026.dto.handover.HandoverItemForm;
import fu.he.training.fall2026.dto.handover.HandoverQueueResponse;
import fu.he.training.fall2026.dto.handover.HandoverResponse;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.TransferCase;
import fu.he.training.fall2026.entity.consignment.VehicleSaleNews;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.HandoverStatus;
import fu.he.training.fall2026.entity.enums.ListingStatus;
import fu.he.training.fall2026.entity.enums.ReservationStatus;
import fu.he.training.fall2026.entity.enums.TransferStatus;
import fu.he.training.fall2026.entity.sales.Handover;
import fu.he.training.fall2026.entity.sales.HandoverItem;
import fu.he.training.fall2026.entity.sales.SalesContract;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.HandoverItemRepository;
import fu.he.training.fall2026.repository.HandoverRepository;
import fu.he.training.fall2026.repository.SaleLookupRepository;
import fu.he.training.fall2026.service.AuditService;
import fu.he.training.fall2026.service.HandoverService;
import fu.he.training.fall2026.service.SaleLedgerService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HandoverServiceImpl implements HandoverService {

    private static final String AUDIT_TARGET = "Handover";
    private static final List<ContractStatus> HANDOVER_CONTRACTS = List.of(ContractStatus.SIGNED, ContractStatus.ACTIVE);
    private static final List<String> DEFAULT_ITEMS = List.of(
            "Chìa khóa xe",
            "Giấy đăng ký xe / giấy hẹn sang tên",
            "Sổ đăng kiểm",
            "Sổ bảo dưỡng / bảo hành",
            "Bộ dụng cụ & lốp dự phòng");
    /** Listing states that become SOLD when the vehicle is handed over. */
    private static final List<ListingStatus> OPEN_LISTINGS =
            List.of(ListingStatus.DRAFT, ListingStatus.READY, ListingStatus.LISTED, ListingStatus.PAUSED);

    private final HandoverRepository handoverRepository;
    private final HandoverItemRepository handoverItemRepository;
    private final SaleLookupRepository saleLookupRepository;
    private final SaleLedgerService saleLedgerService;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<HandoverQueueResponse> queue() {
        Map<Long, Handover> handovers = handoverRepository.findAllWithContract().stream()
                .collect(Collectors.toMap(h -> h.getSalesContract().getId(), Function.identity()));
        return saleLookupRepository.findOpenContracts(HANDOVER_CONTRACTS).stream()
                .map(contract -> {
                    Handover h = handovers.get(contract.getId());
                    return HandoverQueueResponse.builder()
                            .contract(saleLedgerService.summarize(contract))
                            .handoverId(h != null ? h.getId() : null)
                            .handoverCode(h != null ? h.getCode() : null)
                            .handoverStatus(h != null ? h.getStatus() : null)
                            .transferStatus(saleLookupRepository.findTransferCase(contract.getId())
                                    .map(TransferCase::getStatus).orElse(null))
                            .checks(blockerChecks(contract))
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public Long open(Long contractId, Long actorUserId) {
        SalesContract contract = saleLookupRepository.findContract(contractId)
                .orElseThrow(() -> new BusinessException("Hợp đồng bán không tồn tại."));
        if (!HANDOVER_CONTRACTS.contains(contract.getStatus()) || contract.isSaleClosed()) {
            throw new BusinessException("Hợp đồng " + contract.getCode() + " chưa ký hoặc đã đóng.", HttpStatus.CONFLICT);
        }
        if (handoverRepository.existsBySalesContractId(contractId)) {
            throw new BusinessException("Hợp đồng " + contract.getCode() + " đã có biên bản bàn giao.", HttpStatus.CONFLICT);
        }
        Handover handover = new Handover();
        handover.setCode("TMP-" + UUID.randomUUID().toString().substring(0, 12)); // replaced by BG-{id} below
        handover.setSalesContract(contract);
        handover.setStatus(readiness(contract));
        handoverRepository.save(handover);
        handover.setCode("BG-" + handover.getId());
        for (String name : DEFAULT_ITEMS) {
            HandoverItem item = new HandoverItem();
            item.setHandover(handover);
            item.setItemName(name);
            handoverItemRepository.save(item);
        }
        auditService.record(actorUserId, "OPENED", AUDIT_TARGET, handover.getId(),
                "Mở biên bản bàn giao cho " + contract.getCode() + " (" + handover.getStatus() + ").");
        return handover.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public HandoverResponse get(Long id) {
        Handover h = load(id);
        SalesContract contract = h.getSalesContract();
        return HandoverResponse.builder()
                .id(h.getId())
                .code(h.getCode())
                .status(h.getStatus())
                .contract(saleLedgerService.summarize(contract))
                .consignorName(saleLedgerService.consignmentOf(contract)
                        .map(c -> c.getConsignor().getFullName()).orElse(null))
                .intakeOdometerKm(contract.getReservation().getVehicle().getOdometerKm())
                .odometerKm(h.getOdometerKm())
                .keyCount(h.getKeyCount())
                .accessories(h.getAccessories())
                .photoPaths(h.getPhotoPaths())
                .receiverSignaturePath(h.getReceiverSignaturePath())
                .staffSignaturePath(h.getStaffSignaturePath())
                .handedOverAt(toLocal(h.getHandedOverAt()))
                .receiptIssued(h.isReceiptIssued())
                .saleCloseChecked(h.isSaleCloseChecked())
                .items(items(id))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public HandoverChecklistRequest formFor(Long id) {
        Handover h = load(id);
        HandoverChecklistRequest form = new HandoverChecklistRequest();
        form.setId(h.getId());
        form.setOdometerKm(h.getOdometerKm());
        form.setKeyCount(h.getKeyCount());
        form.setAccessories(h.getAccessories());
        form.setPhotoPaths(h.getPhotoPaths());
        form.setReceiverSignaturePath(h.getReceiverSignaturePath());
        form.setStaffSignaturePath(h.getStaffSignaturePath());
        form.setItems(new ArrayList<>(items(id)));
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleCheck> completionChecks(Long id) {
        Handover h = load(id);
        Integer intakeOdo = h.getSalesContract().getReservation().getVehicle().getOdometerKm();
        boolean odoOk = h.getOdometerKm() != null && (intakeOdo == null || h.getOdometerKm() >= intakeOdo);
        List<HandoverItem> missing = handoverItemRepository.findByHandoverIdOrderById(id).stream()
                .filter(item -> !item.isPresent() && !hasText(item.getNotes()))
                .toList();
        List<RuleCheck> checks = new ArrayList<>(blockerChecks(h.getSalesContract()));
        checks.add(RuleCheck.of("Đã ghi số km khi giao", odoOk, h.getOdometerKm() == null
                ? "Chưa nhập số km."
                : "Số km khi giao thấp hơn số km lúc tiếp nhận (" + intakeOdo + " km)."));
        checks.add(RuleCheck.of("Đã ghi số chìa khóa", h.getKeyCount() != null && h.getKeyCount() > 0,
                "Cần ít nhất 1 chìa khóa."));
        checks.add(RuleCheck.of("Có chữ ký người nhận", hasText(h.getReceiverSignaturePath()), "Thiếu chữ ký người mua."));
        checks.add(RuleCheck.of("Có chữ ký nhân viên giao", hasText(h.getStaffSignaturePath()), "Thiếu chữ ký nhân viên bàn giao."));
        checks.add(RuleCheck.of("Hạng mục thiếu đã có ghi chú", missing.isEmpty(), "Chưa tick hoặc ghi chú: "
                + missing.stream().map(HandoverItem::getItemName).collect(Collectors.joining(", ")) + "."));
        return checks;
    }

    @Override
    @Transactional
    public void saveChecklist(HandoverChecklistRequest form, Long actorUserId) {
        Handover h = load(form.getId());
        requireNotSigned(h);
        h.setOdometerKm(form.getOdometerKm());
        h.setKeyCount(form.getKeyCount());
        h.setAccessories(blankToNull(form.getAccessories()));
        h.setPhotoPaths(blankToNull(form.getPhotoPaths()));
        h.setReceiverSignaturePath(blankToNull(form.getReceiverSignaturePath()));
        h.setStaffSignaturePath(blankToNull(form.getStaffSignaturePath()));
        Map<Long, HandoverItem> items = handoverItemRepository.findByHandoverIdOrderById(h.getId()).stream()
                .collect(Collectors.toMap(HandoverItem::getId, Function.identity()));
        for (HandoverItemForm line : form.getItems()) {
            HandoverItem item = line.getId() == null ? null : items.get(line.getId());
            if (item != null) { // ignore ids that do not belong to this handover
                item.setPresent(line.isPresent());
                item.setNotes(blankToNull(line.getNotes()));
            }
        }
        h.setStatus(readiness(h.getSalesContract()));
        auditService.record(actorUserId, "CHECKLIST_SAVED", AUDIT_TARGET, h.getId(),
                "Odo " + h.getOdometerKm() + " km, " + h.getKeyCount() + " chìa khóa.");
    }

    @Override
    @Transactional
    public void complete(Long id, Long actorUserId) {
        Handover h = load(id);
        requireNotSigned(h);
        List<String> failed = completionChecks(id).stream()
                .filter(check -> !check.isPassed())
                .map(check -> check.getLabel() + ": " + check.getDetail())
                .toList();
        if (!failed.isEmpty()) {
            throw new BusinessException("Chưa thể hoàn tất bàn giao: " + String.join(" ", failed), HttpStatus.CONFLICT);
        }
        h.setStatus(HandoverStatus.COMPLETED);
        h.setHandedOverAt(Instant.now());
        h.setReceiptIssued(true);
        saleLedgerService.consignmentOf(h.getSalesContract()).ifPresent(consignment -> {
            consignment.setStatus(ConsignmentStatus.SOLD);
            for (VehicleSaleNews listing : saleLookupRepository.findListings(consignment.getId(), OPEN_LISTINGS)) {
                listing.setStatus(ListingStatus.SOLD);
                listing.setStoppedAt(Instant.now());
            }
        });
        auditService.record(actorUserId, "COMPLETED", AUDIT_TARGET, id,
                "Đã giao xe, phát hành biên nhận; tin đăng và hồ sơ ký gửi chuyển SOLD.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleCheck> closeChecks(Long id) {
        Handover h = load(id);
        ContractSummary contract = saleLedgerService.summarize(h.getSalesContract());
        Optional<TransferCase> transfer = saleLookupRepository.findTransferCase(contract.getId());
        boolean handedOver = h.getStatus() == HandoverStatus.COMPLETED || h.getStatus() == HandoverStatus.CLOSED;
        return List.of(
                RuleCheck.of("Thanh toán hoàn tất", contract.isFullyPaid(),
                        "Còn thiếu " + money(contract.getOutstandingAmount()) + " tiền đã đối soát."),
                RuleCheck.of("Sang tên hoàn tất", transfer.map(t -> t.getStatus() == TransferStatus.COMPLETED).orElse(false),
                        transfer.map(t -> "Hồ sơ " + t.getCode() + " đang " + t.getStatus() + ".")
                                .orElse("Chưa có hồ sơ sang tên.")),
                RuleCheck.of("Bàn giao thực tế hoàn tất", handedOver, "Biên bản " + h.getCode() + " chưa được ký hoàn tất."));
    }

    @Override
    @Transactional
    public void closeSale(Long id, String note, Long actorUserId) {
        Handover h = load(id);
        if (h.getStatus() != HandoverStatus.COMPLETED) {
            throw new BusinessException("Chỉ đóng sale sau khi bàn giao đã hoàn tất và chưa đóng.", HttpStatus.CONFLICT);
        }
        List<String> failed = closeChecks(id).stream()
                .filter(check -> !check.isPassed())
                .map(check -> check.getLabel() + ": " + check.getDetail())
                .toList();
        if (!failed.isEmpty()) {
            throw new BusinessException("Chưa thể đóng sale: " + String.join(" ", failed), HttpStatus.CONFLICT);
        }
        SalesContract contract = h.getSalesContract();
        h.setStatus(HandoverStatus.CLOSED);
        h.setSaleCloseChecked(true);
        contract.setStatus(ContractStatus.CLOSED);
        contract.setSaleClosed(true);
        if (contract.getReservation().getStatus() == ReservationStatus.ACTIVE) {
            contract.getReservation().setStatus(ReservationStatus.CONVERTED);
        }
        auditService.record(actorUserId, "SALE_CLOSED", AUDIT_TARGET, id,
                "Đóng sale " + contract.getCode() + (hasText(note) ? " · " + note.trim() : ""));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoryEntry> history(Long id) {
        return auditService.history(AUDIT_TARGET, id);
    }

    /** BR-08: no handover before the payment milestone and the agreed document milestone. */
    private List<RuleCheck> blockerChecks(SalesContract contract) {
        ContractSummary summary = saleLedgerService.summarize(contract);
        Optional<TransferCase> transfer = saleLookupRepository.findTransferCase(contract.getId());
        boolean docsReady = transfer
                .map(t -> t.getStatus() == TransferStatus.IN_PROGRESS || t.getStatus() == TransferStatus.COMPLETED)
                .orElse(false);
        Optional<Consignment> consignment = saleLedgerService.consignmentOf(contract);
        boolean notWithdrawn = consignment.map(c -> c.getStatus() != ConsignmentStatus.WITHDRAWN).orElse(true);
        return List.of(
                RuleCheck.of("Đã thu đủ tiền (đã đối soát)", summary.isFullyPaid(),
                        "Còn thiếu " + money(summary.getOutstandingAmount()) + "."),
                RuleCheck.of("Giấy tờ sang tên đã nộp", docsReady, transfer
                        .map(t -> "Hồ sơ sang tên " + t.getCode() + " đang " + t.getStatus() + ".")
                        .orElse("Chưa có hồ sơ sang tên cho hợp đồng.")),
                RuleCheck.of("Hồ sơ ký gửi chưa bị rút", notWithdrawn,
                        "Hồ sơ " + consignment.map(Consignment::getCode).orElse("") + " đã WITHDRAWN; không thể bàn giao."));
    }

    private HandoverStatus readiness(SalesContract contract) {
        return blockerChecks(contract).stream().allMatch(RuleCheck::isPassed) ? HandoverStatus.READY : HandoverStatus.BLOCKED;
    }

    private List<HandoverItemForm> items(Long handoverId) {
        return handoverItemRepository.findByHandoverIdOrderById(handoverId).stream()
                .map(item -> new HandoverItemForm(item.getId(), item.getItemName(), item.isPresent(), item.getNotes()))
                .toList();
    }

    private Handover load(Long id) {
        return handoverRepository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException("Biên bản bàn giao", id));
    }

    private static void requireNotSigned(Handover h) {
        if (h.getStatus() == HandoverStatus.COMPLETED || h.getStatus() == HandoverStatus.CLOSED) {
            throw new BusinessException("Biên bản " + h.getCode() + " đã ký, không được sửa.", HttpStatus.CONFLICT);
        }
    }
}
