// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.RequestRideSeatCommand;
import com.converge.api.modules.ride.application.mapper.RideMapper;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.route.application.RouteAccess;
import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.security.application.BlockAccess;
import com.converge.api.modules.user.application.VehicleAccess;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
/**
 * Caso de uso responsável por um passageiro pedir vaga em uma carona.
 *
 * Fluxo geral: valida o passageiro (elegibilidade, pagamento pendente, bloqueios),
 * valida os dois trajetos (o de procura do passageiro e a oferta do motorista),
 * e então registra o pedido: ou numa carona que já existe para aquele horário,
 * ou criando uma nova carona. Também trata a concorrência, quando dois pedidos
 * tentam criar a mesma carona ao mesmo tempo.
 */
@Service
@RequiredArgsConstructor
public class RequestRideSeatUseCase implements UseCase<RequestRideSeatCommand, Ride> {

    private final RideAccess rideAccess;
    private final RideEligibility rideEligibility;
    private final RouteAccess routeAccess;
    private final VehicleAccess vehicleAccess;
    private final RideRepository rideRepository;
    private final RideMapper rideMapper;
    private final BlockAccess blockAccess;
    private final PendingPaymentGuard pendingPaymentGuard;
    /**
     * Executa o pedido de vaga.
     * Valida passageiro e trajetos, calcula o horário de saída e procura uma carona
     * aberta para essa saída. Se existir, pede a vaga nela de forma atômica; se não,
     * cria a carona, registra o pedido e salva. Se outro pedido criar a mesma carona
     * primeiro (DuplicateKeyException), busca a carona existente e refaz o pedido nela.
     */

    @Override
    public Ride execute(RequestRideSeatCommand command) {
        String passengerId = rideAccess.currentUserId(command.firebaseUid());
        rideEligibility.assertCanJoin(passengerId, "Você");
        pendingPaymentGuard.ensureNoPendingPayment(passengerId);

        LocalDate date = command.request().date();

        Route passengerRoute = routeAccess.getOwnedRoute(command.request().passengerRouteId(), passengerId);
        validatePassengerRoute(passengerRoute, date);

        Route driverRoute = routeAccess.getActiveRoute(command.request().driverRouteId());
        validateDriverRoute(driverRoute, passengerId, date);
        assertNotBlocked(passengerId, driverRoute.getUserId());

        LocalDateTime departure = date.atTime(LocalTime.parse(driverRoute.getDepartureTime()));

        var existingRide = rideRepository.findByDriverRouteIdAndDepartureDateTimeAndStatusNot(
                driverRoute.getId().toHexString(), departure, RideStatus.CANCELLED);

        if (existingRide.isPresent() && existingRide.get().getId() != null) {
            String passengerRouteId = passengerRoute.getId().toHexString();
            return requestSeatOnExistingRide(existingRide.get(), passengerId, passengerRouteId, passengerRoute);
        }

        Ride ride = existingRide.orElseGet(() -> createRide(driverRoute, date, departure));
        String passengerRouteId = passengerRoute.getId().toHexString();
        ride.requestSeat(passengerId, passengerRouteId, passengerRoute.getOrigin(), passengerRoute.getDestination());

        try {
            return rideRepository.save(ride);
        } catch (DuplicateKeyException exception) {
            if (!isDuplicatedOpenRide(exception)) {
                throw rideConflict();
            }

            Ride existing = rideRepository
                    .findByDriverRouteIdAndDepartureDateTimeAndStatusNot(
                            driverRoute.getId().toHexString(), departure, RideStatus.CANCELLED)
                    .orElseThrow(this::rideConflict);

            if (existing.getId() != null) {
                return requestSeatOnExistingRide(existing, passengerId, passengerRouteId, passengerRoute);
            }

            existing.requestSeat(
                    passengerId, passengerRouteId, passengerRoute.getOrigin(), passengerRoute.getDestination());

            return rideRepository.save(existing);
        }
    }

    /**
     * Pede a vaga em uma carona que já existe, usando uma operação atômica no
     * repositório para evitar que duas pessoas ocupem a mesma vaga ao mesmo tempo.
     * Se a operação falhar (ex.: carona lotada), lança o erro de conflito.
     */
    private Ride requestSeatOnExistingRide(
            Ride ride,
            String passengerId,
            String passengerRouteId,
            Route passengerRoute
    ) {
        LocalDateTime now = LocalDateTime.now();
        Participant passengerRequest = Participant.passengerRequest(
                passengerId,
                passengerRouteId,
                passengerRoute.getOrigin(),
                passengerRoute.getDestination(),
                now
        );

        return rideRepository.requestSeatAtomic(ride.getId(), passengerRequest, now)
                .orElseThrow(() -> resolveRequestSeatConflict(ride, passengerId, passengerRouteId, passengerRoute));
    }
    /**
     * Monta o erro de conflito quando o pedido atômico não deu certo.
     * Recarrega a carona mais recente e tenta registrar o pedido nela, para que as
     * regras do domínio (ex.: sem vagas, pedido duplicado) possam lançar o erro mais
     * específico. Se nenhuma regra barrar, devolve o conflito genérico.
     */

    private ApplicationException resolveRequestSeatConflict(
            Ride fallbackRide,
            String passengerId,
            String passengerRouteId,
            Route passengerRoute
    ) {
        Ride latest = rideRepository.findById(fallbackRide.getId()).orElse(fallbackRide);
        latest.requestSeat(passengerId, passengerRouteId, passengerRoute.getOrigin(), passengerRoute.getDestination());
        return rideConflict();
    }

    /**
     * Verifica se o DuplicateKeyException foi causado pelo índice único de
     * "carona aberta" (mesmo motorista/trajeto/horário), e não por outro motivo.
     */

    private boolean isDuplicatedOpenRide(DuplicateKeyException exception) {
        String message = exception.getMostSpecificCause().getMessage();

        return (message != null && message.contains(Ride.UNIQUE_OPEN_RIDE_INDEX))
                || (exception.getMessage() != null && exception.getMessage().contains(Ride.UNIQUE_OPEN_RIDE_INDEX));
    }

    // Bloqueio entre motorista e passageiro vale nos dois sentidos (RF47)
    /**
     * Impede o pedido se houver bloqueio entre passageiro e motorista, em qualquer direção.
     */
    private void assertNotBlocked(String passengerId, String driverId) {
        if (blockAccess.isBlockedBetween(passengerId, driverId)) {
            throw new ApplicationException(
                    ErrorCode.NOT_ALLOWED,
                    "Não é possível pedir vaga nesta carona."
            );
        }
    }

    /**
     * Cria o erro padrão de conflito ao registrar o pedido (RIDE_CONFLICT),
     * com mensagem amigável pedindo para tentar de novo.
     */
    private ApplicationException rideConflict() {
        return new ApplicationException(
                ErrorCode.RIDE_CONFLICT,
                "Não foi possível registrar o pedido agora. Tente novamente."
        );
    }

    /**
     * Valida o trajeto do passageiro: precisa ser do tipo procura (SEARCH),
     * estar ativo e valer para a data escolhida.
     */
    private void validatePassengerRoute(Route route, LocalDate date) {
        if (route.getType() != RouteType.SEARCH || route.getStatus() != RouteStatus.ACTIVE) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Só é possível pedir vaga com um trajeto de procura que esteja ativo.",
                    Set.of("passengerRouteId")
            );
        }

        if (!route.runsOn(date)) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O seu pedido de carona não é para a data escolhida.",
                    Set.of("date")
            );
        }
    }

    /**
     * Valida o trajeto do motorista: precisa ser uma oferta (OFFER), acontecer na
     * data escolhida, não pertencer ao próprio passageiro e ter o número de vagas definido.
     */
    private void validateDriverRoute(Route route, String passengerId, LocalDate date) {
        if (route.getType() != RouteType.OFFER) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O trajeto escolhido não é uma oferta de carona.",
                    Set.of("driverRouteId")
            );
        }

        if (!route.runsOn(date)) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "A carona do motorista não acontece na data escolhida.",
                    Set.of("date")
            );
        }

        if (route.isOwnedBy(passengerId)) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Você não pode pedir vaga no seu próprio trajeto.",
                    Set.of("driverRouteId")
            );
        }

        if (route.getTotalSeats() == null) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O trajeto do motorista não tem o número de vagas definido.",
                    Set.of("driverRouteId")
            );
        }
    }

    /**
     * Cria uma nova carona a partir da oferta do motorista.
     * Confere se o motorista pode oferecer carona, carrega o veículo ativo, calcula
     * chegada (somando um dia se passar da meia-noite), monta a carona com número
     * sequencial, vagas, pedágio e estacionamento, e registra o combustível do veículo.
     */
    private Ride createRide(Route driverRoute, LocalDate date, LocalDateTime departure) {
        String driverId = driverRoute.getUserId();
        rideEligibility.assertCanOffer(driverId, "O motorista");

        Vehicle vehicle = loadVehicle(driverRoute, driverId);

        LocalDateTime arrival = date.atTime(LocalTime.parse(driverRoute.getEstimatedArrivalTime()));
        if (arrival.isBefore(departure)) {
            arrival = arrival.plusDays(1);
        }

        Ride ride = new Ride(
                nextSequentialNumber(driverId),
                driverId,
                vehicle.getId().toHexString(),
                rideMapper.toVehicleSnapshot(vehicle),
                driverRoute.getId().toHexString(),
                driverRoute.getOrigin(),
                driverRoute.getDestination(),
                departure,
                arrival,
                driverRoute.getWeekDays(),
                driverRoute.getTotalSeats(),
                driverRoute.getToll(),
                driverRoute.getParking()
        );

        vehicle.getFuelSources().stream()
                .findFirst()
                .ifPresent(fuel -> ride.recordFuel(
                        fuel.energySource(), fuel.averageConsumption(), fuel.currentEnergyPrice()));

        return ride;
    }
    /**
     * Busca o veículo ativo do motorista usado no trajeto.
     */
    private Vehicle loadVehicle(Route route, String driverId) {
        return vehicleAccess.getActiveVehicleOf(route.getVehicleId(), driverId);
    }

    /**
     * Calcula o próximo número sequencial de carona do motorista
     * (último número + 1, ou 1 se for a primeira).
     */
    private int nextSequentialNumber(String driverId) {
        return rideRepository.findTopByDriverIdOrderBySequentialNumberDesc(driverId)
                .map(ride -> ride.getSequentialNumber() + 1)
                .orElse(1);
    }
}
