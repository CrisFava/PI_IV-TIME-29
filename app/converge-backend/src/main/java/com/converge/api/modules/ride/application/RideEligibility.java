package com.converge.api.modules.ride.application;

import com.converge.api.modules.user.domain.enums.user.PaymentStanding;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class RideEligibility {

    private final UserUtils userUtils;
    private final UserRepository userRepository;

    public void assertCanParticipate(String firebaseUid) {
        validate(userUtils.getCurrentUser(firebaseUid), "Você");
    }

    public void assertUserCanParticipate(String userId) {
        if (!ObjectId.isValid(userId)) {
            throw userNotFound();
        }

        User user = userRepository.findById(new ObjectId(userId)).orElseThrow(this::userNotFound);

        validate(user, "O passageiro");
    }

    private void validate(User user, String subject) {
        AcademicRecord academicRecord = user.getAcademicRecord();

        if (academicRecord == null || academicRecord.status() != VerificationStatus.VERIFIED) {
            throw new ApplicationException(
                    ErrorCode.NOT_ALLOWED,
                    subject + " não tem o vínculo acadêmico verificado.",
                    Set.of("academicRecord")
            );
        }

        if (user.getPaymentStanding() == PaymentStanding.BLOCKED) {
            throw new ApplicationException(
                    ErrorCode.NOT_ALLOWED,
                    subject + " está bloqueado por pagamentos vencidos.",
                    Set.of("paymentStanding")
            );
        }
    }

    private ApplicationException userNotFound() {
        return new ApplicationException(ErrorCode.USER_NOT_FOUND, "Usuário não encontrado.");
    }
}
