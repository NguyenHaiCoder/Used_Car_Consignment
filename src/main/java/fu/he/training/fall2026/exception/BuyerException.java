package fu.he.training.fall2026.exception;

import java.util.Map;
public class BuyerException extends RuntimeException {

    public enum Reason { VALIDATION, CONFLICT, NOT_FOUND }

    private final Reason reason;
    private final Map<String, String> errors;

    public BuyerException(Reason reason, String message) {
        this(reason, message, Map.of());
    }

    public BuyerException(Reason reason, String message, Map<String, String> errors) {
        super(message);
        this.reason = reason;
        this.errors = Map.copyOf(errors);
    }

    public Reason getReason() { return reason; }

    public Map<String, String> getErrors() { return errors; }
}

