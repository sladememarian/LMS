# Finance Microservice

## Purpose
A deliberately small money service: wallet charges, debt management, simulated
payments, borrow-extension payments, transaction history, tax collection and
financial reports. Finance **does not own users** — wallet balances live in
Persona and Finance reads/updates them through `PersonaService`. Transactions,
debts and tax are persisted XOR-encrypted to `finance_secure.json`. Every
transaction also carries a creation **timestamp** so history can be shown in
time order. Finance additionally owns the simulated calendar and overdue-loan
accrual (`clock_secure.json`, `loans_secure.json`).

## Role-based experience
`FinanceConsole` routes by role:

| Role | Console | Capabilities |
|------|---------|--------------|
| Guest | `FinanceMemberConsole` | charge wallet, pay debt, view history |
| Student/Teacher | `FinanceMemberConsole` | member + extend return date payment |
| CallCenter | `FinanceOperatorConsole` | **view-only**: debt statistics, financial alerts |
| Admin | `FinanceAdminConsole` | search wallets, all debts, history, tax revenue, admin wallet, reports, encrypted DB, debug |

Users never start in Finance; it is reached from Library/Persona when money is
involved.

## Key service functions (`FinanceService`)
All payment methods now throw specific exceptions on failure instead of
returning a boolean.

| Method | Throws | Description |
|--------|--------|-------------|
| `proccessWalletCharge(...)` | — | Simulated card payment; validates format, credits wallet. |
| `proccessExtentionPayment(persona)` | `InsufficientFundsException` (wallet too low), `ValidationException` (no active borrows / already max extensions) | Charges wallet + tax for borrow-extension; delegates to LoanService. |
| `checkBorrowingPermission(memberId)` | — | Returns `true` if net debt is zero. |
| `getOutstandingDebt(memberId)` | — | Net debt = Σ DEBT − Σ DEBT_PAYMENT. |
| `payDebt(persona)` | `InsufficientFundsException` (wallet too low), `ValidationException` (no outstanding debt) | Pays net debt + 10% tax from wallet; logs DEBT_PAYMENT; unblocks borrowing. |
| `getTransactionsForMember(memberId)` | — | Per-user transaction history (time-sorted). |
| `getAllTransactions()` | — | Full ledger (admin/operator views). |
| `getTaxRevenueCollected()` | — | Σ of all TAX transactions. |
| `recordDebt(persona, amount, desc)` | — | Records an outstanding DEBT (e.g. overdue item). |
| `loadTransactions()` | — | Re-reads transactions from disk. |

`checkBorrowingPermission` was upgraded to use **net** debt so that `payDebt`
can restore borrowing (the existing "extension creates a debt" behaviour is
preserved).

## Date simulation & overdue loans (Admin = god of time)

Finance owns the simulated calendar and turns overdue items into debt by
**reusing** `recordDebt` — no parallel debt logic is introduced.

| Class | Role |
|-------|------|
| `SimulationClock` | A persisted simulated "today" (`clock_secure.json`). `getCurrentDay()` reads it; `advanceDay()` moves to the next day and persists it. Re-read on every access so separate instances agree on the date. |
| `Loan` | One borrowed-item loan: member id, item id, borrow day, **due day**, and `lastChargedDay`. |
| `LoanService` | Persists loans to `loans_secure.json`. `recordLoan` (on borrow) sets the due day to **3 simulated days** after borrowing; `clearLoan` (on return) removes it; `accrueOverdueDebts(currentDay)` injects one daily fine per overdue loan through `FinanceService.recordDebt`, charging each simulated day at most once. |

**Flow:** Library borrow → `LoanService.recordLoan`. Admin presses *Advance
Simulated Day* in the Support Admin inbox → `SimulationClock.advanceDay()` then
`LoanService.accrueOverdueDebts(newDay)`. Once the simulated date passes an
item's due day, the daily overdue fine appears as a normal Finance `DEBT`
(blocks borrowing until paid via *Pay Debt*).

## Simulated payment
`FinancePrinter.chargeWallet` captures card number / holder / CVV / expiry /
amount, validates format only (no real banking) and delegates to
`proccessWalletCharge`.

## Tax system
Every taxed operation (extension, debt payment) sends 10% to the Admin wallet via
`PersonaService.transferToAdmin`. Users never interact with taxes directly.

## Communications
Finance → Persona (wallet read/update, admin tax pool), Finance → Mail (legacy
notifications). Reached from Library (extend, **and borrow/return which record
and clear loans**) and Persona (debt/extension flows). Support reaches Finance
when the Admin advances the simulated day to accrue overdue fines.

All Finance read/write operations now **always reload** `finance_secure.json`
before operating so a debt or charge recorded in one running instance is
visible to any other instance immediately.
