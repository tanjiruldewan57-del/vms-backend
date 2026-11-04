package com.starkgroup.vms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starkgroup.vms.common.api.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.Instant;

/**
 * AuthenticationEntryPoint returning standardized JSON error payloads when an unauthenticated
 * request attempts to access protected endpoints.
 * Managed as a Spring Bean within {@link com.starkgroup.vms.config.SecurityConfig}.
 */
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Full authentication is required to access this resource. Please provide a valid Bearer token.")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}

