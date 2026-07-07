# time Package (Phase 2)

## In plain terms
A single class, `SystemClock`, that Phase 2 features (like reservations) will
ask "what day is it?" instead of talking to the existing simulation clock
directly.

## Why it exists
The app already has a working simulated clock: `SimulationClock` (in the
`finance` package), which the Admin advances one day at a time with a single
menu click. **That behavior is untouched — this was an explicit requirement.**

`SystemClock` is a thin wrapper (a "facade") in front of it:

```java
public final class SystemClock {
    public static int getCurrentDay() {
        return SimulationClock.getCurrentDay();
    }

    public static void advanceOneDay() {
        SimulationClock.advanceDay();
    }
}
```

## Why add a wrapper instead of using `SimulationClock` everywhere?
Two reasons:
1. **Naming/semantics** — new Phase 2 code (reservations, due-date logic, etc.)
   shouldn't need to know or care that the clock happens to live inside the
   `finance` package. `SystemClock` is a neutral, app-wide concept.
   2. **A single seam for future changes** — if the clock implementation ever
   changes, only `SystemClock` needs to be updated; everything built on top of
   it (reservations, etc.) keeps working unchanged.

Nothing about the Admin's existing "advance day" button changes. `SystemClock`
just gives Phase 2 code a clean, dedicated way to read the current day without
importing the `finance` package.
