package com.starkgroup.vms.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary view of the authenticated user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserSummary {

    private String email;
    private String name;
    private String role;
}

