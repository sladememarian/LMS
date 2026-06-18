# Finance Microservice

## Purpose
A deliberately small money service: wallet charges, debt management, simulated
payments, borrow-extension payments, transaction history, tax collection and
financial reports. Finance **does not own users** — wallet balances live in
Persona and Finance reads/updates them through `PersonaService`. Transactions,
debts and tax are persisted XOR-encrypted to `finance_secure.json`.

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
Existing (reused): `proccessWalletCharge`, `proccessExtentionPayment`,
`checkBorrowingPermission`, `loadTransactions`.

Added:
| Method | Description |
|--------|-------------|
| `getOutstandingDebt(memberId)` | Net debt = Σ `DEBT` − Σ `DEBT_PAYMENT`. |
| `getTransactionsForMember(memberId)` | Per-user transaction history. |
| `getAllTransactions()` | Full ledger (admin/operator views). |
| `getTaxRevenueCollected()` | Σ of all `TAX` transactions. |
| `recordDebt(persona, amount, desc)` | Records an outstanding `DEBT` (e.g. overdue item). |
| `payDebt(persona)` | Pays net debt + 10% tax from wallet; logs `DEBT_PAYMENT`; unblocks borrowing. |

`checkBorrowingPermission` was upgraded to use **net** debt so that `payDebt`
can restore borrowing (the existing "extension creates a debt" behaviour is
preserved).

## Simulated payment
`FinancePrinter.chargeWallet` captures card number / holder / CVV / expiry /
amount, validates format only (no real banking) and delegates to
`proccessWalletCharge`.

## Tax system
Every taxed operation (extension, debt payment) sends 10% to the Admin wallet via
`PersonaService.transferToAdmin`. Users never interact with taxes directly.

## Communications
Finance → Persona (wallet read/update, admin tax pool), Finance → Mail (legacy
notifications). Reached from Library (extend) and Persona (debt/extension flows).
