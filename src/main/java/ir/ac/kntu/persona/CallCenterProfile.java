package ir.ac.kntu.persona;

import java.util.EnumSet;
import java.util.Scanner;
import java.util.Set;

import ir.ac.kntu.finance.FinanceOperatorConsole;
import ir.ac.kntu.library.LibraryOperatorConsole;
import ir.ac.kntu.support.SupportSection;
import ir.ac.kntu.support.inbox.CallCenterInbox;

/**
 * CallCenter behaviour profile. Assigned sections are NOT stored on this
 * object: UserProfile.forRole() allocates a fresh instance on every call,
 * so any state kept here would be lost immediately. Instead this class
 * reads/writes through the owning Persona, which is the durable holder.
 */
public class CallCenterProfile extends UserProfile {
    private final Persona owningPersona;

    public CallCenterProfile() {
        this(null);
    }

    public CallCenterProfile(Persona owningPersona) {
        super(UserRole.CALLCENTER);
        this.owningPersona = owningPersona;
    }

    public Set<SupportSection> getAssignedSections() {
        if (owningPersona == null) {
            return EnumSet.noneOf(SupportSection.class);
        }
        return owningPersona.getAssignedSupportSections();
    }

    public void addSection(SupportSection section) {
        if (owningPersona != null) {
            owningPersona.addAssignedSupportSection(section);
        }
    }

    public void removeSection(SupportSection section) {
        if (owningPersona != null) {
            owningPersona.removeAssignedSupportSection(section);
        }
    }

    @Override
    public void openLibraryConsole(Scanner scanner, Persona user) {
        LibraryOperatorConsole.open(scanner);
    }

    @Override
    public void openFinanceConsole(Scanner scanner, Persona user) {
        FinanceOperatorConsole.open(scanner);
    }

    @Override
    public void openSupportConsole(Scanner scanner, Persona user) {
        CallCenterInbox.open(scanner, user);
    }

    @Override
    public boolean canBorrow() {
        return false;
    }

    @Override
    public boolean canExtend() {
        return false;
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
