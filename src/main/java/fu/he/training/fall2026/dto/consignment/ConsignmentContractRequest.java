package fu.he.training.fall2026.dto.consignment;

import jakarta.validation.constraints.AssertTrue;
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
import org.springframework.web.multipart.MultipartFile;

/** Form tạo / sửa bản thảo hợp đồng ký gửi. */
@Getter
@Setter
public class ConsignmentContractRequest {

    /** Ẩn: trống khi tạo, id hợp đồng khi sửa bản thảo. */
    private Long id;

    /** Ẩn: hồ sơ ký gửi mà hợp đồng thuộc về. */
    @NotNull(message = "Thiếu hồ sơ ký gửi.")
    private Long consignmentId;

    /** Hoa hồng theo % giá bán; nhập một trong hai: hoa hồng % hoặc phí cố định. */
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

    /** Bản hợp đồng đã ký (PDF, Word, Excel hoặc ảnh JPG/PNG). Không chọn tệp thì giữ nguyên tệp đã lưu. */
    private MultipartFile signatureEvidence;

    /** BR-03: nhập hoa hồng % hoặc phí cố định, đúng một trong hai. Lỗi hiện ở ô hoa hồng. */
    @AssertTrue(message = "Nhập hoa hồng theo % hoặc phí cố định (chỉ một trong hai).")
    public boolean isCommissionChosen() {
        boolean hasRate = commissionRate != null && commissionRate.signum() > 0;
        boolean hasFee = fixedFee != null && fixedFee.signum() > 0;
        return hasRate != hasFee;
    }

    /** Thời hạn hợp lệ: ngày kết thúc bằng hoặc sau ngày bắt đầu. Lỗi hiện ở ô ngày kết thúc. */
    @AssertTrue(message = "Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.")
    public boolean isPeriodValid() {
        return validFrom == null || validTo == null || !validTo.isBefore(validFrom);
    }
}
