# Domain Model Tests — `DomainModelsTest`

Unit tests for the plain data classes (no services involved).

| Test | Explanation |
|------|-------------|
| `transactionGetters` | A `Transaction` returns the id, member id, amount, type and description it was built with. |
| `supportTicketDefaultsAndOrdering` | A new `SupportTicket` defaults to `LOW` priority; a `CRITICAL` ticket sorts ahead of a `LOW` one (higher priority first). |
| `userCredentialsValidationAndSetters` | `UserCredentials` validates email/phone/password on construction and on setters, throwing `IllegalArgumentException` for bad input. |
| `userCredentialsEquality` | Two credentials with identical fields are `equals` and share a `hashCode`; different email ⇒ not equal. |
| `userRoleMetadata` | `UserRole` exposes the correct borrow limit and member-id prefix (STUDENT 10/`STU-`, GUEST 2, ADMIN `ADM-`). |
| `personaConstructorsAndRoleUpdate` | A sign-up `Persona` starts as GUEST with a `GST-` id and LIGHT theme; `updateRole` switches role and regenerates the id prefix (`STU-`). |
