package ir.ac.kntu.exception;

public class InsufficientCopiesException extends ConflictException {

    public InsufficientCopiesException(String message) {
        super(message);
    }

    public InsufficientCopiesException(String message, Throwable cause) {
        super(message, cause);
    }
}
