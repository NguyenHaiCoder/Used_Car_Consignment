package fu.he.training.fall2026.dto.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Confirms that the bank transfer to the owner was made (no bank call is performed). */
@Getter
@Setter
public class PayoutCompleteRequest {

    @NotNull(message = "Thiếu lệnh chi.")
    private Long id;

    @NotBlank(message = "Vui lòng nhập mã tham chiếu chuyển khoản.")
    @Size(max = 80, message = "Mã tham chiếu tối đa 80 ký tự.")
    private String referenceCode;

    @NotBlank(message = "Vui lòng nhập bằng chứng chi trả.")
    @Size(max = 500, message = "Tối đa 500 ký tự.")
    private String evidencePath;
}
