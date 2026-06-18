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
