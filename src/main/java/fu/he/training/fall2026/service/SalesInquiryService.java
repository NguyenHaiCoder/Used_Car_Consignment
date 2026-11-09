package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.sales.SalesInquiryForm;
import fu.he.training.fall2026.entity.enums.InquiryStatus;
import fu.he.training.fall2026.entity.enums.ViewingStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Luồng Tư vấn & lịch xem xe của Nhân viên kinh doanh (ghi DB). */
public interface SalesInquiryService {

    Page<Map<String, Object>> search(
            String q,
            InquiryStatus status,
            ViewingStatus viewing,
            String channel,
            String need,
            LocalDate from,
            LocalDate to,
            Pageable pageable);

    Map<String, Long> countByStatus();

    Map<String, Object> getCard(Long id);

    List<Map<String, Object>> viewingsOf(Long inquiryId);

    Map<Long, Map<String, Object>> latestViewings(Iterable<Long> inquiryIds);

    Map<String, Object> getViewing(Long inquiryId, Long viewingId);

    /** Xe đang bán (LISTED) để chọn khi tạo tư vấn. key = vehicleId. */
    Map<String, Map<String, Object>> listedCars();

    Optional<Map<String, Object>> findBuyerByPhone(String phone);

    Long createByStaff(SalesInquiryForm form, Long staffUserId);

    void update(Long id, SalesInquiryForm form);

    void claim(Long id, Long staffUserId);

    void saveNote(Long id, String note);

    void close(Long id, String closeReason, String note);

    void scheduleViewing(Long inquiryId, LocalDateTime start, int minutes, String type, String place, String note);

    void rescheduleViewing(
            Long inquiryId, Long viewingId, LocalDateTime start, int minutes, String type, String place, String note);

    void confirmViewing(Long inquiryId, Long viewingId);

    void finishViewing(Long inquiryId, Long viewingId);

    void cancelViewing(Long inquiryId, Long viewingId);

    void markConverted(Long inquiryId);

    /** Hủy phiếu cọc → quay OPEN hoặc VIEWED. */
    void reopenAfterHoldCancel(Long inquiryId);

    boolean canHold(Long inquiryId);

    Optional<Map<String, Object>> latestReservationCard(Long inquiryId);
}
