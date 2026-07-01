package ir.ac.kntu.iam;

import ir.ac.kntu.exception.InvalidEmailFormatException;
import ir.ac.kntu.exception.InvalidPasswordException;
import ir.ac.kntu.exception.InvalidPhoneFormatException;
import ir.ac.kntu.util.Validator;
import java.util.Objects;

public class UserCredentials {

    private final String firstName;
    private final String lastName;
    private String phoneNumber;
    private final String email;
    private String password;

    public UserCredentials(
        String email,
        String password,
        String firstName,
        String lastName,
        String phoneNumber
    ) {
        if (!Validator.isValidEmail(email)) {
            throw new InvalidEmailFormatException("Invalid email format");
        }
        if (!Validator.isValidPassword(password)) {
            throw new InvalidPasswordException(
                "Password must be at least 8 characters long and include uppercase, lowercase, digit, and special character"
            );
        }
        if (!Validator.isValidPhoneNumber(phoneNumber)) {
            throw new InvalidPhoneFormatException(
                "Invalid phone number format"
            );
        }
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPassword(String password) {
        if (!Validator.isValidPassword(password)) {
            throw new InvalidPasswordException(
                "Password must be at least 8 characters long and include uppercase, lowercase, digit, and special character"
            );
        }
        this.password = password;
    }

    public void setPhoneNumber(String phoneNumber) {
        if (!Validator.isValidPhoneNumber(phoneNumber)) {
            throw new InvalidPhoneFormatException(
                "Invalid phone number format"
            );
        }
        this.phoneNumber = phoneNumber;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.firstName);
        hash = 19 * hash + Objects.hashCode(this.lastName);
        hash = 19 * hash + Objects.hashCode(this.phoneNumber);
        hash = 19 * hash + Objects.hashCode(this.email);
        hash = 19 * hash + Objects.hashCode(this.password);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UserCredentials other = (UserCredentials) obj;
        if (!Objects.equals(this.firstName, other.firstName)) {
            return false;
        }
        if (!Objects.equals(this.lastName, other.lastName)) {
            return false;
        }
        if (!Objects.equals(this.phoneNumber, other.phoneNumber)) {
            return false;
        }
        if (!Objects.equals(this.email, other.email)) {
            return false;
        }
        return Objects.equals(this.password, other.password);
    }
}
