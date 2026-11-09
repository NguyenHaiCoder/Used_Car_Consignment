package fu.he.training.fall2026.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final Long relatedId;

    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST, null);
    }

    public BusinessException(String message, HttpStatus status) {
        this(message, status, null);
    }

    public BusinessException(String message, HttpStatus status, Long relatedId) {
        super(message);
        this.status = status;
        this.relatedId = relatedId;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Long getRelatedId() {
        return relatedId;
    }
}
