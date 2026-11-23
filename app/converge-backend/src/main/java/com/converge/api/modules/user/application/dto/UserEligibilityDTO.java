// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application.dto;

// Dados sobre a elegibilidade do usuário para participar ou oferecer caronas
public record UserEligibilityDTO(
        boolean active,
        boolean academicVerified,
        boolean blockedByDebt,
        boolean driverLicenseValid,
        boolean hasPixKey
) {

    /**
     * Verifica se o usuário cumpre os critérios para participar de caronas como passageiro.
     */
    public boolean canJoinRides() {
        return active && academicVerified && !blockedByDebt;
    }

    /**
     * Verifica se o usuário cumpre os critérios para criar e oferecer caronas como motorista.
     */
    public boolean canOfferRides() {
        return canJoinRides() && driverLicenseValid && hasPixKey;
    }
}
