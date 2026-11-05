package com.converge.api.modules.user.application.dto;

public record UserEligibilityDTO(
        boolean active,
        boolean academicVerified,
        boolean blockedByDebt,
        boolean driverLicenseValid
) {

    public boolean canJoinRides() {
        return active && academicVerified && !blockedByDebt;
    }

    public boolean canOfferRides() {
        return canJoinRides() && driverLicenseValid;
    }
}
