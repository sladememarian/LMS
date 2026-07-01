package ir.ac.kntu.exception;

public class InvalidThemeException extends ValidationException {

    public InvalidThemeException(String message) {
        super(message);
    }

    public InvalidThemeException(String message, Throwable cause) {
        super(message, cause);
    }
}
