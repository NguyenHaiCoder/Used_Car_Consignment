package fu.he.training.fall2026.dto.sales;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

/** Form đặt / đổi lịch xem xe. */
@Getter
@Setter
public class ViewingScheduleForm {

    @NotBlank(message = "Vui lòng chọn ngày hẹn.")
    private String date = "";

    @NotBlank(message = "Vui lòng chọn giờ bắt đầu.")
    private String time = "09:00";

    @NotNull(message = "Vui lòng chọn thời lượng.")
    private Integer minutes = 45;

    @NotBlank(message = "Vui lòng chọn loại lịch.")
    private String type = "VIEWING";

    @NotBlank(message = "Vui lòng chọn địa điểm.")
    private String place = "";

    private String note = "";

    public LocalDateTime toStart() {
        return LocalDate.parse(date).atTime(LocalTime.parse(time));
    }

    public int safeMinutes() {
        return minutes == null ? 45 : minutes;
    }
}
