// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.event.RideCompletedEvent;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura para os outros módulos (ex.: indicator): as viagens concluídas depois de uma data,
 * já no formato do RideCompletedEvent. Usada para reprocessar o que não foi contabilizado na hora.
 */
@Service
@RequiredArgsConstructor
public class ListRecentlyCompletedRidesUseCase implements UseCase<LocalDateTime, List<RideCompletedEvent>> {

    private final RideRepository rideRepository;
    private final RideCompletionPublisher completionPublisher;

    /**
     * Busca as viagens COMPLETED concluídas depois da data e converte cada uma no evento de conclusão.
     * As que não contam para os indicadores (sem passageiro efetivo) ficam de fora.
     *
     * @param completedAfter início da janela (exclusivo)
     * @return os eventos das viagens da janela
     */
    @Override
    public List<RideCompletedEvent> execute(LocalDateTime completedAfter) {
        return rideRepository.findByStatusAndCompletedAtAfter(RideStatus.COMPLETED, completedAfter).stream()
                .map(completionPublisher::eventOf)
                .flatMap(Optional::stream)
                .toList();
    }
}
