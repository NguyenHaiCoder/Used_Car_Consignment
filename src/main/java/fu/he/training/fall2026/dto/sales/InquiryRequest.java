package fu.he.training.fall2026.dto.sales;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Form Liên hệ tư vấn xe → tạo Inquiry. */
@Getter
@Setter
public class InquiryRequest {

    @NotNull(message = "Vui lòng chọn xe cần tư vấn.")
    private Long listingId;

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(min = 2, max = 160, message = "Họ và tên từ 2 đến 160 ký tự.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự.")
    private String phone;

    @Email(message = "Email không hợp lệ.")
    @Size(max = 180, message = "Email tối đa 180 ký tự.")
    private String email;

    /** PRICE | VIEWING | TEST_DRIVE */
    @NotBlank(message = "Vui lòng chọn nhu cầu tư vấn.")
    @Pattern(regexp = "PRICE|VIEWING|TEST_DRIVE", message = "Nhu cầu tư vấn không hợp lệ.")
    private String need;

    @Size(max = 1000, message = "Lời nhắn tối đa 1000 ký tự.")
    private String message;

    @Size(max = 40, message = "Kênh tối đa 40 ký tự.")
    private String channel;

    private Boolean consent;

    @AssertTrue(message = "Vui lòng đồng ý cho GaragePro lưu thông tin liên hệ.")
    public boolean isConsentAccepted() {
        return Boolean.TRUE.equals(consent);
    }
}
