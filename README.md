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
3. **Automated Test & Coverage Rule (50% Minimum Gate)**:
   - JaCoCo (`jacoco-maven-plugin`) enforces a strict **minimum 50% line coverage threshold**.
   - If overall test coverage is **>= 50%**, the pipeline passes (`BUILD SUCCESS`).
   - If overall test coverage falls **< 50%**, JaCoCo fails the build (`BUILD FAILURE`), preventing the PR from being merged.
4. **Zero Comment Standard**: Strict avoidance of `//` or `/** */` comments across production Java source files.
5. **Database Migration Integrity**: Master Liquibase configuration (`db.changelog-master.yaml`) is validated before artifacts are packaged.

## 4. Standard Pull Request (PR) Format

All Pull Requests targeting `main` must adhere to the standardized format below:

### PR Title Convention:
`FR-XXX : concise description in imperative mood`
* Example: `FR-001 : implement user authentication and ticket creation sequence`
* Example: `FR-006 : implement public replies private notes and status history`
* Example: `FR-007 : implement secure attachment upload download and calculate SLA deadlines`
* Example: `FR-008 : configure CI/CD pipeline with spotless check and 50% test coverage gate`
### PR Title Convention:
`FR - XXX : concise description in imperative mood`
* Example: `FR - 001 : implement user authentication and ticket creation sequence`

### PR Description Template:
```markdown
### Summary of Changes:
- Bullet points detailing features or fixes introduced.

### Quality Gate Check Sequence:
1. `mvn clean compile -B` (Build & Zero Comment check)
2. `mvn spotless:check -B` (Code style & formatting check)
3. `mvn test -B` (Unit test execution + 50% JaCoCo coverage check)
4. `mvn package -DskipTests=true -B` (Packaging verification)

### Verification & Testing:
- Local test results and Postman endpoint verification notes.
```






