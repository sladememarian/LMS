package ir.ac.kntu.gui;

// Plain launcher. Starting an Application subclass from a non-modular classpath
// can make the JVM complain that JavaFX is missing; launching from a class that
// does NOT extend Application is the standard workaround. This just calls
// App.main(...).
public final class GuiLauncher {

    private GuiLauncher() {
        // utility launcher, not instantiable
    }

    public static void main(String[] args) {
        App.main(args);
    }
}
