// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.domain.repository;

import com.converge.api.modules.security.domain.enums.ReportReason;
import com.converge.api.modules.security.domain.enums.ReportStatus;
import com.converge.api.modules.security.domain.model.Report;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends MongoRepository<Report, ObjectId> {

    // Denúncias feitas pelo usuário, da mais recente para a mais antiga
    List<Report> findByReporterIdOrderByCreatedAtDesc(String reporterId);

    // Já existe denúncia igual ainda aberta (mesmo autor, denunciado, viagem e motivo)?
    boolean existsByReporterIdAndReportedIdAndRideIdAndReasonAndStatus(
            String reporterId, String reportedId, String rideId, ReportReason reason, ReportStatus status);
}
