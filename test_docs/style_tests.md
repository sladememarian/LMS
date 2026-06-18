# Style Quality-Gate Tests

These two tests run the static-analysis tools against `src/main` and fail the
build if there is even one violation. They are why every production class must
respect indentation, method length, parameter count, complexity, duplicate
literals, etc.

## `CheckStyleTest`
| Test | What it checks |
|------|----------------|
| `testCheckStyleIndentation` | Loads `config.xml` and runs CheckStyle over all `src/main` `.java` files; asserts **0** errors. Enforces 4-space indentation, braces, method length ≤ 50, parameter count ≤ 4, one statement per line, switch defaults, private field visibility, etc. |
| `testCheckStyleNaming` | Re-runs CheckStyle (naming-focused) and asserts **0** errors (class/method/variable/package naming conventions). |

## `CheckPMDTest`
| Test | What it checks |
|------|----------------|
| `testPMD` | Runs PMD with `ruleset.xml` over `src/main`; asserts **0** rule violations. Enforces cyclomatic complexity ≤ 10, NCSS ≤ 30, no duplicate literals (> 3), no unused private members, `UseCollectionIsEmpty`, `MissingOverride`, numeric underscores, and many more. |

Implication for new code: the role consoles deliberately split their dispatch
methods and extract repeated literals into constants to stay under these limits.
