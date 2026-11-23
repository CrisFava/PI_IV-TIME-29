// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.domain.model.user.subtypes;

import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import com.converge.api.modules.user.domain.enums.user.VerificationMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDateTime;

/**
 * Dados acadêmicos de um usuário (value object imutável).
 *
 * Guarda a universidade, o curso, a matrícula e o e-mail institucional, além do
 * estado da verificação do vínculo acadêmico (status, método usado, datas e
 * motivo de recusa). É esse vínculo verificado que o RideEligibility exige para
 * o usuário participar de caronas.
 */
public record AcademicRecord(

        @NotBlank
        String university,

        @NotBlank
        String course,

        @NotBlank
        String enrollmentNumber,

        @NotBlank
        @Email
        String institutionalEmail,

        String documentUrl,

        @NotNull
        VerificationStatus status,

        @NotNull
        VerificationMethod verificationMethod,

        @PastOrPresent
        LocalDateTime verifiedAt,

        @PastOrPresent
        LocalDateTime reviewedAt,

        String rejectionReason
) {
    public AcademicRecord(
            String university,
            String course,
            String enrollmentNumber,
            String institutionalEmail
    ) {
        this(
                university,
                course,
                enrollmentNumber,
                institutionalEmail,
                null,
                VerificationStatus.PENDING,
                VerificationMethod.INSTITUTIONAL_EMAIL,
                null,
                null,
                null
        );
    }


}
