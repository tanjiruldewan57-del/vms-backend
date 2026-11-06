# Vehicle Management System (VMS) - Backend

**Service:** `vms-backend`  
**Technology Stack:** Java 21 (LTS) & Spring Boot 3.3.5  
**Security:** Spring Security 6, Stateless JWT (RFC 7519), BCrypt  
**Data Access:** Spring Data JPA & HikariCP  
**Database Drivers:** PostgreSQL, MySQL, H2 (Local / Test Fallback)  

---

## 🏗️ Workspace Layout

```
vms-backend/
├── pom.xml                                    # Dependencies (Spring Web, Security, JJWT, JPA, DB Drivers)
├── .env.example                               # Zero-secret environment template
├── .editorconfig                              # Workspace formatting rules
├── .gitignore                                 # Git ignore (excludes .env, credentials, bytecode)
├── README.md                                  # Setup & execution instructions
├── src/
│   ├── main/
│   │   ├── java/com/starkgroup/vms/
│   │   │   ├── VmsBackendApplication.java     # Application entrypoint
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java        # Security filter chain, endpoints & CORS
│   │   │   │   ├── PasswordEncoderConfig.java # BCryptPasswordEncoder
│   │   │   │   ├── CorsProperties.java        # CORS properties binding
│   │   │   │   ├── JwtProperties.java         # JWT token properties (secret, expiration)
│   │   │   │   └── JpaAuditingConfig.java     # JPA auditing configuration
│   │   │   ├── security/
│   │   │   │   ├── JwtTokenProvider.java      # Token generation, decoding & validation
│   │   │   │   ├── JwtAuthenticationFilter.java# Bearer token validation filter
│   │   │   │   ├── JwtAuthenticationEntryPoint.java # 401 Unauthorized JSON handler
│   │   │   │   ├── VmsUserPrincipal.java      # UserDetails implementation
│   │   │   │   └── CustomUserDetailsService.java # Preconfigured admin credentials
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java        # POST /api/v1/auth/login
│   │   │   │   ├── VehicleController.java     # Protected /api/v1/vehicles CRUD endpoints
│   │   │   │   └── HealthController.java      # Database connectivity health probe
│   │   │   ├── dto/
│   │   │   │   ├── auth/                      # LoginRequest, AuthResponse, AuthUserSummary
│   │   │   │   ├── request/                   # VehicleCreateRequest, VehicleUpdateRequest
│   │   │   │   └── response/                  # VehicleResponse, HealthResponse
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java           # Authentication service
│   │   │   │   └── VehicleService.java        # Vehicle domain service
│   │   │   ├── repository/
│   │   │   │   └── VehicleRepository.java     # Spring Data JPA repository
│   │   │   └── common/
│   │   │       ├── api/                       # ApiResponse, PageResponse, ErrorResponse
│   │   │       └── exception/                 # GlobalExceptionHandler, Custom exceptions
│   │   └── resources/
│   │       ├── application.yml                # Base configuration (Env-driven DB & JWT)
│   │       ├── application-local.yml          # Local profile (H2 fallback / local PostgreSQL)
│   │       └── application-prod.yml           # Production profile (PostgreSQL / MySQL)
│   └── test/
│       └── java/com/starkgroup/vms/
│           ├── VmsBackendApplicationTests.java# Context loads test
│           ├── controller/
│           │   ├── AuthControllerTest.java    # Authentication unit test
│           │   └── VehicleControllerTest.java # Vehicle CRUD mock test
│           ├── security/
│           │   └── VehicleSecurityIntegrationTest.java # Protected endpoint & CORS integration test
│           ├── service/
│           │   └── VehicleServiceTest.java    # Domain business rules unit test
│           └── repository/
│               └── VehicleRepositoryTest.java # JPA persistence & uniqueness test
```

---

## 🔒 Authentication & Default Credentials

The backend authenticates requests statelessly via **JWT Bearer Tokens**.

### Default Admin Credentials
* **Email:** `admin@starkgroup.com`
* **Password:** `Password123!`
* **Role:** `ADMIN`
* **Name:** `Fleet Admin`

### Authentication Flow
1. **Login:** Send credentials to `POST /api/v1/auth/login`.
2. **Bearer Token:** The response returns a signed JWT.
3. **Protected Access:** Include the header `Authorization: Bearer <token>` in requests to `/api/v1/vehicles/**`.

---

## 🚀 Running the Project & Verifying Endpoints

### 1. Execute Test Suite
```bash
mvn clean test
```

### 2. Start Application Locally
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. Quick cURL Test

#### A. Login and Obtain Token
```bash
curl -X POST http://127.0.0.1:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@starkgroup.com",
    "password": "Password123!"
  }'
```

*Response:*
```json
{
  "success": true,
  "message": "Authentication successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "email": "admin@starkgroup.com",
      "name": "Fleet Admin",
      "role": "ADMIN"
    }
  },
  "timestamp": "2026-11-04T04:45:00Z"
}
```

#### B. Access Protected Endpoint Without Token (Expected: 401 Unauthorized)
```bash
curl -i -X GET http://127.0.0.1:8080/api/v1/vehicles
```

#### C. Access Protected Endpoint With Token (Expected: 200 OK)
```bash
TOKEN="<jwt_from_login_above>"

curl -X GET http://127.0.0.1:8080/api/v1/vehicles \
  -H "Authorization: Bearer $TOKEN"
```

---

## ☁️ Render Cloud Deployment

The VMS backend is prepared for direct deployment on [Render](https://render.com) as a Web Service.

### Deployment Settings
* **Runtime:** `Java` (Java 21 via `system.properties`)
* **Build Command:** `mvn clean package -DskipTests`
* **Start Command:** `java -jar target/vms-backend-0.0.1-SNAPSHOT.jar`
* **Health Check Path:** `/actuator/health` (publicly accessible without authentication)

### Environment Variables
Configure the following in the Render Dashboard (**Environment** tab):

| Variable | Recommended Value | Description |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates production configuration |
| `PORT` | *(assigned automatically by Render)* | Default is `8080` if not set |
| `SERVER_ADDRESS` | `0.0.0.0` | Binds to all interfaces for Render reverse proxy |
| `CORS_ALLOWED_ORIGINS` | `https://bespoke-mochi-006b8f.netlify.app,http://localhost:3000` | Whitelisted frontend origins |
| `DB_URL` | `jdbc:postgresql://<host>:<port>/<database>` | Managed PostgreSQL connection string |
| `DB_USERNAME` | `<database_user>` | Database user |
| `DB_PASSWORD` | `<database_password>` | Database password |
| `JPA_DDL_AUTO` | `update` | Auto-manages schema migrations on startup |
