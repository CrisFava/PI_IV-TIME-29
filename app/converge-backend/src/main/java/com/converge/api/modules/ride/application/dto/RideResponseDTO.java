// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application.dto;

import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.subtypes.RideCosts;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.application.dto.PlaceResponseDTO;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record RideResponseDTO(
        String id,
        int sequentialNumber,
        String driverId,
        String vehicleId,
        VehicleSnapshot vehicleSnapshot,
        String driverRouteId,
        PlaceResponseDTO origin,
        PlaceResponseDTO destination,
        LocalDateTime departureDateTime,
        LocalDateTime estimatedArrivalDateTime,
        LocalDateTime completedAt,
        Set<DayOfWeek> weekDays,
        int totalSeats,
        int availableSeats,
        RideStatus status,
        String cancellationReason,
        String cancelledBy,
        String chatRoomId,
        RideCosts costs,
        List<ParticipantResponseDTO> participants,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        // O motorista vê todas as paradas e a rota; o passageiro, só as próprias paradas
        List<RideStopResponseDTO> itinerary,
        Double itineraryDistanceKm,
        String itineraryPolyline,
        // O mesmo caminho em trechos: um até cada parada e o último até o destino (só o motorista)
        List<String> itineraryLegPolylines,
        // Viagem em andamento: a parada em que ela está (o trecho atual termina nela)
        Integer itineraryNextStop,
        // Para o passageiro: onde ficam o embarque e o desembarque dele na viagem (o app
        // desenha só o caminho até eles, sem marcar as paradas dos outros)
        Integer itineraryMyPickupStop,
        Integer itineraryMyDropOffStop,
        // Para o passageiro, com as paradas dos outros: quanto o motorista leva até ele e
        // quanto dura o trecho com ele no carro (os trechos desenhados não vão para ele)
        Integer itineraryMyPickupMinutes,
        Integer itineraryMyRideMinutes,
        // Só para o motorista: o tempo de cada trecho, em segundos (o horário de cada parada)
        List<Integer> itineraryLegSeconds
) {
}