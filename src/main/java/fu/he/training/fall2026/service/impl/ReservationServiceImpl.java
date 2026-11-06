package fu.he.training.fall2026.service.impl;
import java.util.Optional;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.auth.AuditEvent;

import fu.he.training.fall2026.dto.sales.*;
import fu.he.training.fall2026.entity.sales.*;
import java.util.Objects;
import fu.he.training.fall2026.entity.enums.*;
import fu.he.training.fall2026.repository.*;
import fu.he.training.fall2026.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static fu.he.training.fall2026.service.impl.SalesPipelineSupport.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ReservationServiceImpl implements ReservationService {
    private final ReservationRepository reservations;
    private final InquiryRepository inquiries;
    private final BuyerRepository buyers;
    private final SalesContractRepository contracts;
    private final SalesPipelineSupport rules;
    private final SalesAssignmentPolicy assignments;
    private final ConsignmentRepository consignments;
    private final AuditEventRepository audits;
    private final VehicleSaleNewsRepository listings;

    @Transactional(readOnly = true)
    public ReservationEligibilityResponse eligibility(Long inquiryId) {
        assignments.currentSalesperson();
        Inquiry inquiry = inquiries.findById(inquiryId)
            .filter(i -> i.getDeletedAt() == null).orElseThrow(() -> missing("Inquiry"));
        Long vehicleId = inquiry.getVehicle().getId();
        Optional<Consignment> consignment = consignments.findFirstByVehicleIdOrderByIdDesc(vehicleId);
        String status = consignment.map(c -> c.getStatus().name()).orElse("CHƯA CÓ");
        String reason = null;
        if (consignment.isEmpty()) {
            reason = "Xe chưa có hồ sơ ký gửi.";
        } else if (consignment.get().getStatus() != ConsignmentStatus.LISTED) {
            reason = "Hồ sơ ký gửi chưa ở trạng thái LISTED.";
        } else if (!hasValidAgreement(consignment.get())) {
            reason = "Chưa có thỏa thuận ký gửi ACTIVE còn hiệu lực.";
        } else if (!hasListedSaleListing(consignment.get())) {
            reason = "Xe chưa có tin bán ở trạng thái LISTED.";
        } else if (buyers.findByIdAndDeletedAtIsNull(inquiry.getBuyer().getId()).isEmpty()) {
            reason = "Khách mua không còn hợp lệ.";
        } else if (inquiry.getStatus() != InquiryStatus.OPEN && inquiry.getStatus() != InquiryStatus.SCHEDULED) {
            reason = "Phiếu tư vấn phải ở trạng thái OPEN hoặc SCHEDULED.";
        }
        boolean noActive = !reservations.existsByVehicleIdAndStatus(vehicleId, ReservationStatus.ACTIVE);
        return new ReservationEligibilityResponse(inquiryId, status, reason == null, reason, noActive);
    }

    private boolean hasValidAgreement(Consignment consignment) {
        return rules.latest(consignment).filter(rules::valid).isPresent();
    }

    private boolean hasListedSaleListing(Consignment consignment) {
        return listings.existsByConsignmentIdAndStatus(consignment.getId(), ListingStatus.LISTED);
    }

    public ReservationResponse create(ReservationRequest q) {
        rules.validate(q, ReservationRequest.Create.class);
        // Keep the user-before-vehicle lock order shared with scheduling.
        User sales = assignments.assignedSalesperson(q.getAssignedToId());
        rules.lockVehicles(q.getVehicleId());
        conflict(!reservations.existsByVehicleIdAndStatus(q.getVehicleId(), ReservationStatus.ACTIVE), "Vehicle already has an ACTIVE reservation");
        Consignment c = rules.consignment(q.getVehicleId());
        conflict(c.getStatus() == ConsignmentStatus.LISTED, "Vehicle must be LISTED");
        require(hasValidAgreement(c), "An active, valid consignment agreement is required");
        conflict(hasListedSaleListing(c),
            "Vehicle must have a LISTED sale listing");
        Buyer buyer = buyers.findByIdAndDeletedAtIsNull(q.getBuyerId()).orElseThrow(() -> missing("Buyer"));
        require(buyer.getDeletedAt() == null, "Cannot reserve for a deleted buyer");
        Inquiry inquiry = inquiries.lockById(q.getInquiryId()).filter(i -> i.getDeletedAt() == null)
                .orElseThrow(() -> missing("Inquiry"));
        require(inquiry.getVehicle().getId().equals(q.getVehicleId()) && inquiry.getBuyer().getId().equals(q.getBuyerId()),
            "Inquiry, vehicle and buyer must match");
        require(inquiry.getStatus() == InquiryStatus.OPEN || inquiry.getStatus() == InquiryStatus.SCHEDULED,
            "Inquiry must be open or scheduled");
        require(!q.getHoldUntil().isBefore(rules.today()), "Hold date must not be in the past");
        Reservation r = new Reservation();
        r.setCode(rules.code("RSV-")); r.setInquiry(inquiry); r.setVehicle(c.getVehicle()); r.setBuyer(buyer);
        r.setAssignedTo(sales); r.setStatus(ReservationStatus.ACTIVE); r.setDepositAmount(q.getDepositAmount());
        r.setHoldUntil(q.getHoldUntil()); r.setTerms(q.getTerms()); r.setConsentStored(q.getConsentStored());
        r.setCancelDepositPolicy(q.getCancelDepositPolicy()); r.setExpiryDepositPolicy(q.getExpiryDepositPolicy());
        reservations.saveAndFlush(r);
        c.setStatus(ConsignmentStatus.RESERVED);
        consignments.save(c);
        audit(r, sales, "RESERVATION_CREATED", "Vehicle reserved; agreed deposit is not a confirmed payment");
        return rules.response(r);
    }

    @Transactional(readOnly = true)
    public ReservationEditRequest editForm(Long id) {
        assignments.currentSalesperson();
        Reservation r = reservations.findById(id).orElseThrow(() -> missing("Reservation"));
        assertEditable(r);
        require(r.getUpdatedAt() != null, "Reservation revision is missing");
        ReservationEditRequest form = new ReservationEditRequest();
        form.setRevision(r.getUpdatedAt().toString());
        form.setDepositAmount(r.getDepositAmount());
        form.setHoldUntil(r.getHoldUntil());
        form.setTerms(r.getTerms());
        form.setCancelDepositPolicy(r.getCancelDepositPolicy());
        form.setExpiryDepositPolicy(r.getExpiryDepositPolicy());
        // The buyer must agree to the edited terms and policies again.
        form.setConsentStored(false);
        return form;
    }

    public ReservationResponse update(Long id, ReservationEditRequest form) {
        rules.validate(form);
        User actor = assignments.currentSalesperson();
        Long vehicleId = rules.vehicleId(id);
        rules.lockVehicles(vehicleId);
        Reservation r = rules.lockReservation(id);
        assertEditable(r);
        conflict(r.getUpdatedAt() != null && r.getUpdatedAt().toString().equals(form.getRevision()),
            "Reservation changed while editing. Reload the form and try again");
        require(form.getHoldUntil() != null && !form.getHoldUntil().isBefore(rules.today()),
            "Hold date must not be in the past");
        boolean termsLocked = financialTermsLocked(r);
        if (termsLocked) {
            conflict(r.getDepositAmount() != null && form.getDepositAmount() != null
                && r.getDepositAmount().compareTo(form.getDepositAmount()) == 0
                && Objects.equals(r.getTerms(), form.getTerms())
                && r.getCancelDepositPolicy() == form.getCancelDepositPolicy()
                && r.getExpiryDepositPolicy() == form.getExpiryDepositPolicy(),
                "Confirmed deposit terms cannot be changed; only the hold date may be edited");
        }
        boolean changed = !Objects.equals(r.getHoldUntil(), form.getHoldUntil())
            || (!termsLocked && (!r.isConsentStored()
                || !Objects.equals(r.getDepositAmount(), form.getDepositAmount())
                || !Objects.equals(r.getTerms(), form.getTerms())
                || r.getCancelDepositPolicy() != form.getCancelDepositPolicy()
                || r.getExpiryDepositPolicy() != form.getExpiryDepositPolicy()));
        if (!changed) return rules.response(r);
        String detail = termsLocked ? "Hold until " + r.getHoldUntil() + " -> " + form.getHoldUntil()
            + "; confirmed deposit terms unchanged" : "Agreed deposit " + r.getDepositAmount() + " -> " + form.getDepositAmount()
            + "; hold until " + r.getHoldUntil() + " -> " + form.getHoldUntil()
            + "; cancellation policy " + r.getCancelDepositPolicy() + " -> " + form.getCancelDepositPolicy()
            + "; expiry policy " + r.getExpiryDepositPolicy() + " -> " + form.getExpiryDepositPolicy()
            + "; terms changed: " + !Objects.equals(r.getTerms(), form.getTerms());
        r.setHoldUntil(form.getHoldUntil());
        if (!termsLocked) {
            r.setDepositAmount(form.getDepositAmount());
            r.setTerms(form.getTerms());
            r.setCancelDepositPolicy(form.getCancelDepositPolicy());
            r.setExpiryDepositPolicy(form.getExpiryDepositPolicy());
        }
        r.setConsentStored(true);
        reservations.saveAndFlush(r);
        audit(r, actor, "RESERVATION_UPDATED", detail);
        return rules.response(r);
    }

    private void assertEditable(Reservation r) {
        conflict(r.getStatus() == ReservationStatus.ACTIVE && r.getDepositDisposition() == null
            && r.getDepositDecisionActorId() == null && r.getDepositOperationKey() == null
            && r.getCancelledAt() == null && r.getVehicleTransitionReference() == null,
            "Only an ACTIVE reservation without a deposit decision can be edited");
        conflict(r.getHoldUntil() != null && !r.getHoldUntil().isBefore(rules.today()),
            "Expired reservations must be processed before editing");
        conflict((r.getProcessedLiabilityAmount() == null || r.getProcessedLiabilityAmount().signum() == 0)
            && r.getDepositProcessingReference() == null,
            "Reservation with processed deposit activity cannot be edited");
        conflict(!contracts.existsByReservationId(r.getId()), "Reservation with a sales contract cannot be edited");
        conflict(rules.consignment(r.getVehicle().getId()).getStatus() == ConsignmentStatus.RESERVED,
            "Vehicle must remain RESERVED to edit the reservation");
    }

    private boolean financialTermsLocked(Reservation r) {
        return r.getConfirmedCollectedAmount() != null || r.getProcessedLiabilityAmount() != null
            || r.getDepositBalanceReference() != null;
    }

    @Transactional(readOnly = true)
    public DepositPolicyResponse depositPolicy(Long id) {
        Reservation r = reservations.findById(id).orElseThrow(() -> missing("Reservation"));
        boolean overdue = r.getHoldUntil() != null && r.getHoldUntil().isBefore(rules.today());
        return new DepositPolicyResponse(id, r.getStatus(), r.getDepositAmount(), r.getTerms(), r.getHoldUntil(),
            overdue, overdue ? r.getExpiryDepositPolicy() : r.getCancelDepositPolicy(),
            r.getDepositDisposition(), r.getTransferReservation() == null ? null : r.getTransferReservation().getId(),
            "Quyết định xử lý cọc được lưu cùng đơn giữ xe; cọc thỏa thuận không xác nhận giao dịch tiền.");
    }
    public ReservationResponse cancel(Long id, ReservationRequest q) {
        rules.validate(q, ReservationRequest.Finish.class);
        return finish(id, q.getDisposition(), q.getReason(), q.getTransferReservationId(), false);
    }
    public ReservationResponse expire(Long id, ReservationRequest q) {
        rules.validate(q, ReservationRequest.Finish.class);
        return finish(id, q.getDisposition(), q.getReason(), q.getTransferReservationId(), true);
    }
    private ReservationResponse finish(Long id, DepositDisposition disposition, String reason, Long targetId, boolean expire) {
        User actor = assignments.currentSalesperson();
        require(disposition == DepositDisposition.TRANSFER ? targetId != null && !id.equals(targetId) : targetId == null,
            "TRANSFER requires a different target reservation; other dispositions must omit the target");
        Long vehicleId = rules.vehicleId(id);
        Long targetVehicle = targetId == null ? null : rules.vehicleId(targetId);
        rules.lockVehicles(vehicleId, targetVehicle);
        // Vehicle locks are acquired in ascending order before any reservation lock.
        Reservation r = rules.lockReservation(id);
        if (r.getDepositDisposition() != null) {
            conflict(r.getDepositDisposition() == disposition && Objects.equals(r.getCancelReason(), reason)
                && Objects.equals(r.getTransferReservation() == null ? null : r.getTransferReservation().getId(), targetId)
                && r.getDepositOperationKey() != null
                && r.getDepositOperationKey().startsWith(expire ? "EXPIRE-" : "CANCEL-"),
                "A different deposit decision has already been recorded");
            return rules.response(r);
        }
        conflict(r.getStatus() == ReservationStatus.ACTIVE, "Only an ACTIVE reservation can be cancelled or expired");
        if (expire) require(r.getHoldUntil() != null && r.getHoldUntil().isBefore(rules.today()), "Reservation has not expired");
        boolean overdue = r.getHoldUntil() != null && r.getHoldUntil().isBefore(rules.today());
        DepositDisposition policy = overdue ? r.getExpiryDepositPolicy() : r.getCancelDepositPolicy();
        require(r.isConsentStored() && r.getTerms() != null && !r.getTerms().isBlank() && policy != null,
            "Consented structured deposit policy is required; legacy terms require review before processing");
        require(disposition == policy, "Decision must match the consented reservation policy");
        Consignment c = rules.consignment(vehicleId);
        conflict(c.getStatus() == ConsignmentStatus.RESERVED || c.getStatus() == ConsignmentStatus.WITHDRAWN,
            "Consignment must be RESERVED or WITHDRAWN");
        Optional<SalesContract> draft = contracts.findByReservationId(id);
        conflict(draft.isEmpty() || draft.get().getStatus() == ContractStatus.DRAFT
            || draft.get().getStatus() == ContractStatus.CANCELLED, "An effective contract blocks reservation release");
        require(r.getDepositAmount() != null, "Reservation deposit amount is missing");
        Reservation target = null;
        if (disposition == DepositDisposition.TRANSFER) {
            target = rules.lockReservation(targetId);
            require(!vehicleId.equals(targetVehicle), "Deposit must transfer to another vehicle");
            require(target.getBuyer().getId().equals(r.getBuyer().getId()), "Transfer target must belong to the same buyer");
            require(target.getBuyer().getDeletedAt() == null, "Transfer target buyer must be active");
            conflict(target.getStatus() == ReservationStatus.ACTIVE && target.getHoldUntil() != null
                && !target.getHoldUntil().isBefore(rules.today()), "Transfer target must be an unexpired ACTIVE reservation");
            conflict(!contracts.existsByReservationId(targetId), "Transfer target must not already have a sales contract");
        }
        r.setTransferReservation(target);
        r.setDepositDecisionActorId(actor.getId());
        r.setDepositDisposition(disposition);
        r.setCancelReason(reason);
        r.setDepositOperationKey((expire ? "EXPIRE-" : "CANCEL-") + java.util.UUID.randomUUID());
        // WP05 has no callable payment/balance contract yet. Keep the hold and ACTIVE
        // status until the actual collected amount and disposition are confirmed there.
        audit(r, actor, expire ? "EXPIRY_DECISION_PENDING" : "CANCELLATION_DECISION_PENDING",
            "Decision " + disposition + "; reason: " + reason + "; operation: " + r.getDepositOperationKey());
        return rules.response(reservations.save(r));
    }

    private void audit(Reservation r, User actor, String action, String detail) {
        AuditEvent event = new AuditEvent();
        event.setActor(actor);
        event.setAction(action);
        event.setEntityType("Reservation");
        event.setEntityId(r.getId());
        event.setDetail(detail.length() > 1000 ? detail.substring(0, 1000) : detail);
        event.setOccurredAt(rules.now());
        audits.save(event);
    }
}
