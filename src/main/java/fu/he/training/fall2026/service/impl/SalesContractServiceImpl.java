package fu.he.training.fall2026.service.impl;
import fu.he.training.fall2026.entity.consignment.Consignor;
import fu.he.training.fall2026.entity.consignment.ConsignmentAgreement;

import fu.he.training.fall2026.dto.sales.*;
import fu.he.training.fall2026.entity.sales.*;
import fu.he.training.fall2026.entity.enums.*;
import fu.he.training.fall2026.repository.*;
import fu.he.training.fall2026.service.SalesContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.util.Locale;
import static fu.he.training.fall2026.service.impl.SalesPipelineSupport.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class SalesContractServiceImpl implements SalesContractService {
    private final SalesContractRepository contracts;
    private final SalesPipelineSupport rules;

    public SalesContractResponse create(SalesContractRequest q) {
        rules.validate(q, SalesContractRequest.Create.class);
        rules.lockVehicles(rules.vehicleId(q.getReservationId()));
        Reservation r = rules.lockReservation(q.getReservationId());
        active(r);
        conflict(!contracts.existsByReservationId(r.getId()), "Reservation already has a sales contract");
        ConsignmentAgreement a = rules.contractAgreement(r);
        rules.price(q.getSalePrice(), r.getDepositAmount(), a);
        SalesContract s = new SalesContract(); s.setCode(rules.code("SAL-")); s.setReservation(r); s.setConsignmentAgreement(a);
        s.setStatus(ContractStatus.DRAFT); s.setSalePrice(q.getSalePrice()); s.setDepositAmount(r.getDepositAmount());
        // An agreed deposit does not confirm receipt of money.
        s.setRemainingAmount(q.getSalePrice()); s.setTerms(q.getTerms()); s.setShopRole(ShopRole.CONSIGNMENT_AGENT);
        applySeller(s);
        return rules.response(contracts.save(s));
    }
    private void active(Reservation r) {
        conflict(r.getStatus() == ReservationStatus.ACTIVE, "Reservation must be ACTIVE");
        conflict(r.getDepositDisposition() == null, "Reservation has a pending cancellation or expiry decision");
        require(r.getBuyer().getDeletedAt() == null, "Cannot contract with a deleted buyer");
        require(r.getHoldUntil() != null && !r.getHoldUntil().isBefore(rules.today()), "Reservation hold has expired");
    }
    @Transactional(readOnly = true)
    public SalesContractResponse get(Long id) {
        return rules.response(contracts.findById(id).orElseThrow(() -> missing("Sales contract")));
    }
    @Transactional(readOnly = true)
    public Page<SalesContractResponse> list(ContractStatus status, String keyword, Pageable pageable) {
        Specification<SalesContract> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), pattern, '!'),
                cb.like(cb.lower(root.get("reservation").get("buyer").get("fullName")), pattern, '!'),
                cb.like(cb.lower(root.get("reservation").get("vehicle").get("vin")), pattern, '!')));
        }
        return contracts.findAll(spec, pageable).map(rules::response);
    }
    public SalesContractResponse update(Long id, SalesContractRequest q) {
        rules.validate(q);
        Long reservationId = contracts.findReservationId(id).orElseThrow(() -> missing("Sales contract"));
        rules.lockVehicles(rules.vehicleId(reservationId));
        Reservation r = rules.lockReservation(reservationId);
        SalesContract s = contracts.findById(id).orElseThrow(() -> missing("Sales contract"));
        ContractStatus old = s.getStatus(); ContractStatus next = q.getStatus() == null ? old : q.getStatus();
        require(q.getSellerSignaturePath() == null && q.getBuyerSignaturePath() == null && q.getSignedOn() == null,
            "Signature paths and signing dates cannot be entered manually; verified evidence is required");
        conflict(next == old || (next != ContractStatus.SIGNED && next != ContractStatus.ACTIVE),
            "Cannot sign or activate a contract until verified signature evidence is available");
        conflict(old != ContractStatus.CANCELLED && old != ContractStatus.CLOSED, "Terminal contracts cannot be edited");
        boolean editing = q.getSalePrice() != null || q.getTerms() != null;
        conflict(old == ContractStatus.DRAFT || !editing, "Signed contract content is immutable");
        conflict(next == old || (old == ContractStatus.DRAFT && next == ContractStatus.CANCELLED),
            "Invalid contract transition; closing a sale belongs to the settlement workflow");
        require(next != ContractStatus.CANCELLED || !editing, "Cancellation must not include content changes");
        boolean contentChanged = (q.getSalePrice() != null && q.getSalePrice().compareTo(s.getSalePrice()) != 0)
            || (q.getTerms() != null && !java.util.Objects.equals(q.getTerms(), s.getTerms()));
        if (contentChanged) {
            // Signatures on a draft apply to its previous price and terms.
            s.setSellerSignaturePath(null);
            s.setBuyerSignaturePath(null);
            s.setSignedOn(null);
        }
        if (old == ContractStatus.DRAFT && next != ContractStatus.CANCELLED) {
            active(r);
            ConsignmentAgreement a = rules.contractAgreement(r);
            if (q.getSalePrice() != null) s.setSalePrice(q.getSalePrice());
            rules.price(s.getSalePrice(), r.getDepositAmount(), a);
            s.setConsignmentAgreement(a); s.setDepositAmount(r.getDepositAmount());
            s.setRemainingAmount(s.getSalePrice());
            s.setShopRole(ShopRole.CONSIGNMENT_AGENT);
        }
        if (q.getTerms() != null) s.setTerms(q.getTerms());
        s.setStatus(next);
        return rules.response(s);
    }

    private void applySeller(SalesContract contract) {
        Consignor seller = contract.getConsignmentAgreement().getConsignment().getConsignor();
        require(seller != null && seller.getFullName() != null && !seller.getFullName().isBlank()
            && seller.getIdNumber() != null && !seller.getIdNumber().isBlank(),
            "Consignor name and identity number are required");
        contract.setSellerPartyType(SellerPartyType.OWNER);
        contract.setSellerName(seller.getFullName());
        contract.setSellerIdentityNumber(seller.getIdNumber());
        contract.setAuthorizationReference(null);
        contract.setAuthorizationValidUntil(null);
        contract.setShopRole(ShopRole.CONSIGNMENT_AGENT);
    }
}
