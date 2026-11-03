package com.starkgroup.vms.service.impl;

import com.starkgroup.vms.dto.auth.AuthResponse;
import com.starkgroup.vms.dto.auth.AuthUserSummary;
import com.starkgroup.vms.dto.auth.LoginRequest;
import com.starkgroup.vms.security.JwtTokenProvider;
import com.starkgroup.vms.security.VmsUserPrincipal;
import com.starkgroup.vms.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Service implementation managing user authentication and JWT token issuance.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Override
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        VmsUserPrincipal principal = (VmsUserPrincipal) authentication.getPrincipal();
        String token = tokenProvider.generateToken(principal.getUsername(), principal.getName(), principal.getRole());

        log.info("User '{}' authenticated successfully with role '{}'", principal.getUsername(), principal.getRole());

        AuthUserSummary userSummary = AuthUserSummary.builder()
                .email(principal.getUsername())
                .name(principal.getName())
                .role(principal.getRole())
                .build();

        return AuthResponse.builder()
                .token(token)
                .user(userSummary)
                .build();
    }
}

