package fu.he.training.fall2026.dto.consignment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class WithdrawForm {

    @NotBlank(message = "Vui lòng nhập lý do rút hồ sơ.")
    @Size(max = 2000, message = "Lý do tối đa 2000 ký tự.")
    private String reason;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate returnDate;

    @Size(max = 2000, message = "Tình trạng xe tối đa 2000 ký tự.")
    private String condition;

    @Size(max = 500, message = "Tối đa 500 ký tự.")
    private String evidencePath;
}
