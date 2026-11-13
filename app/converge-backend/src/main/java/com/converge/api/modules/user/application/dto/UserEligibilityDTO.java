// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application.dto;

public record UserEligibilityDTO(
        boolean active,
        boolean academicVerified,
        boolean blockedByDebt,
        boolean driverLicenseValid,
        boolean hasPixKey
) {

    public boolean canJoinRides() {
        return active && academicVerified && !blockedByDebt;
    }

    public boolean canOfferRides() {
        return canJoinRides() && driverLicenseValid && hasPixKey;
    }
}
