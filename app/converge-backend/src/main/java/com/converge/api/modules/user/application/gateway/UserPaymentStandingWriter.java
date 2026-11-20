// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application.gateway;

import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Grava a inadimplência do usuário por update parcial atômico.
 * Os filtros conferem o contador atual no banco, então uma decisão feita com um valor
 * antigo nunca grava um estado errado.
 */
public interface UserPaymentStandingWriter {

    /** Soma uma dívida vencida. Devolve o novo total, ou vazio se o usuário não existe. */
    Optional<Integer> incrementOverdue(ObjectId userId, LocalDateTime now);

    /** Tira uma dívida vencida. Devolve o novo total, ou vazio se não existe ou já estava em 0. */
    Optional<Integer> decrementOverdue(ObjectId userId, LocalDateTime now);

    /** Passa para HAS_OVERDUE se o total ainda está entre 1 e o limite (exclusive). */
    boolean markHasOverdue(ObjectId userId, int limit, LocalDateTime now);

    /** Bloqueia se o total ainda está no limite ou acima. A data do bloqueio só é gravada se estiver vazia. */
    boolean block(ObjectId userId, int limit, LocalDateTime now);

    /** Volta para UP_TO_DATE e desbloqueia, só se o total ainda é 0. */
    boolean regularize(ObjectId userId, LocalDateTime now);
}
