# Support Tests

## `SupportServiceTest`
| Test | Explanation |
|------|-------------|
| `technicalTicketIsHighPriority` | A `Technical` ticket is created with `HIGH` priority. |
| `urgentKeywordEscalatesToCritical` | A title containing `URGENT` escalates to `CRITICAL`. |
| `generalTicketIsLowPriority` | A general ticket is `LOW`. |
| `ticketsSortedByPriorityDescending` | `getAllTickets` is sorted highest priority first. |
| `callCenterLoginValidates` | CallCenter credentials validate; a wrong password fails. |
| `stockUpdateRequiresCallCenterRole` | `handleCallCenterStockUpdate` only changes stock when the current user is CALLCENTER. |
| `placeholderRejectsNulls` | `submitLibraryItemPlaceholder` rejects null arguments. |

## `RoleRequestServiceTest` (new)
Covers the Guest → Support → Admin → Persona role-request workflow.

| Test | Explanation |
|------|-------------|
| `submitCreatesPendingRequest` | A submitted request is `PENDING` and appears in `getPending`. |
| `approveUpgradesPersonaRole` | Approving applies the role change in Persona (GUEST → STUDENT) and a second approve returns `false` (already processed). |
| `rejectMarksRejected` | Rejecting sets `REJECTED`; an unknown request id returns `false`. |

## `SupportOperationsTest` (new)
Covers ticket-status transitions, the CallCenter → Support → Library bridge, and
notifications.

| Test | Explanation |
|------|-------------|
| `updateTicketStatusFindsAndUpdates` | `updateTicketStatus` closes an existing ticket and returns `false` for an unknown id. |
| `addLibraryItemViaSupportRequiresOperatorRole` | A CallCenter user can add a catalog item through Support (it appears in the Library); with no current user the bridge is denied. |
| `notificationsAreDeliveredThroughMail` | `NotificationService.notifyAddress` delivers a `SYSTEM_NOTIFICATION` to the recipient's Mail inbox (proves notifications reuse Mail). |
