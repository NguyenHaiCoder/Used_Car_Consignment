package fu.he.training.fall2026.dto.inquiry;

import fu.he.training.fall2026.entity.enums.InquiryStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InquiryHistoryItemResponse {
    private Long inquiryId;
    private String make;
    private String model;
    private Instant createdAt;
    private InquiryStatus status;
    private String salesStaff;

    public ZonedDateTime getCreatedAtLocal() {
        return createdAt == null ? null : createdAt.atZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
