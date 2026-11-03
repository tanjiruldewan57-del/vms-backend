package com.starkgroup.vms.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class DatabaseConnectivityTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Should successfully connect to the configured SQL database")
    void testDatabaseConnectivity() throws SQLException {
        assertThat(dataSource).isNotNull();

        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isValid(2)).isTrue();

            DatabaseMetaData metaData = connection.getMetaData();
            assertThat(metaData).isNotNull();
            assertThat(metaData.getDatabaseProductName()).isNotBlank();
        }
    }
}

