package ir.ac.kntu.persona;

public class Persona {

    private static Persona currentUser;

    private final String email;
    private final String password;
    private final String username;
    private UserRole role;
    private String memberId;
    private int walletBalance;  

    public Persona(String email, String password) {
        this.username = null;
        this.email = email;
        this.password = password;
        this.role = UserRole.GUEST;
        this.walletBalance = 0;
        this.memberId = generateMemberId(UserRole.GUEST.getPrefix());
    }

    public Persona(String username, String password, UserRole role) {
        this.email = null;
        this.username = username;
        this.password = password;
        this.role = role;
        this.memberId = generateMemberId(role.getPrefix());
        this.walletBalance = 0;
    }

    public Persona(String email, String username, String password, UserRole role, String memberId, int walletBalance) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.role = role;
        this.memberId = memberId;
        this.walletBalance = walletBalance;
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

    public String getUsername() {
        return username;
    }
}