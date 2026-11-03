package com.starkgroup.vms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Vehicle Management System (VMS) - Backend Entry Point.
 *
 * Designed following decoupled, clean architecture principles.
 * Houses modular domain services, stateless REST endpoints, and security filters.
 */
@SpringBootApplication
public class VmsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(VmsBackendApplication.class, args);
    }
}

