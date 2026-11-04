package com.starkgroup.vms.service;

import com.starkgroup.vms.common.exception.DuplicateResourceException;
import com.starkgroup.vms.common.exception.DuplicateVehicleNumberException;
import com.starkgroup.vms.dto.request.VehicleCreateRequest;
import com.starkgroup.vms.dto.response.VehicleResponse;
import com.starkgroup.vms.model.Vehicle;
import com.starkgroup.vms.model.VehicleStatus;
import com.starkgroup.vms.repository.VehicleRepository;
import com.starkgroup.vms.service.impl.VehicleServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused unit test suite for the Vehicle service's duplicate Vehicle Number invariant.
 *
 * Runs purely in-memory using JUnit 5 and Mockito (@ExtendWith(MockitoExtension.class)).
 * Does NOT require a database, network, or Spring application context boot.
 */
@ExtendWith(MockitoExtension.class)
class VehicleDuplicateNumberRuleTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    @Test
    @DisplayName("Case 1: When vehicle number already exists, throw DuplicateResourceException and never call save()")
    void shouldThrowDuplicateResourceExceptionAndNeverSave_WhenVehicleNumberAlreadyExists() {
        // Arrange
        String existingNumber = "VMS-777";
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vehicleNumber(existingNumber)
                .company("Stark Logistics")
                .type("Van")
                .brand("Ford")
                .model("Transit")
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleRepository.existsByVehicleNumberIgnoreCase(existingNumber)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> vehicleService.createVehicle(request))
                .isInstanceOf(DuplicateResourceException.class)
                .isInstanceOf(DuplicateVehicleNumberException.class)
                .hasMessage("Vehicle with number 'VMS-777' already exists in the system.");

        // Critical verification: ensure repository.save() is NEVER invoked
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("Case 2: When vehicle number is unique/new, successfully save and return the vehicle")
    void shouldSaveAndReturnVehicle_WhenVehicleNumberIsUnique() {
        // Arrange
        String uniqueNumber = "VMS-888";
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vehicleNumber(uniqueNumber)
                .company("Stark Logistics")
                .type("Truck")
                .brand("Volvo")
                .model("FH16")
                .status(VehicleStatus.AVAILABLE)
                .build();

        Vehicle savedEntity = Vehicle.builder()
                .id(100L)
                .vehicleNumber(uniqueNumber)
                .company("Stark Logistics")
                .type("Truck")
                .brand("Volvo")
                .model("FH16")
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleRepository.existsByVehicleNumberIgnoreCase(uniqueNumber)).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(savedEntity);

        // Act
        VehicleResponse response = vehicleService.createVehicle(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getVehicleNumber()).isEqualTo(uniqueNumber);
        assertThat(response.getCompany()).isEqualTo("Stark Logistics");
        assertThat(response.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);

        // Critical verification: ensure repository.save() was called exactly once
        verify(vehicleRepository).save(any(Vehicle.class));
    }
}

