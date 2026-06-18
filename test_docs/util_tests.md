# Util Tests — `ValidatorTest`

| Test | Explanation |
|------|-------------|
| `emailValidation` | Accepts well-formed emails, rejects malformed ones. |
| `phoneValidation` | Accepts Iranian mobile formats (`+98/98/0` + `9` + 9 digits), rejects others. |
| `passwordPolicy` | Enforces the password strength policy. |
| `idValidation` | Validates member-id and item-id patterns. |
| `publicationValidation` | Validates ISBN-13 / ISSN / publish-year rules. |
| `downloadUrlValidation` | Validates digital-item download URLs. |
