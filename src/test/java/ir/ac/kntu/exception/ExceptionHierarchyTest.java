package ir.ac.kntu.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionHierarchyTest {

    @Test
    void baseExceptionExtendsRuntimeException() {
        assertTrue(RuntimeException.class.isAssignableFrom(BaseException.class));
    }

    @Test
    void directBaseExceptionSubclassesExtendBaseException() {
        assertTrue(BaseException.class.isAssignableFrom(AuthorizationException.class));
        assertTrue(BaseException.class.isAssignableFrom(ConflictException.class));
        assertTrue(BaseException.class.isAssignableFrom(NotFoundException.class));
        assertTrue(BaseException.class.isAssignableFrom(ValidationException.class));
    }

    @Test
    void nestedSubclassesStillExtendBaseException() {
        assertTrue(BaseException.class.isAssignableFrom(DuplicateEmailException.class));
        assertTrue(BaseException.class.isAssignableFrom(InvalidCredentialsException.class));
        assertTrue(BaseException.class.isAssignableFrom(InvalidEmailFormatException.class));
        assertTrue(BaseException.class.isAssignableFrom(InvalidPasswordException.class));
        assertTrue(BaseException.class.isAssignableFrom(InvalidPhoneFormatException.class));
        assertTrue(BaseException.class.isAssignableFrom(InvalidThemeException.class));
        assertTrue(BaseException.class.isAssignableFrom(InvalidVerificationCodeException.class));
        assertTrue(BaseException.class.isAssignableFrom(UserNotFoundException.class));
    }

    @Test
    void messageAndCauseArePropagated() {
        Throwable cause = new IllegalStateException("root cause");
        BaseException withMessage = new ValidationException("bad input");
        BaseException withCause = new ConflictException("conflict", cause);

        assertEquals("bad input", withMessage.getMessage());
        assertEquals("conflict", withCause.getMessage());
        assertEquals(cause, withCause.getCause());
    }

    @Test
    void userNotFoundIsAlsoANotFoundException() {
        UserNotFoundException ex = new UserNotFoundException("no such user");
        assertTrue(ex instanceof NotFoundException);
        assertTrue(ex instanceof BaseException);
    }

    @Test
    void invalidCredentialsIsAlsoAnAuthorizationException() {
        InvalidCredentialsException ex = new InvalidCredentialsException("bad login");
        assertTrue(ex instanceof AuthorizationException);
        assertTrue(ex instanceof BaseException);
    }
}
