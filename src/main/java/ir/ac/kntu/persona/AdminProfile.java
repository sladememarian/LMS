package ir.ac.kntu.persona;

import java.util.Scanner;

import ir.ac.kntu.finance.FinanceAdminConsole;
import ir.ac.kntu.library.LibraryAdminConsole;
import ir.ac.kntu.support.inbox.AdminInbox;

public class AdminProfile extends UserProfile {

    public AdminProfile() {
        super(UserRole.ADMIN);
    }

    @Override
    public void openLibraryConsole(Scanner scanner, Persona user) {
        LibraryAdminConsole.open(scanner);
    }

    @Override
    public void openFinanceConsole(Scanner scanner, Persona user) {
        FinanceAdminConsole.open(scanner);
    }

    @Override
    public void openSupportConsole(Scanner scanner, Persona user) {
        AdminInbox.open(scanner, user);
    }

    @Override
    public boolean canBorrow() {
        return true;
    }

    @Override
    public boolean canExtend() {
        return true;
    }

    @Override
    public boolean canRequestRoleUpgrade() {
        return false;
    }

    @Override
    public boolean isStaff() {
        return true;
    }

    @Override
    public boolean canReserve() {
        return false;
    }

    @Override
    public int reservationLimit() {
        return 0;
    }
}
