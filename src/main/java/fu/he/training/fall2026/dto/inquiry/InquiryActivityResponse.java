package fu.he.training.fall2026.dto.inquiry;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InquiryActivityResponse {
    private LocalDateTime occurredAt;
    private String action;
    private String actor;
}
