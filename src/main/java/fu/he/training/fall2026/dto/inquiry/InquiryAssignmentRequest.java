package fu.he.training.fall2026.dto.inquiry;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class InquiryAssignmentRequest {
    @NotNull(message = "Select a salesperson.")
    @Positive(message = "Select a valid salesperson.")
    private Long salespersonId;
}
