package fu.he.training.fall2026.dto.inspection;

import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/** Result of one step, bound as {@code steps[i].passed}, {@code steps[i].note}, {@code steps[i].proofs}. */
@Getter
@Setter
public class InspectionStepResultRequest {

    private boolean passed;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự.")
    private String note;

    private List<MultipartFile> proofs = new ArrayList<>();
}
