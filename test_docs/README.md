# Test Documentation

This folder explains every JUnit test in `src/test/java/ir/ac/kntu/`. The suite
has **92 tests** across functional tests, domain-model tests, and two
quality-gate tests (CheckStyle + PMD) that fail the build on any style violation.

Run everything with:

```
./gradlew clean test
```

A green build means: all functional tests pass **and** `src/main` is style-clean.

## Index
| Doc | Covers |
|-----|--------|
| [style_tests.md](style_tests.md) | CheckStyle + PMD quality gates |
| [domain_models_tests.md](domain_models_tests.md) | `DomainModelsTest` |
| [iam_tests.md](iam_tests.md) | `IamServiceTest` |
| [persona_tests.md](persona_tests.md) | `PersonaServiceTest`, `PersonaInventoryTest` |
| [finance_tests.md](finance_tests.md) | `FinanceServiceTest`, `FinanceDebtTest` |
| [library_tests.md](library_tests.md) | `LibraryServiceTest`, `ItemModelsTest`, `LibraryAdminOpsTest` |
| [mail_tests.md](mail_tests.md) | `MailServiceTest`, `MailModelsTest` |
| [support_tests.md](support_tests.md) | `SupportServiceTest`, `RoleRequestServiceTest`, `SupportOperationsTest` |
| [sso_tests.md](sso_tests.md) | `SsoServiceTest` |
| [report_tests.md](report_tests.md) | `ReportServiceTest` |
| [util_tests.md](util_tests.md) | `ValidatorTest` |

Tests marked **(new)** were added in this iteration; the rest are existing tests
kept green.
