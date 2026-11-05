package fu.he.training.fall2026.dto.sales;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ViewingFormRequest {
    private String revision;
    @NotNull(message = "Vui lòng chọn tư vấn.")
    @Positive(message = "Tư vấn không hợp lệ.")
    private Long inquiryId;

    @NotNull(message = "Vui lòng chọn nhân viên bán hàng.")
    @Positive(message = "Nhân viên bán hàng không hợp lệ.")
    private Long salespersonId;

    @NotBlank(message = "Vui lòng chọn loại lịch.")
    @Pattern(regexp = "|VIEWING|TEST_DRIVE", message = "Loại lịch không hợp lệ.")
    private String type = "VIEWING";

    @NotBlank(message = "Vui lòng nhập thời gian bắt đầu.")
    @ValidViewingDateTime(message = "Thời gian bắt đầu không hợp lệ.")
    private String startTime;

    @NotBlank(message = "Vui lòng nhập thời gian kết thúc.")
    @ValidViewingDateTime(message = "Thời gian kết thúc không hợp lệ.")
    private String endTime;

    @NotBlank(message = "Vui lòng nhập địa điểm.")
    @Size(max = 160, message = "Địa điểm tối đa 160 ký tự.")
    private String location;

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự.")
    private String notes;
}
