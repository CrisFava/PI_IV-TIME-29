// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.ride.infrastructure.RideCostCalculator;
import com.converge.api.modules.route.application.RouteAccess;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * "Confirmar chegada": o motorista encerra a viagem de um passageiro que desceu, com a viagem ainda
 * em andamento. O passageiro recebe o valor do rateio do momento e a cobrança já é emitida (RF39).
 */
@Service
@RequiredArgsConstructor
public class ArrivePassengerUseCase implements UseCase<PassengerRequestCommand, Ride> {

    private final RideAccess rideAccess;
    private final RideCostCalculator rideCostCalculator;
    private final RideRepository rideRepository;
    private final RouteAccess routeAccess;
    private final RidePaymentNotifier paymentNotifier;

    /**
     * Confirma a chegada de um passageiro (RF73), tudo na mesma transação:
     *
     * 1. Confere que o usuário logado é o motorista da viagem.
     * 2. Marca o passageiro como COMPLETED (a viagem precisa estar IN_PROGRESS e ele CONFIRMED).
     * 3. Calcula o valor dele com o rateio do momento, dividido entre quem já desceu e quem ainda está a
     *    bordo. Esse valor não muda mais no encerramento da viagem (RF38).
     * 4. Emite a cobrança de quem concluiu e ainda não foi cobrado.
     * 5. Depois de salvar, encerra a procura de um dia só do passageiro e notifica os cobrados (PAYMENT_DUE).
     *
     * @param command viagem, passageiro que desceu e firebaseUid do motorista
     * @return a viagem salva
     */
    @Override
    @Transactional(transactionManager = "mongoTransactionManager")
    public Ride execute(PassengerRequestCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());

        ride.arrivePassenger(command.passengerId());
        rideCostCalculator.calculateArrivalShare(ride, command.passengerId());
        List<String> charged = ride.chargeCompletedPassengers();

        Ride saved = rideRepository.save(ride);
        // A procura de um dia só de quem desceu acabou: ele deixa de aparecer como disponível no matching
        passengerRouteIdOf(saved, command.passengerId()).forEach(routeAccess::endIfOnce);
        paymentNotifier.notifyCharged(saved, charged);

        return saved;
    }

    /**
     * Trajeto de procura do passageiro na viagem, só se ele já concluiu. Volta lista vazia (e não
     * Optional) para encaixar direto no forEach de quem chama.
     */
    private static List<String> passengerRouteIdOf(Ride ride, String passengerId) {
        return ride.getParticipants().stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getUserId().equals(passengerId))
                .filter(participant -> participant.getStatus() == ParticipantStatus.COMPLETED)
                .map(Participant::getPassengerRouteId)
                .filter(Objects::nonNull)
                .toList();
    }
}
