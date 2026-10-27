package fu.he.training.fall2026.dto.consignment;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class AgreementRequest {

    /** Hidden field: empty when creating, the agreement id when editing the draft. */
    private Long id;

    /** Hidden field: the consignment this agreement belongs to. */
    @NotNull(message = "Thiếu hồ sơ ký gửi.")
    private Long consignmentId;

    /** Commission as a percentage of the sale price; fill this or {@code fixedFee}, not both. */
    @DecimalMin(value = "0.0", message = "Hoa hồng không được âm.")
    @DecimalMax(value = "100.0", message = "Hoa hồng phải từ 0 đến 100%.")
    private BigDecimal commissionRate;

    @DecimalMin(value = "0.0", message = "Phí cố định không được âm.")
    private BigDecimal fixedFee;

    @NotNull(message = "Vui lòng nhập giá sàn của chủ xe.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá sàn phải lớn hơn 0.")
    private BigDecimal priceFloor;

    @NotNull(message = "Vui lòng chọn ngày bắt đầu hiệu lực.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate validFrom;

    @NotNull(message = "Vui lòng chọn ngày kết thúc hiệu lực.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate validTo;

    @NotBlank(message = "Vui lòng nhập điều khoản giữ xe.")
    @Size(max = 4000, message = "Tối đa 4000 ký tự.")
    private String custodyTerms;

    @NotBlank(message = "Vui lòng nhập điều khoản chấm dứt.")
    @Size(max = 4000, message = "Tối đa 4000 ký tự.")
    private String terminationTerms;

    @NotBlank(message = "Vui lòng nhập điều khoản chi trả cho chủ xe.")
    @Size(max = 4000, message = "Tối đa 4000 ký tự.")
    private String payoutTerms;

    @Size(max = 500, message = "Tối đa 500 ký tự.")
    private String signatureEvidencePath;
}
