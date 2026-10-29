package fu.he.training.fall2026.dto.handover;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One line of the handover checklist (keys, registration papers, tools…). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HandoverItemForm {
    private Long id;
    private String itemName;
    private boolean present;

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự.")
    private String notes;
}
