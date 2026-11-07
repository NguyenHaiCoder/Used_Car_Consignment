package fu.he.training.fall2026.dto.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Vũ Mạnh Cường: form chi trả chủ xe. */
@Getter
@Setter
public class PayoutRequest {

    @NotBlank(message = "Vui lòng nhập mã tham chiếu chi trả.")
    @Size(max = 80, message = "Mã tham chiếu tối đa 80 ký tự.")
    private String reference;
}
