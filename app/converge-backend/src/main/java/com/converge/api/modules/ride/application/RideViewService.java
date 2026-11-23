// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.RideResponseDTO;
import com.converge.api.modules.ride.application.mapper.RideMapper;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.route.application.RouteAccess;
import com.converge.api.modules.user.application.GetUserNamesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * Serviço que transforma caronas (entidade Ride) em respostas prontas para a API
 * (RideResponseDTO), do ponto de vista de quem está olhando.
 *
 * Para montar a resposta, busca em lote o que a entidade sozinha não tem: os nomes
 * dos usuários que o visualizador pode ver e, quando ele é o motorista, o horário
 * de saída que cada passageiro escolheu no próprio trajeto. Fazer isso em lote
 * (uma consulta para todas as caronas) evita uma consulta por carona.
 */
@Service
@RequiredArgsConstructor
public class RideViewService {

    private final RideAccess rideAccess;
    private final GetUserNamesUseCase getUserNames;
    private final RideMapper rideMapper;
    private final RouteAccess routeAccess;

    /**
     * Monta a resposta de uma única carona para o usuário logado.
     * Reaproveita o viewAll passando uma lista com um só item.
     */
    public RideResponseDTO view(Ride ride, String firebaseUid) {
        return viewAll(List.of(ride), firebaseUid).getFirst();
    }

    /**
     * Monta a resposta de várias caronas para o usuário logado.
     * Descobre o id do visualizador, coleta os ids de usuários que ele tem
     * permissão de ver e busca os nomes de todos de uma vez. Para as caronas em
     * que ele é o motorista, também busca o horário de saída escolhido por cada
     * passageiro. No fim, o mapper converte cada carona em DTO usando esses dados.
     */
    public List<RideResponseDTO> viewAll(List<Ride> rides, String firebaseUid) {
        String viewerId = rideAccess.currentUserId(firebaseUid);

        Set<String> userIds = rides.stream()
                .flatMap(ride -> rideMapper.visibleUserIds(ride, viewerId).stream())
                .collect(Collectors.toSet());

        Map<String, String> namesByUserId = getUserNames.execute(userIds);

        // Para o motorista: a hora que cada passageiro escolheu, do trajeto dele
        Set<String> passengerRouteIds = rides.stream()
                .filter(ride -> ride.isOwnedBy(viewerId))
                .flatMap(ride -> ride.getParticipants().stream())
                .map(Participant::getPassengerRouteId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> departureTimes = routeAccess.departureTimesOf(passengerRouteIds);

        return rides.stream()
                .map(ride -> rideMapper.toResponse(ride, viewerId, namesByUserId, departureTimes))
                .toList();
    }
}
