package fu.he.training.fall2026.dto.consignment;

import lombok.Value;

/** One condition shown on the withdraw screen; {@code detail} explains why it failed (empty when it passed). */
@Value
public class WithdrawCheckResponse {

    String label;
    boolean passed;
    String detail;
}
