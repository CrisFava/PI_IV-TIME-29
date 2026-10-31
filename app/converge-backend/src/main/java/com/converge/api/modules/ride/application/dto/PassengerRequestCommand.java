package com.converge.api.modules.ride.application.dto;

public record PassengerRequestCommand(String rideId, String passengerId, String firebaseUid) {
}
