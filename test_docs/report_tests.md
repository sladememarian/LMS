# Report Tests — `ReportServiceTest`

| Test | Explanation |
|------|-------------|
| `clearUser` | Helper/setup ensuring no current user leaks between authorization tests. |
| `authorizationByRole` | `isAuthorized` is true for ADMIN / CALLCENTER and false otherwise. |
| `financialsCoverAllSuppliers` | `computeSupplierFinancials` produces an entry for every supplier. |
| `htmlContainsHeadingAndCompany` | The generated HTML contains the report heading and supplier names. |
| `exportRequiresAuthorization` | `exportReport` throws `IllegalStateException` when the current user is not authorized. |
| `exportWritesFileWhenAuthorized` | When authorized, `exportReport` writes the HTML file to disk. |
