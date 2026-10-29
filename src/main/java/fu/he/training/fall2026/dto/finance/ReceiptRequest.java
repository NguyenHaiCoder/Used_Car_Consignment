package fu.he.training.fall2026.dto.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/** 05.02 Record a buyer receipt against a sales contract. */
@Getter
@Setter
public class ReceiptRequest {

    @NotNull(message = "Vui lòng chọn hợp đồng bán.")
    private Long contractId;

    @NotBlank(message = "Vui lòng chọn phương thức.")
    @Pattern(regexp = "TRANSFER|CASH|PAYOS", message = "Phương thức không hợp lệ.")
    private String method;

    @NotNull(message = "Vui lòng nhập số tiền.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Số tiền phải lớn hơn 0.")
    private BigDecimal amount;

    @NotBlank(message = "Vui lòng nhập mã tham chiếu.")
    @Size(max = 80, message = "Mã tham chiếu tối đa 80 ký tự.")
    private String referenceCode;

    @NotBlank(message = "Vui lòng nhập người thanh toán.")
    @Size(max = 80, message = "Tên người thanh toán tối đa 80 ký tự.")
    private String payerName;

    @NotNull(message = "Vui lòng nhập thời gian thu.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime paidAt;

    @Size(max = 500, message = "Tối đa 500 ký tự.")
    private String evidencePath;
}
