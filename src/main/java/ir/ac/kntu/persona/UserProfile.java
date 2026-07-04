package ir.ac.kntu.persona;

/**
 * Abstract role profile: replaces scattered if(role==X) BEHAVIOR checks with polymorphism.
 *
 * Important distinction (see docs/step2.md and docs/step3.md):
 *  - Pure per-role DATA (borrow limit, display label, member-id prefix) already lives
 *    correctly on the UserRole enum constants — that is not duplicated here, it is
 *    looked up from the wrapped UserRole so there is exactly one source of truth.
 *  - Per-role BEHAVIOR (can this role extend a loan? request a role upgrade? etc.)
 *    is what actually varied by scattered if(role==X) checks across multiple console
 *    classes, and THAT is what the abstract methods below replace.
 *
 * Each concrete subclass (GuestProfile, StudentProfile, etc.) encapsulates the
 * behavioral answers for one role. Consoles call these methods instead of
 * comparing UserRole enum constants directly.
 */
public abstract class UserProfile {

    private final UserRole role;

    protected UserProfile(UserRole role) {
        this.role = role;
    }

    /**
     * Human-readable label shown in dashboard banners (e.g. "GUEST", "STUDENT").
     * Pure data — delegates to the UserRole enum constant, not overridden per subclass.
     */
    public final String dashboardLabel() {
        return role.name();
    }

    /**
     * Maximum number of items this role can hold at one time.
     * Pure data — delegates to UserRole.getMaxBorrowLimit(), not overridden per subclass.
     * Returns Integer.MAX_VALUE for unlimited (ADMIN).
     */
    public final int borrowLimit() {
        return role.getMaxBorrowLimit();
    }

    /**
     * Whether this role is permitted to borrow library items at all.
     * Behavioral — differs per role in a way that isn't just a number lookup.
     */
    public abstract boolean canBorrow();

    /**
     * Whether this role can request a loan extension (pay to extend due date).
     */
    public abstract boolean canExtend();

    /**
     * Whether this role can submit role-upgrade requests through Support.
     */
    public abstract boolean canRequestRoleUpgrade();

    /**
     * Whether this role has access to staff (admin/callcenter) dashboards.
     */
    public abstract boolean isStaff();

    // -----------------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------------

    /**
     * Returns the correct UserProfile subclass for the given role.
     * This is the single place that maps UserRole → profile behaviour.
     */
    public static UserProfile forRole(UserRole role) {
        if (role == null) {
            return new GuestProfile();
        }
        switch (role) {
            case ADMIN:
                return new AdminProfile();
            case CALLCENTER:
                return new CallCenterProfile();
            case TEACHER:
                return new TeacherProfile();
            case STUDENT:
                return new StudentProfile();
            case GUEST:
            default:
                return new GuestProfile();
        }
    }
}
