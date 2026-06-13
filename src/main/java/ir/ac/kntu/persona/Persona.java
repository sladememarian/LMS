package ir.ac.kntu.persona;

public class Persona {
    private final String email;
    private final String password;
    private UserRole role;
    private String memberId;

    public Persona(String email, String password) {
        this.email = email;
        this.password = password;
        this.role = UserRole.GUEST;
        this.memberId = generateMemberId(UserRole.GUEST.getPrefix());
    }

    public Persona(String email, String password, UserRole role, String memberId) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.memberId = memberId;
    }

    private String generateMemberId(String prefix) {
        int uniqueNumber = (int) (Math.random() * 900000) + 100000;
        return prefix + uniqueNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public UserRole getRole() {
        return role;
    }

    public void updateRole(UserRole newRole) {
        this.role = newRole;
        this.memberId = generateMemberId(newRole.getPrefix());
    }

    public String getMemberId() {
        return memberId;
    }
}