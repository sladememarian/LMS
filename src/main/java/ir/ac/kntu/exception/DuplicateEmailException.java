package ir.ac.kntu.exception;

public class DuplicateEmailException extends ConflictException {

    public DuplicateEmailException(String message) {
        super(message);
    }

    public DuplicateEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
