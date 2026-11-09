package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.constant.sales.SalesDealCatalog;
import fu.he.training.fall2026.dto.sales.DealListFilter;
import fu.he.training.fall2026.dto.sales.ReservationResponse;
import fu.he.training.fall2026.dto.sales.SalesContractForm;
import fu.he.training.fall2026.dto.sales.SalesContractResponse;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.HandoverStatus;
import fu.he.training.fall2026.entity.enums.ListingStatus;
import fu.he.training.fall2026.entity.enums.ReservationStatus;
import fu.he.training.fall2026.entity.sales.Inquiry;
import fu.he.training.fall2026.entity.sales.Reservation;
import fu.he.training.fall2026.entity.sales.SalesContract;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import fu.he.training.fall2026.repository.HandoverRepository;
import fu.he.training.fall2026.repository.PaymentRequestRepository;
import fu.he.training.fall2026.repository.ReservationRepository;
import fu.he.training.fall2026.repository.SalesContractRepository;
import fu.he.training.fall2026.repository.TransferCaseRepository;
import fu.he.training.fall2026.repository.VehicleSaleNewsRepository;
import fu.he.training.fall2026.service.ReservationService;
import fu.he.training.fall2026.service.SalesContractService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

@Service
@RequiredArgsConstructor
@Transactional
public class SalesContractServiceImpl implements SalesContractService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String SIGNATURE_PREFIX = "data:image/png;base64,";

    private final SalesContractRepository salesContractRepository;
    private final ReservationRepository reservationRepository;
    private final ConsignmentRepository consignmentRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final TransferCaseRepository transferCaseRepository;
    private final HandoverRepository handoverRepository;
    private final VehicleSaleNewsRepository newsRepository;
    private final ReservationService reservationService;

    @Override
    @Transactional(readOnly = true)
    public void bindList(Model model, String base, DealListFilter filter) {
        List<SalesContractResponse> all = salesContractRepository.findAllDetail().stream()
                .map(this::toDto)
                .toList();
        ReservationServiceImpl.bindFiltered(
                model, base, all, "salePrice", "created", SalesDealCatalog.CONTRACT_STATUSES, filter);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesContractResponse findById(Long id) {
        return toDto(require(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SalesContractResponse> findByReservationId(Long reservationId) {
        return salesContractRepository.findByReservationId(reservationId).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> activeReservations() {
        return reservationRepository.findAllActive().stream()
                .filter(r -> !salesContractRepository.existsByReservationId(r.getId()))
                .map(r -> reservationService.findById(r.getId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SalesContractResponse draftFromReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findDetailById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu đặt cọc", reservationId));
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new BusinessException("Chỉ lập được hợp đồng từ phiếu đặt cọc còn hiệu lực.", HttpStatus.CONFLICT);
        }
        if (salesContractRepository.existsByReservationId(reservationId)) {
            throw new BusinessException("Phiếu đặt cọc này đã có hợp đồng.", HttpStatus.CONFLICT);
        }
        return draftDto(reservation);
    }

    @Override
    public Long create(Long reservationId, SalesContractForm form) {
        Reservation reservation = reservationRepository.findDetailById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu đặt cọc", reservationId));
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new BusinessException("Chỉ lập được hợp đồng từ phiếu đặt cọc còn hiệu lực.", HttpStatus.CONFLICT);
        }
        if (salesContractRepository.existsByReservationId(reservationId)) {
            throw new BusinessException("Phiếu đặt cọc này đã có hợp đồng.", HttpStatus.CONFLICT);
        }

        SalesContract contract = new SalesContract();
        contract.setReservation(reservation);
        contract.setStatus(ContractStatus.DRAFT);
        contract.setCode("TMP");
        contract.setDepositAmount(reservation.getDepositAmount());
        contract.setSaleClosed(false);
        contract.setPaymentTerms(SalesDealCatalog.PAYMENT_TERMS);
        contract.setHandoverTerms(SalesDealCatalog.HANDOVER_TERMS);
        contract.setTerminationTerms(SalesDealCatalog.TERMINATION_TERMS);
        applyForm(contract, form, true);
        contract = salesContractRepository.save(contract);
        contract.setCode(String.format("HDBX-%06d", contract.getId()));
        // Phiếu cọc chỉ chuyển CONVERTED khi ký — lưu nháp vẫn giữ ACTIVE để hủy/hết hạn được.
        if (contract.getStatus() == ContractStatus.SIGNED) {
            reservation.setStatus(ReservationStatus.CONVERTED);
        }
        markSold(reservation.getVehicle().getId());
        return contract.getId();
    }

    @Override
    public void update(Long id, SalesContractForm form) {
        SalesContract contract = require(id);
        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new BusinessException("Hợp đồng đã ký nên không sửa được.", HttpStatus.CONFLICT);
        }
        applyForm(contract, form, true);
        if (contract.getStatus() == ContractStatus.SIGNED) {
            contract.getReservation().setStatus(ReservationStatus.CONVERTED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> steps(SalesContractResponse contract) {
        boolean signed = contract.getSignedOn() != null && !contract.getSignedOn().isBlank();
        List<Map<String, String>> steps = new ArrayList<>();
        steps.add(step("Tư vấn", contract.getInquiryCode(), "done"));
        steps.add(step("Đặt cọc giữ xe", contract.getReservationCode(), "done"));
        steps.add(step("Ký hợp đồng", signed ? "Đã ký hai bên" : "Chờ ký", signed ? "done" : "current"));
        steps.add(milestone("Thanh toán", contract.isPaid()));
        steps.add(milestone("Sang tên", contract.isTransferred()));
        steps.add(handoverStep(contract));
        return steps;
    }

    private void applyForm(SalesContract contract, SalesContractForm form, boolean allowSign) {
        Long salePrice = ReservationServiceImpl.parseMoney(form.getSalePrice());
        long deposit = contract.getDepositAmount() == null ? 0L : contract.getDepositAmount().longValue();
        if (salePrice == null || salePrice <= deposit) {
            throw new BusinessException("Giá bán phải lớn hơn tiền cọc đã đặt.", HttpStatus.BAD_REQUEST);
        }
        contract.setSalePrice(BigDecimal.valueOf(salePrice));
        contract.setRemainingAmount(BigDecimal.valueOf(salePrice - deposit));
        contract.setPaymentTerms(blankTo(form.getPaymentTerms(), SalesDealCatalog.PAYMENT_TERMS));
        contract.setHandoverTerms(blankTo(form.getHandoverTerms(), SalesDealCatalog.HANDOVER_TERMS));
        contract.setTerminationTerms(blankTo(form.getTerminationTerms(), SalesDealCatalog.TERMINATION_TERMS));

        Inquiry inquiry = contract.getReservation().getInquiry();
        if (form.getBuyerIdNumber() != null) {
            inquiry.setIdNumber(form.getBuyerIdNumber().trim());
        }
        if (form.getBuyerAddress() != null) {
            inquiry.setAddress(form.getBuyerAddress().trim());
        }

        if (!allowSign || !"sign".equals(form.getIntent())) {
            return;
        }
        String idNumber = blank(form.getBuyerIdNumber());
        if (!idNumber.matches("\\d{12}")) {
            throw new BusinessException("Số CCCD của người mua phải đủ 12 số.", HttpStatus.BAD_REQUEST);
        }
        if (blank(form.getBuyerAddress()).isEmpty()) {
            throw new BusinessException("Vui lòng nhập địa chỉ người mua.", HttpStatus.BAD_REQUEST);
        }
        String seller = blank(form.getSellerSignature());
        String buyer = blank(form.getBuyerSignature());
        if (!seller.startsWith(SIGNATURE_PREFIX) || !buyer.startsWith(SIGNATURE_PREFIX)) {
            throw new BusinessException("Hợp đồng cần đủ chữ ký của hai bên.", HttpStatus.BAD_REQUEST);
        }
        inquiry.setIdNumber(idNumber);
        inquiry.setAddress(form.getBuyerAddress().trim());
        contract.setSellerSignaturePath(seller);
        contract.setBuyerSignaturePath(buyer);
        contract.setStatus(ContractStatus.SIGNED);
        contract.setSignedOn(LocalDate.now(ZONE));
    }

    private SalesContractResponse draftDto(Reservation reservation) {
        SalesContractResponse dto = baseFromReservation(reservation);
        dto.setId(null);
        dto.setCode("HDBX-NHÁP");
        dto.setStatus(ContractStatus.DRAFT.name());
        dto.setCreated(LocalDateTime.now(ZONE).withSecond(0).withNano(0).toString());
        dto.setSignedOn("");
        dto.setBuyerIdNumber(nullToEmpty(reservation.getInquiry().getIdNumber()));
        dto.setBuyerAddress(nullToEmpty(reservation.getInquiry().getAddress()));
        dto.setPaymentTerms(SalesDealCatalog.PAYMENT_TERMS);
        dto.setHandoverTerms(SalesDealCatalog.HANDOVER_TERMS);
        dto.setTerminationTerms(SalesDealCatalog.TERMINATION_TERMS);
        dto.setSellerSignature("");
        dto.setBuyerSignature("");
        dto.setPaid(false);
        dto.setTransferred(false);
        dto.setHandedOver(false);
        dto.setPaymentRequested(false);
        return dto;
    }

    private SalesContractResponse toDto(SalesContract contract) {
        Reservation reservation = contract.getReservation();
        SalesContractResponse dto = baseFromReservation(reservation);
        dto.setId(contract.getId());
        dto.setCode(contract.getCode());
        dto.setStatus(contract.getStatus().name());
        dto.setCreated(LocalDateTime.ofInstant(contract.getCreatedAt(), ZONE).withSecond(0).withNano(0).toString());
        dto.setSignedOn(contract.getSignedOn() == null ? "" : contract.getSignedOn().toString());
        long sale = contract.getSalePrice() == null ? dto.getListedPrice() : contract.getSalePrice().longValue();
        long deposit = contract.getDepositAmount() == null ? 0L : contract.getDepositAmount().longValue();
        dto.setSalePrice(sale);
        dto.setDeposit(deposit);
        dto.setRemaining(contract.getRemainingAmount() == null ? sale - deposit : contract.getRemainingAmount().longValue());
        Inquiry inquiry = reservation.getInquiry();
        dto.setBuyerIdNumber(nullToEmpty(inquiry.getIdNumber()));
        dto.setBuyerAddress(nullToEmpty(inquiry.getAddress()));
        dto.setPaymentTerms(blankTo(contract.getPaymentTerms(), SalesDealCatalog.PAYMENT_TERMS));
        dto.setHandoverTerms(blankTo(contract.getHandoverTerms(), SalesDealCatalog.HANDOVER_TERMS));
        dto.setTerminationTerms(blankTo(contract.getTerminationTerms(), SalesDealCatalog.TERMINATION_TERMS));
        dto.setSellerSignature(nullToEmpty(contract.getSellerSignaturePath()));
        dto.setBuyerSignature(nullToEmpty(contract.getBuyerSignaturePath()));

        boolean paymentRequested = paymentRequestRepository.existsBySalesContractId(contract.getId());
        boolean paid = paymentRequested && paymentRequestRepository.findBySalesContractId(contract.getId())
                .map(pr -> pr.getStatus() == fu.he.training.fall2026.entity.enums.PaymentStatus.PAID)
                .orElse(false);
        boolean transferred = transferCaseRepository.findBySalesContractId(contract.getId())
                .map(t -> t.getCompletedOn() != null)
                .orElse(false);
        String handoverStatus = handoverRepository.findBySalesContractId(contract.getId())
                .map(h -> h.getStatus().name())
                .orElse("");
        if (("".equals(handoverStatus) || "PENDING".equals(handoverStatus)) && paid && transferred) {
            handoverStatus = "READY";
        }
        boolean handed = "COMPLETED".equals(handoverStatus) || "FINALIZED".equals(handoverStatus);

        dto.setPaymentRequested(paymentRequested);
        dto.setPaid(paid);
        dto.setTransferred(transferred);
        dto.setHandoverStatus(handoverStatus);
        dto.setHandedOver(handed);
        return dto;
    }

    private SalesContractResponse baseFromReservation(Reservation reservation) {
        Inquiry inquiry = reservation.getInquiry();
        Vehicle vehicle = reservation.getVehicle();
        Consignment consignment = consignmentRepository.findByVehicleIdOrderByIdDesc(vehicle.getId()).stream()
                .findFirst()
                .orElse(null);
        long deposit = reservation.getDepositAmount() == null ? 0L : reservation.getDepositAmount().longValue();
        long price = listedPriceOf(vehicle);

        SalesContractResponse dto = new SalesContractResponse();
        dto.setReservationId(reservation.getId());
        dto.setReservationCode(reservation.getCode());
        dto.setInquiryId(inquiry.getId());
        dto.setInquiryCode(inquiry.getCode());
        dto.setBuyer(nullToEmpty(inquiry.getFullName()));
        dto.setPhone(nullToEmpty(inquiry.getPhone()));
        dto.setEmail(nullToEmpty(inquiry.getEmail()));
        dto.setCar(vehicle.getDisplayLabel() + (vehicle.getYear() == null ? "" : " " + vehicle.getYear()));
        dto.setFile(consignment == null ? "" : consignment.getCode());
        dto.setPlate(nullToEmpty(vehicle.getLicensePlate()));
        dto.setKm(vehicle.getOdometerKm() == null ? 0 : vehicle.getOdometerKm());
        dto.setColor(nullToEmpty(vehicle.getColor()));
        dto.setFuel(nullToEmpty(vehicle.getFuelType()));
        dto.setOwner(consignment == null || consignment.getConsignor() == null
                ? ""
                : nullToEmpty(consignment.getConsignor().getFullName()));
        dto.setVin(nullToEmpty(vehicle.getVin()));
        dto.setStaff(reservation.getAssignedTo() == null
                ? ""
                : nullToEmpty(reservation.getAssignedTo().getFullName()));
        dto.setMake(nullToEmpty(vehicle.getMake()));
        dto.setModel(nullToEmpty(vehicle.getModel()));
        dto.setYear(vehicle.getYear() == null ? "" : String.valueOf(vehicle.getYear()));
        dto.setListedPrice(price);
        dto.setSalePrice(price);
        dto.setDeposit(deposit);
        dto.setRemaining(price - deposit);
        return dto;
    }

    private SalesContract require(Long id) {
        return salesContractRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hợp đồng bán xe", id));
    }

    /** Hồ sơ và tin đăng sang Đã bán — tin không còn trên trang bán xe. */
    private void markSold(Long vehicleId) {
        consignmentRepository.findByVehicleIdOrderByIdDesc(vehicleId).stream()
                .findFirst()
                .ifPresent(row -> row.setStatus(ConsignmentStatus.SOLD));
        newsRepository.findByVehicleId(vehicleId).forEach(news -> {
            if (news.getStatus() == ListingStatus.LISTED
                    || news.getStatus() == ListingStatus.PAUSED
                    || news.getStatus() == ListingStatus.RESERVED) {
                news.setStatus(ListingStatus.SOLD);
            }
        });
    }

    /** Cùng quy tắc giá với phiếu đặt cọc: listPrice tin đăng trước, askingPrice sau. */
    private long listedPriceOf(Vehicle vehicle) {
        long fallback = vehicle.getAskingPrice() == null ? 0L : vehicle.getAskingPrice().longValue();
        return newsRepository.findByVehicleId(vehicle.getId()).stream()
                .filter(n -> n.getStatus() == ListingStatus.LISTED
                        || n.getStatus() == ListingStatus.RESERVED
                        || n.getStatus() == ListingStatus.PAUSED
                        || n.getStatus() == ListingStatus.SOLD)
                .map(n -> n.getListPrice() != null ? n.getListPrice().longValue() : fallback)
                .findFirst()
                .orElse(fallback);
    }

    private static Map<String, String> handoverStep(SalesContractResponse contract) {
        String status = contract.getHandoverStatus() == null ? "" : contract.getHandoverStatus();
        if ("FINALIZED".equals(status)) {
            return step("Bàn giao xe", "Hoàn tất giao dịch", "done");
        }
        if ("COMPLETED".equals(status) || contract.isHandedOver()) {
            return step("Bàn giao xe", "Đã bàn giao", "done");
        }
        if ("READY".equals(status)) {
            return step("Bàn giao xe", "Sẵn sàng bàn giao", "current");
        }
        return step("Bàn giao xe", "Chưa thực hiện", "");
    }

    private static Map<String, String> milestone(String label, boolean done) {
        return step(label, done ? "Đã hoàn tất" : "Chưa thực hiện", done ? "done" : "");
    }

    private static Map<String, String> step(String label, String note, String state) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("label", label);
        row.put("note", note);
        row.put("state", state);
        return row;
    }

    private static String blank(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankTo(String value, String fallback) {
        String t = blank(value);
        return t.isEmpty() ? fallback : t;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
