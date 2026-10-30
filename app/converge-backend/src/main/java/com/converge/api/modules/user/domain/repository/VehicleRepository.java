package com.converge.api.modules.user.domain.repository;

import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;

@Repository
public interface VehicleRepository extends MongoRepository<Vehicle, ObjectId> {

    List<Vehicle> findByUserIdAndActiveTrue(String userId);                         // meus veículos ativos

    boolean existsByUserIdAndLicensePlate(String userId, String licensePlate);      // essa placa já está cadastrada para este dono?

    Optional<Vehicle> findByUserIdAndLicensePlate(String userId, String licensePlate);
}

