package fu.he.training.fall2026.dto.inquiry;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InquiryDeleteRequest {
    @NotBlank @Size(max = 80)
    private String revision;

    @AssertTrue(message = "Confirm deletion before submitting.")
    private boolean confirmed;
}
