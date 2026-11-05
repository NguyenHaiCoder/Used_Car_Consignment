package fu.he.training.fall2026.dto.inquiry;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/** Form data shared by inquiry creation and editing. */
@Data
public class InquirySaveRequest {
    @NotBlank(message = "Vui lòng chọn khách mua.")
    @ValidInquiryId(message = "Khách mua không hợp lệ.")
    private String buyerId;

    @NotBlank(message = "Vui lòng chọn xe quan tâm.")
    @ValidInquiryId(message = "Xe quan tâm không hợp lệ.")
    private String vehicleId;

    @Positive
    private Long salespersonId;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate inquiryDate;

    @NotBlank(message = "Vui lòng chọn kênh liên hệ.")
    @Pattern(regexp = "|SHOWROOM|PHONE|WEBSITE", message = "Kênh liên hệ không hợp lệ.")
    private String channel;

    @NotBlank(message = "Vui lòng chọn nhu cầu.")
    @Pattern(regexp = "|PRICE|VIEWING|TEST_DRIVE", message = "Nhu cầu không hợp lệ.")
    private String need;

    @Size(max = 2000)
    private String notes;

    /** Used by the edit flow for optimistic concurrency; blank on create. */
    @Size(max = 80)
    private String revision;

    public Long selectedBuyerId() { return validIdOrNull(buyerId); }
    public Long selectedVehicleId() { return validIdOrNull(vehicleId); }

    private Long validIdOrNull(String value) {
        if (value == null || !value.matches("[1-9][0-9]*")) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
