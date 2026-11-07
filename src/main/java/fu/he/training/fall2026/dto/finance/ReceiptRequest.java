package fu.he.training.fall2026.dto.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Vũ Mạnh Cường: form ghi nhận khoản thu (ô tiền dạng 76.000.000). */
@Getter
@Setter
public class ReceiptRequest {

    @NotBlank(message = "Vui lòng nhập số tiền thu.")
    private String amount;

    @NotBlank(message = "Vui lòng chọn phương thức.")
    private String method;

    @NotBlank(message = "Vui lòng nhập mã tham chiếu.")
    @Size(max = 80, message = "Mã tham chiếu tối đa 80 ký tự.")
    private String reference;

    public BigDecimal amountValue() {
        String digits = amount == null ? "" : amount.replaceAll("\\D", "");
        return digits.isEmpty() ? null : new BigDecimal(digits);
    }
}
