package ir.ac.kntu.iam;

import ir.ac.kntu.util.Validator;

public class UserCredentials {

    private final String firstName;
    private final String lastName;
    private String phoneNumber;
    private final String email;
    private String password;

    public UserCredentials(String email, String password, String firstName, String lastName, String phoneNumber) {
        if (!Validator.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (!Validator.isValidPassword(password)) {
            throw new IllegalArgumentException("Password must be at least 8 characters long and include uppercase, lowercase, digit, and special character");
        }
        if (!Validator.isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Invalid phone number format");
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
            throw new IllegalArgumentException("[SECURITY ERROR]Password must be at least 8 characters long and include uppercase, lowercase, digit, and special character");
        }
        this.password = password;
    }

    public void setPhoneNumber(String phoneNumber) {
        if (!Validator.isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("[VALIDATION ERROR]Invalid phone number format");
        }
        this.phoneNumber = phoneNumber;
    }
}
