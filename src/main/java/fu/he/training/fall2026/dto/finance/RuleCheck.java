package fu.he.training.fall2026.dto.finance;

import lombok.Value;

/** One business-rule check shown on a screen; {@code detail} explains why it failed (empty when it passed). */
@Value
public class RuleCheck {
    String label;
    boolean passed;
    String detail;

    public static RuleCheck of(String label, boolean passed, String failDetail) {
        return new RuleCheck(label, passed, passed ? "" : failDetail);
    }
}
