package ir.ac.kntu.persona;

public class Persona {

    private static Persona currentUser;

    private final String email;
    private final String password;
    private UserRole role;
    private String memberId;
    private int walletBalance;  

    public Persona(String email, String password) {
        this.email = email;
        this.password = password;
        this.role = UserRole.GUEST;
        this.walletBalance = 0;
        this.memberId = generateMemberId(UserRole.GUEST.getPrefix());
    }

    public Persona(String email, String password, UserRole role, String memberId) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.memberId = memberId;
        this.walletBalance = 0;
    }

    private String generateMemberId(String prefix) {
        int uniqueNumber = (int) (Math.random() * 900_000) + 100_000;
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

    public int getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(int walletBalance) {
        this.walletBalance = walletBalance;
    }

    public static Persona getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(Persona currentUser) {
        Persona.currentUser = currentUser;
    }
}