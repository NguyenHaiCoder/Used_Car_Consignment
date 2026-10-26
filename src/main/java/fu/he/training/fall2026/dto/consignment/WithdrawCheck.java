package fu.he.training.fall2026.dto.consignment;

/** One condition shown on the withdraw screen; {@code detail} explains why it failed (empty when it passed). */
public record WithdrawCheck(String label, boolean passed, String detail) {
}
