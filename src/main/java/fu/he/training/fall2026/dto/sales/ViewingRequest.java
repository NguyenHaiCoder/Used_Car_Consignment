package fu.he.training.fall2026.dto.sales;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
@Data
public class ViewingRequest {
    @NotNull @Positive private Long inquiryId;
    @NotNull @Positive private Long salespersonId;
    @NotBlank @Pattern(regexp = "VIEWING|TEST_DRIVE") private String type = "VIEWING";
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) private LocalDateTime startTime;
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) private LocalDateTime endTime;
    @NotBlank @Size(max = 160) private String location;
    @Size(max = 1000) private String notes;
}
