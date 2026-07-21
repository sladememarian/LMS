package ir.ac.kntu.gui;

/**
 * Plain launcher class.
 *
 * <p>When an application that extends {@link javafx.application.Application} is
 * started from a non-modular classpath, the JVM can complain that the JavaFX
 * runtime components are missing. Launching through a separate class that does
 * <em>not</em> extend {@code Application} is the standard workaround: this
 * {@code main} simply delegates to {@link App#main(String[])}.
 */
public final class GuiLauncher {

    private GuiLauncher() {
        // utility launcher, not instantiable
    }

    public static void main(String[] args) {
        App.main(args);
    }
}
