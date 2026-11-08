package fu.he.training.fall2026.dto.sales;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Form tạo / sửa tư vấn (Sales). */
@Getter
@Setter
public class SalesInquiryForm {

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự.")
    private String phone = "";

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(min = 2, max = 160, message = "Họ và tên từ 2 đến 160 ký tự.")
    private String fullName = "";

    @Size(max = 180, message = "Email tối đa 180 ký tự.")
    private String email = "";

    @Size(max = 120, message = "Khu vực tối đa 120 ký tự.")
    private String area = "";

    @Size(max = 500, message = "Ghi chú khách tối đa 500 ký tự.")
    private String buyerNote = "";

    /** Id xe (vehicleId) đang bán. */
    @NotBlank(message = "Vui lòng chọn xe quan tâm.")
    private String car = "";

    private String channel = "PHONE";

    @NotBlank(message = "Vui lòng chọn nhu cầu của khách.")
    private String need = "PRICE";

    @Size(max = 2000, message = "Ghi chú tư vấn tối đa 2000 ký tự.")
    private String note = "";

    private String budgetMin = "";
    private String budgetMax = "";
    private String priorities = "";
    private String buyTime = "";

    /** Checkbox đồng ý lưu thông tin (chỉ tạo mới; sửa thì set sẵn true). */
    private Boolean consent;

    @AssertTrue(message = "Khách cần đồng ý cho lưu thông tin để liên hệ tư vấn.")
    public boolean isConsentAccepted() {
        return Boolean.TRUE.equals(consent);
    }
}
