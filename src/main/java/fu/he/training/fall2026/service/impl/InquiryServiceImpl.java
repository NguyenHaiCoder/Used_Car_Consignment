package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.inquiry.*;
import fu.he.training.fall2026.dto.sales.SalesOptionResponse;
import fu.he.training.fall2026.dto.common.PaginationResponse;
import fu.he.training.fall2026.exception.BuyerException;
import fu.he.training.fall2026.exception.BuyerException.Reason;
import fu.he.training.fall2026.entity.sales.*;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.auth.AuditEvent;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.enums.*;
import fu.he.training.fall2026.exception.SalesSchedulingException;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.repository.*;
import fu.he.training.fall2026.service.InquiryService;
import fu.he.training.fall2026.service.SalesSchedulingService;
import fu.he.training.fall2026.service.SalesTimeService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryServiceImpl implements InquiryService {
    public static final ZoneId ZONE = SalesTimeService.ZONE;
    private final InquiryRepository inquiries;
    private final SalesSchedulingService schedulingService;
    private final BuyerRepository buyers;
    private final VehicleRepository vehicles;
    private final UserRepository users;
    private final ConsignmentRepository consignments;
    private final ReservationRepository reservations;
    private final Validator validator;
    private final SalesTimeService time;
    private final SalesAssignmentPolicy assignments;
    private final AuditEventRepository auditEvents;

    private void recordActivity(Inquiry inquiry, User actor, String action, String description) {
        AuditEvent event = new AuditEvent();
        event.setActor(actor);
        event.setEntityType("Inquiry");
        event.setEntityId(inquiry.getId());
        event.setAction(action);
        event.setDetail(description);
        event.setOccurredAt(time.now());
        auditEvents.save(event);
    }

    private void validate(Object form) {
        validator.validate(form).stream().sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .findFirst().ifPresent(v -> { throw invalid(v.getPropertyPath().toString(), v.getMessage()); });
    }
    private SalesSchedulingException invalid(String field, String message) {
        return new SalesSchedulingException(field, message);
    }
    private User salesperson(Long id) {
        try {
            return assignments.assignedSalesperson(id);
        } catch (BusinessException ex) {
            throw invalid("salespersonId", ex.getMessage());
        }
    }
    private ConsignmentStatus vehicleStatus(Long id) {
        return consignments.findFirstByVehicleIdOrderByIdDesc(id)
                .orElseThrow(() -> invalid("vehicleId", "Vehicle has no consignment.")).getStatus();
    }
    private boolean inquiryVehicleEligible(Long vehicleId, Long buyerId, ConsignmentStatus status) {
        if (status != ConsignmentStatus.LISTED && status != ConsignmentStatus.RESERVED) return false;
        List<Reservation> holds = reservations.findCurrentHolds(vehicleId, time.today());
        if (holds.isEmpty()) return status == ConsignmentStatus.LISTED;
        return buyerId != null
                && holds.stream().allMatch(r -> r.getHoldUntil() != null
                    && !r.getHoldUntil().isBefore(time.today())
                    && r.getBuyer() != null && buyerId.equals(r.getBuyer().getId()));
    }

    private Inquiry existing(long id) {
        return inquiries.findById(id).filter(i -> i.getDeletedAt() == null)
                .orElseThrow(() -> invalid("inquiryId", "Inquiry does not exist."));
    }

    private void requireOwner(Inquiry inquiry, User actor) {
        if (inquiry.getAssignedTo() == null || !actor.getId().equals(inquiry.getAssignedTo().getId())) {
            throw new BusinessException("Only the assigned salesperson can edit or delete this inquiry.", HttpStatus.FORBIDDEN);
        }
    }

    private boolean linkedRecords(long id) {
        return inquiries.hasViewings(id) || inquiries.hasReservations(id);
    }

    private void requireRevision(Inquiry inquiry, String revision) {
        if (!Objects.equals(inquiry.getUpdatedAt().toString(), revision)) {
            throw invalid("revision", "Inquiry has changed. Reload this page before trying again.");
        }
    }

    // All relation writers lock user -> vehicle(s) -> inquiry. Recheck after waiting.
    private Inquiry lockInquiry(long id, Long requestedVehicleId) {
        Long vehicleId = inquiries.findVehicleId(id)
                .orElseThrow(() -> invalid("inquiryId", "Inquiry does not exist."));
        SortedSet<Long> ids = new TreeSet<>();
        ids.add(vehicleId);
        if (requestedVehicleId != null) ids.add(requestedVehicleId);
        for (Long lockId : ids) {
            vehicles.lockById(lockId).orElseThrow(() -> invalid("vehicleId", "Vehicle does not exist."));
        }
        Inquiry inquiry = inquiries.lockById(id).filter(i -> i.getDeletedAt() == null)
                .orElseThrow(() -> invalid("inquiryId", "Inquiry does not exist."));
        if (!vehicleId.equals(inquiry.getVehicle().getId())) {
            throw new ConcurrencyFailureException("Inquiry vehicle changed while waiting for locks.");
        }
        return inquiry;
    }

    @Override
    public InquirySaveRequest formFor(long id) {
        User actor = assignments.currentSalesperson();
        Inquiry inquiry = existing(id);
        requireOwner(inquiry, actor);
        InquirySaveRequest form = new InquirySaveRequest();
        form.setBuyerId(inquiry.getBuyer().getId().toString());
        form.setVehicleId(inquiry.getVehicle().getId().toString());
        form.setSalespersonId(inquiry.getAssignedTo().getId());
        form.setInquiryDate(inquiry.getInquiryDate() == null
                ? inquiry.getCreatedAt().atZone(ZONE).toLocalDate() : inquiry.getInquiryDate());
        form.setNotes(inquiry.getNotes());
        form.setChannel(inquiry.getChannel());
        form.setNeed(inquiry.getNeed());
        form.setRevision(inquiry.getUpdatedAt().toString());
        return form;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void update(long id, InquirySaveRequest form) {
        validate(form);
        User actor = salesperson(form.getSalespersonId());
        Long buyerId = form.selectedBuyerId();
        Long vehicleId = form.selectedVehicleId();
        Inquiry inquiry = lockInquiry(id, vehicleId);
        requireOwner(inquiry, actor);
        requireRevision(inquiry, form.getRevision());
        boolean changed = !inquiry.getBuyer().getId().equals(buyerId)
                || !inquiry.getVehicle().getId().equals(vehicleId);
        if (changed) {
            if (linkedRecords(id)) {
                throw invalid("buyerId", "Cannot change Buyer or Vehicle while this inquiry has Viewings or Reservations.");
            }
            Buyer buyer = buyers.findByIdAndDeletedAtIsNull(buyerId)
                    .orElseThrow(() -> invalid("buyerId", "Select an existing, non-deleted buyer."));
            Vehicle vehicle = vehicles.lockById(vehicleId)
                    .orElseThrow(() -> invalid("vehicleId", "Vehicle does not exist."));
            if (!inquiryVehicleEligible(vehicle.getId(), buyer.getId(), vehicleStatus(vehicle.getId()))) {
                throw invalid("vehicleId", "Vehicle must be LISTED or RESERVED for this buyer. Any current hold must have a verified buyer and expiry date.");
            }
            inquiry.setBuyer(buyer);
            inquiry.setVehicle(vehicle);
        }
        inquiry.setInquiryDate(form.getInquiryDate());
        inquiry.setNotes(form.getNotes());
        inquiry.setChannel(form.getChannel());
        inquiry.setNeed(form.getNeed());
        inquiries.saveAndFlush(inquiry);
        recordActivity(inquiry, actor, "UPDATE", "Cập nhật thông tin tư vấn");
    }

    @Override
    public InquiryDeleteResponse deletionFor(long id) {
        User actor = assignments.currentSalesperson();
        Inquiry inquiry = existing(id);
        requireOwner(inquiry, actor);
        return new InquiryDeleteResponse(inquiryRow(inquiry), inquiry.getUpdatedAt().toString(), linkedRecords(id));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(long id, InquiryDeleteRequest form) {
        validate(form);
        User actor = assignments.assignedSalesperson(null);
        Inquiry inquiry = lockInquiry(id, null);
        requireOwner(inquiry, actor);
        requireRevision(inquiry, form.getRevision());
        if (linkedRecords(id)) {
            throw invalid("confirmed", "Cannot delete an inquiry with Viewings or Reservations, including cancelled or completed records.");
        }
        inquiry.setDeletedAt(time.now());
        inquiries.saveAndFlush(inquiry);
    }
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long create(InquirySaveRequest form) {
        validate(form);
        User salesperson = salesperson(form.getSalespersonId());
        Long buyerId = form.selectedBuyerId();
        Long vehicleId = form.selectedVehicleId();
        Vehicle vehicle = vehicles.lockById(vehicleId)
                .orElseThrow(() -> invalid("vehicleId", "Xe quan tâm không hợp lệ."));
        Buyer buyer = buyers.findByIdAndDeletedAtIsNull(buyerId)
                .orElseThrow(() -> invalid("buyerId", "Khách mua không hợp lệ."));
        ConsignmentStatus status = consignments.findFirstByVehicleIdOrderByIdDesc(vehicle.getId())
                .orElseThrow(() -> invalid("vehicleId", "Xe quan tâm chưa đủ điều kiện tư vấn.")).getStatus();
        if (!inquiryVehicleEligible(vehicle.getId(), buyer.getId(), status))
            throw invalid("vehicleId", "Xe quan tâm chưa đủ điều kiện tư vấn cho khách mua này.");
        Inquiry inquiry = new Inquiry();
        inquiry.setCode("INQ-" + UUID.randomUUID());
        inquiry.setBuyer(buyer);
        inquiry.setVehicle(vehicle);
        inquiry.setAssignedTo(salesperson);
        inquiry.setInquiryDate(form.getInquiryDate());
        inquiry.setNotes(form.getNotes());
        inquiry.setChannel(form.getChannel());
        inquiry.setNeed(form.getNeed());
        inquiry.setStatus(InquiryStatus.OPEN);
        inquiry = inquiries.saveAndFlush(inquiry);
        recordActivity(inquiry, salesperson, "CREATE", switch (form.getNeed()) {
            case "PRICE" -> "Ghi nhận nhu cầu hỏi giá";
            case "TEST_DRIVE" -> "Ghi nhận nhu cầu lái thử";
            default -> "Ghi nhận nhu cầu xem xe";
        });
        return inquiry.getId();
    }
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void assign(long id, InquiryAssignmentRequest form) {
        validate(form);
        User salesperson = salesperson(form.getSalespersonId());
        Inquiry inquiry = inquiries.lockById(id)
                .orElseThrow(() -> invalid("inquiryId", "Inquiry does not exist."));
        inquiry.setAssignedTo(salesperson);
        inquiries.saveAndFlush(inquiry);
    }
    private Pageable page(int page, int size, String sort) {
        return PageRequest.of(Math.max(0, page - 1), Math.max(1, Math.min(size, 100)),
                Sort.by(Sort.Direction.DESC, sort, "id"));
    }
    @Override
    public Page<InquiryResponse> list(String search, InquiryStatus status, Long salespersonId, int page, int size) {
        String pattern = "%" + (search == null ? "" : search.trim().toLowerCase(Locale.ROOT))
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return inquiries.search(pattern, status, salespersonId, page(page, size, "inquiryDate"))
                .map(this::inquiryRow);
    }
    @Override
    public InquiryResponse getDetails(long id) {
        Inquiry inquiry = inquiries.findById(id).orElseThrow(() -> invalid("inquiryId", "Inquiry does not exist."));
        InquiryResponse response = inquiryRow(inquiry);
        response.setViewings(schedulingService.viewingsForInquiry(id));
        List<AuditEvent> events = auditEvents.findByEntityTypeAndEntityIdOrderByOccurredAtDescIdDesc("Inquiry", id);
        List<InquiryActivityResponse> activities = events.stream()
                .map(e -> new InquiryActivityResponse(LocalDateTime.ofInstant(e.getOccurredAt(), ZONE), e.getDetail(),
                        e.getActor() == null ? "Hệ thống" : e.getActor().getFullName())).toList();
        if (inquiry.getCreatedAt() != null && events.stream().noneMatch(e -> "CREATE".equals(e.getAction()))) {
            List<InquiryActivityResponse> withCreation = new ArrayList<>(activities);
            withCreation.add(new InquiryActivityResponse(LocalDateTime.ofInstant(inquiry.getCreatedAt(), ZONE), "Tạo tư vấn mua xe", "Không rõ người tạo"));
            withCreation.sort(Comparator.comparing(InquiryActivityResponse::getOccurredAt).reversed());
            activities = withCreation;
        }
        response.setActivities(activities);
        return response;
    }
    @Override
    public List<SalesOptionResponse> buyers() {
        return buyers.findByDeletedAtIsNull(Sort.by("fullName", "id")).stream()
                .map(b -> new SalesOptionResponse(b.getId(), b.getFullName() + " - " + b.getPhone())).toList();
    }
    @Override
    public List<SalesOptionResponse> vehicles(Long buyerId) {
        return vehicles.findAll(Sort.by("make", "model", "id")).stream().filter(v ->
                consignments.findFirstByVehicleIdOrderByIdDesc(v.getId()).map(c ->
                    inquiryVehicleEligible(v.getId(), buyerId, c.getStatus())
                ).orElse(false)).map(v -> new SalesOptionResponse(v.getId(), v.getDisplayLabel() + " - " + v.getVin())).toList();
    }
    @Override
    public List<SalesOptionResponse> salespeople() {
        return users.findActiveSalespeople().stream().map(u -> new SalesOptionResponse(u.getId(), u.getFullName())).toList();
    }
    @Override
    public List<SalesOptionResponse> assignableSalespeople() {
        try {
            User actor = assignments.currentSalesperson();
            return List.of(new SalesOptionResponse(actor.getId(), actor.getFullName()));
        } catch (BusinessException ex) {
            throw invalid("salespersonId", ex.getMessage());
        }
    }
    private InquiryResponse inquiryRow(Inquiry i) {
        InquiryResponse response = new InquiryResponse(i.getId(), i.getCode(), i.getBuyer().getId(), i.getBuyer().getFullName(),
                i.getBuyer().getPhone(), i.getBuyer().getEmail(), i.getVehicle().getDisplayLabel(),
                i.getVehicle().getVin(), i.getAssignedTo() == null ? "Unassigned" : i.getAssignedTo().getFullName(),
                i.getAssignedTo() == null ? null : i.getAssignedTo().getId(),
                i.getInquiryDate() == null ? i.getCreatedAt().atZone(ZONE).toLocalDate() : i.getInquiryDate(),
                i.getStatus().name(), i.getNotes());
        response.setChannel(i.getChannel());
        response.setNeed(i.getNeed());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public InquiryHistoryResponse getBuyerHistory(long buyerId, int page, int size) {
        validateId(buyerId);
        validatePagination(page, size);

        if (buyers.findById(buyerId).isEmpty()) {
            throw new BuyerException(Reason.NOT_FOUND, "Buyer not found.");
        }

        Page<Inquiry> result = inquiries.findByBuyerId(buyerId,
                PageRequest.of(page - 1, size,
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

        return new InquiryHistoryResponse(
                result.getContent().stream().map(this::toInquiryHistoryItem).toList(),
                PaginationResponse.of(page, size, result.getTotalElements(), result.getTotalPages()));
    }

    private void validateId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Buyer ID must be a positive integer.");
        }
    }

    private void validatePagination(int page, int size) {
        if (page < 1 || size < 1 || size > 100 || (long) (page - 1) * size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "page must be >= 1; size must be between 1 and 100; offset must fit an integer.");
        }
    }

    private InquiryHistoryItemResponse toInquiryHistoryItem(Inquiry inquiry) {
        return new InquiryHistoryItemResponse(
                inquiry.getId(),
                inquiry.getVehicle() != null ? inquiry.getVehicle().getMake() : null,
                inquiry.getVehicle() != null ? inquiry.getVehicle().getModel() : null,
                inquiry.getCreatedAt(),
                inquiry.getStatus(),
                inquiry.getAssignedTo() != null ? inquiry.getAssignedTo().getFullName() : null);
    }
}
