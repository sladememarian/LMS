# SSO Tests — `SsoServiceTest`

| Test | Explanation |
|------|-------------|
| `viewProfileContainsIdentity` | The profile string includes member id, name, email, role and theme. |
| `editProfileUpdatesFields` | Editing the profile persists the new name/phone. |
| `editProfileRejectsInvalidPhone` | An invalid phone number throws `IllegalArgumentException`. |
| `changePasswordRequiresMatchingConfirm` | A mismatched confirmation throws. |
| `themeSettingsValidatedAndPersisted` | Only LIGHT/DARK are accepted and the choice persists. |
| `sessionLifecycleAndLogout` | A session can be created and logout destroys it. |
| `createSessionRejectsNull` | Creating a session for a null user throws. |
