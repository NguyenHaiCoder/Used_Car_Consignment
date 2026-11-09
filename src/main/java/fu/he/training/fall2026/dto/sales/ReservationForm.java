package fu.he.training.fall2026.dto.sales;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Form tạo / sửa phiếu đặt cọc. */
@Getter
@Setter
public class ReservationForm {

    /** Id xe (vehicles.id) — select chọn xe. */
    @NotBlank(message = "Vui lòng chọn xe.")
    private String car = "";

    @NotBlank(message = "Vui lòng chọn khách hàng.")
    private String inquiryId = "";

    @NotBlank(message = "Vui lòng nhập số tiền cọc.")
    private String deposit = "";

    @NotBlank(message = "Vui lòng chọn hạn giữ xe.")
    private String holdUntil = "";

    private String terms = "";

    /** Checkbox đồng ý (chỉ tạo mới; sửa thì set sẵn). */
    private String consent;

    @AssertTrue(message = "Khách cần đồng ý lưu thông tin và điều khoản giữ xe.")
    public boolean isConsentAccepted() {
        return consent != null && !consent.isBlank();
    }
}
