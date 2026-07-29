# System Specification & Prompt Guide: Library Management System (LMS) — Phase 3

This document serves as a comprehensive, English-translated specification and prompt guide for the third phase of the Library Management System (LMS) project. It contains all project requirements, architectural guidelines, UI mappings, multithreading constraints, and learning resources.

---

## 1. Project Overview & Meta-Information
* **Course:** Advanced Programming (Java)
* **Instructors:** Dr. Khanmirza, Mr. Zamanian
* **Semester:** Spring 1405 (2026)
* **Core Technologies:** JavaFX GUI, Java Streams API, Multithreading & Concurrency

### The Three-Phase Roadmap
1. **Phase 1:** Encapsulation, Inheritance, Regular Expressions, Collections
2. **Phase 2:** Polymorphism, Generics, Interfaces, Exception Handling
3. **Phase 3 (Current):** Threads & Concurrency, JavaFX GUI

### Project Goal
Transform the CLI-based LMS from Phases 1 & 2 into a professional, real-world desktop application. The user interface must be fully migrated to JavaFX without losing any existing functionality. The system must utilize concurrent background processing to maintain a highly responsive UI, and declarative stream operations (Java Streams API) to compute analytics and filter datasets.

---

## 2. JavaFX UI & UX Architecture

All user interaction must occur via the JavaFX graphical interface. Direct terminal/console input/output is prohibited for user interactions.

### Core Design Rules
* **Multi-Scene Navigation:** Implement a multi-scene or tab-based architecture. Every core section (Login, Library, Profile, Support, etc.) must reside in its own distinct `Scene`.
* **Pagination:** Tables and listings displaying large datasets (e.g., items, users) must be paginated to prevent memory and UI performance issues.
* **Visual Progress Feedback:** Any long-running or background operations (e.g., loading databases, exporting reports) must display a `ProgressBar` or `ProgressIndicator`.
* **User Notifications:** All confirmation, warning, and error messages must use JavaFX `Alert` dialogs or custom visual notifications. Directing errors to the standard console output is not allowed.

### Mandatory Pages & Views
Your implementation must contain at least the following 11 pages:

| # | Page / Scene | Description |
|---|---|---|
| 1 | **Login / Registration** | User authentication and new user registration with strict field validation. |
| 2 | **Dashboard** | Dynamic dashboard customized for each role (User, Support, Admin). |
| 3 | **Library / Search** | Catalog browser featuring real-time search and filter controls. |
| 4 | **Item Management** | Administrator interface to add, edit, and delete library inventory. |
| 5 | **Loans & Reservations** | Displays the user's active loans, past loan history, and pending reservations. |
| 6 | **Wallet & Transactions** | Wallet management page for checking balances, topping up funds, and viewing transaction logs. |
| 7 | **Support** | Ticket registration system and messaging portal for users to interact with support staff. |
| 8 | **User Management** | Administrator interface to search, list, edit, and toggle activation status of user accounts. |
| 9 | **Support Staff Management** | Administrative control panel to create support profiles and assign responsibility domains. |
| 10| **System Settings** | Panel to configure core system constants and global variables. |
| 11| **Fines** | Tabular overview of indebted users, accessible by support and admins. |

---

## 3. Advanced Role-Based Features & Dashboards

### 3.1 Regular User Dashboard & Features
* **Personalized Analytics:** Upon login, show visual widgets (charts, progress bars, or info cards) summarizing:
  * Number of items currently borrowed.
  * Active reservation counts.
  * Outstanding debt / fines.
  * Recent transaction logs.
  * *Note: All calculations must be executed using Java Streams.*
* **Real-Time Advanced Search:** Build a search bar that queries the library catalog dynamically as the user types (keystroke-by-keystroke). 
  * *Concurrency Rule:* This search must run on a background thread to prevent UI freezing.
* **Instant Notifications:** Display a visual, non-blocking toast/alert inside the UI as soon as a user's reserved item is checked back in and made available.

### 3.2 Support Dashboard & Features
* **Ticketing Metrics:** Display a dashboard showing:
  * Number of currently open support tickets.
  * Number of tickets resolved today.
  * A trend line chart displaying incoming vs. resolved ticket volume over the past week.
* **Fine and Debt Table:** A dedicated `TableView` showing all indebted users.
  * Must support sorting by fine amount.
  * Must support filtering by date range using date pickers.
  * *Stream Rule: Filtering and sorting computations must be written with the Java Streams API.*

### 3.3 Admin Dashboard & Features
* **Global Metrics:** A unified administration panel displaying:
  * Total number of registered users.
  * Total catalog inventory count.
  * Total revenue accumulated from fines.
  * System activity overview.
  * *Stream Rule: All statistical aggregation must be performed using the Java Streams API.*
* **Analytical Visualizations:**
  * **Item Popularity Chart:** A `BarChart` representing the top 10 most frequently borrowed library items (computed from transaction history using Streams).
  * **Revenue Chart:** A `LineChart` illustrating fine revenues broken down month-by-month.

---

## 4. Multithreading & Concurrency Directives

To maintain a fluid 60-FPS user interface, developers must strictly adhere to Java's concurrency framework:
* **The Golden Rule:** Never block the **JavaFX Application Thread** with network requests, disk I/O, database operations, or heavy stream operations.
* **Background Workers:** Use JavaFX's native concurrent utilities, specifically the `Task<V>` and `Service<V>` classes.
* **UI Thread Synchronization:** When updating UI elements (labels, lists, charts) from a background thread, the updates must be wrapped inside `Platform.runLater(Runnable)` to prevent threading exceptions (`IllegalStateException: Not on FX application thread`).

---

## 5. Java Streams API Mandates

Loops (`for`, `while`, `do-while`) are **strictly prohibited** for data processing tasks.
* **Applicability:** Any operation involving filtering, sorting, transforming (mapping), grouping, or aggregating collections must be refactored to use `java.util.stream.Stream`.
* **Examples of Declarative Pipeline Requirements:**
  ```java
  // Correct declarative approach:
  List<User> delinquentUsers = users.stream()
      .filter(user -> user.getFineBalance() > 0)
      .sorted(Comparator.comparingDouble(User::getFineBalance).reversed())
      .collect(Collectors.toList());
  ```

---

## 6. Internal Notification System

* **Reservation Alerts:** Trigger an internal notification record when a reserved book becomes available. The user must see this notification on their personal dashboard.
* **Due Date Warning:** If a checked-out item is within **3 days** of its return deadline, trigger an alert upon user login.
* **Notification Inbox:** Provide a designated center where users can view all notifications (read/unread) and mark them as read.
* **Optimized Check Execution (Time Simulation Note):**
  * Time in this project is simulated via an integer counter (e.g., "Current Day").
  * Do **not** run an infinite background thread checking the system clock.
  * Instead, trigger the background check logic strictly on two events:
    1. **At Login:** Launch a background `Task` to calculate due-date warnings using streams.
    2. **On Clicking "Next Day":** If a simulated clock-advancement button is clicked, execute the notification calculation in a background thread to prevent UI micro-stutters.

---

## 7. Mandatory Design Guidelines & Best Practices

* **Feature Parity:** 100% of the features from Phase 1 and Phase 2 must work seamlessly within the new JavaFX environment.
* **Object-Oriented Integrity:**
  * Avoid placing massive blocks of logic inside a single class or within Controller files. Keep a clean Model-View-Controller (MVC) or Model-View-ViewModel (MVVM) structure.
  * Excessive use of `static` methods and variables is penalized. Ensure static context is strictly limited to utility classes, or defend your design pattern during evaluation.
  * Avoid global singletons unless absolutely justified.
* **Exception Handling:**
  * Catch all expected exceptions. Instead of dumping stack traces to `System.err` or printing to console, catch them and display the messages gracefully to the user in a JavaFX `Alert` dialog.

---

## 8. Bonus Capabilities (Grade Multipliers)

1. **Version Control:** Adhere to **Conventional Commits** (e.g., `feat:`, `fix:`, `docs:`) and maintain an active, logical Git commit history.
2. **Animations & UI Polishing:** Use JavaFX transitions (e.g., `FadeTransition`, `TranslateTransition`) for scene transitions, sliding sidebar menus, and notification pop-ups.
3. **HTML Report Engine:** Build a utility that generates complete financial statements and inventory reports formatted in HTML (including embedded CSS styles and responsive tables). This engine must run inside a background thread and show progress via a `ProgressBar`.
4. **Data Persistence Engine:** Integrate a persistence layer. Store all data using either database technologies via **JDBC** (SQLite, MySQL, H2) or serialized file structures (**JSON** or **XML**). Data load and save procedures must execute asynchronously.
5. **Dynamic Theme Engine:** Add Light / Dark mode settings. The chosen preference must persist in a configuration file and load automatically upon application startup.

---

## 9. Developer & Student Reference Materials

### Official Documentation & Libraries
* **JavaFX Official Javadoc:** [openjfx.io/javadoc/21](https://openjfx.io/javadoc/21/)
* **Gluon JavaFX Ecosystem:** [gluonhq.com/products/javafx](https://gluonhq.com/products/javafx/)
* **JavaFX Gradle Plugin Guide:** [openjfx.io/openjfx-docs](https://openjfx.io/openjfx-docs/)

### Learning Portals & Courses
* **JavaFX Setup & Configuration:** [javacup.ir/free-course-javafx](http://javacup.ir/free-course-javafx/)
* **Graphics Context & Canvas:** [KNTU Layout Guide (PDF)](https://drive.google.com/file/d/1V_31oj5oGaQGBOvysTa48vH_a7vavCwB/view?usp=sharing)
* **Understanding Built-in Layout Panes:**
  * [Oracle Layout Guide](https://docs.oracle.com/javafx/2/layout/builtin_layouts.htm)
  * [JavaTpoint Layout Tutorials](https://www.javatpoint.com/javafx-layouts)
* **KNTU Archive Lectures:** [Recorded Stream Playback](https://meet2.kntu.ac.ir/playback/presentation/2.3/ef631c1130b4ebf46a9d666cb2518981db6a5e13-1652086472832)
* **JavaFX Complete Video Masterclass:** [JavaFX GUI Full Course (YouTube)](https://www.youtube.com/watch?v=9XJicRt_FaI)

### VM Arguments for Execution
When running your JavaFX application manually, configure your IDE or runtime VM options with the following arguments:
```bash
--module-path "[path-to-javafx-lib];mods\production"
--add-modules=javafx.controls,javafx.fxml
```

### Framework Conceptual Guide
* **Baeldung Tutorials:**
  * [JavaFX Basics](https://www.baeldung.com/javafx)
  * [Java Streams API Complete Guide](https://www.baeldung.com/java-8-streams)
  * [Java Concurrency Core API](https://www.baeldung.com/java-util-concurrent)
  * [Java ExecutorService Deep-Dive](https://www.baeldung.com/java-executor-service-tutorial)
