package com.starkgroup.vms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starkgroup.vms.common.api.PageResponse;
import com.starkgroup.vms.common.exception.DuplicateVehicleNumberException;
import com.starkgroup.vms.common.exception.GlobalExceptionHandler;
import com.starkgroup.vms.common.exception.ResourceNotFoundException;
import com.starkgroup.vms.config.CorsProperties;
import com.starkgroup.vms.config.SecurityConfig;
import com.starkgroup.vms.dto.request.VehicleCreateRequest;
import com.starkgroup.vms.dto.request.VehicleUpdateRequest;
import com.starkgroup.vms.dto.response.VehicleResponse;
import com.starkgroup.vms.model.VehicleStatus;
import com.starkgroup.vms.service.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehicleController.class)
@Import({SecurityConfig.class, CorsProperties.class, GlobalExceptionHandler.class})
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VehicleService vehicleService;

    // =========================================================================
    // 1. CREATE ENDPOINTS (POST /api/v1/vehicles)
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/vehicles - Should return 201 Created and Location header on valid input")
    void testCreateVehicle_ValidInput_Returns201() throws Exception {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vehicleNumber("VMS-101")
                .company("Stark Logistics")
                .type("Van")
                .brand("Ford")
                .model("Transit")
                .status(VehicleStatus.AVAILABLE)
                .build();

        VehicleResponse mockResponse = VehicleResponse.builder()
                .id(1L)
                .vehicleNumber("VMS-101")
                .company("Stark Logistics")
                .type("Van")
                .brand("Ford")
                .model("Transit")
                .status(VehicleStatus.AVAILABLE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(vehicleService.createVehicle(any(VehicleCreateRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/vehicles/1")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.vehicleNumber").value("VMS-101"))
                .andExpect(jsonPath("$.data.company").value("Stark Logistics"));
    }

    @Test
    @DisplayName("POST /api/v1/vehicles - Should return 409 Conflict when vehicleNumber is duplicate")
    void testCreateVehicle_DuplicateVehicleNumber_Returns409() throws Exception {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vehicleNumber("VMS-101")
                .company("Stark Logistics")
                .type("Van")
                .brand("Ford")
                .model("Transit")
                .build();

        when(vehicleService.createVehicle(any(VehicleCreateRequest.class)))
                .thenThrow(new DuplicateVehicleNumberException("VMS-101"));

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Vehicle with number 'VMS-101' already exists in the system."));
    }

    @Test
    @DisplayName("POST /api/v1/vehicles - Should return 400 Bad Request on blank mandatory fields")
    void testCreateVehicle_MissingFields_Returns400() throws Exception {
        VehicleCreateRequest invalidRequest = VehicleCreateRequest.builder()
                .vehicleNumber("") // Blank
                .company("")       // Blank
                .type("")          // Blank
                .brand("")         // Blank
                .model("")         // Blank
                .build();

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    // =========================================================================
    // 2. READ / LIST ENDPOINTS (GET /api/v1/vehicles)
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/vehicles/{id} - Should return 200 OK when vehicle exists")
    void testGetVehicleById_Success_Returns200() throws Exception {
        VehicleResponse response = VehicleResponse.builder()
                .id(1L)
                .vehicleNumber("VMS-101")
                .company("Stark Logistics")
                .type("Van")
                .brand("Ford")
                .model("Transit")
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleService.getVehicleById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/vehicles/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.vehicleNumber").value("VMS-101"));
    }

    @Test
    @DisplayName("GET /api/v1/vehicles/{id} - Should return 404 Not Found when ID does not exist")
    void testGetVehicleById_NotFound_Returns404() throws Exception {
        when(vehicleService.getVehicleById(999L))
                .thenThrow(new ResourceNotFoundException("Vehicle", "id", 999L));

        mockMvc.perform(get("/api/v1/vehicles/999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Vehicle not found with id: '999'"));
    }

    @Test
    @DisplayName("GET /api/v1/vehicles/number/{vehicleNumber} - Should return 200 OK when number exists")
    void testGetVehicleByNumber_Success_Returns200() throws Exception {
        VehicleResponse response = VehicleResponse.builder()
                .id(1L)
                .vehicleNumber("VMS-101")
                .company("Stark Logistics")
                .type("Van")
                .brand("Ford")
                .model("Transit")
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleService.getVehicleByNumber("VMS-101")).thenReturn(response);

        mockMvc.perform(get("/api/v1/vehicles/number/VMS-101")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vehicleNumber").value("VMS-101"));
    }

    @Test
    @DisplayName("GET /api/v1/vehicles - Should return 200 OK and paginated vehicle list")
    void testGetVehicles_ReturnsPaginatedList() throws Exception {
        VehicleResponse vehicle = VehicleResponse.builder()
                .id(1L)
                .vehicleNumber("VMS-101")
                .company("Stark Logistics")
                .type("Van")
                .brand("Ford")
                .model("Transit")
                .status(VehicleStatus.AVAILABLE)
                .build();

        PageResponse<VehicleResponse> pageResponse = PageResponse.from(
                new PageImpl<>(List.of(vehicle), PageRequest.of(0, 20), 1)
        );

        when(vehicleService.getVehicles(any(), any(), any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/vehicles")
                        .param("company", "Stark Logistics")
                        .param("status", "AVAILABLE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].vehicleNumber").value("VMS-101"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    // =========================================================================
    // 3. UPDATE ENDPOINTS (PUT /api/v1/vehicles/{id})
    // =========================================================================

    @Test
    @DisplayName("PUT /api/v1/vehicles/{id} - Should return 200 OK on valid update")
    void testUpdateVehicle_Success_Returns200() throws Exception {
        VehicleUpdateRequest request = VehicleUpdateRequest.builder()
                .vehicleNumber("VMS-101-UPDATED")
                .company("Stark Enterprise")
                .type("Van")
                .brand("Ford")
                .model("Transit Custom")
                .status(VehicleStatus.IN_USE)
                .build();

        VehicleResponse response = VehicleResponse.builder()
                .id(1L)
                .vehicleNumber("VMS-101-UPDATED")
                .company("Stark Enterprise")
                .type("Van")
                .brand("Ford")
                .model("Transit Custom")
                .status(VehicleStatus.IN_USE)
                .build();

        when(vehicleService.updateVehicle(eq(1L), any(VehicleUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/vehicles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vehicleNumber").value("VMS-101-UPDATED"))
                .andExpect(jsonPath("$.data.company").value("Stark Enterprise"))
                .andExpect(jsonPath("$.data.status").value("IN_USE"));
    }

    @Test
    @DisplayName("PUT /api/v1/vehicles/{id} - Should return 409 Conflict when update conflicts with another vehicle number")
    void testUpdateVehicle_DuplicateVehicleNumber_Returns409() throws Exception {
        VehicleUpdateRequest request = VehicleUpdateRequest.builder()
                .vehicleNumber("VMS-999") // Already taken by another vehicle
                .company("Stark Enterprise")
                .type("Van")
                .brand("Ford")
                .model("Transit Custom")
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleService.updateVehicle(eq(1L), any(VehicleUpdateRequest.class)))
                .thenThrow(new DuplicateVehicleNumberException("VMS-999"));

        mockMvc.perform(put("/api/v1/vehicles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Vehicle with number 'VMS-999' already exists in the system."));
    }

    @Test
    @DisplayName("PUT /api/v1/vehicles/{id} - Should return 404 Not Found when updating non-existent vehicle")
    void testUpdateVehicle_NotFound_Returns404() throws Exception {
        VehicleUpdateRequest request = VehicleUpdateRequest.builder()
                .vehicleNumber("VMS-101")
                .company("Stark Enterprise")
                .type("Van")
                .brand("Ford")
                .model("Transit")
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleService.updateVehicle(eq(999L), any(VehicleUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Vehicle", "id", 999L));

        mockMvc.perform(put("/api/v1/vehicles/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // =========================================================================
    // 4. DELETE ENDPOINTS (DELETE /api/v1/vehicles/{id})
    // =========================================================================

    @Test
    @DisplayName("DELETE /api/v1/vehicles/{id} - Should return 204 No Content on successful deletion")
    void testDeleteVehicle_Success_Returns204() throws Exception {
        doNothing().when(vehicleService).deleteVehicle(1L);

        mockMvc.perform(delete("/api/v1/vehicles/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/vehicles/{id} - Should return 404 Not Found when deleting non-existent vehicle")
    void testDeleteVehicle_NotFound_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Vehicle", "id", 999L))
                .when(vehicleService).deleteVehicle(999L);

        mockMvc.perform(delete("/api/v1/vehicles/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
