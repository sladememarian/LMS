# GUI Documentation: Library Management System

This document provides an overview of the JavaFX-based GUI added to the Library Management System.

## Architecture Overview
The GUI is built within the `ir.ac.kntu.gui` package. It follows a multi-scene architecture where the `Navigator` manages the primary stage and switches between different `View` implementations.

### Key Components
- **`GuiLauncher`**: The entry point for the GUI application.
- **`App`**: Extends `javafx.application.Application`, initializes the UI.
- **`Navigator`**: Manages the main `Stage` and provides methods to switch views.
- **`View`**: Interface that all UI screens implement, defining a root `Parent` node and a title.
- **`concurrency/BackgroundJobs`**: A shared executor service to run heavy tasks off the JavaFX Application Thread.
- **`util/Dialogs`**: Utility for displaying error/info alerts on the FX thread.
- **`util/UiTheme`**: Handles theme switching and persistence.

## Running the Application
The project now supports both CLI and GUI modes via Gradle:

```bash
# Launch the JavaFX GUI
./gradlew run

# Launch the original CLI
./gradlew runCli
```

For IDE-based execution, ensure the JavaFX modules (`javafx.controls`, `javafx.fxml`) are configured on the module path.
