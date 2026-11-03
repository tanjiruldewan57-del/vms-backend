package com.starkgroup.vms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing for automatic population of createdAt and updatedAt timestamps.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}

