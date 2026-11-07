package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import fu.he.training.fall2026.dto.handover.HandoverConfirmRequest;
import fu.he.training.fall2026.dto.handover.HandoverFilter;
import fu.he.training.fall2026.dto.handover.HandoverView;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.HandoverStatus;
import fu.he.training.fall2026.entity.enums.TransferStatus;
import fu.he.training.fall2026.entity.sales.Handover;
import fu.he.training.fall2026.entity.sales.HandoverItem;
import fu.he.training.fall2026.entity.sales.SalesContract;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import fu.he.training.fall2026.repository.HandoverItemRepository;
import fu.he.training.fall2026.repository.HandoverRepository;
import fu.he.training.fall2026.repository.SalesContractRepository;
import fu.he.training.fall2026.repository.TransferCaseRepository;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.service.FileStorageService;
import fu.he.training.fall2026.service.HandoverService;
import fu.he.training.fall2026.service.PaymentService;
import fu.he.training.fall2026.service.SettlementService;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandoverServiceImpl implements HandoverService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);
    private static final String SIGNATURE_PREFIX = "data:image/png;base64,";

    private final HandoverRepository handoverRepository;
    private final HandoverItemRepository handoverItemRepository;
    private final SalesContractRepository salesContractRepository;
    private final TransferCaseRepository transferCaseRepository;
    private final ConsignmentRepository consignmentRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final SettlementService settlementService;
    private final FileStorageService fileStorageService;

    @Override
    public Map<String, Map<String, String>> statusMeta() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put("PENDING", meta("Chờ điều kiện", "slate", "hourglass", "draft"));
        map.put("READY", meta("Sẵn sàng", "blue", "car", "signed"));
        map.put("COMPLETED", meta("Đã bàn giao", "amber", "handshake", "pending"));
        map.put("FINALIZED", meta("Hoàn tất giao dịch", "green", "check", "listed"));
        return map;
    }

    @Override
    @Transactional
    public void ensureRowsForSignedContracts() {
        for (SalesContract contract : salesContractRepository.findAllSignedActive()) {
            Handover row = ensureLoaded(contract.getId());
            refreshComputedStatus(row);
        }
    }

    @Override
    @Transactional
    public Page<HandoverView> search(HandoverFilter filter, int page, Integer size) {
        ensureRowsForSignedContracts();
        HandoverFilter f = filter == null ? new HandoverFilter() : filter;
        String keyword = f.getQ();
        String pattern = keyword == null || keyword.isBlank()
                ? "%"
                : "%" + keyword.trim().toLowerCase() + "%";
        String staff = f.getStaff() == null || f.getStaff().isBlank() ? null : f.getStaff().trim();
        int pageSize = CatalogPageSupport.normalizeSize(size);
        return handoverRepository
                .search(
                        pattern,
                        parseStatus(f.getStatus()),
                        staff,
                        f.getFrom(),
                        f.getTo(),
                        millions(f.getAmountMin()),
                        millions(f.getAmountMax()),
                        PageRequest.of(Math.max(page, 0), pageSize, Sort.by(Sort.Direction.DESC, "id")))
                .map(this::toDto);
    }

    @Override
    @Transactional
    public Map<String, Long> countByStatus() {
        ensureRowsForSignedContracts();
        Map<String, Long> counts = new LinkedHashMap<>();
        statusMeta().keySet().forEach(k -> counts.put(k, 0L));
        for (Object[] row : handoverRepository.countByStatus()) {
            HandoverStatus st = (HandoverStatus) row[0];
            if (counts.containsKey(st.name())) {
                counts.put(st.name(), (Long) row[1]);
            }
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listStaffNames() {
        Set<String> names = new LinkedHashSet<>();
        names.addAll(handoverRepository.findAssignedStaffNames());
        names.addAll(handoverRepository.findHandedOverStaffNames());
        return new ArrayList<>(names);
    }

    @Override
    @Transactional
    public HandoverView getByContractId(Long contractId) {
        Handover handover = ensureLoaded(contractId);
        refreshComputedStatus(handover);
        return toDto(handover);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> saleMilestones(Long contractId) {
        return paymentService.saleMilestones(contractId);
    }

    @Override
    @Transactional
    public void confirm(Long contractId, HandoverConfirmRequest form, Long actorUserId, String actorName) {
        Handover handover = ensureLoaded(contractId);
        refreshComputedStatus(handover);
        if (handover.getStatus() != HandoverStatus.READY) {
            throw new BusinessException("Chỉ bàn giao khi đã thu đủ tiền và sang tên xong.", HttpStatus.CONFLICT);
        }
        Integer odometer = form.getOdo();
        Integer keyCount = form.getKeys();
        if (odometer == null) {
            throw new BusinessException("Vui lòng nhập số km lúc bàn giao.", HttpStatus.BAD_REQUEST);
        }
        if (keyCount == null || keyCount < 1) {
            throw new BusinessException("Vui lòng nhập số chìa khóa.", HttpStatus.BAD_REQUEST);
        }
        String staffSignature = form.getStaffSignature();
        String receiverSignature = form.getReceiverSignature();
        if (!isSignature(staffSignature) || !isSignature(receiverSignature)) {
            throw new BusinessException("Cần đủ chữ ký của nhân viên và khách nhận xe.", HttpStatus.BAD_REQUEST);
        }
        User actor = actorUserId == null ? null : userRepository.findById(actorUserId).orElse(null);
        String staffPath = storeSignature(staffSignature, "handover-staff");
        String receiverPath;
        try {
            receiverPath = storeSignature(receiverSignature, "handover-buyer");
        } catch (BusinessException ex) {
            deleteQuietly(staffPath);
            throw ex;
        }
        List<String> items = form.getItems();
        handover.setOdometerKm(odometer);
        handover.setKeyCount(keyCount);
        handover.setNotes(form.getNotes() == null ? "" : form.getNotes().trim());
        handover.setHandedOverBy(actor);
        handover.setHandedOverAt(Instant.now());
        handover.setStatus(HandoverStatus.COMPLETED);
        handover.setReceiptIssued(true);
        handover.setStaffSignaturePath(staffPath);
        handover.setReceiverSignaturePath(receiverPath);
        if (items != null) {
            handover.setAccessories(String.join(", ", items));
        }
        handoverItemRepository.deleteByHandoverId(handover.getId());
        List<String> chosen = items == null ? List.of() : items;
        for (String name : ITEMS) {
            HandoverItem item = new HandoverItem();
            item.setHandover(handover);
            item.setItemName(name);
            item.setPresent(chosen.contains(name));
            handoverItemRepository.save(item);
        }
        if (actorName != null && handover.getHandedOverBy() == null) {
            handover.setNotes(handover.getNotes() == null ? "" : handover.getNotes());
        }
    }

    @Override
    @Transactional
    public void closeSale(Long contractId) {
        Handover handover = ensureLoaded(contractId);
        refreshComputedStatus(handover);
        if (handover.getStatus() != HandoverStatus.COMPLETED) {
            throw new BusinessException("Chỉ đóng sale khi đã bàn giao xe.", HttpStatus.CONFLICT);
        }
        boolean paid = paymentService.isPaid(contractId);
        boolean transferred = isTransferred(contractId);
        if (!paid || !transferred) {
            throw new BusinessException("Chỉ đóng sale khi đã thu đủ tiền và sang tên xong.", HttpStatus.CONFLICT);
        }
        SalesContract contract = handover.getSalesContract();
        contract.setStatus(ContractStatus.COMPLETED);
        contract.setSaleClosed(true);
        handover.setStatus(HandoverStatus.FINALIZED);
        handover.setSaleCloseChecked(true);
        Consignment consignment = consignmentRepository
                .findByVehicleIdOrderByIdDesc(contract.getReservation().getVehicle().getId()).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("Không tìm thấy hồ sơ ký gửi.", HttpStatus.CONFLICT));
        consignment.setStatus(ConsignmentStatus.SOLD);
        settlementService.openFor(contract);
    }

    @Override
    @Transactional
    public HandoverView receipt(Long contractId) {
        HandoverView view = getByContractId(contractId);
        if (!view.isHandedOver()) {
            throw new BusinessException("Bàn giao xong mới có biên nhận.", HttpStatus.CONFLICT);
        }
        return view;
    }

    private Handover ensureLoaded(Long contractId) {
        return handoverRepository.findDetailByContractId(contractId).orElseGet(() -> {
            SalesContract contract = salesContractRepository.findDetailById(contractId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bàn giao", contractId));
            if (contract.getSignedOn() == null || contract.getStatus() == ContractStatus.CANCELLED) {
                throw new ResourceNotFoundException("Bàn giao", contractId);
            }
            Handover row = new Handover();
            row.setCode(temporaryCode());
            row.setSalesContract(contract);
            row.setStatus(HandoverStatus.PENDING);
            row.setReceiptIssued(false);
            row.setSaleCloseChecked(false);
            handoverRepository.save(row);
            row.setCode(generateCode(contractId));
            return row;
        });
    }

    private void refreshComputedStatus(Handover handover) {
        if (handover.getStatus() == HandoverStatus.COMPLETED || handover.getStatus() == HandoverStatus.FINALIZED) {
            return;
        }
        Long contractId = handover.getSalesContract().getId();
        boolean paid = paymentService.isPaid(contractId);
        boolean transferred = isTransferred(contractId);
        HandoverStatus next = paid && transferred ? HandoverStatus.READY : HandoverStatus.PENDING;
        if (handover.getStatus() != next) {
            handover.setStatus(next);
        }
    }

    private HandoverView toDto(Handover handover) {
        refreshComputedStatus(handover);
        SalesContract contract = handover.getSalesContract();
        boolean paid = paymentService.isPaid(contract.getId());
        boolean transferred = isTransferred(contract.getId());
        List<String> items = handoverItemRepository.findByHandoverId(handover.getId()).stream()
                .filter(HandoverItem::isPresent)
                .map(HandoverItem::getItemName)
                .toList();
        if (items.isEmpty() && handover.getAccessories() != null && !handover.getAccessories().isBlank()) {
            items = List.of(handover.getAccessories().split(",\\s*"));
        }
        String staff = handover.getHandedOverBy() != null
                ? handover.getHandedOverBy().getFullName()
                : (contract.getReservation().getAssignedTo() != null
                        ? contract.getReservation().getAssignedTo().getFullName() : "");
        Integer odo = handover.getOdometerKm() != null
                ? handover.getOdometerKm()
                : contract.getReservation().getVehicle().getOdometerKm();
        return HandoverView.builder()
                .id(contract.getId())
                .code(handover.getCode())
                .contractCode(contract.getCode())
                .car(contract.getReservation().getVehicle().getDisplayLabel())
                .plate(nullToEmpty(contract.getReservation().getVehicle().getLicensePlate()))
                .file(fileCode(contract))
                .km(contract.getReservation().getVehicle().getOdometerKm())
                .color(nullToEmpty(contract.getReservation().getVehicle().getColor()))
                .vin(contract.getReservation().getVehicle().getVin())
                .buyer(nullToEmpty(contract.getReservation().getInquiry().getFullName()))
                .phone(nullToEmpty(contract.getReservation().getInquiry().getPhone()))
                .buyerIdNumber(nullToEmpty(contract.getReservation().getInquiry().getIdNumber()))
                .buyerAddress(nullToEmpty(contract.getReservation().getInquiry().getAddress()))
                .signedOn(contract.getSignedOn() == null ? "" : contract.getSignedOn().toString())
                .salePrice(contract.getSalePrice() == null ? 0L : contract.getSalePrice().longValue())
                .paid(paid)
                .transferred(transferred)
                .handedOver(handover.getStatus() == HandoverStatus.COMPLETED
                        || handover.getStatus() == HandoverStatus.FINALIZED)
                .status(handover.getStatus().name())
                .staff(staff)
                .odo(odo == null ? 0 : odo)
                .keys(handover.getKeyCount() == null ? 2 : handover.getKeyCount())
                .items(items)
                .notes(nullToEmpty(handover.getNotes()))
                .handedOn(handover.getHandedOverAt() == null
                        ? ""
                        : LocalDate.ofInstant(handover.getHandedOverAt(), ZONE).toString())
                .staffSignature(url(handover.getStaffSignaturePath()))
                .receiverSignature(url(handover.getReceiverSignaturePath()))
                .build();
    }

    private boolean isTransferred(Long contractId) {
        return transferCaseRepository.findBySalesContractId(contractId)
                .map(t -> t.getStatus() == TransferStatus.COMPLETED)
                .orElse(false);
    }

    private String storeSignature(String dataUrl, String folder) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(dataUrl.substring(SIGNATURE_PREFIX.length()));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Chữ ký không hợp lệ, vui lòng ký lại.", HttpStatus.BAD_REQUEST);
        }
        return fileStorageService
                .uploadImage(new InMemoryMultipartFile("signature.png", "image/png", bytes), folder)
                .getPath();
    }

    private void deleteQuietly(String path) {
        try {
            fileStorageService.delete(path);
        } catch (BusinessException ex) {
            log.warn("Còn sót chữ ký trên kho tệp: {}", path);
        }
    }

    private String url(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        try {
            String resolved = fileStorageService.getUrl(path);
            return resolved == null ? path : resolved;
        } catch (RuntimeException ex) {
            return path;
        }
    }

    private String fileCode(SalesContract contract) {
        return consignmentRepository.findByVehicleIdOrderByIdDesc(contract.getReservation().getVehicle().getId())
                .stream().findFirst().map(Consignment::getCode).orElse("");
    }

    private static boolean isSignature(String value) {
        return value != null && value.startsWith(SIGNATURE_PREFIX);
    }

    private static HandoverStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return HandoverStatus.valueOf(raw.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static BigDecimal millions(Integer value) {
        return value == null ? null : MILLION.multiply(BigDecimal.valueOf(value.longValue()));
    }

    private static Map<String, String> meta(String label, String tone, String icon, String pill) {
        return Map.of("label", label, "tone", tone, "icon", icon, "pill", pill);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String generateCode(Long contractId) {
        return String.format("BG-%06d", contractId);
    }

    private static String temporaryCode() {
        return "TMP-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private static final class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String contentType;
        private final byte[] bytes;

        private InMemoryMultipartFile(String name, String contentType, byte[] bytes) {
            this.name = name;
            this.contentType = contentType;
            this.bytes = bytes;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), bytes);
        }
    }
}
