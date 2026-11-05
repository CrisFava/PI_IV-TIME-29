// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.domain.repository;

import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.PlatformIndicator;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Autora: Daniela Mikie Kikuchi Gonçalves
// Acesso ao banco dos indicadores. O Spring gera a implementação pelos nomes dos métodos.
@Repository
public interface PlatformIndicatorRepository extends MongoRepository<PlatformIndicator, ObjectId> {

    // Indicador de um período específico (ex.: MONTHLY + "2025-05").
    Optional<PlatformIndicator> findByPeriodAndReference(IndicatorPeriod period, String reference);

    // Indicador mais recente de um período (quando não informam a referência).
    Optional<PlatformIndicator> findFirstByPeriodOrderByReferenceDesc(IndicatorPeriod period);
}
