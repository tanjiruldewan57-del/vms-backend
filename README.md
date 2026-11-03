# Vehicle Management System (VMS) - Backend

**Service:** `vms-backend`  
**Technology Stack:** Java 21 (LTS) & Spring Boot 3.3.5  
**Data Access:** Spring Data JPA & HikariCP  
**Database Drivers:** PostgreSQL, MySQL, H2 (Local / Test Fallback)  

---

## 🏗️ Workspace Layout

```
vms-backend/
├── pom.xml                                    # Dependencies (Spring Web, JPA, PostgreSQL, MySQL, H2)
├── .env.example                               # Zero-secret environment template
├── .editorconfig                              # Workspace formatting rules
├── .gitignore                                 # Git ignore (excludes .env, credentials, bytecode)
├── README.md                                  # Setup & execution instructions
├── src/
│   ├── main/
│   │   ├── java/com/starkgroup/vms/
│   │   │   ├── VmsBackendApplication.java     # Application entrypoint
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java        # Security filter chain & permit endpoints
│   │   │   │   ├── CorsProperties.java        # CORS properties binding
│   │   │   │   └── JpaAuditingConfig.java     # Auditing configuration
│   │   │   ├── controller/
│   │   │   │   └── HealthController.java      # Database connectivity health probe (/api/v1/health)
│   │   │   ├── dto/
│   │   │   │   └── HealthResponse.java        # Health response DTO
│   │   │   └── common/
│   │   │       ├── api/                       # ApiResponse, ErrorResponse
│   │   │       └── exception/                 # GlobalExceptionHandler, Custom exceptions
│   │   └── resources/
│   │       ├── application.yml                # Base configuration (Env-driven DB & HikariCP)
│   │       ├── application-local.yml          # Local profile (H2 fallback or local PostgreSQL)
│   │       └── application-prod.yml           # Production profile (PostgreSQL / MySQL)
│   └── test/
│       └── java/com/starkgroup/vms/
│           ├── VmsBackendApplicationTests.java# Context loads test
│           ├── config/
│           │   └── DatabaseConnectivityTest.java # Live DataSource connection & metadata verification
│           └── controller/
│               └── HealthControllerTest.java  # Health API & Actuator probe test
```

---

## 🔌 SQL Database Configuration

Credentials and connection strings are managed **strictly via environment variables** and are kept out of source control.

### Supported SQL Databases
- **PostgreSQL** (Driver: `org.postgresql.Driver`)
- **MySQL** (Driver: `com.mysql.cj.jdbc.Driver`)
- **H2** (In-memory development fallback)

### Environment Variables
Configure these variables in your environment or a local `.env` file (copied from `.env.example`):

| Variable | Description | Default (Local Profile) |
| :--- | :--- | :--- |
| `DB_URL` | JDBC Connection URL | `jdbc:h2:mem:vms_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` |
| `DB_USERNAME` | Database username | `sa` |
| `DB_PASSWORD` | Database password | *(empty)* |
| `DB_DRIVER` | JDBC Driver class (optional) | Auto-detected from URL |
| `DB_POOL_MAX_SIZE` | Maximum HikariCP pool size | `10` |
| `DB_POOL_MIN_IDLE` | Minimum idle connections | `2` |
| `JPA_DDL_AUTO` | Hibernate schema management | `update` (local) / `validate` (prod) |
| `JPA_SHOW_SQL` | Log SQL queries | `true` (local) / `false` (prod) |

---

## 🚀 Running the Project & Verifying Connectivity

### 1. Build and Run Automated Tests
```bash
# Execute unit and database connectivity tests
mvn clean test
```

### 2. Start Application Locally
```bash
# Start with default local profile (H2 in-memory)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Or start pointing to your external PostgreSQL/MySQL database
DB_URL="jdbc:postgresql://127.0.0.1:5432/vms_db" \
DB_USERNAME="vms_user" \
DB_PASSWORD="your_password" \
mvn spring-boot:run
```

### 3. Verify Health Check Endpoints
Once the application starts, ping the health endpoints:

```bash
# Custom SQL Database Health Probe
curl -s http://127.0.0.1:8080/api/v1/health

# Output:
# {
#   "success": true,
#   "message": "System and database are healthy",
#   "data": {
#     "status": "UP",
#     "database": "CONNECTED",
#     "databaseProductName": "PostgreSQL",
#     "databaseProductVersion": "16.1",
#     "timestamp": "..."
#   }
# }

# Spring Boot Actuator Health Probe
curl -s http://127.0.0.1:8080/actuator/health
```
