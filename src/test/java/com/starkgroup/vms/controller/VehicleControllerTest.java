package com.starkgroup.vms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starkgroup.vms.common.exception.GlobalExceptionHandler;
import com.starkgroup.vms.common.exception.ResourceNotFoundException;
import com.starkgroup.vms.config.CorsProperties;
import com.starkgroup.vms.config.SecurityConfig;
import com.starkgroup.vms.dto.request.VehicleCreateRequest;
import com.starkgroup.vms.dto.response.VehicleResponse;
import com.starkgroup.vms.model.FuelType;
import com.starkgroup.vms.model.VehicleStatus;
import com.starkgroup.vms.model.VehicleType;
import com.starkgroup.vms.service.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @DisplayName("POST /api/v1/vehicles - Should return 201 Created and Location header on valid input")
    void testCreateVehicle_ValidInput_Returns201() throws Exception {
        UUID generatedId = UUID.randomUUID();
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vin("1HGCR2F83HA987654")
                .licensePlate("XYZ-7890")
                .make("Ford")
                .model("Transit")
                .year(2023)
                .type(VehicleType.VAN)
                .fuelType(FuelType.DIESEL)
                .mileage(5000.0)
                .build();

        VehicleResponse mockResponse = VehicleResponse.builder()
                .id(generatedId)
                .vin("1HGCR2F83HA987654")
                .licensePlate("XYZ-7890")
                .make("Ford")
                .model("Transit")
                .year(2023)
                .type(VehicleType.VAN)
                .status(VehicleStatus.AVAILABLE)
                .fuelType(FuelType.DIESEL)
                .mileage(5000.0)
                .build();

        when(vehicleService.createVehicle(any(VehicleCreateRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vin").value("1HGCR2F83HA987654"))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("POST /api/v1/vehicles - Should return 400 Bad Request when VIN is invalid")
    void testCreateVehicle_InvalidVin_Returns400() throws Exception {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vin("SHORT_VIN") // Invalid length and pattern
                .licensePlate("XYZ-7890")
                .make("Ford")
                .model("Transit")
                .year(2023)
                .type(VehicleType.VAN)
                .fuelType(FuelType.DIESEL)
                .mileage(5000.0)
                .build();

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/vehicles/{id} - Should return 404 when vehicle does not exist")
    void testGetVehicleById_NotFound_Returns404() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(vehicleService.getVehicleById(missingId))
                .thenThrow(new ResourceNotFoundException("Vehicle", "id", missingId));

        mockMvc.perform(get("/api/v1/vehicles/{id}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Vehicle not found")));
    }

    @Test
    @DisplayName("DELETE /api/v1/vehicles/{id} - Should return 204 No Content")
    void testDeleteVehicle_Returns204() throws Exception {
        UUID targetId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/vehicles/{id}", targetId))
                .andExpect(status().isNoContent());
    }
}

