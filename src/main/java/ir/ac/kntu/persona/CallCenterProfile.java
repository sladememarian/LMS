package ir.ac.kntu.persona;

import java.util.HashSet;
import java.util.Set;
import ir.ac.kntu.support.SupportSection;

public class CallCenterProfile extends UserProfile {
    private final Set<SupportSection> assignedSections = new HashSet<>();

    public CallCenterProfile() {
        super(UserRole.CALLCENTER);
    }

    public Set<SupportSection> getAssignedSections() {
        return new HashSet<>(assignedSections);
    }

    public void addSection(SupportSection section) {
        assignedSections.add(section);
    }

    public void removeSection(SupportSection section) {
        assignedSections.remove(section);
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
