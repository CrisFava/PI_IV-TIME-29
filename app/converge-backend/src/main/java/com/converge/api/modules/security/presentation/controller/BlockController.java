// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.presentation.controller;

import com.converge.api.modules.security.application.CreateBlockUseCase;
import com.converge.api.modules.security.application.DeleteBlockUseCase;
import com.converge.api.modules.security.application.ListBlocksUseCase;
import com.converge.api.modules.security.application.dto.BlockResponseDTO;
import com.converge.api.modules.security.application.dto.CreateBlockRequestDTO;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.presentation.dto.ApplicationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Endpoints de bloqueio entre usuários (RF46).
@RestController
@RequestMapping("/api/v1/blocks")
@RequiredArgsConstructor
public class BlockController {

    private final CreateBlockUseCase createBlock;
    private final DeleteBlockUseCase deleteBlock;
    private final ListBlocksUseCase listBlocks;
    private final UserUtils userUtils;

    // Bloqueia um usuário. Quem bloqueia é o usuário logado. Repetir o pedido devolve o mesmo bloqueio.
    @PostMapping
    public ResponseEntity<ApplicationResponse<BlockResponseDTO>> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateBlockRequestDTO request
    ) {
        var block = createBlock.execute(
                new CreateBlockUseCase.Command(currentUserId(principal), request.blockedUserId()));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApplicationResponse.success(BlockResponseDTO.from(block, null)));
    }

    // Desbloqueia um usuário. Responde 204 mesmo que não houvesse bloqueio.
    @DeleteMapping("/{blockedUserId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String blockedUserId
    ) {
        deleteBlock.execute(new DeleteBlockUseCase.Command(currentUserId(principal), blockedUserId));
        return ResponseEntity.noContent().build();
    }

    // Lista quem o usuário logado bloqueou.
    @GetMapping
    public ResponseEntity<ApplicationResponse<List<BlockResponseDTO>>> list(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(ApplicationResponse.success(listBlocks.execute(currentUserId(principal))));
    }

    private String currentUserId(AuthenticatedUser principal) {
        return userUtils.getCurrentUser(principal).getId().toHexString();
    }
}
