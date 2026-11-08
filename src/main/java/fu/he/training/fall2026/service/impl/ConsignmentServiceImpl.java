package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.consignment.ConsignmentResponse;
import fu.he.training.fall2026.dto.consignment.VehicleResponse;
import fu.he.training.fall2026.dto.consignment.WithdrawResponse;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.enums.ApprovalType;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.ApprovalRepository;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import fu.he.training.fall2026.repository.ConsignorRepository;
import fu.he.training.fall2026.repository.VehicleRepository;
import fu.he.training.fall2026.service.ConsignmentService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsignmentServiceImpl implements ConsignmentService {

    private static final String APPROVAL_TARGET = "Consignment";

    private final ConsignmentRepository consignmentRepository;
    private final ConsignorRepository consignorRepository;
    private final VehicleRepository vehicleRepository;
    private final ApprovalRepository approvalRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ConsignmentResponse> search(String keyword, ConsignmentStatus status, int page, int size) {
        String pattern = keyword == null || keyword.isBlank()
                ? "%"
                : "%" + keyword.trim().toLowerCase() + "%";
        return consignmentRepository.search(
                        pattern, status, PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(ConsignmentServiceImpl::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ConsignmentStatus, Long> countByStatus() {
        Map<ConsignmentStatus, Long> counts = new EnumMap<>(ConsignmentStatus.class);
        for (ConsignmentStatus status : ConsignmentStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : consignmentRepository.countByStatus()) {
            counts.put((ConsignmentStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WithdrawResponse> findWithdrawRequest(Long id) {
        Consignment consignment = consignmentRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ ký gửi", id));
        return approvalRepository.findRequests(ApprovalType.WITHDRAWAL, APPROVAL_TARGET, id).stream()
                .findFirst()
                .map(request -> WithdrawResponse.builder()
                        .status(request.getStatus())
                        .reason(request.getReason())
                        .requestedByName(request.getRequestedBy().getFullName())
                        .createdAt(LocalDateTime.ofInstant(request.getCreatedAt(), ZoneId.systemDefault()))
                        .returnDate(consignment.getWithdrawReturnDate())
                        .condition(consignment.getWithdrawCondition())
                        .evidencePath(consignment.getWithdrawEvidencePath())
                        .build());
    }

    private static ConsignmentResponse toDto(Consignment c) {
        return ConsignmentResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .consignorId(c.getConsignor().getId())
                .consignorName(c.getConsignor().getFullName())
                .vehicle(toDto(c.getVehicle()))
                .status(c.getStatus())
                .expectedPrice(c.getExpectedPrice())
                .intakeDate(c.getIntakeDate())
                .termNotes(c.getTermNotes())
                .ownerConsent(c.isOwnerConsent())
                .build();
    }

    private static VehicleResponse toDto(Vehicle v) {
        return VehicleResponse.builder()
                .id(v.getId())
                .vin(v.getVin())
                .displayLabel(v.getDisplayLabel())
                .make(v.getMake())
                .model(v.getModel())
                .year(v.getYear())
                .color(v.getColor())
                .odometerKm(v.getOdometerKm())
                .chassisNumber(v.getChassisNumber())
                .engineNumber(v.getEngineNumber())
                .build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }


}
