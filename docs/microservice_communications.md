# Microservice Communications

This document is the single source of truth for **how the microservices talk to
each other** and **which flow uses which connection**. The project is a modular
monolith: every microservice is a package under `ir.ac.kntu` and "communication"
means a plain Java method call from one package into another.

## Communication map (who calls whom)

```
FrontPanel (Main)  ── orchestrates every microservice UI
   │
   ├─ IAM        → Persona, Mail
   ├─ SSO        → IAM, Persona, (Session)
   ├─ Persona    → Mail              (owns users, roles, wallet, borrowed items)
   ├─ Persona.Inventory → Library    (My Inventory reads item details)
   ├─ Finance    → Persona, Mail     (money only; balances live in Persona)
   ├─ Library    → Support           (operator catalog changes route via Support)
   ├─ Support    → Persona, Library, Mail
   │     ├─ ticket        (SupportService, SupportTicket, TicketPrinter)
   │     ├─ inbox         (AdminInbox, CallCenterInbox)
   │     ├─ notification  (NotificationService → Mail)
   │     └─ rolerequest   (RoleRequest, RoleRequestService → Persona)
   ├─ Report     → Library           (reads inventory to build the HTML report)
   └─ Mail       → nobody            (leaf simulated mail provider)
```

### Allowed dependencies (kept strict on purpose)

| Caller | Callees |
|--------|---------|
| Main (FrontPanel) | every console (Library/Finance/Support), IAM, SSO, Mail, Persona |
| IAM | Persona, Mail |
| SSO | IAM, Persona, SessionManager |
| Persona | Mail |
| Persona.InventoryConsole | Library (read-only item details) |
| Finance | Persona, Mail |
| Library (operator/admin consoles) | Support (bridge), Report, Persona |
| Support | Persona, Library, Mail |
| Report | Library |
| Mail | — |

## Flow-by-flow communication

### 1. Sign-up
`Main → IAM.signUpMenu` → `IAM → PersonaService.registerPersona` (creates GUEST)
→ `IAM → PersonaService.updateProfile` → `IAM → MailService.sendWelcome`.

### 2. Login + 2FA
`Main → IAM.loginMenu` → `PersonaService.validateCredentials`
→ `MailService.deliver2FACode` / `MailService.verifyCode`
→ `Persona.setCurrentUser` → `Main → SessionManager.createSession`.

### 3. Settings (SSO)
`Main → SsoController` → `SsoService → PersonaService` (profile/theme),
`SsoService → IamService.changePassword → MailService.sendPasswordReset`.

### 4. Library borrow (Guest/Student/Teacher)
`Main → LibraryConsole → LibraryMemberConsole`
→ borrow-limit check on `Persona.getBorrowCount()` + `UserRole.getMaxBorrowLimit()`
→ debt check `FinanceService.checkBorrowingPermission(memberId)`
→ `LibraryService.executeBorrow(itemId)`
→ `PersonaService.recordBorrow(email, itemId)` (ownership stored in Persona).

### 5. My Inventory (Library shortcut → Persona)
`LibraryMemberConsole → InventoryConsole.show` (Persona package)
→ `LibraryService.getItemById` for each owned id (Persona reads Library details).
Discovery lives in Library; ownership lives in Persona.

### 6. Return / Extend
Return: `LibraryService.executeReturn` + `PersonaService.recordReturn`.
Extend: `FinanceService.proccessExtentionPayment(persona, fee)`
→ `PersonaService.updateWalletBalance` + `PersonaService.transferToAdmin(tax)`.

### 7. Wallet charge (simulated payment)
`FinanceConsole → FinanceMemberConsole → FinancePrinter.chargeWallet`
→ validate card → `FinanceService.proccessWalletCharge(persona, amount)`
→ `PersonaService.updateWalletBalance`.

### 8. Debt payment
`FinanceMemberConsole → FinanceService.payDebt(persona)`
→ `PersonaService.updateWalletBalance` (−total) + `transferToAdmin(tax)`
→ logs `DEBT_PAYMENT`; `checkBorrowingPermission` becomes true again.

### 9. Tax collection
Every `TAX` transaction adds to the Admin persona wallet through
`PersonaService.transferToAdmin`. Users never touch taxes directly.

### 10. Role-request workflow (Guest → Support → Admin → Persona)
`SupportMemberConsole → RoleRequestService.submit`
→ Admin reviews in `AdminInbox` → `RoleRequestService.approve`
→ `PersonaService.promoteRole(email, role)`
→ `NotificationService.notifyAddress → MailService.sendSystemNotification`.

### 11. Ticket workflow (User → Support → CallCenter → notification)
`SupportMemberConsole → SupportService.createTicket`
→ `CallCenterInbox` responds/closes via `SupportService.updateTicketStatus`
→ user reads result via `NotificationService` (Mail inbox).

### 12. Library communication workflow (CallCenter → Support → Library)
`CallCenterInbox / LibraryOperatorConsole`
→ `SupportService.addLibraryItemViaSupport(item)` or
  `SupportService.handleCallCenterStockUpdate(itemId, qty)`
→ `LibraryService.addItem` / `LibraryService.updateItemQuantityFromCallCenter`.

### 13. Financial report
`LibraryAdminConsole / FinanceAdminConsole → ReportService.exportReport`
→ `ReportService → LibraryService.getAllItems/getAllSuppliers` → HTML file.

## Encrypted databases (XOR at rest)

| Microservice | File | Stores |
|--------------|------|--------|
| Persona | `persona_secure.json` | users, roles, **wallet balances**, borrowed item ids |
| Finance | `finance_secure.json` | transactions, debts, tax, payment history |
| Library | `library.enc` | items + suppliers |
| Support | `support_tickets.json` | tickets (role requests/notifications are session/Mail-backed) |
| Mail | `mail.enc` | simulated messages incl. notifications |

Wallet balances stay in **Persona**; Finance only reads/updates them through
`PersonaService`. Only Admin can inspect encrypted databases from the dashboards.
