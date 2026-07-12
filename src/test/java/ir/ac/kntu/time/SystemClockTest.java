package ir.ac.kntu.time;

import ir.ac.kntu.finance.SimulationClock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemClockTest {

    @Test
    void getCurrentDayDelegatesToSimulationClock() {
        assertEquals(SimulationClock.getCurrentDay(), SystemClock.getCurrentDay());
    }

    @Test
    void advanceOneDayDelegatesToSimulationClockAndPersists() {
        int before = SystemClock.getCurrentDay();
        SystemClock.advanceOneDay();
        assertEquals(before + 1, SystemClock.getCurrentDay());
        assertEquals(before + 1, SimulationClock.getCurrentDay());
    }

    @Test
    void utilityClassCannotBeInstantiated() throws NoSuchMethodException {
        java.lang.reflect.Constructor<SystemClock> constructor = SystemClock.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        java.lang.reflect.InvocationTargetException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                java.lang.reflect.InvocationTargetException.class, constructor::newInstance);
        org.junit.jupiter.api.Assertions.assertTrue(thrown.getCause() instanceof UnsupportedOperationException);
    }
}
