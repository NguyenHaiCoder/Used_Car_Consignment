package fu.he.training.fall2026.dto.buyer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BuyerRequest {
    @NotBlank(message = "Vui lòng nhập họ tên.")
    @Size(max = 160, message = "Họ tên không được vượt quá 160 ký tự.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(regexp = "\\s*|0[0-9]{9,10}", message = "Số điện thoại phải gồm 10–11 chữ số và bắt đầu bằng số 0, không chứa dấu cách.")
    @Size(max = 30, message = "Số điện thoại không được vượt quá 30 ký tự.")
    private String phone;

    @Email(message = "Vui lòng nhập email đúng định dạng.")
    @Size(max = 180, message = "Email không được vượt quá 180 ký tự.")
    private String email;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự.")
    private String address;
    private boolean consentStored;
    private String revision;
    private String status;
}
