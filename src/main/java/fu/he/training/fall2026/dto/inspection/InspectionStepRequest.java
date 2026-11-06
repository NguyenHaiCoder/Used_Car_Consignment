package fu.he.training.fall2026.dto.inspection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** One row of the "Các bước kiểm định" editor, bound as {@code steps[i].title} / {@code steps[i].hint}. */
@Getter
@Setter
public class InspectionStepRequest {

    @NotBlank(message = "Vui lòng nhập nội dung cần kiểm định.")
    @Size(max = 160, message = "Nội dung kiểm định tối đa 160 ký tự.")
    private String title;

    @Size(max = 500, message = "Mô tả / gợi ý tối đa 500 ký tự.")
    private String hint;
}
