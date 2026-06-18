# IAM Tests — `IamServiceTest`

| Test | Explanation |
|------|-------------|
| `registerUserCreatesPersonaProfileAndWelcomeMail` | Registering a user creates a GUEST Persona with the given name **and** drops exactly one `WELCOME` message into their Mail inbox (verifies IAM → Persona and IAM → Mail). |
| `changePasswordSucceedsWithCorrectCurrent` | With the correct current password, the password changes and the new one validates. |
| `changePasswordFailsWithWrongCurrent` | A wrong current password throws `IllegalArgumentException`. |
| `changePasswordRejectsWeakNewPassword` | A new password that fails the policy throws `IllegalArgumentException`. |
| `invalidRegistrationThrows` | Registering with an invalid email throws (validation happens in `UserCredentials`). |
| `twoFactorCodeIsDeliverable` | Mail delivers a 2FA code; a wrong code fails verification and the real code succeeds. |
