package ir.ac.kntu.persona;

public class TeacherProfile extends UserProfile {

    public TeacherProfile() {
        super(UserRole.TEACHER);
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
}
