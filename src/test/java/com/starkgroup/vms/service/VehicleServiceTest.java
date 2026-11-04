package com.starkgroup.vms.service;

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
 * Unit tests for {@link VehicleServiceImpl}.
 * Uses JUnit 5 and Mockito to test business logic in isolation without a real database or Spring context boot.
 */
@ExtendWith(MockitoExtension.class)
public class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    @Test
    @DisplayName("When vehicle number already exists, throws DuplicateVehicleNumberException and repository.save() is never called")
    public void whenVehicleNumberAlreadyExists_throwsException() {
        // Arrange
        String existingNumber = "VMS-101";
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
                .isInstanceOf(DuplicateVehicleNumberException.class)
                .hasMessage("Vehicle with number 'VMS-101' already exists in the system.");

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("When vehicle number is new, vehicle is successfully saved and returned")
    public void whenVehicleNumberIsNew_savesSuccessfully() {
        // Arrange
        String newNumber = "VMS-202";
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vehicleNumber(newNumber)
                .company("Stark Logistics")
                .type("Truck")
                .brand("Volvo")
                .model("FH16")
                .status(VehicleStatus.AVAILABLE)
                .build();

        Vehicle savedVehicle = Vehicle.builder()
                .id(1L)
                .vehicleNumber(newNumber)
                .company("Stark Logistics")
                .type("Truck")
                .brand("Volvo")
                .model("FH16")
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleRepository.existsByVehicleNumberIgnoreCase(newNumber)).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(savedVehicle);

        // Act
        VehicleResponse response = vehicleService.createVehicle(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getVehicleNumber()).isEqualTo(newNumber);
        assertThat(response.getCompany()).isEqualTo("Stark Logistics");
        assertThat(response.getType()).isEqualTo("Truck");
        assertThat(response.getBrand()).isEqualTo("Volvo");
        assertThat(response.getModel()).isEqualTo("FH16");
        assertThat(response.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);

        verify(vehicleRepository).save(any(Vehicle.class));
    }
}
