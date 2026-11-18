// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// Conta +1 viagem concluída na janela do motorista e de cada passageiro efetivo.
// Não guarda os ids das viagens: quem chama (o listener do RideCompletedEvent) é acionado
// uma vez por viagem, porque a viagem só conclui uma vez.
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordCompletedRideUseCase {

    private final UserRepository userRepository;

    public void execute(String driverId, List<String> passengerIds) {
        LocalDateTime now = LocalDateTime.now();
        record(driverId, ParticipationRole.DRIVER, now);
        if (passengerIds != null) {
            passengerIds.forEach(passengerId -> record(passengerId, ParticipationRole.PASSENGER, now));
        }
    }

    // Falha em um usuário não impede a contagem dos outros.
    private void record(String userId, ParticipationRole role, LocalDateTime now) {
        if (userId == null || !ObjectId.isValid(userId)) {
            return;
        }
        try {
            userRepository.findById(new ObjectId(userId)).ifPresent(user -> {
                apply(user, role, now);
                userRepository.save(user);
            });
        } catch (RuntimeException e) {
            log.warn("Não foi possível contar a viagem concluída do usuário {} ({}): {}", userId, role, e.getMessage());
        }
    }

    private void apply(User user, ParticipationRole role, LocalDateTime now) {
        switch (role) {
            case DRIVER -> user.recordDriverCompletedRide(now);
            case PASSENGER -> user.recordPassengerCompletedRide(now);
        }
    }
}
