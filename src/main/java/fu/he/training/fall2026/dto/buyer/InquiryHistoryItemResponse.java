package fu.he.training.fall2026.dto.buyer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import fu.he.training.fall2026.entity.enums.InquiryStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InquiryHistoryItemResponse {
    private Long inquiryId;
    private String code;
    private String make;
    private String model;
    private Instant createdAt;
    private InquiryStatus status;
    private String salesStaff;
}