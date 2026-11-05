package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.buyer.BuyerResponse;
import fu.he.training.fall2026.dto.buyer.BuyerRequest;
import fu.he.training.fall2026.dto.buyer.BuyerListResponse;
import fu.he.training.fall2026.dto.common.PaginationResponse;
import fu.he.training.fall2026.entity.sales.Buyer;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.ReservationStatus;
import fu.he.training.fall2026.exception.BuyerCreateException;
import fu.he.training.fall2026.exception.BuyerException;
import fu.he.training.fall2026.exception.BuyerException.Reason;
import fu.he.training.fall2026.repository.BuyerRepository;
import fu.he.training.fall2026.repository.ReservationRepository;
import fu.he.training.fall2026.repository.SalesContractRepository;
import fu.he.training.fall2026.repository.ViewingRepository;
import fu.he.training.fall2026.service.BuyerConsentRecorder;
import fu.he.training.fall2026.service.BuyerService;
import fu.he.training.fall2026.validation.BuyerValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BuyerServiceImpl implements BuyerService {

    private final BuyerRepository buyerRepository;
    private final ReservationRepository reservationRepository;
    private final SalesContractRepository salesContractRepository;
    private final ViewingRepository viewingRepository;
    private final BuyerConsentRecorder consentRecorder;

    @Override
    @Transactional(readOnly = true)
    public BuyerResponse getDetails(long id) {
        validateId(id);
        Buyer buyer = buyerRepository.findById(id)
                .orElseThrow(() -> new BuyerException(Reason.NOT_FOUND, "Buyer not found."));
        return toResponse(buyer);
    }

    @Override
    @Transactional(readOnly = true)
    public BuyerRequest formFor(long id) {
        validateId(id);
        BuyerResponse buyer = toResponse(buyerRepository.findById(id)
                .orElseThrow(() -> new BuyerException(Reason.NOT_FOUND, "Buyer not found.")));
        BuyerRequest form = new BuyerRequest();
        form.setFullName(buyer.getFullName());
        form.setPhone(buyer.getPhone());
        form.setEmail(buyer.getEmail());
        form.setAddress(buyer.getAddress());
        form.setRevision(buyer.getRevision() == null ? null : buyer.getRevision().toString());
        form.setStatus(buyer.isActive() ? "active" : "inactive");
        return form;
    }

    @Override
    @Transactional
    public BuyerResponse create(BuyerRequest form) {
        Map<String, String> errors = BuyerValidation.errors(
                form.getFullName(), form.getPhone(), form.getEmail(), form.getAddress());
        if (!form.isConsentStored()) {
            errors.put("consentStored", BuyerValidation.CONSENT_REQUIRED);
        }
        if (!errors.isEmpty()) {
            throw new BuyerCreateException(Reason.VALIDATION, errors);
        }

        buyerRepository.lockForCreate();
        String email = BuyerValidation.clean(form.getEmail());
        email = email == null ? null : email.toLowerCase(Locale.ROOT);
        checkDuplicates(form.getPhone(), email, null);

        Buyer buyer = new Buyer();
        buyer.setFullName(BuyerValidation.name(form.getFullName()));
        buyer.setPhone(form.getPhone());
        buyer.setEmail(email);
        buyer.setAddress(BuyerValidation.clean(form.getAddress()));
        buyer.setConsentStored(true);
        buyerRepository.saveAndFlush(buyer);

        consentRecorder.record(buyer.getId());
        return toResponse(buyer);
    }

    @Override
    @Transactional
    public BuyerResponse update(long id, BuyerRequest form) {
        validateId(id);
        Map<String, String> errors = BuyerValidation.errors(
                form.getFullName(), form.getPhone(), form.getEmail(), form.getAddress());
        if (!"active".equals(form.getStatus()) && !"inactive".equals(form.getStatus())) {
            errors.put("status", "Select a valid buyer status.");
        }

        Instant revision = null;
        try {
            revision = Instant.parse(form.getRevision());
        } catch (RuntimeException ex) {
            errors.put("revision", "Reload the buyer before editing; a valid revision is required.");
        }
        if (!errors.isEmpty()) {
            throw new BuyerCreateException(Reason.VALIDATION, errors);
        }

        buyerRepository.lockForCreate();
        Buyer buyer = buyerRepository.findById(id)
                .orElseThrow(() -> new BuyerException(Reason.NOT_FOUND, "Buyer not found."));

        if (!revision.equals(buyer.getUpdatedAt())) {
            throw new BuyerException(Reason.CONFLICT,
                    "This buyer was changed by another request. Copy your input, cancel and reopen Edit to review the latest data.");
        }

        boolean reactivating = buyer.getDeletedAt() != null && "active".equals(form.getStatus());
        if (buyer.getDeletedAt() == null && "inactive".equals(form.getStatus())) {
            throw new BuyerCreateException(Reason.VALIDATION,
                    Map.of("status", "Use Delete Buyer to deactivate this buyer and record a reason."));
        }
        if (reactivating && !buyer.isConsentStored()) {
            throw new BuyerCreateException(Reason.VALIDATION,
                    Map.of("status", "Cannot reactivate this buyer without stored consent."));
        }

        String email = BuyerValidation.clean(form.getEmail());
        email = email == null ? null : email.toLowerCase(Locale.ROOT);
        checkDuplicates(form.getPhone(), email, id);

        buyer.setFullName(BuyerValidation.name(form.getFullName()));
        buyer.setPhone(form.getPhone());
        buyer.setEmail(email);
        buyer.setAddress(BuyerValidation.clean(form.getAddress()));
        if (reactivating) {
            buyer.setDeletedAt(null);
        }
        buyerRepository.saveAndFlush(buyer);

        return toResponse(buyer);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id, String reason) {
        validateId(id);
        String deleteReason = reason == null ? null : reason.strip();
        if (deleteReason == null || deleteReason.isEmpty() || deleteReason.length() > 500
                || deleteReason.codePoints().anyMatch(Character::isISOControl)) {
            throw new BuyerException(Reason.VALIDATION, "Vui lòng nhập lý do xóa hợp lệ, tối đa 500 ký tự.");
        }

        buyerRepository.lockForCreate();
        Buyer buyer = buyerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BuyerException(Reason.NOT_FOUND, "Buyer not found."));

        String blockReason = deletionBlockReasonFor(id);
        if (blockReason != null) throw new BuyerException(Reason.CONFLICT, blockReason);

        buyer.setDeleteReason(deleteReason);
        buyer.setDeletedAt(Instant.now());
        buyerRepository.saveAndFlush(buyer);
    }

    @Override
    @Transactional(readOnly = true)
    public String deletionBlockReason(long id) {
        validateId(id);
        buyerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BuyerException(Reason.NOT_FOUND, "Buyer not found."));
        return deletionBlockReasonFor(id);
    }

    private String deletionBlockReasonFor(long id) {
        List<String> reasons = new ArrayList<>();
        if (viewingRepository.existsByInquiry_Buyer_IdAndViewingTypeAndStatusAndDeletedAtIsNull(
                id, "TEST_DRIVE", "SCHEDULED")) {
            reasons.add("lịch lái thử");
        }
        if (reservationRepository.existsByBuyer_IdAndStatus(id, ReservationStatus.ACTIVE)) {
            reasons.add("đặt xe");
        }
        if (salesContractRepository.existsByReservation_Buyer_IdAndStatusNotIn(
                id, Set.of(ContractStatus.CLOSED, ContractStatus.CANCELLED))) {
            reasons.add("giao dịch mua xe");
        }
        if (reasons.isEmpty()) return null;
        if (reasons.size() == 3) {
            return "Không thể xóa người mua này vì đang có lịch lái thử, đặt xe hoặc mua xe.";
        }
        return "Không thể xóa người mua này vì đang có " + String.join(" hoặc ", reasons) + ".";
    }

    @Override
    @Transactional(readOnly = true)
    public BuyerListResponse list(int page, int size) {
        return list(page, size, null);
    }

    @Override
    @Transactional(readOnly = true)
    public BuyerListResponse list(int page, int size, String search) {
        return list(page, size, search, "desc", "all", "default");
    }

    @Override
    @Transactional(readOnly = true)
    public BuyerListResponse list(int page, int size, String search, String idSort, String status, String createdSort) {
        validatePagination(page, size);
        if (!Set.of("asc", "desc").contains(idSort)
                || !Set.of("all", "active", "inactive").contains(status)
                || !Set.of("default", "newest", "oldest").contains(createdSort)) {
            throw new IllegalArgumentException("Invalid buyer list filter or sort option.");
        }

        String keyword = search == null ? "" : search.strip();
        if (keyword.length() > 180 || keyword.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                    "Search must be at most 180 characters without control characters.");
        }

        Sort.Direction idDirection = "asc".equals(idSort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(new Sort.Order(idDirection, "id"));
        if (!"default".equals(createdSort)) {
            Sort.Direction dateDirection = "newest".equals(createdSort) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(new Sort.Order(dateDirection, "createdAt"), new Sort.Order(idDirection, "id"));
        }
        PageRequest pageable = PageRequest.of(page - 1, size, sort);
        String pattern = "%" + keyword.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replace("[", "![") + "%";
        Page<Buyer> result = buyerRepository.searchBuyerList(pattern, keyword.isEmpty(), status, pageable);

        return new BuyerListResponse(
                result.getContent().stream().map(this::toResponse).toList(),
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

    private void checkDuplicates(String phone, String email, Long id) {
        Map<String, String> errors = new LinkedHashMap<>();
        boolean phoneExists = (id == null)
                ? buyerRepository.contactPhoneExists(phone)
                : buyerRepository.otherPhoneExists(phone, id);
        if (phoneExists) {
            errors.put("phone", "This phone number already belongs to a buyer.");
        }

        if (email != null) {
            boolean emailExists = (id == null)
                    ? buyerRepository.contactEmailExists(email)
                    : buyerRepository.otherEmailExists(email, id);
            if (emailExists) {
                errors.put("email", "This email already belongs to a buyer.");
            }
        }

        if (!errors.isEmpty()) {
            throw new BuyerCreateException(Reason.CONFLICT, errors);
        }
    }

    private BuyerResponse toResponse(Buyer buyer) {
        return new BuyerResponse(
                buyer.getId(),
                buyer.getFullName(),
                buyer.getPhone(),
                buyer.getEmail(),
                buyer.getAddress(),
                buyer.getCreatedAt(),
                buyer.getUpdatedAt(),
                buyer.getDeletedAt() == null,
                buyer.getDeleteReason());
    }

}
