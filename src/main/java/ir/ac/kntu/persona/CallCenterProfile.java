package ir.ac.kntu.persona;

public class CallCenterProfile extends UserProfile {

    public CallCenterProfile() {
        super(UserRole.CALLCENTER);
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
}
