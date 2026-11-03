package com.starkgroup.vms.repository;

import com.starkgroup.vms.model.Vehicle;
import com.starkgroup.vms.model.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access repository for Vehicle entities.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID>, JpaSpecificationExecutor<Vehicle> {

    Optional<Vehicle> findByIdAndDeletedFalse(UUID id);

    Optional<Vehicle> findByVinIgnoreCaseAndDeletedFalse(String vin);

    Optional<Vehicle> findByLicensePlateIgnoreCaseAndDeletedFalse(String licensePlate);

    boolean existsByVinIgnoreCaseAndDeletedFalse(String vin);

    boolean existsByVinIgnoreCaseAndIdNotAndDeletedFalse(String vin, UUID id);

    boolean existsByLicensePlateIgnoreCaseAndDeletedFalse(String licensePlate);

    boolean existsByLicensePlateIgnoreCaseAndIdNotAndDeletedFalse(String licensePlate, UUID id);

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(VehicleStatus status);

    @Query("SELECT COALESCE(AVG(v.mileage), 0.0) FROM Vehicle v WHERE v.deleted = false")
    Double getAverageMileage();
}

