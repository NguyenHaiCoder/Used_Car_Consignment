package fu.he.training.fall2026.service.impl;
import jakarta.validation.ConstraintViolation;

import fu.he.training.fall2026.entity.consignment.*;
import fu.he.training.fall2026.entity.enums.*;
import fu.he.training.fall2026.entity.sales.*;
import fu.he.training.fall2026.dto.sales.*;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.repository.*;
import fu.he.training.fall2026.service.SalesTimeService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

@Component
@RequiredArgsConstructor
public class SalesPipelineSupport {
    private final VehicleRepository vehicles;
    private final ReservationRepository reservations;
    private final ConsignmentRepository consignments;
    private final ConsignmentAgreementRepository agreements;
    private final ApprovalRepository approvals;
    private final Validator validator;
    private final SalesTimeService time;
    public LocalDate today() { return time.today(); }
    public Instant now() { return time.now(); }
    public static BusinessException missing(String entity) { return new BusinessException(entity + " not found", HttpStatus.NOT_FOUND); }
    public static void require(boolean condition, String message) { if (!condition) throw new BusinessException(message); }
    public static void conflict(boolean condition, String message) { if (!condition) throw new BusinessException(message, HttpStatus.CONFLICT); }
    public void validate(Object request, Class<?>... groups) {
        Set<ConstraintViolation<Object>> violations = validator.validate(request, groups);
        if (!violations.isEmpty()) throw new BusinessException(violations.stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().findFirst().orElseThrow());
    }
    public String code(String prefix) { return prefix + UUID.randomUUID().toString(); }
    public void lockVehicles(Long... ids) {
        Arrays.stream(ids).filter(Objects::nonNull).distinct().sorted().forEach(id ->
            vehicles.lockById(id).orElseThrow(() -> missing("Vehicle")));
    }
    public Long vehicleId(Long reservationId) {
        return reservations.findVehicleId(reservationId).orElseThrow(() -> missing("Reservation"));
    }
    public Reservation lockReservation(Long id) {
        return reservations.lockById(id).orElseThrow(() -> missing("Reservation"));
    }
    public Consignment consignment(Long vehicleId) {
        return consignments.findFirstByVehicleIdOrderByIdDesc(vehicleId).orElseThrow(() -> missing("Consignment"));
    }
    public Optional<ConsignmentAgreement> latest(Consignment c) {
        return agreements.findFirstByConsignmentIdOrderByVersionNoDesc(c.getId());
    }
    public boolean valid(ConsignmentAgreement a) {
        return a.getStatus() == AgreementStatus.ACTIVE && a.getValidFrom() != null && a.getValidTo() != null
            && !a.getValidFrom().isAfter(today()) && !a.getValidTo().isBefore(today());
    }
    public ConsignmentAgreement contractAgreement(Reservation r) {
        Consignment c = consignment(r.getVehicle().getId());
        conflict(c.getStatus() == ConsignmentStatus.RESERVED, "Consignment must be RESERVED");
        ConsignmentAgreement a = latest(c).orElseThrow(() -> new BusinessException("Consignment agreement is required"));
        require(valid(a), "Consignment agreement must be ACTIVE and within its validity period");
        return a;
    }
    public void price(BigDecimal salePrice, BigDecimal deposit, ConsignmentAgreement a) {
        require(salePrice != null && salePrice.signum() > 0, "Sale price must be positive");
        require(deposit != null && deposit.signum() >= 0 && salePrice.compareTo(deposit) >= 0,
            "Sale price must cover the reservation deposit");
        require(a.getPriceFloor() != null, "Agreement price floor is required");
        boolean approved = approvals.existsByTypeAndTargetTypeAndTargetIdAndStatus(
                ApprovalType.BELOW_FLOOR_PRICE, "VEHICLE", a.getConsignment().getVehicle().getId(), ApprovalStatus.APPROVED)
            || approvals.existsByTypeAndTargetTypeAndTargetIdAndStatus(
                ApprovalType.BELOW_FLOOR_PRICE, "SALES_PRICE_DISCOUNT", a.getId(), ApprovalStatus.APPROVED)
            || approvals.existsByTypeAndTargetTypeAndTargetIdAndStatus(
                ApprovalType.BELOW_FLOOR_PRICE, "SALES_PRICE_DISCOUNT", a.getConsignment().getVehicle().getId(), ApprovalStatus.APPROVED);
        require(salePrice.compareTo(a.getPriceFloor()) >= 0 || approved, "Price below floor requires an approved price discount");
    }
    public ReservationResponse response(Reservation r) {
        return new ReservationResponse(r.getId(), r.getCode(), r.getInquiry() == null ? null : r.getInquiry().getId(), r.getVehicle().getId(),
            r.getBuyer().getId(), r.getAssignedTo() == null ? null : r.getAssignedTo().getId(), r.getStatus(),
            r.getDepositAmount(), r.getHoldUntil(), r.getTerms(), r.isConsentStored(), r.getCancelledAt(),
            r.getCancelReason(), r.getDepositDisposition(), r.getTransferReservation() == null ? null : r.getTransferReservation().getId(),
            r.getBuyer().getFullName(), r.getVehicle().getVin(), r.getVehicle().getMake(), r.getVehicle().getModel(),
            r.getAssignedTo() == null ? null : r.getAssignedTo().getFullName(),
            r.getCancelDepositPolicy(), r.getExpiryDepositPolicy(), r.getConfirmedCollectedAmount(),
            r.getProcessedLiabilityAmount(), r.getDepositDecisionActorId(), r.getDepositOperationKey(),
            r.getDepositBalanceReference(), r.getDepositProcessingReference(), r.getVehicleTransitionReference());
    }
    public SalesContractResponse response(SalesContract s) {
        Reservation r = s.getReservation(); Vehicle v = r.getVehicle();
        Consignment c = s.getConsignmentAgreement() == null ? consignment(v.getId()) : s.getConsignmentAgreement().getConsignment();
        // Agreed deposits are not receipts of money.
        return new SalesContractResponse(s.getId(), s.getCode(), r.getId(), s.getStatus(), r.getBuyer().getId(),
            r.getBuyer().getFullName(), null,
            s.getSellerName(), c.getConsignor().getId(), c.getConsignor().getFullName(),
            v.getId(), v.getVin(), v.getMake(), v.getModel(), s.getSalePrice(), s.getDepositAmount(),
            s.getSalePrice(), s.getTerms(), s.getSignedOn(), s.getSellerSignaturePath(), s.getBuyerSignaturePath(),
            s.isSaleClosed(), s.getSellerPartyType(), s.getSellerIdentityNumber(),
            s.getAuthorizationReference(), s.getAuthorizationValidUntil(), s.getShopRole(),
            null, false);
    }
}
