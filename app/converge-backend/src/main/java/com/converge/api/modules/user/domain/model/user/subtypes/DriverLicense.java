package com.converge.api.modules.user.domain.model.user.subtypes;

import com.converge.api.modules.user.domain.enums.user.DriverLicenseCategory;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDateTime;

public record DriverLicense(
        @NotBlank
        String number,

        @NotNull
        DriverLicenseCategory category,

        String documentUrl,

        @NotNull
        VerificationStatus status,

        @NotNull
        @FutureOrPresent(message = "INVALID_EXPIRATION_DATE")
        LocalDateTime expiresAt,

        @PastOrPresent(message = "INVALID_VERIFICATION_DATE")
        LocalDateTime verifiedAt,

        String rejectionReason
) {
    public DriverLicense(String number, DriverLicenseCategory category, LocalDateTime expiresAt, String documentUrl) {
        this(
                number,
                category,
                documentUrl,
                VerificationStatus.PENDING,
                expiresAt,
                null,
                null
        );
    }
}
