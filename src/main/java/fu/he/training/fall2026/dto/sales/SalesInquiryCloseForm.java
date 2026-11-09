package fu.he.training.fall2026.dto.sales;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Form đóng tư vấn. */
@Getter
@Setter
public class SalesInquiryCloseForm {

    @NotBlank(message = "Vui lòng chọn lý do đóng tư vấn.")
    private String closeReason = "";

    private String note = "";
}
