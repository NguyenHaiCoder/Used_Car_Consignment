package fu.he.training.fall2026.dto.manager;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DecideApprovalRequest {

    /** APPROVED | REJECTED | NEED_MORE_INFO */
    @NotBlank
    private String decision;

    @NotBlank
    private String reason;
}
