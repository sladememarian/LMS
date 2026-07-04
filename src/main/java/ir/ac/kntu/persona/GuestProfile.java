package ir.ac.kntu.persona;

public class GuestProfile extends UserProfile {

    public GuestProfile() {
        super(UserRole.GUEST);
    }

    @Override
    public boolean canBorrow() {
        return true;
    }

    @Override
    public boolean canExtend() {
        return false;
    }

    @Override
    public boolean canRequestRoleUpgrade() {
        return true;
    }

    @Override
    public boolean isStaff() {
        return false;
    }
}
