// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application.dto;

// Comando para submissão de CNH do motorista
public record SubmitDriverLicenseCommand (
        String firebaseUid,
        DriverLicenseRequestDTO request
) {
}
