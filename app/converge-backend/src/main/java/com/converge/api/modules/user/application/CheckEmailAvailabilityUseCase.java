// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.dto.EmailAvailabilityResponseDTO;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckEmailAvailabilityUseCase implements UseCase<String, EmailAvailabilityResponseDTO> {

    private final UserRepository userRepository;

    @Override
    public EmailAvailabilityResponseDTO execute(String email) {
        if (email == null || email.isBlank()) {
            return new EmailAvailabilityResponseDTO(false);
        }

        String normalized = email.trim();
        boolean inUse = userRepository.existsByEmailIgnoreCase(normalized)
                || userRepository.existsByAcademicRecordInstitutionalEmailIgnoreCase(normalized);
        return new EmailAvailabilityResponseDTO(!inUse);
    }
}
