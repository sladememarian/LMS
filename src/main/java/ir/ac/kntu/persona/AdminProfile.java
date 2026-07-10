package ir.ac.kntu.persona;

public class AdminProfile extends UserProfile {

    public AdminProfile() {
        super(UserRole.ADMIN);
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
