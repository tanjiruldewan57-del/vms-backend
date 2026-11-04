package com.starkgroup.vms.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starkgroup.vms.dto.auth.LoginRequest;
import com.starkgroup.vms.dto.request.VehicleCreateRequest;
import com.starkgroup.vms.dto.request.VehicleUpdateRequest;
import com.starkgroup.vms.model.Vehicle;
import com.starkgroup.vms.model.VehicleStatus;
import com.starkgroup.vms.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test suite verifying Vehicle CRUD API endpoints,
 * database persistence, validation constraints, duplicate number business rules,
 * and Spring Security JWT enforcement.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class VehicleSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VehicleRepository vehicleRepository;

    private String adminJwtToken;

    @BeforeEach
    void setUp() throws Exception {
        vehicleRepository.deleteAll();

        // Authenticate with default admin credentials to obtain a fresh JWT Bearer token
        LoginRequest loginRequest = LoginRequest.builder()
                .email("admin@starkgroup.com")
                .password("Password123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        adminJwtToken = responseNode.path("data").path("token").asText();
        assertThat(adminJwtToken).isNotBlank();
    }

    // =========================================================================
    // 1. CREATE CRUD & VALIDATION SCENARIOS
    // =========================================================================

    @Test
    @DisplayName("CRUD Create: POST /api/v1/vehicles - Should persist vehicle into database and return 201 Created")
    void testCreateVehicle_AgainstDatabase_Returns201AndPersists() throws Exception {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vehicleNumber("VMS-INT-001")
                .company("Stark Logistics")
                .type("Van")
                .brand("Ford")
                .model("Transit")
                .status(VehicleStatus.AVAILABLE)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vehicleNumber").value("VMS-INT-001"))
                .andExpect(jsonPath("$.data.company").value("Stark Logistics"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Long createdId = body.path("data").path("id").asLong();

        // Verify entity is actually committed in the database
        Optional<Vehicle> inDb = vehicleRepository.findById(createdId);
        assertThat(inDb).isPresent();
        assertThat(inDb.get().getVehicleNumber()).isEqualTo("VMS-INT-001");
        assertThat(inDb.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Validation Error: POST /api/v1/vehicles - Should return 400 Bad Request on blank mandatory fields")
    void testCreateVehicle_InvalidFields_AgainstDatabase_Returns400() throws Exception {
        VehicleCreateRequest invalidRequest = VehicleCreateRequest.builder()
                .vehicleNumber("") // Blank
                .company("")       // Blank
                .type("")          // Blank
                .brand("")         // Blank
                .model("")         // Blank
                .build();

        mockMvc.perform(post("/api/v1/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors").isArray());

        // Verify nothing was saved in the database
        assertThat(vehicleRepository.count()).isZero();
    }

    @Test
    @DisplayName("Business Rule Error: POST /api/v1/vehicles - Should return 409 Conflict when vehicleNumber already exists")
    void testCreateVehicle_DuplicateNumber_AgainstDatabase_Returns409() throws Exception {
        // Pre-save initial vehicle
        Vehicle existing = Vehicle.builder()
                .vehicleNumber("VMS-DUP-001")
                .company("Acme Fleet")
                .type("Truck")
                .brand("Volvo")
                .model("FH16")
                .status(VehicleStatus.AVAILABLE)
                .build();
        vehicleRepository.save(existing);

        // Attempt creating another vehicle with identical vehicleNumber (case-insensitive)
        VehicleCreateRequest duplicateRequest = VehicleCreateRequest.builder()
                .vehicleNumber("vms-dup-001")
                .company("Other Corp")
                .type("Van")
                .brand("Mercedes")
                .model("Sprinter")
                .status(VehicleStatus.AVAILABLE)
                .build();

        mockMvc.perform(post("/api/v1/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Vehicle with number 'VMS-DUP-001' already exists in the system."));

        // Verify database still only contains the original record
        assertThat(vehicleRepository.count()).isEqualTo(1);
    }

    // =========================================================================
    // 2. READ CRUD SCENARIOS (GET /api/v1/vehicles)
    // =========================================================================

    @Test
    @DisplayName("CRUD Read Single: GET /api/v1/vehicles/{id} - Should return 200 OK with vehicle details")
    void testGetVehicleById_AgainstDatabase_Returns200() throws Exception {
        Vehicle vehicle = vehicleRepository.save(Vehicle.builder()
                .vehicleNumber("VMS-READ-01")
                .company("Stark Fleet")
                .type("Sedan")
                .brand("Tesla")
                .model("Model 3")
                .status(VehicleStatus.AVAILABLE)
                .build());

        mockMvc.perform(get("/api/v1/vehicles/" + vehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(vehicle.getId()))
                .andExpect(jsonPath("$.data.vehicleNumber").value("VMS-READ-01"))
                .andExpect(jsonPath("$.data.model").value("Model 3"));
    }

    @Test
    @DisplayName("CRUD Read List: GET /api/v1/vehicles - Should return 200 OK with paginated list")
    void testGetVehicles_AgainstDatabase_ReturnsPaginatedList() throws Exception {
        vehicleRepository.save(Vehicle.builder()
                .vehicleNumber("VMS-LIST-01")
                .company("Alpha Co")
                .type("Truck")
                .brand("Scania")
                .model("R500")
                .status(VehicleStatus.AVAILABLE)
                .build());

        vehicleRepository.save(Vehicle.builder()
                .vehicleNumber("VMS-LIST-02")
                .company("Beta Co")
                .type("Van")
                .brand("Ford")
                .model("Transit")
                .status(VehicleStatus.IN_USE)
                .build());

        mockMvc.perform(get("/api/v1/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("CRUD Read Not Found: GET /api/v1/vehicles/{id} - Should return 404 Not Found")
    void testGetVehicleById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // =========================================================================
    // 3. UPDATE CRUD SCENARIOS (PUT /api/v1/vehicles/{id})
    // =========================================================================

    @Test
    @DisplayName("CRUD Update: PUT /api/v1/vehicles/{id} - Should update entity in database and return 200 OK")
    void testUpdateVehicle_AgainstDatabase_Returns200AndUpdates() throws Exception {
        Vehicle original = vehicleRepository.save(Vehicle.builder()
                .vehicleNumber("VMS-UPD-01")
                .company("Original Corp")
                .type("Van")
                .brand("Ford")
                .model("Transit")
                .status(VehicleStatus.AVAILABLE)
                .build());

        VehicleUpdateRequest updateRequest = VehicleUpdateRequest.builder()
                .vehicleNumber("VMS-UPD-01-MOD")
                .company("Updated Corp")
                .type("Van")
                .brand("Ford")
                .model("Transit Custom")
                .status(VehicleStatus.IN_USE)
                .build();

        mockMvc.perform(put("/api/v1/vehicles/" + original.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vehicleNumber").value("VMS-UPD-01-MOD"))
                .andExpect(jsonPath("$.data.company").value("Updated Corp"))
                .andExpect(jsonPath("$.data.status").value("IN_USE"));

        // Verify updates in database
        Vehicle inDb = vehicleRepository.findById(original.getId()).orElseThrow();
        assertThat(inDb.getVehicleNumber()).isEqualTo("VMS-UPD-01-MOD");
        assertThat(inDb.getCompany()).isEqualTo("Updated Corp");
        assertThat(inDb.getStatus()).isEqualTo(VehicleStatus.IN_USE);
    }

    @Test
    @DisplayName("Business Rule Error on Update: PUT /api/v1/vehicles/{id} - Should return 409 Conflict when new number conflicts with another vehicle")
    void testUpdateVehicle_DuplicateNumber_AgainstDatabase_Returns409() throws Exception {
        Vehicle vehicleA = vehicleRepository.save(Vehicle.builder()
                .vehicleNumber("VMS-AAA")
                .company("Alpha")
                .type("Van")
                .brand("Ford")
                .model("Transit")
                .status(VehicleStatus.AVAILABLE)
                .build());

        Vehicle vehicleB = vehicleRepository.save(Vehicle.builder()
                .vehicleNumber("VMS-BBB")
                .company("Beta")
                .type("Truck")
                .brand("Volvo")
                .model("FH16")
                .status(VehicleStatus.AVAILABLE)
                .build());

        // Attempt updating vehicle B's number to "VMS-AAA", which is owned by vehicle A
        VehicleUpdateRequest conflictRequest = VehicleUpdateRequest.builder()
                .vehicleNumber("VMS-AAA")
                .company("Beta Updated")
                .type("Truck")
                .brand("Volvo")
                .model("FH16")
                .status(VehicleStatus.AVAILABLE)
                .build();

        mockMvc.perform(put("/api/v1/vehicles/" + vehicleB.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Vehicle with number 'VMS-AAA' already exists in the system."));

        // Verify vehicle B still has "VMS-BBB"
        Vehicle refreshedB = vehicleRepository.findById(vehicleB.getId()).orElseThrow();
        assertThat(refreshedB.getVehicleNumber()).isEqualTo("VMS-BBB");
    }

    // =========================================================================
    // 4. DELETE CRUD SCENARIOS (DELETE /api/v1/vehicles/{id})
    // =========================================================================

    @Test
    @DisplayName("CRUD Delete: DELETE /api/v1/vehicles/{id} - Should remove vehicle from database and return 204 No Content")
    void testDeleteVehicle_AgainstDatabase_Returns204AndRemoves() throws Exception {
        Vehicle vehicle = vehicleRepository.save(Vehicle.builder()
                .vehicleNumber("VMS-DEL-01")
                .company("Delete Me Corp")
                .type("Sedan")
                .brand("Toyota")
                .model("Camry")
                .status(VehicleStatus.AVAILABLE)
                .build());

        mockMvc.perform(delete("/api/v1/vehicles/" + vehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isNoContent());

        // Verify entity is permanently removed from DB
        assertThat(vehicleRepository.findById(vehicle.getId())).isEmpty();

        // Subsequent GET should return 404
        mockMvc.perform(get("/api/v1/vehicles/" + vehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // 5. SECURITY & CORS SCENARIOS
    // =========================================================================

    @Test
    @DisplayName("Security: GET /api/v1/vehicles - Should reject unauthenticated requests with 401 Unauthorized")
    void testGetVehicles_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource. Please provide a valid Bearer token."));
    }

    @Test
    @DisplayName("CORS Preflight: OPTIONS /api/v1/vehicles from Flutter Web should allow Authorization header")
    void testCorsPreflight_AllowsAuthorizationHeader() throws Exception {
        mockMvc.perform(options("/api/v1/vehicles")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
    }
}
