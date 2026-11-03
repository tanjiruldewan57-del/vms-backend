package com.starkgroup.vms.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service loading users for authentication.
 * Pre-configures the VMS fleet administration credentials.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final Map<String, VmsUserPrincipal> userStore = new ConcurrentHashMap<>();

    public CustomUserDetailsService(PasswordEncoder passwordEncoder) {
        // Pre-configure the administrator credentials requested by VMS specification:
        // Email: admin@starkgroup.com
        // Password: Password123!
        // Name: Fleet Admin
        // Role: ADMIN
        userStore.put("admin@starkgroup.com", new VmsUserPrincipal(
                "admin@starkgroup.com",
                passwordEncoder.encode("Password123!"),
                "Fleet Admin",
                "ADMIN"
        ));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Email cannot be blank");
        }

        String normalizedEmail = username.trim().toLowerCase(Locale.ROOT);
        VmsUserPrincipal user = userStore.get(normalizedEmail);

        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + username);
        }

        return user;
    }
}

