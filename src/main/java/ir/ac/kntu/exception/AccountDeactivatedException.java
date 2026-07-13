package ir.ac.kntu.exception;

public class AccountDeactivatedException extends BaseException {

    public AccountDeactivatedException(String message) {
        super(message);
    }

    public AccountDeactivatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
