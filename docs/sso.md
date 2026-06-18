# SSO Microservice (Settings & Session)

## Purpose
Self-service settings for the logged-in user plus session lifecycle. SSO does
not store anything itself; it orchestrates IAM and Persona.

## Key functions
`SsoController.settingsMenu` offers: view profile, edit profile, change password,
theme (LIGHT/DARK), logout. `SsoService` delegates to `PersonaService`
(profile/theme) and `IamService.changePassword`. `SessionManager` tracks the
active session token and login time.

## Communications
SSO → IAM (password change), SSO → Persona (profile, theme), SSO → SessionManager.
