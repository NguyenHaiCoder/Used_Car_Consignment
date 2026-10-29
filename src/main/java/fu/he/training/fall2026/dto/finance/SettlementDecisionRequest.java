package fu.he.training.fall2026.dto.finance;

import fu.he.training.fall2026.entity.enums.ApprovalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Manager decision on a settlement in PENDING_REVIEW: APPROVED, REJECTED or NEED_MORE_INFO. */
@Getter
@Setter
public class SettlementDecisionRequest {

    @NotNull(message = "Thiếu quyết toán.")
    private Long id;

    @NotNull(message = "Vui lòng chọn quyết định.")
    private ApprovalStatus decision;

    @NotBlank(message = "Vui lòng nhập lý do quyết định.")
    @Size(max = 2000, message = "Lý do tối đa 2000 ký tự.")
    private String reason;
}
