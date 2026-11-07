package fu.he.training.fall2026.dto.handover;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Vũ Mạnh Cường: form xác nhận bàn giao. */
@Getter
@Setter
public class HandoverConfirmRequest {

    @NotNull(message = "Vui lòng nhập số km lúc bàn giao.")
    @Min(value = 0, message = "Số km không hợp lệ.")
    private Integer odo;

    @NotNull(message = "Vui lòng nhập số chìa khóa.")
    @Min(value = 1, message = "Số chìa khóa phải từ 1 trở lên.")
    private Integer keys;

    private List<String> items = new ArrayList<>();

    private String notes;

    @NotBlank(message = "Cần chữ ký nhân viên giao xe.")
    private String staffSignature;

    @NotBlank(message = "Cần chữ ký khách nhận xe.")
    private String receiverSignature;
}
