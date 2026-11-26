# Enterprise Ticket Management System (ETMS)

## 1. System Requirements & Environment

Component	Required Version
Java (JDK)	Java 25 (OpenJDK 25.0.2+)
Build Tool	Apache Maven 3.9+
Framework	Spring Boot 3.4.4
Database	PostgreSQL 17 / 18
Liquibase	5.0.4


## 2. Local Setup & Build Instructions

### Step 1: Start PostgreSQL Service
Ensure PostgreSQL is running locally on port `5432` with database `ticket_dev_db`:
```powershell
# Windows PowerShell (Run as Administrator)
net start postgresql-x64-18
```

### Step 2: Build the Application
Set `JAVA_HOME` to your JDK 25 directory and compile with Maven:
```powershell
$env:JAVA_HOME = "C:\Users\DS\.jdks\openjdk-25.0.2"
mvn clean compile
```

### Step 3: Run the Application
```powershell
mvn spring-boot:run
```

## 3. CI/CD Pipeline & Quality Gate Rules

The repository enforces automated continuous integration via GitHub Actions (`.github/workflows/ci-cd.yml`):

### Pipeline Quality Gates & Rules:
1. **Trigger Scope**: Automatically executes on Pull Requests to `main` and on pushes to `main`, `FR-*`, and `feature/*`.
2. **JDK 25 Runtime**: Builds and verifies against clean Oracle/OpenJDK 25.
3. **Spotless Code Formatting Check**:
   - `mvn spotless:check` verifies strict Google Java code formatting and import cleanliness before tests are executed.
   - Run `mvn spotless:apply` locally to auto-format any changed files.
4. **Automated Test & Coverage Rule (50% Minimum Gate)**:
   - JaCoCo (`jacoco-maven-plugin`) enforces a strict **minimum 50% line coverage threshold**.
   - If overall test coverage is **>= 50%**, the pipeline passes (`BUILD SUCCESS`).
   - If overall test coverage falls **< 50%**, JaCoCo fails the build (`BUILD FAILURE`), preventing the PR from being merged.
5. **Zero Comment Standard**: Strict avoidance of `//` or `/** */` comments across production Java source files.
6. **Database Migration Integrity**: Master Liquibase configuration (`db.changelog-master.yaml`) is validated before artifacts are packaged.





