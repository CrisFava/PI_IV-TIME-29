// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application;

import com.converge.api.modules.security.domain.repository.BlockRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Desbloqueia um usuário (RF46). Remove só o bloqueio feito por quem pede: se o outro também
// bloqueou, o bloqueio dele continua valendo. É idempotente: sem bloqueio, não faz nada.
@Service
@RequiredArgsConstructor
public class DeleteBlockUseCase implements UseCase<DeleteBlockUseCase.Command, Void> {

    private final BlockRepository blockRepository;

    // Entrada: quem desbloqueia (do token) e quem foi bloqueado.
    public record Command(String userId, String blockedUserId) {
    }

    @Override
    public Void execute(Command command) {
        blockRepository.deleteByUserIdAndBlockedUserId(command.userId(), command.blockedUserId());
        return null;
    }
}
