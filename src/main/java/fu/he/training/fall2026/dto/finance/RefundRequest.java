package fu.he.training.fall2026.dto.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** 05.02 Record a refund against an original (PAID) payment. */
@Getter
@Setter
public class RefundRequest {

    @NotNull(message = "Vui lòng chọn khoản thu gốc.")
    private Long paymentId;

    @NotNull(message = "Vui lòng nhập số tiền hoàn.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Số tiền hoàn phải lớn hơn 0.")
    private BigDecimal amount;

    @NotBlank(message = "Vui lòng nhập lý do hoàn.")
    @Size(max = 2000, message = "Lý do tối đa 2000 ký tự.")
    private String reason;

    /** Links the refund to the contract's reservation (deposit refund decided in 04.04). */
    private boolean depositRefund;

    @Size(max = 500, message = "Tối đa 500 ký tự.")
    private String evidencePath;
}
