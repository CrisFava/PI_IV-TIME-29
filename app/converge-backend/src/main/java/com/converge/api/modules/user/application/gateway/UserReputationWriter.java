// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application.gateway;

import com.converge.api.modules.user.domain.enums.user.Badge;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverReputation;
import com.converge.api.modules.user.domain.model.user.subtypes.passenger.PassengerReputation;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

/**
 * Grava a reputação e os selos do usuário por update parcial atômico.
 * Cada método altera só o campo do papel (ou o selo) e nunca regrava o documento inteiro,
 * assim avaliação, penalidade e job não sobrescrevem as alterações uns dos outros.
 */
public interface UserReputationWriter {

    boolean saveDriverReputation(ObjectId userId, DriverReputation reputation, LocalDateTime now);

    boolean savePassengerReputation(ObjectId userId, PassengerReputation reputation, LocalDateTime now);

    /**
     * Grava só a janela, o level e a data de cálculo do motorista, sem tocar na média nem nas avaliações.
     * Devolve false se o usuário não existe ou não tem reputação de motorista.
     */
    boolean saveDriverWindow(ObjectId userId, DriverReputation reputation, LocalDateTime now);

    /** Igual a saveDriverWindow, para o papel de passageiro. */
    boolean savePassengerWindow(ObjectId userId, PassengerReputation reputation, LocalDateTime now);

    /** Concede o selo só se o usuário ainda não o tem. Devolve true quando concedeu. */
    boolean grantBadge(ObjectId userId, Badge badge, LocalDateTime now);

    /** Remove o selo. Devolve true quando algo foi removido. */
    boolean revokeBadge(ObjectId userId, Badge badge);
}