package com.starkgroup.vms.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starkgroup.vms.dto.auth.LoginRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class VehicleSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/vehicles - Should reject unauthenticated requests with 401 Unauthorized")
    void testGetVehicles_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource. Please provide a valid Bearer token."));
    }

    @Test
    @DisplayName("POST /api/v1/vehicles - Should reject unauthenticated creation requests with 401 Unauthorized")
    void testCreateVehicle_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("End-to-End Auth: Login with configured admin credentials, obtain JWT, and access /api/v1/vehicles")
    void testLogin_AndAccessProtectedEndpoint_WithBearerToken() throws Exception {
        // 1. Login with configured credentials: admin@starkgroup.com / Password123!
        LoginRequest loginRequest = LoginRequest.builder()
                .email("admin@starkgroup.com")
                .password("Password123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.user.email").value("admin@starkgroup.com"))
                .andExpect(jsonPath("$.data.user.name").value("Fleet Admin"))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"))
                .andReturn();

        // 2. Extract JWT token from response
        String responseBody = loginResult.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseBody);
        String token = root.path("data").path("token").asText();
        assertThat(token).isNotBlank();

        // 3. Access protected vehicle endpoint with Bearer token
        mockMvc.perform(get("/api/v1/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("CORS Preflight: OPTIONS /api/v1/vehicles from Flutter Web (http://localhost:3000) should allow Authorization header")
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

