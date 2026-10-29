package fu.he.training.fall2026.dto.handover;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 05.05 Handover checklist. Fields may be saved incomplete as a draft; completing the handover
 * requires odometer, keys, both signatures and every item ticked or explained.
 */
@Getter
@Setter
public class HandoverChecklistRequest {

    @NotNull(message = "Thiếu biên bản bàn giao.")
    private Long id;

    @Min(value = 0, message = "Số km không được âm.")
    private Integer odometerKm;

    @Min(value = 0, message = "Số chìa khóa không được âm.")
    @Max(value = 10, message = "Số chìa khóa tối đa 10.")
    private Integer keyCount;

    @Size(max = 2000, message = "Phụ kiện tối đa 2000 ký tự.")
    private String accessories;

    @Size(max = 1000, message = "Tối đa 1000 ký tự.")
    private String photoPaths;

    @Size(max = 500, message = "Tối đa 500 ký tự.")
    private String receiverSignaturePath;

    @Size(max = 500, message = "Tối đa 500 ký tự.")
    private String staffSignaturePath;

    @Valid
    private List<HandoverItemForm> items = new ArrayList<>();
}
