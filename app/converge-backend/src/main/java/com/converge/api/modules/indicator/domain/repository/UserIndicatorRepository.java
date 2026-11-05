// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.domain.repository;

import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.UserIndicator;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Acesso ao banco dos indicadores por usuário.
@Repository
public interface UserIndicatorRepository extends MongoRepository<UserIndicator, ObjectId> {

    // Indicador de um usuário em um período específico (ex.: MONTHLY + "2025-05").
    Optional<UserIndicator> findByUserIdAndPeriodAndReference(String userId, IndicatorPeriod period, String reference);

    // Indicador mais recente do usuário num período (quando não informam a referência).
    Optional<UserIndicator> findFirstByUserIdAndPeriodOrderByReferenceDesc(String userId, IndicatorPeriod period);

    // Todos os documentos mensais do usuário, usados para montar o acumulado histórico.
    List<UserIndicator> findByUserIdAndPeriod(String userId, IndicatorPeriod period);
}
