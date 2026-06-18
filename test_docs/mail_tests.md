# Mail Tests

## `MailServiceTest`
| Test | Explanation |
|------|-------------|
| `deliverAndVerifyTwoFactorCode` | A delivered 2FA code verifies correctly. |
| `welcomeMessageLandsInInbox` | `sendWelcome` places a WELCOME message in the inbox. |
| `systemNotificationAndMarkRead` | A system notification is delivered and `markInboxRead` marks messages read. |
| `deleteInboxRemovesMessages` | `deleteInbox` clears a recipient's messages. |
| `envConfigDefaults` | Mail configuration falls back to sensible defaults from `EnvConfig`. |
| `inboxRespectsMailboxCap` | The inbox honours the configured maximum message cap. |

## `MailModelsTest`
| Test | Explanation |
|------|-------------|
| `messageTypeLabelRoundTrip` | `MessageType.fromLabel` round-trips a known label and defaults unknowns to `SYSTEM_NOTIFICATION`. |
| `mailMessageDefaults` | A new `MailMessage` defaults to unread with the given fields. |
| `inboxBoundedAndCountsUnread` | `Inbox` respects its bound and counts unread messages. |
