package ir.ac.kntu.time;

import ir.ac.kntu.finance.SimulationClock;

public final class SystemClock {

    private SystemClock() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static int getCurrentDay() {
        return SimulationClock.getCurrentDay();
    }

    public static void advanceOneDay() {
        SimulationClock.advanceDay();
    }
}
