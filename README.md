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




