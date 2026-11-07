package fu.he.training.fall2026.dto.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Vũ Mạnh Cường: form ghi nhận hoàn cọc. */
@Getter
@Setter
public class RefundRequest {

    @NotBlank(message = "Vui lòng chọn phương thức.")
    private String method;

    @NotBlank(message = "Vui lòng nhập mã tham chiếu.")
    @Size(max = 80, message = "Mã tham chiếu tối đa 80 ký tự.")
    private String reference;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự.")
    private String note;
}
