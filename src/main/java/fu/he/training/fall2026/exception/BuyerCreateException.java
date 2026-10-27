package fu.he.training.fall2026.exception;

import java.util.Map;

public class BuyerCreateException extends BuyerException {

    public BuyerCreateException(Reason reason, Map<String, String> errors) {
        super(reason,
                reason == Reason.CONFLICT
                        ? "A buyer with this contact information already exists."
                        : "Please correct the highlighted fields.",
                errors);
    }
}

