package fu.he.training.fall2026.dto.consignment;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/** Form tạo / sửa hồ sơ ký gửi: chủ xe + xe + hồ sơ trong cùng một lần lưu (phẳng, một DTO). */
@Getter
@Setter
public class ConsignmentCreateRequest {

    /** Ẩn: trống khi tạo, id hồ sơ khi sửa. */
    private Long id;

    // ── Chủ xe ──
    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 160, message = "Họ và tên tối đa 160 ký tự.")
    private String consignorName;

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(regexp = "^\\d{10,11}$", message = "Số điện thoại cần 10–11 chữ số.")
    private String phone;

    @Email(message = "Email chưa đúng định dạng.")
    @NotBlank(message = "Vui lòng nhập email.")
    @Size(max = 180, message = "Email tối đa 180 ký tự.")
    private String email;

    @NotBlank(message = "Vui lòng nhập số CCCD.")
    @Pattern(regexp = "^\\d{12}$", message = "CCCD cần 12 chữ số.")
    private String idNumber;

    @NotBlank(message = "Vui lòng nhập địa chỉ.")
    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự.")
    private String address;

    @NotBlank(message = "Vui lòng nhập ngân hàng.")
    @Size(max = 120, message = "Tên ngân hàng tối đa 120 ký tự.")
    private String bank;

    @NotBlank(message = "Vui lòng nhập số tài khoản.")
    @Pattern(regexp = "^\\d{6,20}$", message = "Số tài khoản cần 6–20 chữ số.")
    private String accountNumber;

    @NotBlank(message = "Vui lòng nhập tên chủ tài khoản.")
    @Size(max = 160, message = "Tên chủ tài khoản tối đa 160 ký tự.")
    private String accountName;

    /** Chủ xe đồng ý lưu thông tin; cũng là xác nhận đồng ý (ownerConsent) của hồ sơ. */
    @AssertTrue(message = "Cần xác nhận chủ xe đồng ý lưu thông tin cá nhân.")
    private boolean consentStored;

    // ── Xe ──
    @NotNull(message = "Vui lòng chọn dòng xe.")
    private Long vehicleModelId;

    @NotBlank(message = "Vui lòng nhập biển số.")
    @Size(max = 20, message = "Biển số tối đa 20 ký tự.")
    private String licensePlate;

    @NotNull(message = "Vui lòng nhập năm sản xuất.")
    @Min(value = 1950, message = "Năm sản xuất không hợp lệ.")
    @Max(value = 2100, message = "Năm sản xuất không hợp lệ.")
    private Integer year;

    @NotBlank(message = "Vui lòng nhập màu sắc.")
    @Size(max = 40, message = "Màu sắc tối đa 40 ký tự.")
    private String color;

    @NotNull(message = "Vui lòng nhập số km.")
    @Min(value = 0, message = "Số km không được âm.")
    private Integer odometerKm;

    @NotBlank(message = "Vui lòng nhập số khung (VIN).")
    @Size(max = 32, message = "Số khung (VIN) tối đa 32 ký tự.")
    private String vin;

    @NotBlank(message = "Vui lòng nhập số máy.")
    @Size(max = 40, message = "Số máy tối đa 40 ký tự.")
    private String engineNumber;

    @NotBlank(message = "Vui lòng chọn loại xe.")
    @Size(max = 40, message = "Loại xe tối đa 40 ký tự.")
    private String vehicleType;

    @NotBlank(message = "Vui lòng chọn hộp số.")
    @Size(max = 40, message = "Hộp số tối đa 40 ký tự.")
    private String transmission;

    @NotBlank(message = "Vui lòng chọn nhiên liệu.")
    @Size(max = 40, message = "Nhiên liệu tối đa 40 ký tự.")
    private String fuelType;

    @NotBlank(message = "Vui lòng nhập nơi để xe.")
    @Size(max = 160, message = "Nơi để xe tối đa 160 ký tự.")
    private String location;

    @Size(max = 1000, message = "Ghi chú tiếp nhận tối đa 1000 ký tự.")
    private String intakeCondition;

    // ── Hồ sơ ký gửi ──
    @NotNull(message = "Vui lòng chọn ngày tiếp nhận.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate intakeDate;

    @NotBlank(message = "Vui lòng nhập nơi lưu giữ.")
    @Size(max = 160, message = "Nơi lưu giữ tối đa 160 ký tự.")
    private String custodyLocation;

    @NotNull(message = "Vui lòng nhập giá mong muốn.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá mong muốn phải lớn hơn 0.")
    private BigDecimal expectedPrice;

    @Size(max = 2000, message = "Điều khoản tối đa 2000 ký tự.")
    private String termNotes;
}
