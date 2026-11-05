package fu.he.training.fall2026.service.impl;
import jakarta.persistence.criteria.Predicate;

import fu.he.training.fall2026.dto.sales.*;
import fu.he.training.fall2026.entity.sales.*;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.enums.*;
import fu.he.training.fall2026.exception.SalesSchedulingException;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.repository.*;
import fu.he.training.fall2026.service.SalesSchedulingService;
import fu.he.training.fall2026.service.SalesTimeService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesSchedulingServiceImpl implements SalesSchedulingService {
    public static final ZoneId ZONE = SalesTimeService.ZONE;
    private final InquiryRepository inquiries;
    private final ViewingRepository viewings;
    private final VehicleRepository vehicles;
    private final UserRepository users;
    private final ConsignmentRepository consignments;
    private final Validator validator;
    private final SalesTimeService time;
    private final SalesAssignmentPolicy assignments;

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
            throw invalid("salespersonId", "Không thể gán lịch cho nhân viên bán hàng này.");
        }
    }
    private ConsignmentStatus vehicleStatus(Long id) {
        return consignments.findFirstByVehicleIdOrderByIdDesc(id)
                .orElseThrow(() -> invalid("vehicleId", "Vehicle has no consignment.")).getStatus();
    }
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long scheduleViewingFromForm(ViewingFormRequest form) {
        validate(form);
        ViewingRequest request = new ViewingRequest();
        request.setInquiryId(form.getInquiryId());
        request.setSalespersonId(form.getSalespersonId());
        request.setType(form.getType());
        request.setStartTime(LocalDateTime.parse(form.getStartTime()));
        request.setEndTime(LocalDateTime.parse(form.getEndTime()));
        request.setLocation(form.getLocation());
        request.setNotes(form.getNotes());
        return scheduleViewing(request);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long scheduleViewing(ViewingRequest form) {
        validate(form);
        Instant start = form.getStartTime().atZone(ZONE).toInstant();
        Instant end = form.getEndTime().atZone(ZONE).toInstant();
        if (!end.isAfter(start)) throw invalid("endTime", "Thời gian kết thúc phải sau thời gian bắt đầu.");
        User salesperson = salesperson(form.getSalespersonId());
        Long vehicleId = inquiries.findVehicleId(form.getInquiryId())
                .orElseThrow(() -> invalid("inquiryId", "Inquiry does not exist."));
        Vehicle vehicle = vehicles.lockById(vehicleId)
                .orElseThrow(() -> invalid("inquiryId", "Inquiry vehicle no longer exists."));
        Inquiry inquiry = inquiries.lockById(form.getInquiryId())
                .filter(i -> i.getDeletedAt() == null)
                .orElseThrow(() -> invalid("inquiryId", "Inquiry does not exist."));
        if (!vehicleId.equals(inquiry.getVehicle().getId()))
            throw invalid("inquiryId", "Inquiry vehicle has changed. Reload the form.");
        if (inquiry.getStatus() != InquiryStatus.OPEN && inquiry.getStatus() != InquiryStatus.SCHEDULED)
            throw invalid("inquiryId", "Only open or scheduled inquiries can have a viewing.");
        if (inquiry.getBuyer().getDeletedAt() != null)
            throw invalid("inquiryId", "The inquiry buyer has been deleted.");
        // Recheck after waiting for locks, so waiting cannot admit a now-past appointment.
        if (start.isBefore(time.now())) throw invalid("startTime", "Thời gian bắt đầu không được ở quá khứ.");
        ConsignmentStatus status = vehicleStatus(vehicle.getId());
        if (status == ConsignmentStatus.SOLD || status == ConsignmentStatus.WITHDRAWN)
            throw invalid("inquiryId", "Cannot schedule a SOLD or WITHDRAWN vehicle.");
        // Locks remain held until commit; concurrent requests for either resource must wait.
        if (viewings.overlapsSalesperson(salesperson.getId(), start, end))
            throw invalid("salespersonId", "Nhân viên bán hàng đã có lịch hẹn trùng thời gian.");
        if (viewings.overlapsVehicle(vehicle.getId(), start, end))
            throw invalid("startTime", "Xe đã có lịch hẹn trùng thời gian.");
        Viewing viewing = new Viewing();
        viewing.setCode("VW-" + UUID.randomUUID());
        viewing.setInquiry(inquiry);
        viewing.setAssignedTo(salesperson);
        viewing.setViewingType(form.getType());
        viewing.setScheduledAt(start);
        viewing.setEndAt(end);
        viewing.setStatus("SCHEDULED");
        viewing.setLocation(form.getLocation().trim());
        viewing.setNotes(form.getNotes());
        inquiry.setStatus(InquiryStatus.SCHEDULED);
        return viewings.saveAndFlush(viewing).getId();
    }

    @Override
    public ViewingFormRequest formForViewing(long id) {
        Viewing viewing = viewings.findById(id)
                .orElseThrow(() -> invalid("viewingId", "Viewing does not exist."));
        if (!"SCHEDULED".equals(viewing.getStatus()))
            throw invalid("viewingId", "Only scheduled viewings can be edited.");
        ViewingFormRequest form = new ViewingFormRequest();
        form.setInquiryId(viewing.getInquiry().getId());
        User assigned = viewing.getAssignedTo() == null ? viewing.getInquiry().getAssignedTo() : viewing.getAssignedTo();
        form.setSalespersonId(assigned == null ? null : assigned.getId());
        form.setType(viewing.getViewingType());
        form.setStartTime(LocalDateTime.ofInstant(viewing.getScheduledAt(), ZONE).toString());
        form.setEndTime(viewing.getEndAt() == null ? null : LocalDateTime.ofInstant(viewing.getEndAt(), ZONE).toString());
        form.setLocation(viewing.getLocation());
        form.setNotes(viewing.getNotes());
        form.setRevision(viewing.getUpdatedAt().toString());
        return form;
    }

    @Override
    public ViewingResponse viewingForDelete(long id) {
        Viewing viewing = viewings.findById(id)
                .orElseThrow(() -> invalid("viewingId", "Viewing does not exist."));
        return viewingRow(viewing);
    }

    private Viewing lockViewing(long id) {
        Viewing current = viewings.findById(id)
                .orElseThrow(() -> invalid("viewingId", "Viewing does not exist."));
        Long vehicleId = current.getInquiry().getVehicle().getId();
        vehicles.lockById(vehicleId).orElseThrow(() -> invalid("viewingId", "Viewing vehicle does not exist."));
        Inquiry inquiry = inquiries.lockById(current.getInquiry().getId())
                .orElseThrow(() -> invalid("viewingId", "Viewing inquiry does not exist."));
        Viewing locked = viewings.lockById(id)
                .orElseThrow(() -> invalid("viewingId", "Viewing does not exist."));
        if (!inquiry.getId().equals(locked.getInquiry().getId())
                || !vehicleId.equals(inquiry.getVehicle().getId()))
            throw invalid("viewingId", "Viewing changed. Reload the page.");
        return locked;
    }

    private void requireRevision(Viewing viewing, String revision) {
        if (revision == null || !viewing.getUpdatedAt().toString().equals(revision))
            throw invalid("revision", "Viewing changed. Reload the page before saving.");
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updateViewing(long id, ViewingFormRequest form) {
        validate(form);
        Viewing viewing = lockViewing(id);
        requireRevision(viewing, form.getRevision());
        if (!"SCHEDULED".equals(viewing.getStatus()))
            throw invalid("revision", "Only scheduled viewings can be edited.");
        if (!viewing.getInquiry().getId().equals(form.getInquiryId()))
            throw invalid("inquiryId", "Viewing inquiry cannot be changed.");
        Inquiry inquiry = viewing.getInquiry();
        if (inquiry.getStatus() != InquiryStatus.OPEN && inquiry.getStatus() != InquiryStatus.SCHEDULED)
            throw invalid("inquiryId", "This inquiry is no longer open for scheduling.");
        if (inquiry.getBuyer().getDeletedAt() != null)
            throw invalid("inquiryId", "The inquiry buyer has been deleted.");
        ConsignmentStatus vehicleStatus = vehicleStatus(inquiry.getVehicle().getId());
        if (vehicleStatus == ConsignmentStatus.SOLD || vehicleStatus == ConsignmentStatus.WITHDRAWN)
            throw invalid("inquiryId", "Cannot schedule a SOLD or WITHDRAWN vehicle.");
        Instant start = LocalDateTime.parse(form.getStartTime()).atZone(ZONE).toInstant();
        Instant end = LocalDateTime.parse(form.getEndTime()).atZone(ZONE).toInstant();
        if (!end.isAfter(start)) throw invalid("endTime", "Thời gian kết thúc phải sau thời gian bắt đầu.");
        if (start.isBefore(time.now())) throw invalid("startTime", "Thời gian bắt đầu không được ở quá khứ.");
        User assigned = viewing.getAssignedTo() == null ? inquiry.getAssignedTo() : viewing.getAssignedTo();
        User salesperson = assigned != null && assigned.getId().equals(form.getSalespersonId())
                ? assigned : salesperson(form.getSalespersonId());
        if (viewings.overlapsSalespersonExcept(id, salesperson.getId(), start, end))
            throw invalid("salespersonId", "Nhân viên bán hàng đã có lịch hẹn trùng thời gian.");
        if (viewings.overlapsVehicleExcept(id, inquiry.getVehicle().getId(), start, end))
            throw invalid("startTime", "Xe đã có lịch hẹn trùng thời gian.");
        viewing.setAssignedTo(salesperson);
        viewing.setViewingType(form.getType());
        viewing.setScheduledAt(start);
        viewing.setEndAt(end);
        viewing.setLocation(form.getLocation().trim());
        viewing.setNotes(form.getNotes());
        viewings.saveAndFlush(viewing);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteViewing(long id, String revision) {
        Viewing viewing = lockViewing(id);
        requireRevision(viewing, revision);
        if (!"SCHEDULED".equals(viewing.getStatus()))
            throw invalid("revision", "Only scheduled viewings can be deleted.");
        Inquiry inquiry = viewing.getInquiry();
        viewing.setDeletedAt(time.now());
        viewings.saveAndFlush(viewing);
        if (inquiry.getStatus() == InquiryStatus.SCHEDULED
                && !viewings.existsByInquiryIdAndDeletedAtIsNull(inquiry.getId())
                && !inquiries.hasReservations(inquiry.getId())) {
            inquiry.setStatus(InquiryStatus.OPEN);
        }
    }
    private Pageable page(int page, int size, String sort) {
        return PageRequest.of(Math.max(0, page - 1), Math.max(1, Math.min(size, 100)),
                Sort.by(Sort.Direction.DESC, sort, "id"));
    }
    @Override
    public Page<ViewingResponse> viewings(LocalDate date, String status, Long salespersonId, int page, int size) {
        return viewings(date, status, salespersonId, null, null, page, size);
    }

    @Override
    public Page<ViewingResponse> viewings(LocalDate date, String status, Long salespersonId, String search,
                                          String type, int page, int size) {
        Specification<Viewing> spec = (r, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (date != null) {
                predicates.add(cb.greaterThanOrEqualTo(r.get("scheduledAt"), date.atStartOfDay(ZONE).toInstant()));
                predicates.add(cb.lessThan(r.get("scheduledAt"), date.plusDays(1).atStartOfDay(ZONE).toInstant()));
            }
            if (status != null && !status.isBlank()) predicates.add(cb.equal(r.get("status"), status));
            if (salespersonId != null) predicates.add(cb.equal(r.get("assignedTo").get("id"), salespersonId));
            if (type != null && !type.isBlank()) predicates.add(cb.equal(r.get("viewingType"), type));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(r.get("inquiry").get("buyer").get("fullName")), pattern, '!'),
                        cb.like(cb.lower(r.get("inquiry").get("vehicle").get("make")), pattern, '!'),
                        cb.like(cb.lower(r.get("inquiry").get("vehicle").get("model")), pattern, '!'),
                        cb.like(cb.lower(cb.concat(cb.concat(r.get("inquiry").get("vehicle").get("make"), " "),
                                r.get("inquiry").get("vehicle").get("model"))), pattern, '!')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return viewings.findAll(spec, page(page, size, "scheduledAt")).map(this::viewingRow);
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
    private ViewingResponse viewingRow(Viewing v) {
        User assigned = v.getAssignedTo() == null ? v.getInquiry().getAssignedTo() : v.getAssignedTo();
        return new ViewingResponse(v.getId(), v.getInquiry().getId(), v.getInquiry().getBuyer().getFullName(),
                v.getInquiry().getVehicle().getDisplayLabel(), v.getViewingType(),
                LocalDateTime.ofInstant(v.getScheduledAt(), ZONE),
                v.getEndAt() == null ? null : LocalDateTime.ofInstant(v.getEndAt(), ZONE),
                assigned == null ? "Unassigned" : assigned.getFullName(),
                v.getStatus(), v.getLocation(), v.getNotes());
    }

    @Override
    public List<ViewingResponse> viewingsForInquiry(long inquiryId) {
        return viewings.findByInquiryIdOrderByScheduledAtDesc(inquiryId).stream()
                .map(this::viewingRow).toList();
    }
}
