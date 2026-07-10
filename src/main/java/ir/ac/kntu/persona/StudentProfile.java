package ir.ac.kntu.persona;

public class StudentProfile extends UserProfile {

    public StudentProfile() {
        super(UserRole.STUDENT);
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
        return false;
    }

    @Override
    public boolean canReserve() {
        return true;
    }

    @Override
    public int reservationLimit() {
        return 5;
    }
}
