// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.domain.model.user.subtypes.driver;

import com.converge.api.modules.user.domain.enums.user.DriverLicenseCategory;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDateTime;

/**
 * Dados da CNH (carteira de motorista) de um usuário (value object imutável).
 *
 * Guarda número, categoria, validade, URL do documento e o estado da verificação
 * (status, data de verificação e motivo de recusa). Quando é criada a partir do
 * cadastro, nasce com status UNDER_REVIEW, aguardando verificação.
 * O status salvo pode ficar desatualizado com o tempo (uma CNH verificada pode
 * vencer), por isso há métodos que calculam o status efetivo na data atual.
 */
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
                VerificationStatus.UNDER_REVIEW,
                expiresAt,
                null,
                null
        );
    }

    /**
     * Indica se a CNH está vencida na data informada. Compara só a data (ignora
     * a hora): a CNH vale até o fim do dia da validade e só é considerada
     * vencida a partir do dia seguinte.
     */
    public boolean isExpiredAt(LocalDateTime now) {
        return expiresAt.toLocalDate().isBefore(now.toLocalDate());
    }

    /**
     * Calcula o status que vale na data informada. Se está VERIFIED mas já
     * venceu, devolve EXPIRED; em qualquer outro caso devolve o status salvo.
     * Não altera o registro.
     */
    public VerificationStatus effectiveStatus(LocalDateTime now) {
        return status == VerificationStatus.VERIFIED && isExpiredAt(now)
                ? VerificationStatus.EXPIRED
                : status;
    }
    /**
     * Devolve uma cópia da CNH com o status trocado pelo status efetivo na data
     * informada. Usado para exibir o status atualizado (como no UserMapper)
     * sem mexer no que está salvo.
     */
    public DriverLicense withEffectiveStatus(LocalDateTime now) {
        return new DriverLicense(
                number,
                category,
                documentUrl,
                effectiveStatus(now),
                expiresAt,
                verifiedAt,
                rejectionReason
        );
    }
}
