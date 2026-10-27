package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.buyer.BuyerDetailsResponse;
import fu.he.training.fall2026.dto.buyer.BuyerItemResponse;
import fu.he.training.fall2026.dto.buyer.BuyerListResponse;
import fu.he.training.fall2026.dto.buyer.CreateBuyerRequest;
import fu.he.training.fall2026.dto.buyer.InquiryHistoryItemResponse;
import fu.he.training.fall2026.dto.buyer.InquiryHistoryResponse;
import fu.he.training.fall2026.dto.buyer.UpdateBuyerRequest;
import fu.he.training.fall2026.entity.sales.Buyer;
import fu.he.training.fall2026.entity.sales.Inquiry;
import fu.he.training.fall2026.exception.BuyerCreateException;
import fu.he.training.fall2026.exception.BuyerException;
import fu.he.training.fall2026.exception.BuyerException.Reason;
import fu.he.training.fall2026.repository.BuyerRepository;
import fu.he.training.fall2026.repository.InquiryRepository;
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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BuyerServiceImpl implements BuyerService {

    private final BuyerRepository buyerRepository;
    private final InquiryRepository inquiryRepository;
    private final BuyerConsentRecorder consentRecorder;

    @Override
    @Transactional(readOnly = true)
    public BuyerDetailsResponse getDetails(long id) {
        validateId(id);
        Buyer buyer = buyerRepository.findById(id)
                .orElseThrow(() -> new BuyerException(Reason.NOT_FOUND, "Buyer not found."));
        return toDetails(buyer);
    }

    @Override
    @Transactional
    public BuyerDetailsResponse create(CreateBuyerRequest input) {
        Map<String, String> errors = BuyerValidation.errors(input);
        if (!Boolean.TRUE.equals(input.getConsentStored())) {
            errors.put("consentStored", BuyerValidation.CONSENT_REQUIRED);
        }
        if (!errors.isEmpty()) {
            throw new BuyerCreateException(Reason.VALIDATION, errors);
        }

        CreateBuyerRequest request = BuyerValidation.normalize(input);

        buyerRepository.lockForCreate();
        checkDuplicates(request, null);

        Buyer buyer = new Buyer();
        buyer.setFullName(request.getFullName());
        buyer.setPhone(request.getPhone());
        buyer.setEmail(request.getEmail());
        buyer.setAddress(request.getAddress());
        buyer.setConsentStored(true);
        buyerRepository.saveAndFlush(buyer);

        consentRecorder.record(buyer.getId());


        return toPersistedDetails(buyer);
    }

    @Override
    @Transactional
    public BuyerDetailsResponse update(long id, UpdateBuyerRequest input) {
        validateId(id);
        Map<String, Object> patch = input.getFields();

        Map<String, String> errors = new LinkedHashMap<>();
        Set<String> allowed = Set.of("fullName", "phone", "email", "address", "revision");
        patch.forEach((field, value) -> {
            if (!allowed.contains(field)) {
                errors.put(field, "This field cannot be edited.");
            } else if (value != null && !(value instanceof String)) {
                errors.put(field, "Expected a string or null.");
            }
        });

        Instant revision = null;
        try {
            revision = Instant.parse((String) patch.get("revision"));
        } catch (RuntimeException ex) {
            errors.put("revision", "Reload the buyer before editing; a valid revision is required.");
        }
        if (!errors.isEmpty()) {
            throw new BuyerCreateException(Reason.VALIDATION, errors);
        }

        buyerRepository.lockForCreate();
        Buyer buyer = buyerRepository.findById(id)
                .filter(b -> b.getDeletedAt() == null)
                .orElseThrow(() -> new BuyerException(Reason.NOT_FOUND, "Buyer not found."));

        if (!revision.equals(buyer.getUpdatedAt())) {
            throw new BuyerException(Reason.CONFLICT,
                    "This buyer was changed by another request. Copy your input, cancel and reopen Edit to review the latest data.");
        }

        CreateBuyerRequest request = new CreateBuyerRequest(
                patch.containsKey("fullName") ? (String) patch.get("fullName") : buyer.getFullName(),
                patch.containsKey("phone") ? (String) patch.get("phone") : buyer.getPhone(),
                patch.containsKey("email") ? (String) patch.get("email") : buyer.getEmail(),
                patch.containsKey("address") ? (String) patch.get("address") : buyer.getAddress(),
                buyer.isConsentStored());
        errors.putAll(BuyerValidation.errors(request));
        if (!errors.isEmpty()) {
            throw new BuyerCreateException(Reason.VALIDATION, errors);
        }
        request = BuyerValidation.normalize(request);

        checkDuplicates(request, id);

        if (patch.containsKey("fullName")) buyer.setFullName(request.getFullName());
        if (patch.containsKey("phone")) buyer.setPhone(request.getPhone());
        if (patch.containsKey("email")) buyer.setEmail(request.getEmail());
        if (patch.containsKey("address")) buyer.setAddress(request.getAddress());
        buyerRepository.saveAndFlush(buyer);

        return toPersistedDetails(buyer);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        validateId(id);

        buyerRepository.lockForCreate();
        Buyer buyer = buyerRepository.findById(id)
                .filter(b -> b.getDeletedAt() == null)
                .orElseThrow(() -> new BuyerException(Reason.NOT_FOUND, "Buyer not found."));

        if (buyerRepository.hasActiveReservation(id) || buyerRepository.hasProcessingContract(id)) {
            throw new BuyerException(Reason.CONFLICT,
                    "Cannot delete buyer with an ACTIVE reservation or a processing sales contract.");
        }

        buyer.setDeletedAt(Instant.now());
        buyerRepository.saveAndFlush(buyer);
    }

    @Override
    @Transactional(readOnly = true)
    public BuyerListResponse list(int page, int size) {
        return list(page, size, null);
    }

    @Override
    @Transactional(readOnly = true)
    public BuyerListResponse list(int page, int size, String search) {
        // 1. Validate phân trang
        validatePagination(page, size);

        // 2. Xử lý keyword tìm kiếm
        String keyword = search == null ? "" : search.strip();
        if (keyword.length() > 180 || keyword.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                    "Search must be at most 180 characters without control characters.");
        }

        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        Page<Buyer> result;
        if (keyword.isEmpty()) {
            result = buyerRepository.findBuyerList(pageable);
        } else {
            String pattern = buildSearchPattern(keyword);
            result = buyerRepository.searchBuyerList(pattern, pageable);
        }

        return new BuyerListResponse(
                result.getContent().stream().map(this::toListItem).toList(),
                new BuyerListResponse.Pagination(page, size, result.getTotalElements(), result.getTotalPages()));
    }

    @Override
    @Transactional(readOnly = true)
    public InquiryHistoryResponse getInquiryHistory(long buyerId, int page, int size) {
        validateId(buyerId);
        validatePagination(page, size);

        if (!buyerRepository.existsIncludingDeleted(buyerId)) {
            throw new BuyerException(Reason.NOT_FOUND, "Buyer not found.");
        }

        Page<Inquiry> result = inquiryRepository.findByBuyerId(buyerId,
                PageRequest.of(page - 1, size,
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

        return new InquiryHistoryResponse(
                result.getContent().stream().map(this::toInquiryHistoryItem).toList(),
                new BuyerListResponse.Pagination(page, size, result.getTotalElements(), result.getTotalPages()));
    }

    private void validateId(long id) {
        if (id <= 0) throw new IllegalArgumentException("Buyer ID must be a positive integer.");
    }

    private void validatePagination(int page, int size) {
        if (page < 1 || size < 1 || size > 100 || (long) (page - 1) * size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "page must be >= 1; size must be between 1 and 100; offset must fit an integer.");
        }
    }

    private void checkDuplicates(CreateBuyerRequest request, Long id) {
        Map<String, String> errors = new LinkedHashMap<>();
        boolean phoneExists = (id == null)
                ? buyerRepository.contactPhoneExists(request.getPhone())
                : buyerRepository.otherPhoneExists(request.getPhone(), id);
        if (phoneExists) {
            errors.put("phone", "This phone number already belongs to a buyer.");
        }

        if (request.getEmail() != null) {
            boolean emailExists = (id == null)
                    ? buyerRepository.contactEmailExists(request.getEmail())
                    : buyerRepository.otherEmailExists(request.getEmail(), id);
            if (emailExists) {
                errors.put("email", "This email already belongs to a buyer.");
            }
        }

        if (!errors.isEmpty()) {
            throw new BuyerCreateException(Reason.CONFLICT, errors);
        }
    }

    /** Xây dựng LIKE pattern cho tìm kiếm (escape ký tự đặc biệt). */
    private String buildSearchPattern(String keyword) {
        return "%" + keyword.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replace("[", "![") + "%";
    }

   private BuyerDetailsResponse toDetails(Buyer buyer) {
        return new BuyerDetailsResponse(
                buyer.getId(),
                buyer.getFullName(),
                buyer.getPhone(),
                buyer.getEmail(),
                buyer.getAddress(),
                buyer.getCreatedAt(),
                buyer.getUpdatedAt());
    }

      private BuyerDetailsResponse toPersistedDetails(Buyer buyer) {
        return buyerRepository.findById(buyer.getId())
                .map(this::toDetails)
                .orElseGet(() -> toDetails(buyer));
    }

    private BuyerItemResponse toListItem(Buyer buyer) {
        return new BuyerItemResponse(
                buyer.getId(),
                buyer.getFullName(),
                buyer.getPhone(),
                buyer.getEmail(),
                buyer.getCreatedAt());
    }

    private InquiryHistoryItemResponse toInquiryHistoryItem(Inquiry inquiry) {
        return new InquiryHistoryItemResponse(
                inquiry.getId(),
                inquiry.getCode(),
                inquiry.getVehicle() != null ? inquiry.getVehicle().getMake() : null,
                inquiry.getVehicle() != null ? inquiry.getVehicle().getModel() : null,
                inquiry.getCreatedAt(),
                inquiry.getStatus(),
                inquiry.getAssignedTo() != null ? inquiry.getAssignedTo().getFullName() : null);
    }
}
