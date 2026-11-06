package fu.he.training.fall2026.service;
import fu.he.training.fall2026.entity.sales.Reservation;
import fu.he.training.fall2026.entity.sales.Inquiry;

import fu.he.training.fall2026.dto.sales.*;
import fu.he.training.fall2026.entity.enums.*;
import fu.he.training.fall2026.repository.*;
import fu.he.training.fall2026.service.impl.SalesPipelineSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.Locale;
import java.util.*;
import static fu.he.training.fall2026.service.impl.SalesPipelineSupport.missing;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class SalesPipelineViewService {
    private final ReservationRepository reservations;
    private final InquiryRepository inquiries;
    private final SalesContractRepository contracts;
    private final SalesPipelineSupport rules;
    private final AuditEventRepository audits;

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ReservationRow {
        private Long id;
        private String code;
        private Long buyerId;
        private Long vehicleId;
        private String buyerName;
        private String vehicleLabel;
        private java.math.BigDecimal depositAmount;
        private java.time.LocalDate holdUntil;
        private ReservationStatus status;
        private boolean decisionPending;
        private boolean editable;
    }

    public Page<ReservationRow> reservations(ReservationStatus status, int page) {
        return reservations(status, null, null, null, page, 10);
    }
    public Page<ReservationRow> reservations(ReservationStatus status, String keyword, Long vehicleId,
                                             LocalDate holdUntil, int page, int size) {
        return reservations(status, keyword, vehicleId, holdUntil, page, size, "desc");
    }
    public Page<ReservationRow> reservations(ReservationStatus status, String keyword, Long vehicleId,
                                             LocalDate holdUntil, int page, int size, String sort) {
        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)),
            Sort.by("asc".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC, "id"));
        Specification<Reservation> filter = (root, query, cb) -> cb.conjunction();
        if (status != null) filter = filter.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (vehicleId != null) filter = filter.and((root, query, cb) -> cb.equal(root.get("vehicle").get("id"), vehicleId));
        if (holdUntil != null) filter = filter.and((root, query, cb) -> cb.equal(root.get("holdUntil"), holdUntil));
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            filter = filter.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), pattern, '!'),
                cb.like(cb.lower(root.get("buyer").get("fullName")), pattern, '!'),
                cb.like(cb.lower(root.get("vehicle").get("vin")), pattern, '!'),
                cb.like(cb.lower(root.get("vehicle").get("make")), pattern, '!'),
                cb.like(cb.lower(root.get("vehicle").get("model")), pattern, '!')));
        }
        LocalDate today = rules.today();
        return reservations.findAll(filter, pageable).map(r -> new ReservationRow(r.getId(),r.getCode(),
            r.getBuyer().getId(),r.getVehicle().getId(),r.getBuyer().getFullName(),r.getVehicle().getDisplayLabel(),
            r.getDepositAmount(),r.getHoldUntil(),r.getStatus(), r.getDepositDisposition() != null,
            r.getStatus() == ReservationStatus.ACTIVE && r.getDepositDisposition() == null
                && r.getDepositDecisionActorId() == null && r.getDepositOperationKey() == null
                && r.getCancelledAt() == null && r.getVehicleTransitionReference() == null
                && r.getHoldUntil() != null && !r.getHoldUntil().isBefore(today)
                && (r.getProcessedLiabilityAmount() == null || r.getProcessedLiabilityAmount().signum() == 0)
                && r.getDepositProcessingReference() == null
                && !contracts.existsByReservationId(r.getId())
                && rules.consignment(r.getVehicle().getId()).getStatus() == ConsignmentStatus.RESERVED));
    }
    public List<SalesOptionResponse> reservationVehicles() {
        return reservations.findReservedVehicles().stream()
            .map(v -> new SalesOptionResponse(v.getId(), v.getDisplayLabel())).toList();
    }
    public ReservationResponse reservation(Long id) {
        return rules.response(reservations.findById(id).orElseThrow(() -> missing("Reservation")));
    }
    public List<ReservationHistoryResponse> reservationHistory(Long id) {
        if (!reservations.existsById(id)) throw missing("Reservation");
        return audits.findByEntityTypeAndEntityIdOrderByOccurredAtDescIdDesc("Reservation", id).stream()
            .map(e -> new ReservationHistoryResponse(e.getOccurredAt(), e.getAction(),
                e.getActor() == null ? "Hệ thống" : e.getActor().getFullName(), e.getDetail())).toList();
    }
    public Long contractId(Long reservationId) {
        return contracts.findByReservationId(reservationId).map(c -> c.getId()).orElse(null);
    }
    public List<SalesOptionResponse> inquiries() {
        return inquiries.findAll(Sort.by(Sort.Direction.DESC,"id")).stream()
            .filter(i -> (i.getStatus()==InquiryStatus.OPEN || i.getStatus()==InquiryStatus.SCHEDULED) && i.getBuyer().getDeletedAt()==null)
            .map(i -> new SalesOptionResponse(i.getId(), i.getCode()+" — "+i.getBuyer().getFullName()+" — "+i.getVehicle().getDisplayLabel())).toList();
    }
    public ReservationRequest reservationForm(Long inquiryId) {
        if(inquiryId == null) return new ReservationRequest(null,null,null,null,null,null,"",null);
        Inquiry i=inquiries.findById(inquiryId).orElseThrow(() -> missing("Inquiry"));
        return new ReservationRequest(i.getId(),i.getVehicle().getId(),i.getBuyer().getId(),
            null,null,null,"",null);
    }
    public List<SalesOptionResponse> availableReservations() {
        return reservations.findByStatus(ReservationStatus.ACTIVE, Sort.by(Sort.Direction.DESC,"id")).stream()
            .filter(r -> r.getDepositDisposition() == null && r.getHoldUntil()!=null && !r.getHoldUntil().isBefore(rules.today()) && !contracts.existsByReservationId(r.getId()))
            .map(r -> new SalesOptionResponse(r.getId(),r.getCode()+" — "+r.getBuyer().getFullName()+" — "+r.getVehicle().getDisplayLabel())).toList();
    }
    public List<SalesOptionResponse> transferTargets(Long sourceId) {
        Reservation source=reservations.findById(sourceId).orElseThrow(() -> missing("Reservation"));
        return reservations.findByStatus(ReservationStatus.ACTIVE,Sort.by("id")).stream()
            .filter(r -> r.getDepositDisposition() == null && !r.getId().equals(sourceId) && r.getBuyer().getId().equals(source.getBuyer().getId())
                && !r.getVehicle().getId().equals(source.getVehicle().getId()) && r.getHoldUntil()!=null
                && !r.getHoldUntil().isBefore(rules.today()) && !contracts.existsByReservationId(r.getId()))
            .map(r -> new SalesOptionResponse(r.getId(),r.getCode()+" — "+r.getVehicle().getDisplayLabel())).toList();
    }
}
