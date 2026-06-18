# Finance Tests

## `FinanceServiceTest`
| Test | Explanation |
|------|-------------|
| `walletChargeAddsFunds` | `proccessWalletCharge` increases the Persona wallet by the amount. |
| `walletChargeRejectsNonPositive` | A non-positive charge throws `IllegalArgumentException`. |
| `extensionPaymentSucceedsWithTax` | A 100 extension on a 1,000 wallet leaves 890 (100 + 10% tax). |
| `extensionPaymentFailsWhenInsufficient` | Extension fails when the wallet cannot cover amount + tax. |
| `extensionPaymentRejectsNonPositive` | A non-positive extension returns `false`. |
| `debtBlocksBorrowingPermission` | After an extension payment records a debt, `checkBorrowingPermission` becomes `false`. |

## `FinanceDebtTest` (new)
Covers the debt accounting, history accessors and tax aggregation added to
`FinanceService`.

| Test | Explanation |
|------|-------------|
| `recordDebtIncreasesOutstandingAndBlocksBorrowing` | `recordDebt` raises `getOutstandingDebt` and blocks borrowing. |
| `recordDebtRejectsNonPositive` | A non-positive debt throws `IllegalArgumentException`. |
| `payDebtClearsOutstandingAndRestoresBorrowing` | `payDebt` clears the net debt and re-enables borrowing. |
| `payDebtFailsWithoutDebt` | Paying with no outstanding debt returns `false`. |
| `transactionHistoryRecordsCharges` | `getTransactionsForMember` returns each charge for that member. |
| `taxRevenueAccumulates` | `getTaxRevenueCollected` grows after a taxed extension payment. |
