package fu.he.training.fall2026.service.consignmentrequest.impl;

import fu.he.training.fall2026.dto.catalog.VehicleModelResponse;
import fu.he.training.fall2026.dto.consignment.ConsignmentCreateRequest;
import fu.he.training.fall2026.dto.consignment.ConsignmentRequestResponse;
import fu.he.training.fall2026.dto.consignment.WithdrawCheckResponse;
import fu.he.training.fall2026.dto.consignment.WithdrawRequest;
import fu.he.training.fall2026.entity.auth.Approval;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.Consignor;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.enums.ApprovalStatus;
import fu.he.training.fall2026.entity.enums.ApprovalType;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.ReservationStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.ApprovalRepository;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import fu.he.training.fall2026.repository.ConsignorRepository;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.repository.VehicleRepository;
import fu.he.training.fall2026.service.VehicleModelService;
import fu.he.training.fall2026.service.consignmentrequest.ConsignmentRequestService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsignmentRequestServiceImpl implements ConsignmentRequestService {

    private static final String APPROVAL_TARGET = "Consignment";

    private final ConsignmentRepository consignmentRepository;
    private final ConsignorRepository consignorRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleModelService vehicleModelService;
    private final ApprovalRepository approvalRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ConsignmentRequestResponse> listAll() {
        return consignmentRepository.findAllWithDetail().stream()
                .map(ConsignmentRequestServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConsignmentRequestResponse get(Long id) {
        return toDto(consignmentRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ ký gửi", id)));
    }

    @Override
    @Transactional
    public Long create(ConsignmentCreateRequest form) {
        // Chủ xe và xe luôn đi cùng hồ sơ: tạo mới cả ba trong một lần lưu.
        VehicleModelResponse model = vehicleModelService.get(form.getVehicleModelId());

        Consignor consignor = new Consignor();
        consignor.setFullName(form.getConsignorName().trim());
        consignor.setPhone(form.getPhone().trim());
        consignor.setEmail(blankToNull(form.getEmail()));
        consignor.setIdNumber(form.getIdNumber().trim());
        consignor.setAddress(blankToNull(form.getAddress()));
        consignor.setBank(form.getBank().trim());
        consignor.setAccountNumber(form.getAccountNumber().trim());
        consignor.setAccountName(form.getAccountName().trim());
        consignor.setConsentStored(form.isConsentStored());
        consignorRepository.save(consignor);

        Vehicle vehicle = new Vehicle();
        vehicle.setMake(model.getBrandName());
        vehicle.setModel(model.getName());
        vehicle.setVin(form.getVin().trim());
        vehicle.setEngineNumber(blankToNull(form.getEngineNumber()));
        vehicle.setLicensePlate(blankToNull(form.getLicensePlate()));
        vehicle.setYear(form.getYear());
        vehicle.setColor(form.getColor().trim());
        vehicle.setOdometerKm(form.getOdometerKm());
        vehicle.setVehicleType(blankToNull(form.getVehicleType()));
        vehicle.setTransmission(blankToNull(form.getTransmission()));
        vehicle.setFuelType(form.getFuelType().trim());
        vehicle.setLocation(blankToNull(form.getLocation()));
        vehicle.setIntakeCondition(blankToNull(form.getIntakeCondition()));
        vehicleRepository.save(vehicle);

        Consignment consignment = new Consignment();
        consignment.setCode("TMP-" + UUID.randomUUID().toString().substring(0, 12)); // đổi thành KG-{id} bên dưới
        consignment.setStatus(ConsignmentStatus.DRAFT);
        consignment.setConsignor(consignor);
        consignment.setVehicle(vehicle);
        consignment.setExpectedPrice(form.getExpectedPrice());
        consignment.setIntakeDate(form.getIntakeDate());
        consignment.setTermNotes(blankToNull(form.getTermNotes()));
        consignment.setCustodyLocation(blankToNull(form.getCustodyLocation()));
        consignment.setOwnerConsent(form.isConsentStored());
        consignmentRepository.save(consignment);
        consignment.setCode("KG-" + consignment.getId());
        return consignment.getId();
    }

    @Override
    @Transactional
    public void updateDraft(Long id, ConsignmentCreateRequest form) {
        Consignment consignment = consignmentRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ ký gửi", id));
        if (consignment.getStatus() != ConsignmentStatus.DRAFT) {
            throw new BusinessException("Chỉ hồ sơ DRAFT mới được sửa hoặc gửi duyệt.", HttpStatus.CONFLICT);
        }
        VehicleModelResponse model = vehicleModelService.get(form.getVehicleModelId());

        Consignor consignor = consignment.getConsignor();
        consignor.setFullName(form.getConsignorName().trim());
        consignor.setPhone(form.getPhone().trim());
        consignor.setEmail(blankToNull(form.getEmail()));
        consignor.setIdNumber(form.getIdNumber().trim());
        consignor.setAddress(blankToNull(form.getAddress()));
        consignor.setBank(form.getBank().trim());
        consignor.setAccountNumber(form.getAccountNumber().trim());
        consignor.setAccountName(form.getAccountName().trim());
        consignor.setConsentStored(form.isConsentStored());

        Vehicle vehicle = consignment.getVehicle();
        vehicle.setMake(model.getBrandName());
        vehicle.setModel(model.getName());
        vehicle.setVin(form.getVin().trim());
        vehicle.setEngineNumber(blankToNull(form.getEngineNumber()));
        vehicle.setLicensePlate(blankToNull(form.getLicensePlate()));
        vehicle.setYear(form.getYear());
        vehicle.setColor(form.getColor().trim());
        vehicle.setOdometerKm(form.getOdometerKm());
        vehicle.setVehicleType(blankToNull(form.getVehicleType()));
        vehicle.setTransmission(blankToNull(form.getTransmission()));
        vehicle.setFuelType(form.getFuelType().trim());
        vehicle.setLocation(blankToNull(form.getLocation()));
        vehicle.setIntakeCondition(blankToNull(form.getIntakeCondition()));

        consignment.setExpectedPrice(form.getExpectedPrice());
        consignment.setIntakeDate(form.getIntakeDate());
        consignment.setTermNotes(blankToNull(form.getTermNotes()));
        consignment.setCustodyLocation(blankToNull(form.getCustodyLocation()));
        consignment.setOwnerConsent(form.isConsentStored());
    }

    @Override
    @Transactional
    public void submit(Long id) {
        Consignment consignment = consignmentRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ ký gửi", id));
        if (consignment.getStatus() != ConsignmentStatus.DRAFT) {
            throw new BusinessException("Chỉ hồ sơ DRAFT mới được sửa hoặc gửi duyệt.", HttpStatus.CONFLICT);
        }
        consignment.setStatus(ConsignmentStatus.SUBMITTED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithdrawCheckResponse> withdrawChecks(Long id) {
        Consignment consignment = consignmentRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ ký gửi", id));
        ConsignmentStatus status = consignment.getStatus();
        boolean open = status != ConsignmentStatus.SOLD
                && status != ConsignmentStatus.COMPLETED
                && status != ConsignmentStatus.WITHDRAW_REQUESTED
                && status != ConsignmentStatus.WITHDRAWN
                && status != ConsignmentStatus.CLOSED;
        String buyerHold = buyerHoldOf(consignment.getVehicle().getId());
        boolean pending = approvalRepository.existsByTypeAndTargetTypeAndTargetIdAndStatus(
                ApprovalType.WITHDRAWAL, APPROVAL_TARGET, id, ApprovalStatus.PENDING);

        return List.of(
                new WithdrawCheckResponse("Hồ sơ chưa bán hoặc đã đóng", open,
                        open ? "" : "Trạng thái hiện tại: " + status + "."),
                new WithdrawCheckResponse("Xe chưa có khách đặt cọc hoặc ký hợp đồng", buyerHold.isEmpty(), buyerHold),
                new WithdrawCheckResponse("Chưa có yêu cầu rút đang chờ duyệt", !pending,
                        pending ? "Đã có yêu cầu gửi quản lý, đang chờ duyệt." : ""));
    }

    @Override
    @Transactional(readOnly = true)
    public String buyerHoldDetail(Long id) {
        Consignment consignment = consignmentRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ ký gửi", id));
        return buyerHoldOf(consignment.getVehicle().getId());
    }

    /** Chủ xe rút lúc nào cũng được, trừ khi xe còn phiếu đặt cọc hiệu lực hoặc đã có hợp đồng bán chưa hủy. */
    private String buyerHoldOf(Long vehicleId) {
        StringBuilder detail = new StringBuilder();
        List<String> reservations = consignmentRepository.findReservationCodes(vehicleId, ReservationStatus.ACTIVE);
        if (!reservations.isEmpty()) {
            BigDecimal deposit = consignmentRepository.sumReservationDeposits(vehicleId, ReservationStatus.ACTIVE);
            detail.append("Còn phiếu đặt cọc ").append(String.join(", ", reservations))
                    .append(" (tiền cọc đang giữ: ").append(deposit.stripTrailingZeros().toPlainString()).append(" ₫). ");
        }
        List<String> contracts = consignmentRepository.findContractCodes(vehicleId, ContractStatus.CANCELLED);
        if (!contracts.isEmpty()) {
            detail.append("Đã có hợp đồng bán ").append(String.join(", ", contracts)).append(".");
        }
        return detail.toString().trim();
    }

    @Override
    @Transactional
    public void requestWithdraw(Long id, WithdrawRequest form, Long actorUserId) {
        List<String> failed = withdrawChecks(id).stream()
                .filter(check -> !check.isPassed())
                .map(check -> check.getLabel() + ". " + check.getDetail())
                .toList();
        if (!failed.isEmpty()) {
            throw new BusinessException("Chưa thể gửi yêu cầu rút: " + String.join(" ", failed), HttpStatus.CONFLICT);
        }
        Consignment consignment = consignmentRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ ký gửi", id));
        consignment.setWithdrawReason(form.getReason().trim());
        consignment.setWithdrawReturnDate(form.getReturnDate());
        consignment.setWithdrawCondition(blankToNull(form.getCondition()));
        consignment.setWithdrawEvidencePath(blankToNull(form.getEvidencePath()));
        consignment.setStatus(ConsignmentStatus.WITHDRAW_REQUESTED);

        Approval request = new Approval();
        request.setCode("WD-" + id + "-" + System.currentTimeMillis());
        request.setType(ApprovalType.WITHDRAWAL);
        request.setStatus(ApprovalStatus.PENDING);
        request.setTargetType(APPROVAL_TARGET);
        request.setTargetId(id);
        request.setReason(consignment.getWithdrawReason());
        request.setRequestedBy(userRepository.getReferenceById(actorUserId));
        approvalRepository.save(request);
    }

    private static ConsignmentRequestResponse toDto(Consignment c) {
        Consignor o = c.getConsignor();
        Vehicle v = c.getVehicle();
        return ConsignmentRequestResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .status(c.getStatus())
                .expectedPrice(c.getExpectedPrice())
                .intakeDate(c.getIntakeDate())
                .termNotes(c.getTermNotes())
                .custodyLocation(c.getCustodyLocation())
                .ownerConsent(c.isOwnerConsent())
                .consignorId(o.getId())
                .consignorName(o.getFullName())
                .phone(o.getPhone())
                .email(o.getEmail())
                .idNumber(o.getIdNumber())
                .address(o.getAddress())
                .bank(o.getBank())
                .accountNumber(o.getAccountNumber())
                .accountName(o.getAccountName())
                .consentStored(o.isConsentStored())
                .vehicleId(v.getId())
                .displayLabel(v.getDisplayLabel())
                .make(v.getMake())
                .model(v.getModel())
                .vin(v.getVin())
                .engineNumber(v.getEngineNumber())
                .licensePlate(v.getLicensePlate())
                .year(v.getYear())
                .color(v.getColor())
                .odometerKm(v.getOdometerKm())
                .vehicleType(v.getVehicleType())
                .transmission(v.getTransmission())
                .fuelType(v.getFuelType())
                .location(v.getLocation())
                .intakeCondition(v.getIntakeCondition())
                .build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
