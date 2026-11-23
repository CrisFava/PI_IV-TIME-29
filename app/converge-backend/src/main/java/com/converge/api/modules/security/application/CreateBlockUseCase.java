// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application;

import com.converge.api.modules.security.application.gateway.UserLookupGateway;
import com.converge.api.modules.security.domain.model.Block;
import com.converge.api.modules.security.domain.repository.BlockRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

// Bloqueia um usuário (RF46). É idempotente: se o bloqueio já existe, devolve o que está salvo.
@Service
@RequiredArgsConstructor
public class CreateBlockUseCase implements UseCase<CreateBlockUseCase.Command, Block> {

    private final BlockRepository blockRepository;
    private final UserLookupGateway userLookup;
    private final ApplicationEventPublisher eventPublisher;

    // Entrada: quem bloqueia (do token) e quem será bloqueado.
    public record Command(String userId, String blockedUserId) {
    }

    @Override
    public Block execute(Command command) {
        // O construtor recusa campos vazios e o auto-bloqueio
        Block block = new Block(command.userId(), command.blockedUserId());
        if (!userLookup.exists(command.blockedUserId())) {
            throw new ApplicationException(ErrorCode.USER_NOT_FOUND, "Usuário não encontrado.");
        }
        return blockRepository.findByUserIdAndBlockedUserId(command.userId(), command.blockedUserId())
                .orElseGet(() -> {
                    Block saved = save(block);
                    // As caronas que eles ainda iam fazer juntos deixam de ter os dois
                    eventPublisher.publishEvent(new UserBlockedEvent(command.userId(), command.blockedUserId()));
                    return saved;
                });
    }

    private Block save(Block block) {
        try {
            return blockRepository.save(block);
        } catch (DuplicateKeyException e) {
            // Dois pedidos iguais ao mesmo tempo: o índice único barra o segundo e devolvemos o primeiro.
            return blockRepository.findByUserIdAndBlockedUserId(block.getUserId(), block.getBlockedUserId())
                    .orElseThrow(() -> e);
        }
    }
}
