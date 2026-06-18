# IAM Microservice (Identity & Access Management)

## Purpose
Registration, login with simulated 2FA, and password changes. Owns
`UserCredentials`; delegates user storage to Persona and code/mail delivery to
Mail.

## Key functions (`IamService`)
| Method | Description |
|--------|-------------|
| `signUpMenu(scanner)` | Registration portal. **A Back option (`0`) was added** at the first prompt and at the email prompt so users can return to the main menu / cancel and redo the flow. |
| `loginMenu(scanner)` | Login portal. **A Back option (`0`) was added** at the email prompt. Validates credentials, then 2FA via Mail. |
| `registerUser(credentials)` | Creates credentials, registers a Persona, sends welcome mail. |
| `changePassword(email, current, new)` | Validates and updates the password, then sends a reset confirmation. |

## Communications
IAM → Persona (`registerPersona`, `validateCredentials`, `updateProfile`,
`updatePassword`), IAM → Mail (`sendWelcome`, `deliver2FACode`, `verifyCode`,
`sendPasswordReset`).
