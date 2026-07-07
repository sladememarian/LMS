# exception Package (Phase 2)

## In plain terms
This package is the app's "family tree" of error types. Instead of throwing
generic `IllegalArgumentException` everywhere (which tells you *that*
something went wrong but not *what kind* of thing), every error now has its
own specific, descriptively-named class.

Think of it like a hospital's triage categories instead of one big "sick"
label — `ValidationException` (bad input), `NotFoundException` (record
doesn't exist), `ConflictException` (already exists / already done),
`AuthorizationException` (not allowed to do that).

## The hierarchy
```
RuntimeException
  └─ BaseException                 (root of every custom exception in the app)
       ├─ ValidationException      (input is malformed / doesn't meet rules)
       ├─ AuthorizationException   (wrong credentials / not permitted)
       ├─ NotFoundException        (looked for something that doesn't exist)
       └─ ConflictException        (action conflicts with existing state)
```

Every exception has two constructors, matching the existing `DatabaseException`
style already used in the project:
- `new SomeException("message")`
- `new SomeException("message", causeThrowable)`

## Why this matters for the console UI
Because every specific exception (e.g. `UserNotFoundException`) extends one of
these 4 categories, and all 4 extend `BaseException`, the console layer can
catch everything with **one single catch block**:

```java
try {
    IamService.registerUser(credentials);
} catch (BaseException ex) {
    ConsoleColor.printError(ex.getMessage());
}
```

No matter which specific exception was thrown deep inside a service, the
console always knows how to handle it the same way: print the message, don't
crash, keep the menu running. This is **polymorphism** in action — the catch
block treats many different exception types uniformly through their shared
parent type.

## Where the concrete exceptions live
As of Step 2, 8 concrete exceptions were added for the IAM/Persona/SSO domain
(see [`iam.md`](iam.md), [`persona.md`](persona.md), [`sso.md`](sso.md) for
details): `DuplicateEmailException`, `InvalidPasswordException`,
`InvalidCredentialsException`, `InvalidVerificationCodeException`,
`UserNotFoundException`, `InvalidEmailFormatException`,
`InvalidPhoneFormatException`, `InvalidThemeException`.

More modules (Library, Finance, Support) will get their own specific
exceptions in later steps.
