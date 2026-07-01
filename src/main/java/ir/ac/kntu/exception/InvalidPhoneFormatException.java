package ir.ac.kntu.exception;

public class InvalidPhoneFormatException extends ValidationException {

    public InvalidPhoneFormatException(String message) {
        super(message);
    }

    public InvalidPhoneFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
