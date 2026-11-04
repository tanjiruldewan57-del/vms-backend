package com.starkgroup.vms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integrated Spring Boot test suite for VehicleController.
 * Verifies the full vertical slice (Controller -> Service -> Repository -> SQL
 * Database)
 * working end-to-end against an in-memory H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("local")
class VehicleControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private VehicleRepository vehicleRepository;

        @BeforeEach
        void setUp() {
                vehicleRepository.deleteAll();
        }

        // =========================================================================
        // 1. CREATE SCENARIOS (POST /api/v1/vehicles)
        // =========================================================================

        @Test
        @DisplayName("Create: POST /api/v1/vehicles - Should persist vehicle into database and return 201 Created")
        void testCreateVehicle_ValidInput_PersistsInDbAndReturns201() throws Exception {
                VehicleCreateRequest request = VehicleCreateRequest.builder()
                                .vehicleNumber("VMS-101")
                                .company("Stark Logistics")
                                .type("Van")
                                .brand("Ford")
                                .model("Transit")
                                .status(VehicleStatus.AVAILABLE)
                                .build();

                mockMvc.perform(post("/api/v1/vehicles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(header().exists("Location"))
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.vehicleNumber").value("VMS-101"))
                                .andExpect(jsonPath("$.data.company").value("Stark Logistics"))
                                .andExpect(jsonPath("$.data.type").value("Van"))
                                .andExpect(jsonPath("$.data.brand").value("Ford"))
                                .andExpect(jsonPath("$.data.model").value("Transit"))
                                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));

                // Verify entity is persisted in the database
                assertThat(vehicleRepository.count()).isEqualTo(1);
                Vehicle persisted = vehicleRepository.findByVehicleNumberIgnoreCase("VMS-101").orElseThrow();
                assertThat(persisted.getId()).isNotNull();
                assertThat(persisted.getCompany()).isEqualTo("Stark Logistics");
                assertThat(persisted.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Duplicate Rule: POST /api/v1/vehicles - Should return 409 Conflict when vehicleNumber already exists")
        void testCreateVehicle_DuplicateVehicleNumber_Returns409() throws Exception {
                // Pre-persist an existing vehicle
                vehicleRepository.save(Vehicle.builder()
                                .vehicleNumber("VMS-101")
                                .company("Initial Fleet")
                                .type("Truck")
                                .brand("Volvo")
                                .model("FH16")
                                .status(VehicleStatus.AVAILABLE)
                                .build());

                // Attempt creating duplicate (case-insensitive)
                VehicleCreateRequest duplicateRequest = VehicleCreateRequest.builder()
                                .vehicleNumber("vms-101")
                                .company("Conflicting Company")
                                .type("Van")
                                .brand("Ford")
                                .model("Transit")
                                .status(VehicleStatus.AVAILABLE)
                                .build();

                mockMvc.perform(post("/api/v1/vehicles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(duplicateRequest)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.status").value(409))
                                .andExpect(jsonPath("$.error").value("Conflict"))
                                .andExpect(jsonPath("$.message")
                                                .value("Vehicle with number 'VMS-101' already exists in the system."));

                // Database should still contain only the original record
                assertThat(vehicleRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Validation: POST /api/v1/vehicles - Should return 400 Bad Request on blank mandatory fields")
        void testCreateVehicle_MissingRequiredFields_Returns400() throws Exception {
                VehicleCreateRequest invalidRequest = VehicleCreateRequest.builder()
                                .vehicleNumber("")
                                .company("")
                                .type("")
                                .brand("")
                                .model("")
                                .build();

                mockMvc.perform(post("/api/v1/vehicles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.error").value("Bad Request"))
                                .andExpect(jsonPath("$.fieldErrors").isArray());

                // Database must remain clean
                assertThat(vehicleRepository.count()).isZero();
        }

        // =========================================================================
        // 2. READ / LIST SCENARIOS (GET /api/v1/vehicles)
        // =========================================================================

        @Test
        @DisplayName("List: GET /api/v1/vehicles - Should return 200 OK with correct pagination from database")
        void testGetVehicles_ReturnsPaginatedList() throws Exception {
                vehicleRepository.save(Vehicle.builder()
                                .vehicleNumber("VMS-PAGE-01")
                                .company("Alpha Fleet")
                                .type("Van")
                                .brand("Ford")
                                .model("Transit")
                                .status(VehicleStatus.AVAILABLE)
                                .build());

                vehicleRepository.save(Vehicle.builder()
                                .vehicleNumber("VMS-PAGE-02")
                                .company("Beta Fleet")
                                .type("Truck")
                                .brand("Volvo")
                                .model("FH16")
                                .status(VehicleStatus.IN_USE)
                                .build());

                mockMvc.perform(get("/api/v1/vehicles")
                                .param("page", "0")
                                .param("size", "10")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.totalElements").value(2))
                                .andExpect(jsonPath("$.data.totalPages").value(1))
                                .andExpect(jsonPath("$.data.pageNumber").value(0))
                                .andExpect(jsonPath("$.data.pageSize").value(10))
                                .andExpect(jsonPath("$.data.content").isArray())
                                .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("Read Single: GET /api/v1/vehicles/{id} - Should return 200 OK with persisted vehicle")
        void testGetVehicleById_Success_Returns200() throws Exception {
                Vehicle saved = vehicleRepository.save(Vehicle.builder()
                                .vehicleNumber("VMS-201")
                                .company("Stark Logistics")
                                .type("Van")
                                .brand("Ford")
                                .model("Transit")
                                .status(VehicleStatus.AVAILABLE)
                                .build());

                mockMvc.perform(get("/api/v1/vehicles/" + saved.getId())
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                                .andExpect(jsonPath("$.data.vehicleNumber").value("VMS-201"))
                                .andExpect(jsonPath("$.data.company").value("Stark Logistics"));
        }

        @Test
        @DisplayName("Read Not Found: GET /api/v1/vehicles/{id} - Should return 404 Not Found when ID does not exist")
        void testGetVehicleById_NotFound_Returns404() throws Exception {
                mockMvc.perform(get("/api/v1/vehicles/999999")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.error").value("Not Found"))
                                .andExpect(jsonPath("$.message").value("Vehicle not found with id: '999999'"));
        }

        @Test
        @DisplayName("Read by Number: GET /api/v1/vehicles/number/{vehicleNumber} - Should return 200 OK")
        void testGetVehicleByNumber_Success_Returns200() throws Exception {
                vehicleRepository.save(Vehicle.builder()
                                .vehicleNumber("VMS-NUM-01")
                                .company("Stark Logistics")
                                .type("Truck")
                                .brand("Scania")
                                .model("R500")
                                .status(VehicleStatus.AVAILABLE)
                                .build());

                mockMvc.perform(get("/api/v1/vehicles/number/VMS-NUM-01")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.vehicleNumber").value("VMS-NUM-01"))
                                .andExpect(jsonPath("$.data.brand").value("Scania"));
        }

        // =========================================================================
        // 3. UPDATE SCENARIOS (PUT /api/v1/vehicles/{id})
        // =========================================================================

        @Test
        @DisplayName("Update: PUT /api/v1/vehicles/{id} - Should return 200 OK and update database")
        void testUpdateVehicle_Success_UpdatesDbAndReturns200() throws Exception {
                Vehicle original = vehicleRepository.save(Vehicle.builder()
                                .vehicleNumber("VMS-ORIG")
                                .company("Original Corp")
                                .type("Van")
                                .brand("Ford")
                                .model("Transit")
                                .status(VehicleStatus.AVAILABLE)
                                .build());

                VehicleUpdateRequest updateRequest = VehicleUpdateRequest.builder()
                                .vehicleNumber("VMS-UPDATED")
                                .company("Updated Corp")
                                .type("Van")
                                .brand("Ford")
                                .model("Transit Custom")
                                .status(VehicleStatus.IN_USE)
                                .build();

                mockMvc.perform(put("/api/v1/vehicles/" + original.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.vehicleNumber").value("VMS-UPDATED"))
                                .andExpect(jsonPath("$.data.company").value("Updated Corp"))
                                .andExpect(jsonPath("$.data.status").value("IN_USE"));

                // Verify update in DB
                Vehicle inDb = vehicleRepository.findById(original.getId()).orElseThrow();
                assertThat(inDb.getVehicleNumber()).isEqualTo("VMS-UPDATED");
                assertThat(inDb.getCompany()).isEqualTo("Updated Corp");
                assertThat(inDb.getStatus()).isEqualTo(VehicleStatus.IN_USE);
        }

        @Test
        @DisplayName("Update Duplicate Rule: PUT /api/v1/vehicles/{id} - Should return 409 Conflict when new number is taken")
        void testUpdateVehicle_DuplicateVehicleNumber_Returns409() throws Exception {
                Vehicle vehicle1 = vehicleRepository.save(Vehicle.builder()
                                .vehicleNumber("VMS-FIRST")
                                .company("Fleet 1")
                                .type("Van")
                                .brand("Ford")
                                .model("Transit")
                                .status(VehicleStatus.AVAILABLE)
                                .build());

                Vehicle vehicle2 = vehicleRepository.save(Vehicle.builder()
                                .vehicleNumber("VMS-SECOND")
                                .company("Fleet 2")
                                .type("Truck")
                                .brand("Volvo")
                                .model("FH16")
                                .status(VehicleStatus.AVAILABLE)
                                .build());

                // Attempt updating vehicle2 to have vehicle1's number
                VehicleUpdateRequest conflictRequest = VehicleUpdateRequest.builder()
                                .vehicleNumber("VMS-FIRST")
                                .company("Fleet 2 Updated")
                                .type("Truck")
                                .brand("Volvo")
                                .model("FH16")
                                .status(VehicleStatus.AVAILABLE)
                                .build();

                mockMvc.perform(put("/api/v1/vehicles/" + vehicle2.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(conflictRequest)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.status").value(409))
                                .andExpect(jsonPath("$.error").value("Conflict"))
                                .andExpect(jsonPath("$.message").value(
                                                "Vehicle with number 'VMS-FIRST' already exists in the system."));

                // Verify vehicle2 still has its original number in DB
                Vehicle refreshed2 = vehicleRepository.findById(vehicle2.getId()).orElseThrow();
                assertThat(refreshed2.getVehicleNumber()).isEqualTo("VMS-SECOND");
        }

        @Test
        @DisplayName("Update Not Found: PUT /api/v1/vehicles/{id} - Should return 404 Not Found when ID does not exist")
        void testUpdateVehicle_NotFound_Returns404() throws Exception {
                VehicleUpdateRequest request = VehicleUpdateRequest.builder()
                                .vehicleNumber("VMS-NON-EXISTENT")
                                .company("Ghost Corp")
                                .type("Van")
                                .brand("Ford")
                                .model("Transit")
                                .status(VehicleStatus.AVAILABLE)
                                .build();

                mockMvc.perform(put("/api/v1/vehicles/999999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.error").value("Not Found"));
        }

        // =========================================================================
        // 4. DELETE SCENARIOS (DELETE /api/v1/vehicles/{id})
        // =========================================================================

        @Test
        @DisplayName("Delete: DELETE /api/v1/vehicles/{id} - Should remove vehicle from database and return 204 No Content")
        void testDeleteVehicle_Success_RemovesFromDbAndReturns204() throws Exception {
                Vehicle vehicle = vehicleRepository.save(Vehicle.builder()
                                .vehicleNumber("VMS-DEL-01")
                                .company("Removal Corp")
                                .type("Sedan")
                                .brand("Toyota")
                                .model("Camry")
                                .status(VehicleStatus.AVAILABLE)
                                .build());

                mockMvc.perform(delete("/api/v1/vehicles/" + vehicle.getId()))
                                .andExpect(status().isNoContent());

                // Verify entity is permanently removed from DB
                assertThat(vehicleRepository.findById(vehicle.getId())).isEmpty();
                assertThat(vehicleRepository.count()).isZero();
        }

        @Test
        @DisplayName("Delete Not Found: DELETE /api/v1/vehicles/{id} - Should return 404 Not Found when ID does not exist")
        void testDeleteVehicle_NotFound_Returns404() throws Exception {
                mockMvc.perform(delete("/api/v1/vehicles/999999"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.error").value("Not Found"));
        }
}
