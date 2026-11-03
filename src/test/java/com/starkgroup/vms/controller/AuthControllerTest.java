package com.starkgroup.vms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starkgroup.vms.common.exception.GlobalExceptionHandler;
import com.starkgroup.vms.dto.auth.AuthResponse;
import com.starkgroup.vms.dto.auth.AuthUserSummary;
import com.starkgroup.vms.dto.auth.LoginRequest;
import com.starkgroup.vms.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/v1/auth/login - Should return 200 OK with token and user profile on valid credentials")
    void testLogin_ValidCredentials_Returns200WithToken() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("admin@starkgroup.com")
                .password("Password123!")
                .build();

        AuthResponse mockResponse = AuthResponse.builder()
                .token("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummyPayload")
                .user(AuthUserSummary.builder()
                        .email("admin@starkgroup.com")
                        .name("Fleet Admin")
                        .role("ADMIN")
                        .build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Authentication successful"))
                .andExpect(jsonPath("$.data.token").value("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummyPayload"))
                .andExpect(jsonPath("$.data.user.email").value("admin@starkgroup.com"))
                .andExpect(jsonPath("$.data.user.name").value("Fleet Admin"))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should return 401 Unauthorized on invalid credentials")
    void testLogin_InvalidCredentials_Returns401() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("admin@starkgroup.com")
                .password("WrongPassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should return 400 Bad Request on invalid email format or blank password")
    void testLogin_BlankFields_Returns400() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("not-an-email")
                .password("")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }
}

