package fu.he.training.fall2026.dto.inquiry;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class InquiryAssignmentRequest {
    @NotNull(message = "Vui lòng chọn nhân viên bán hàng.")
    @Positive(message = "Vui lòng chọn nhân viên bán hàng hợp lệ.")
    private Long salespersonId;
}
