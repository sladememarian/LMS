# Mail Microservice

## Purpose
A fully offline, simulated mail provider. Delivers welcome mails, 2FA codes,
password-reset confirmations and **system notifications**. Messages are stored
XOR-encrypted in `mail.enc`. Mail is a leaf service — it calls no one.

## Key functions (`MailService`)
`deliverMessage`, `deliver2FACode`, `verifyCode`, `sendWelcome`,
`sendPasswordReset`, `sendSystemNotification`, `getInbox`, `markInboxRead`,
`deleteInbox`.

## Role in the new design
The Support **notification centre** reuses Mail instead of building its own
store: `NotificationService` calls `MailService.sendSystemNotification`, and
"View Notifications" reads the recipient's inbox filtered by the
`SYSTEM_NOTIFICATION` message type. This keeps one single source of truth for
everything delivered to a user.

## Message types (`MessageType`)
`TWO_FA`, `WELCOME`, `PASSWORD_RESET`, `SYSTEM_NOTIFICATION`.

## Cross-process notification visibility (updated)

`MailService.ensureLoaded()` now **always reloads** `mail.enc` rather than
loading once per process. When the CallCenter sends a ticket reply (which
creates a notification via `NotificationService`), the recipient's inbox in a
second running instance will show the new notification on the next "View
Notifications" access — without a restart.
