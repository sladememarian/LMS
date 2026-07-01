package ir.ac.kntu.exception;

public class InvalidVerificationCodeException extends ValidationException {

    public InvalidVerificationCodeException(String message) {
        super(message);
    }

    public InvalidVerificationCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
