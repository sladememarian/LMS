# SSO Microservice (Settings & Session)

## Purpose
Self-service settings for the logged-in user plus session lifecycle. SSO does
not store anything itself; it orchestrates IAM and Persona.

## Key functions

| Method | Throws | Description |
|--------|--------|-------------|
| `viewProfile(email)` | `UserNotFoundException` | Returns a formatted profile summary string. |
| `editProfile(email, first, last, phone)` | `UserNotFoundException`, `InvalidPhoneFormatException` | Validates phone format, then delegates to PersonaService. |
| `changePassword(email, current, new, confirm)` | `InvalidPasswordException` (mismatch) | Validates confirmation, then delegates to IamService. |
| `changeTheme(email, theme)` | `InvalidThemeException` (not LIGHT/DARK), `UserNotFoundException` | Normalises and validates, then delegates to PersonaService. |
| `getTheme(email)` | `UserNotFoundException` | Returns the user's current theme string. |
| `logout()` | — | Destroys the active session. |

All methods that look up a profile now throw `UserNotFoundException` instead
of returning `null`, keeping error handling consistent across the app.

## Communications
SSO → IAM (password change), SSO → Persona (profile, theme), SSO → SessionManager.
