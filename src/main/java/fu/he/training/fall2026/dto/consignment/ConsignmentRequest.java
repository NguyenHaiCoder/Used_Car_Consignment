package fu.he.training.fall2026.dto.consignment;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class ConsignmentRequest {

    /** Hidden field: empty when creating, the consignment id when editing. */
    private Long id;

    @NotNull(message = "Vui lòng chọn chủ xe.")
    private Long consignorId;

    @NotNull(message = "Vui lòng chọn xe vật lý.")
    private Long vehicleId;

    @NotNull(message = "Vui lòng nhập giá kỳ vọng.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá kỳ vọng phải lớn hơn 0.")
    private BigDecimal expectedPrice;

    @NotNull(message = "Vui lòng chọn ngày dự kiến tiếp nhận.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate intakeDate;

    @Size(max = 2000, message = "Điều khoản tối đa 2000 ký tự.")
    private String termNotes;

    @AssertTrue(message = "Cần xác nhận chủ xe đồng ý cung cấp thông tin.")
    private boolean ownerConsent;
}
