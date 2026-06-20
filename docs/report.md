# Financial Report — What the Admin Sees

When an Admin or Call Center user selects **"Generate HTML Report"** (Library dashboard) or **"View Financial Reports"** (Finance dashboard), the app creates a file called `library_financial_report.html` in the project folder. Open it in any browser — no internet needed.

## The report has 4 sections:

### 1. Quick Summary Cards (top row)
Six big-number boxes showing the **total picture** of the library's inventory:

| Box | What it means |
|---|---|
| **Companies** | How many supplier companies the library works with |
| **Catalogued Items** | Total number of different book/item types in the system (not copies — distinct titles) |
| **Total Copies** | How many physical copies the library owns, across all items |
| **Copies With Users** | How many copies are currently checked out (borrowed by members) |
| **Inventory Value** | How much the entire collection costs to buy (unit price × total copies), summed up |
| **Value In Circulation** | The value of all items currently borrowed (unit price × borrowed copies) |

### 2. Inventory Value Bar Chart
A horizontal bar for each supplier company. **The longer the bar, the more the library paid for items from that company.** You can compare at a glance which suppliers the library has invested the most in.

### 3. Value In Circulation Donut Chart
A donut chart (colored ring) showing how the value of currently-borrowed items splits between suppliers. Hover/tap is not needed — the legend beside it lists each company's share in **credits (cr)** and **percentage**. If a company has a big slice, it means a large portion of the library's active loans are from that supplier.

### 4. Per-Company Breakdown Table
A table with one row per supplier:

| Column | Meaning |
|---|---|
| **Company** / **ID** | Supplier name and its internal ID |
| **Items** | How many different item types the library buys from them |
| **Total Copies** | How many physical copies the library owns from them |
| **With Users** | How many of those copies are currently out on loan |
| **Inventory Value** | What the library paid to buy all those copies |
| **In Circulation** | Value of the copies currently checked out |

## Who can generate it?
Only **Admin** and **Call Center** roles. If a regular user tries, the app shows an error.

## How is it styled?
Dark theme (dark blue background), blue cards, colored bars and charts. Inline CSS — nothing to download. Just double-click the file to open it.

## Where does it save?
Same folder as the app. The exact path is printed in green after generation, for example: `Report generated at D:\uni\prj1_again\project-1\library_financial_report.html`
