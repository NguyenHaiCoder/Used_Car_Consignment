package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.consignment.ConsignmentForm;
import fu.he.training.fall2026.dto.consignment.WithdrawCheck;
import fu.he.training.fall2026.dto.consignment.WithdrawForm;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.Consignor;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.auth.Approval;
import fu.he.training.fall2026.entity.enums.ApprovalStatus;
import fu.he.training.fall2026.entity.enums.ApprovalType;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.entity.enums.ListingStatus;
import fu.he.training.fall2026.entity.enums.ReservationStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.ApprovalRepository;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import fu.he.training.fall2026.repository.ConsignorRepository;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.repository.VehicleRepository;
import fu.he.training.fall2026.service.ConsignmentService;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsignmentServiceImpl implements ConsignmentService {

    private static final String APPROVAL_TARGET = "Consignment";

    private final ConsignmentRepository consignmentRepository;
    private final ConsignorRepository consignorRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final ApprovalRepository approvalRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Consignment> search(String keyword, ConsignmentStatus status, int page, int size) {
        String pattern = keyword == null || keyword.isBlank()
                ? "%"
                : "%" + keyword.trim().toLowerCase() + "%";
        return consignmentRepository.search(
                pattern, status, PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "createdAt")));
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
    public Consignment get(Long id) {
        return consignmentRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ ký gửi", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> listVehicles() {
        return vehicleRepository.findAll(Sort.by("make", "model"));
    }

    @Override
    @Transactional(readOnly = true)
    public ConsignmentForm formFor(Long id) {
        Consignment c = get(id);
        requireDraft(c);
        ConsignmentForm form = new ConsignmentForm();
        form.setConsignorId(c.getConsignor().getId());
        form.setVehicleId(c.getVehicle().getId());
        form.setExpectedPrice(c.getExpectedPrice());
        form.setIntakeDate(c.getIntakeDate());
        form.setTermNotes(c.getTermNotes());
        form.setOwnerConsent(c.isOwnerConsent());
        return form;
    }

    @Override
    @Transactional
    public Long createDraft(ConsignmentForm form) {
        Consignment consignment = new Consignment();
        consignment.setCode("TMP-" + UUID.randomUUID().toString().substring(0, 12)); // replaced by KG-{id} below
        consignment.setStatus(ConsignmentStatus.DRAFT);
        applyForm(consignment, form);
        consignmentRepository.save(consignment);
        consignment.setCode("KG-" + consignment.getId());
        return consignment.getId();
    }

    @Override
    @Transactional
    public void updateDraft(Long id, ConsignmentForm form) {
        Consignment consignment = get(id);
        requireDraft(consignment);
        applyForm(consignment, form);
    }

    @Override
    @Transactional
    public void submit(Long id) {
        Consignment consignment = get(id);
        requireDraft(consignment);
        consignment.setStatus(ConsignmentStatus.SUBMITTED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithdrawCheck> withdrawChecks(Long id) {
        Consignment consignment = get(id);
        Long vehicleId = consignment.getVehicle().getId();
        ConsignmentStatus status = consignment.getStatus();
        boolean open = status != ConsignmentStatus.SOLD
                && status != ConsignmentStatus.WITHDRAWN
                && status != ConsignmentStatus.CLOSED;

        List<String> reservations = consignmentRepository.findReservationCodes(vehicleId, ReservationStatus.ACTIVE);
        String reservationDetail = "";
        if (!reservations.isEmpty()) {
            BigDecimal deposit = consignmentRepository.sumReservationDeposits(vehicleId, ReservationStatus.ACTIVE);
            reservationDetail = "Còn reservation " + String.join(", ", reservations)
                    + " (tiền cọc đang giữ: " + deposit.stripTrailingZeros().toPlainString() + " ₫).";
        }

        List<String> listings = consignmentRepository.findListingCodes(id, List.of(ListingStatus.LISTED, ListingStatus.PAUSED));
        boolean pending = hasPendingWithdrawRequest(id);

        return List.of(
                new WithdrawCheck("Hồ sơ chưa bán hoặc đã đóng", open, open ? "" : "Trạng thái hiện tại: " + status + "."),
                new WithdrawCheck("Không có reservation ACTIVE", reservations.isEmpty(), reservationDetail),
                new WithdrawCheck("Không có tin đang đăng", listings.isEmpty(),
                        listings.isEmpty() ? "" : "Còn tin: " + String.join(", ", listings) + "."),
                new WithdrawCheck("Chưa có yêu cầu rút đang chờ duyệt", !pending,
                        pending ? "Đã có yêu cầu gửi quản lý, đang chờ duyệt." : ""));
    }

    @Override
    @Transactional
    public void requestWithdraw(Long id, WithdrawForm form, Long actorUserId) {
        List<String> failed = withdrawChecks(id).stream()
                .filter(check -> !check.passed())
                .map(check -> check.label() + ". " + check.detail())
                .toList();
        if (!failed.isEmpty()) {
            throw new BusinessException("Chưa thể gửi yêu cầu rút: " + String.join(" ", failed), HttpStatus.CONFLICT);
        }
        Consignment consignment = get(id);
        consignment.setWithdrawReason(form.getReason().trim());
        consignment.setWithdrawReturnDate(form.getReturnDate());
        consignment.setWithdrawCondition(blankToNull(form.getCondition()));
        consignment.setWithdrawEvidencePath(blankToNull(form.getEvidencePath()));

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

    @Override
    @Transactional(readOnly = true)
    public Optional<Approval> findWithdrawRequest(Long id) {
        return approvalRepository.findRequests(ApprovalType.WITHDRAWAL, APPROVAL_TARGET, id).stream().findFirst();
    }

    private boolean hasPendingWithdrawRequest(Long id) {
        return approvalRepository.existsByTypeAndTargetTypeAndTargetIdAndStatus(
                ApprovalType.WITHDRAWAL, APPROVAL_TARGET, id, ApprovalStatus.PENDING);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** BR-11: only a DRAFT can be edited or submitted. */
    private static void requireDraft(Consignment consignment) {
        if (consignment.getStatus() != ConsignmentStatus.DRAFT) {
            throw new BusinessException("Chỉ hồ sơ DRAFT mới được sửa hoặc gửi duyệt.", HttpStatus.CONFLICT);
        }
    }

    private void applyForm(Consignment target, ConsignmentForm form) {
        Consignor consignor = consignorRepository.findById(form.getConsignorId())
                .orElseThrow(() -> new BusinessException("Chủ xe không tồn tại."));
        Vehicle vehicle = vehicleRepository.findById(form.getVehicleId())
                .orElseThrow(() -> new BusinessException("Xe không tồn tại."));
        target.setConsignor(consignor);
        target.setVehicle(vehicle);
        target.setExpectedPrice(form.getExpectedPrice());
        target.setIntakeDate(form.getIntakeDate());
        target.setTermNotes(form.getTermNotes() == null || form.getTermNotes().isBlank() ? null : form.getTermNotes().trim());
        target.setOwnerConsent(form.isOwnerConsent());
    }
}
