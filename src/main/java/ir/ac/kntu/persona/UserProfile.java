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

    /**
     * Whether this role is permitted to place reservations on library items.
     */
    public abstract boolean canReserve();

    /**
     * Maximum number of active (WAITING + ACTIVE) reservations this role may hold.
     */
    public abstract int reservationLimit();

    // -----------------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------------

    /**
     * Returns the correct UserProfile subclass for the given role.
     * This is the single place that maps UserRole → profile behaviour.
     *
     * Note: this overload has no owning Persona to attach, so a CALLCENTER
     * profile built through this path cannot see/modify assigned support
     * sections. Prefer forRole(Persona) when a Persona is available.
     */
    public static UserProfile forRole(UserRole role) {
        return forRole(role, null);
    }

    /**
     * Returns the correct UserProfile subclass for the given persona's role,
     * wiring the profile back to its owning Persona where the subclass needs
     * durable per-persona state (e.g. CallCenterProfile's assigned sections).
     */
    public static UserProfile forRole(Persona persona) {
        return forRole(persona == null ? null : persona.getRole(), persona);
    }

    private static UserProfile forRole(UserRole role, Persona owningPersona) {
        if (role == null) {
            return new GuestProfile();
        }
        switch (role) {
            case ADMIN:
                return new AdminProfile();
            case CALLCENTER:
                return new CallCenterProfile(owningPersona);
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
