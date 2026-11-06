package fu.he.training.fall2026.dto.buyer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteBuyerRequest {
    @NotBlank(message = "Vui lòng nhập lý do xóa khách mua.")
    @Size(max = 500, message = "Lý do xóa không được vượt quá 500 ký tự.")
    private String reason;
}
