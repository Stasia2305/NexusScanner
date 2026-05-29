# Repository Guidelines

## Project Structure & Module Organization
NexusScan is a JavaFX application organized into a standard layered architecture under the `com.nexusscan` package:
- **`com.nexusscan.controller`**: Contains JavaFX controllers that manage the UI logic for views.
- **`com.nexusscan.model`**: Defines the data entities (e.g., `User`, `Archive`, `Box`, `Document`).
- **`com.nexusscan.service`**: Implements business logic and data access, including `DatabaseService` for SQLite interaction and `AppState` for global state management.
- **`src/main/resources/com/nexusscan`**: Contains FXML files defining the application's user interface layouts.

## Build, Test, and Development Commands
The project uses Maven with the JavaFX Maven plugin.
- **Run application**: `./mvnw clean javafx:run`
- **Build project**: `./mvnw clean compile`
- **Run tests**: `./mvnw test`
- **Run single test**: `./mvnw -Dtest=TestClassName test`
- **Package application**: `./mvnw clean package`

## Coding Style & Naming Conventions
- **Java Version**: The project is configured for **Java 25**.
- **Naming**: Controllers must be suffixed with `Controller` (e.g., `ScanningController.java`).
- **Database**: Uses SQLite via JDBC; schema interactions are managed in `DatabaseService.java`.

## Testing Guidelines
- **Framework**: JUnit 5 (Jupiter) is used for unit and integration testing.
- **Organization**: Tests are located in `src/test/java`.
