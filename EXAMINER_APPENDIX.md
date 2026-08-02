# Appendix: Technical Information for Examiners

This document provides the necessary information to install, configure, and run the NexusScan application.

## 1. System Requirements
- **Java Development Kit (JDK)**: Version 21 or higher.
- **Build Tool**: Maven (wrapper included in the project).
- **Operating System**: Windows, macOS, or Linux.
- **Database**: Access to the School's MSSQL Server (VPN may be required depending on network location).

## 2. Installation and Running the Application
The project is provided as a standard IntelliJ IDEA project.

1. **Importing the Project**:
   - Open IntelliJ IDEA.
   - Select `File -> Open` and navigate to the project root directory.
   - Wait for Maven to sync and download all dependencies.

2. **Running the Application**:
   - Use the Maven wrapper provided in the root:
     ```bash
     ./mvnw clean javafx:run
     ```
   - Alternatively, use the IntelliJ Maven tool window: `nexusscan -> Plugins -> javafx -> javafx:run`.

3. **Running Tests**:
   - Execute tests via Maven:
     ```bash
     ./mvnw test
     ```

## 3. Database Configuration
The application is pre-configured to connect to the School's Microsoft SQL Server.

- **JDBC URL**: `jdbc:sqlserver://10.176.111.34:1433;databaseName=NexusScan;trustServerCertificate=true`
- **Username**: `CS2025b_e_4`
- **Password**: `CS2025bE4#23`

The configuration is located in `./src/main/resources/db.properties`.

## 4. Default Application Credentials
Upon the first run, the system automatically seeds the database with the following default accounts:

### Admin Account
- **Username**: `admin`
- **Password**: `admin`
- **Role**: ADMIN (Full access to User Management, Profile Configuration, and Logs)

### Default Scanning Profile
- **Profile Name**: `Default Profile`
- **Assigned To**: `admin`

## 5. Project Architecture
The application follows a strict **3-layered architecture**:
- **Presentation**: JavaFX Controllers and FXML views.
- **Logic (Service)**: Business rules and coordination.
- **Data Access (DAL)**: DAO interfaces and MSSQL-specific implementations.

**Design Patterns used**:
- **Singleton**: For service and database connection management (e.g., `DatabaseService`, `AppState`, `LoggingService`).
- **DAO (Data Access Object)**: For abstracting database operations and decoupling the persistence layer.
- **Factory**: For creating DAO instances (`DAOFactory`) without coupling the logic layer to concrete implementations.
- **Strategy**: Used in document splitting logic. The `ISplitStrategy` interface allows different algorithms (barcode vs. fixed page count) to be used interchangeably.
- **Composite**: The `CompositeSplitStrategy` allows multiple splitting strategies to be combined and evaluated as a single unit.
