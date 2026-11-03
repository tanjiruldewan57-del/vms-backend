package com.starkgroup.vms.service;

import com.starkgroup.vms.dto.auth.AuthResponse;
import com.starkgroup.vms.dto.auth.LoginRequest;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {

    AuthResponse login(LoginRequest request);
}

