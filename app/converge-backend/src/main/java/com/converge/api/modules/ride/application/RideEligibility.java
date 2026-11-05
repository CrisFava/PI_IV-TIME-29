package com.converge.api.modules.ride.application;

import com.converge.api.modules.user.application.GetUserEligibilityUseCase;
import com.converge.api.modules.user.application.dto.UserEligibilityDTO;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class RideEligibility {

    private final GetUserEligibilityUseCase getUserEligibility;

    public void assertCanJoin(String userId, String subject) {
        validateJoin(getUserEligibility.execute(userId), subject);
    }

    public void assertCanOffer(String userId, String subject) {
        UserEligibilityDTO eligibility = getUserEligibility.execute(userId);

        validateJoin(eligibility, subject);

        if (!eligibility.driverLicenseValid()) {
            throw notAllowed(subject + " não tem a CNH verificada e dentro da validade.", "driverLicense");
        }
    }

    private void validateJoin(UserEligibilityDTO eligibility, String subject) {
        if (!eligibility.active()) {
            throw notAllowed(subject + " está com a conta desativada.", "active");
        }

        if (!eligibility.academicVerified()) {
            throw notAllowed(subject + " não tem o vínculo acadêmico verificado.", "academicRecord");
        }

        if (eligibility.blockedByDebt()) {
            throw notAllowed(subject + " está bloqueado por pagamentos vencidos.", "paymentStanding");
        }
    }

    private ApplicationException notAllowed(String message, String field) {
        return new ApplicationException(ErrorCode.NOT_ALLOWED, message, Set.of(field));
    }
}
