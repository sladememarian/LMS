# Report Microservice

## Purpose
Generates a self-contained HTML financial report describing every supplier
company: inventory value, value in circulation, popularity and charts (inline
CSS + SVG, no internet required).

## Key functions (`ReportService`)
`isAuthorized` (Admin / CallCenter), `computeSupplierFinancials`,
`exportReport(path)` → writes `library_financial_report.html`, plus the HTML
builders. `SupplierFinancials` accumulates per-supplier totals from items.

## Where it is reached
The report is now generated from the **Admin Library dashboard**
("Generate HTML Report") and the **Admin Finance dashboard**
("View Financial Reports"). It reads Library data via
`LibraryService.getAllItems` / `getAllSuppliers`.
