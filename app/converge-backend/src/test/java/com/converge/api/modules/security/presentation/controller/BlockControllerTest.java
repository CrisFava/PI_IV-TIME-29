// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.presentation.controller;

import com.converge.api.modules.security.application.CreateBlockUseCase;
import com.converge.api.modules.security.application.DeleteBlockUseCase;
import com.converge.api.modules.security.application.ListBlocksUseCase;
import com.converge.api.modules.security.application.dto.BlockResponseDTO;
import com.converge.api.modules.security.domain.model.Block;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.infrastructure.exception.GlobalExceptionHandler;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BlockControllerTest {

    private final ObjectId loggedUserId = new ObjectId();

    @Mock
    private CreateBlockUseCase createBlock;

    @Mock
    private DeleteBlockUseCase deleteBlock;

    @Mock
    private ListBlocksUseCase listBlocks;

    @Mock
    private UserUtils userUtils;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BlockController controller = new BlockController(createBlock, deleteBlock, listBlocks, userUtils);
        // Simula o usuário logado, como o filtro do Firebase faria.
        HandlerMethodArgumentResolver authenticatedUser = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return AuthenticatedUser.class.equals(parameter.getParameterType());
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory
            ) {
                return new AuthenticatedUser("firebase-uid", "ana@puc-campinas.edu.br");
            }
        };
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authenticatedUser)
                .build();
    }

    private void loggedUserIsKnown() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(loggedUserId);
        when(userUtils.getCurrentUser(any(AuthenticatedUser.class))).thenReturn(user);
    }

    @Test
    @DisplayName("POST /blocks deve bloquear em nome do usuário logado e responder 201 no envelope")
    void shouldCreateBlockForLoggedUser() throws Exception {
        loggedUserIsKnown();
        when(createBlock.execute(new CreateBlockUseCase.Command(loggedUserId.toHexString(), "alvo")))
                .thenReturn(new Block(loggedUserId.toHexString(), "alvo"));

        mockMvc.perform(post("/api/v1/blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blockedUserId\":\"alvo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.blockedUserId").value("alvo"));
    }

    @Test
    @DisplayName("POST /blocks ignora quem bloqueia vindo no corpo: vale o usuário do token")
    void shouldIgnoreUserIdSentInBody() throws Exception {
        loggedUserIsKnown();
        when(createBlock.execute(new CreateBlockUseCase.Command(loggedUserId.toHexString(), "alvo")))
                .thenReturn(new Block(loggedUserId.toHexString(), "alvo"));

        mockMvc.perform(post("/api/v1/blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"outro-usuario\",\"blockedUserId\":\"alvo\"}"))
                .andExpect(status().isCreated());

        verify(createBlock).execute(new CreateBlockUseCase.Command(loggedUserId.toHexString(), "alvo"));
    }

    @Test
    @DisplayName("POST /blocks sem o usuário a bloquear deve responder 400 sem chamar o caso de uso")
    void shouldRejectMissingBlockedUser() throws Exception {
        mockMvc.perform(post("/api/v1/blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blockedUserId\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));

        verifyNoInteractions(createBlock);
    }

    @Test
    @DisplayName("POST /blocks com usuário inexistente deve responder 404 no envelope de erro")
    void shouldAnswerUserNotFound() throws Exception {
        loggedUserIsKnown();
        when(createBlock.execute(any()))
                .thenThrow(new ApplicationException(ErrorCode.USER_NOT_FOUND, "Usuário não encontrado."));

        mockMvc.perform(post("/api/v1/blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blockedUserId\":\"alvo\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /blocks/{id} deve desbloquear em nome do usuário logado e responder 204")
    void shouldDeleteBlockForLoggedUser() throws Exception {
        loggedUserIsKnown();

        mockMvc.perform(delete("/api/v1/blocks/alvo"))
                .andExpect(status().isNoContent());

        verify(deleteBlock).execute(new DeleteBlockUseCase.Command(loggedUserId.toHexString(), "alvo"));
    }

    @Test
    @DisplayName("GET /blocks deve listar os bloqueados do usuário logado")
    void shouldListBlocksOfLoggedUser() throws Exception {
        loggedUserIsKnown();
        when(listBlocks.execute(loggedUserId.toHexString())).thenReturn(List.of(
                new BlockResponseDTO(new ObjectId().toHexString(), "alvo", "Bruno", Instant.parse("2026-11-20T10:00:00Z"))));

        mockMvc.perform(get("/api/v1/blocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].blockedUserId").value("alvo"))
                .andExpect(jsonPath("$.data[0].blockedUserName").value("Bruno"));
    }
}
