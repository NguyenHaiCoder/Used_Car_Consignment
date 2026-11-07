package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.sales.InquiryRequest;
import fu.he.training.fall2026.entity.consignment.VehicleSaleNews;
import fu.he.training.fall2026.entity.enums.InquiryStatus;
import fu.he.training.fall2026.entity.enums.ListingStatus;
import fu.he.training.fall2026.entity.sales.Inquiry;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.InquiryRepository;
import fu.he.training.fall2026.repository.VehicleSaleNewsRepository;
import fu.he.training.fall2026.service.InquiryService;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryServiceImpl implements InquiryService {

    private static final Map<String, String> NEED_LABELS = Map.of(
            "PRICE", "Hỏi giá",
            "VIEWING", "Xem xe",
            "TEST_DRIVE", "Lái thử");

    private final InquiryRepository inquiryRepository;
    private final VehicleSaleNewsRepository newsRepository;

    @Override
    @Transactional
    public String createFromListing(InquiryRequest form) {
        VehicleSaleNews listing = newsRepository.findDetailById(form.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Tin bán xe", form.getListingId()));
        if (listing.getStatus() != ListingStatus.LISTED) {
            throw new BusinessException("Tin này không còn đang hiển thị để nhận liên hệ.", HttpStatus.CONFLICT);
        }

        String phone = normalizePhone(form.getPhone());
        if (!isValidVnMobile(phone)) {
            throw new BusinessException(
                    "Vui lòng nhập số di động Việt Nam hợp lệ (10 số hoặc +84).", HttpStatus.BAD_REQUEST);
        }

        Inquiry inquiry = new Inquiry();
        inquiry.setCode(temporaryCode());
        inquiry.setVehicle(listing.getConsignment().getVehicle());
        inquiry.setStatus(InquiryStatus.OPEN);
        inquiry.setChannel(blankTo(form.getChannel(), "WEBSITE"));
        inquiry.setFullName(form.getFullName().trim());
        inquiry.setPhone(phone);
        inquiry.setEmail(blankToNull(form.getEmail()));
        inquiry.setNeed(form.getNeed().trim());
        inquiry.setNotes(buildNotes(form.getNeed(), form.getMessage()));
        inquiry.setConsentStored(true);
        inquiryRepository.save(inquiry);
        inquiry.setCode(generateCode(inquiry.getId()));
        return inquiry.getCode();
    }

    private static String buildNotes(String need, String message) {
        String needLabel = NEED_LABELS.getOrDefault(need, need);
        String body = message == null ? "" : message.trim();
        if (body.isEmpty()) {
            return "Nhu cầu: " + needLabel;
        }
        return "Nhu cầu: " + needLabel + "\n" + body;
    }

    static String normalizePhone(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.startsWith("84") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        return digits;
    }

    static boolean isValidVnMobile(String phone) {
        return phone != null && phone.matches("0[35789]\\d{8}");
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private static String blankTo(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String temporaryCode() {
        return "TMP-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private static String generateCode(Long id) {
        return String.format("TV-%06d", id);
    }
}
