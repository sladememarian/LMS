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
   ├─ Library    → Support, Finance  (catalog changes via Support; borrow/return record loans)
   ├─ Support    → Persona, Library, Mail, Finance
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
| Library (operator/admin consoles) | Support (bridge), Report, Persona, Finance (loans) |
| Support | Persona, Library, Mail, Finance (simulated clock / overdue loans) |
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
→ `PersonaService.recordBorrow(email, itemId)` (ownership stored in Persona)
→ `LoanService.recordLoan(memberId, itemId, SimulationClock.getCurrentDay())`
  (the loan is due on the next simulated day).

### 5. My Inventory (Library shortcut → Persona)
`LibraryMemberConsole → InventoryConsole.show` (Persona package)
→ `LibraryService.getItemById` for each owned id (Persona reads Library details).
Discovery lives in Library; ownership lives in Persona.

### 6. Return / Extend
Return: `LibraryService.executeReturn` + `PersonaService.recordReturn`
+ `LoanService.clearLoan(memberId, itemId)`.
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
`SupportMemberConsole → RoleRequestService.submit` (persisted to
`role_requests.json`)
→ Admin reviews in `AdminInbox` → `RoleRequestService.approve`
→ `PersonaService.promoteRole(email, role)`
→ `NotificationService.notifyAddress → MailService.sendSystemNotification`.
Because every call reloads `role_requests.json`, a request submitted in one
running instance is visible to a separate Admin instance without a restart.

### 11. Ticket workflow (User → Support → CallCenter → notification)
`SupportMemberConsole → SupportService.createTicket`
→ `CallCenterInbox` **responds** via `SupportService.respondToTicket(id, message)`
  (stores the reply, marks `IN_PROGRESS`, notifies the creator) or closes via
  `SupportService.updateTicketStatus`
→ user reads the reply via `NotificationService` (Mail inbox).

### 12. Library communication workflow (CallCenter → Support → Library)
`CallCenterInbox / LibraryOperatorConsole`
→ `SupportService.addLibraryItemViaSupport(item)` or
  `SupportService.handleCallCenterStockUpdate(itemId, qty)`
→ `LibraryService.addItem` / `LibraryService.updateItemQuantityFromCallCenter`.

### 14. Date simulation (Admin → Finance clock + overdue debts)
`AdminInbox` *Advance Simulated Day* → `SimulationClock.advanceDay()` (persists
`clock_secure.json`) → `LoanService.accrueOverdueDebts(newDay)` → for each loan
whose due day has passed, `FinanceService.recordDebt(borrower, dailyFine, ...)`
injects the overdue fine as a normal Finance debt (charged once per simulated
day). Returning the item earlier (`LoanService.clearLoan`) stops accrual.

### 13. Financial report
`LibraryAdminConsole / FinanceAdminConsole → ReportService.exportReport`
→ `ReportService → LibraryService.getAllItems/getAllSuppliers` → HTML file.

## Encrypted databases (XOR at rest)

| Microservice | File | Stores |
|--------------|------|--------|
| Persona | `persona_secure.json` | users, roles, **wallet balances**, borrowed item ids |
| Finance | `finance_secure.json` | transactions, debts, tax, payment history |
| Library | `library.enc` | items + suppliers |
| Support | `support_tickets.json` | tickets (incl. CallCenter reply text) |
| Support | `role_requests.json` | Guest role-upgrade requests (now persisted, cross-instance) |
| Finance | `loans_secure.json` | active borrowed-item loans + due days |
| Finance | `clock_secure.json` | the simulated "today" |
| Mail | `mail.enc` | simulated messages incl. notifications |

Wallet balances stay in **Persona**; Finance only reads/updates them through
`PersonaService`. Only Admin can inspect encrypted databases from the dashboards.
