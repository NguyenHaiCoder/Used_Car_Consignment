package fu.he.training.fall2026.exception;
import lombok.Getter;
@Getter
public class SalesSchedulingException extends RuntimeException {
    private final String field;
    public SalesSchedulingException(String field, String message) {
        super(message);
        this.field = field;
    }
}
