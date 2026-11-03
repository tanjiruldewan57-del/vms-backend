package com.starkgroup.vms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class VmsBackendApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring application context boots successfully with clean configurations.
    }
}

