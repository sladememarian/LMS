package ir.ac.kntu.persona;

import java.util.ArrayList;
import java.util.List;

public class Persona {

    private static final String DEFAULT_THEME = "LIGHT";

    private static Persona currentUser;

    private final String email;
    private String password;
    private final String username;
    private UserRole role;
    private String memberId;
    private int walletBalance;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String theme;
    private final List<String> borrowedItemIds = new ArrayList<>();

    public Persona(String email, String password) {
        this.username = null;
        this.email = email;
        this.password = password;
        this.role = UserRole.GUEST;
        this.walletBalance = 0;
        this.memberId = generateMemberId(UserRole.GUEST.getPrefix());
        this.theme = DEFAULT_THEME;
    }

    public Persona(String username, String password, UserRole role) {
        this.email = null;
        this.username = username;
        this.password = password;
        this.role = role;
        this.memberId = generateMemberId(role.getPrefix());
        this.walletBalance = 0;
        this.theme = DEFAULT_THEME;
    }

    public Persona(String email, String username, String password, UserRole role, String memberId, int walletBalance) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.role = role;
        this.memberId = memberId;
        this.walletBalance = walletBalance;
        this.theme = DEFAULT_THEME;
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

    public void setPassword(String password) {
        this.password = password;
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
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

    public List<String> getBorrowedItemIds() {
        return new ArrayList<>(borrowedItemIds);
    }

    public int getBorrowCount() {
        return borrowedItemIds.size();
    }

    public boolean hasBorrowed(String itemId) {
        return borrowedItemIds.contains(itemId);
    }

    public void addBorrowedItem(String itemId) {
        if (itemId != null && !itemId.isEmpty()) {
            borrowedItemIds.add(itemId);
        }
    }

    public boolean removeBorrowedItem(String itemId) {
        return borrowedItemIds.remove(itemId);
    }
}
