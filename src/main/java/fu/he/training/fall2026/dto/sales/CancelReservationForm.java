package fu.he.training.fall2026.dto.sales;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Form hủy đặt cọc / xử lý tiền cọc. */
@Getter
@Setter
public class CancelReservationForm {

    @NotBlank(message = "Vui lòng nhập lý do.")
    private String reason = "";

    @NotBlank(message = "Vui lòng chọn hoàn hay giữ cọc.")
    private String decision = "";
}
