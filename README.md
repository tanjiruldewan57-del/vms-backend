# Vehicle Management System (VMS) - Backend

**Service:** `vms-backend`  
**Technology Stack:** Java 21 (LTS) & Spring Boot 3.3.5  
**Architecture:** Decoupled Hexagonal / Modular Monolith Architecture  

---

## 🏗️ Workspace Layout

```
vms-backend/
├── pom.xml                               # Maven project descriptor & dependency management
├── .env.example                          # Secret & environment variable template
├── .editorconfig                         # Workspace code formatting rules
├── .gitignore                            # Production Java/Spring ignore definitions
├── src/
│   ├── main/
│   │   ├── java/com/starkgroup/vms/
│   │   │   ├── VmsBackendApplication.java # Spring Boot Application entrypoint
│   │   │   ├── common/                  # Cross-cutting concerns & shared DTOs
│   │   │   ├── config/                  # CORS, Security, WebMvc configurations
│   │   │   ├── security/                # JWT filters & auth providers
│   │   │   └── modules/                 # Bounded context feature domains (Vehicle, etc.)
│   │   └── resources/
│   │       ├── application.yml          # Base configuration (Host: 127.0.0.1, Port: 8080)
│   │       ├── application-local.yml    # Development profile (H2 / local PostgreSQL)
│   │       └── application-prod.yml     # Production profile (PostgreSQL, connection pool)
│   └── test/
│       └── java/com/starkgroup/vms/     # Automated test suites
```

---

## 🚀 Getting Started

### Prerequisites
- **JDK 21** or later
- **Apache Maven 3.9+** (or IDE integrated runner)
- Local PostgreSQL instance or in-memory H2 (default in local profile)

### Run Locally
```bash
# Copy and configure environment variables
cp .env.example .env

# Run using Maven
mvn clean spring-boot:run -Dspring-boot.run.profiles=local
```

### Health Check
Once running, verify service liveness:
```bash
curl -X GET http://127.0.0.1:8080/actuator/health
```

---

## 🔒 Security & Decoupling Boundaries
- **Strict Network Isolation:** Defaults to `127.0.0.1` binding to avoid unwanted interface exposure.
- **Independent Project:** Kept strictly decoupled from `vms-frontend`. All communication happens exclusively via RESTful JSON APIs (`http://127.0.0.1:8080/api/v1`).
- **CORS Whitelist:** Whitelisted to communicate with `http://127.0.0.1:3000` / `http://localhost:3000` (Flutter Web).
