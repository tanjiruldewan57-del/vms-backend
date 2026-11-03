package com.starkgroup.vms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starkgroup.vms.common.exception.DuplicateVehicleNumberException;
import com.starkgroup.vms.common.exception.GlobalExceptionHandler;
import com.starkgroup.vms.config.CorsProperties;
import com.starkgroup.vms.config.SecurityConfig;
import com.starkgroup.vms.dto.request.VehicleCreateRequest;
import com.starkgroup.vms.dto.response.VehicleResponse;
import com.starkgroup.vms.model.VehicleStatus;
import com.starkgroup.vms.service.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
    @DisplayName("POST /api/v1/vehicles - Should return 201 Created when vehicle is valid and unique")
    void testCreateVehicle_Success_Returns201() throws Exception {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vehicleNumber("VMS-200")
                .company("Stark Group")
                .type("SUV")
                .brand("Audi")
                .model("Q7")
                .status(VehicleStatus.AVAILABLE)
                .build();

        VehicleResponse mockResponse = VehicleResponse.builder()
                .id(10L)
                .vehicleNumber("VMS-200")
                .company("Stark Group")
                .type("SUV")
                .brand("Audi")
                .model("Q7")
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleService.createVehicle(any(VehicleCreateRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.vehicleNumber").value("VMS-200"));
    }

    @Test
    @DisplayName("POST /api/v1/vehicles - Should return 409 Conflict when vehicleNumber already exists")
    void testCreateVehicle_DuplicateVehicleNumber_Returns409Conflict() throws Exception {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vehicleNumber("VMS-200")
                .company("Stark Group")
                .type("SUV")
                .brand("Audi")
                .model("Q7")
                .build();

        when(vehicleService.createVehicle(any(VehicleCreateRequest.class)))
                .thenThrow(new DuplicateVehicleNumberException("VMS-200"));

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Vehicle with number 'VMS-200' already exists in the system."));
    }

    @Test
    @DisplayName("POST /api/v1/vehicles - Should return 400 Bad Request when vehicleNumber is blank")
    void testCreateVehicle_MissingVehicleNumber_Returns400() throws Exception {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vehicleNumber("") // Blank vehicle number
                .company("Stark Group")
                .type("SUV")
                .brand("Audi")
                .model("Q7")
                .build();

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }
}
