package fu.he.training.fall2026.dto.consignor;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsignorForm {

    /** Hidden field: empty when creating, the consignor id when editing. */
    private Long id;

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 160, message = "Họ và tên tối đa 160 ký tự.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(regexp = "^\\d{10,11}$", message = "Số điện thoại cần 10–11 chữ số.")
    private String phone;

    @Email(message = "Email chưa đúng định dạng.")
    @Size(max = 180, message = "Email tối đa 180 ký tự.")
    private String email;

    @NotBlank(message = "Vui lòng nhập số CCCD.")
    @Pattern(regexp = "^\\d{12}$", message = "CCCD cần 12 chữ số.")
    private String idNumber;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự.")
    private String address;

    @NotBlank(message = "Vui lòng nhập ngân hàng.")
    @Size(max = 120, message = "Tên ngân hàng tối đa 120 ký tự.")
    private String bank;

    @NotBlank(message = "Vui lòng nhập số tài khoản.")
    @Pattern(regexp = "^\\d{6,20}$", message = "Số tài khoản cần 6–20 chữ số.")
    private String accountNumber;

    @NotBlank(message = "Vui lòng nhập tên chủ tài khoản.")
    @Size(max = 160, message = "Tên chủ tài khoản tối đa 160 ký tự.")
    private String accountName;

    @AssertTrue(message = "Cần xác nhận chủ xe đồng ý lưu thông tin cá nhân.")
    private boolean consentStored;
}
