package fu.he.training.fall2026.dto.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Finance-entered terms of a DRAFT/REJECTED settlement; base, commission and refunds are calculated. */
@Getter
@Setter
public class SettlementAdjustRequest {

    @NotNull(message = "Thiếu quyết toán.")
    private Long id;

    @NotNull(message = "Nhập 0 nếu không có phí.")
    @DecimalMin(value = "0.0", message = "Phí không được âm.")
    private BigDecimal feeAmount;

    @NotNull(message = "Nhập 0 nếu không có chi phí.")
    @DecimalMin(value = "0.0", message = "Chi phí không được âm.")
    private BigDecimal approvedCostAmount;

    @NotNull(message = "Nhập 0 nếu không có cọc giữ lại.")
    @DecimalMin(value = "0.0", message = "Cọc giữ lại không được âm.")
    private BigDecimal depositHeldAmount;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự.")
    private String notes;
}
