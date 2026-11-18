// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

import 'package:converge/core/network/api_error_code.dart';
import 'package:converge/core/network/api_exception.dart';
import 'package:converge/features/security/data/models/block_model.dart';
import 'package:converge/features/security/data/models/create_report_request.dart';
import 'package:converge/features/security/data/models/report_model.dart';
import 'package:converge/features/security/data/models/report_status.dart';
import 'package:converge/features/security/data/repository/security_repository.dart';

/// Bloqueios e denúncias em memória. Repete as regras do backend: bloqueio é idempotente
/// e a mesma denúncia pendente não pode ser enviada duas vezes.
class FakeSecurityRepository implements SecurityRepository {
  final List<BlockModel> blocks = [];
  final List<ReportModel> reports = [];

  /// Quando preenchido, a próxima chamada que muda dados falha com este erro.
  ApiException? failNext;

  void _failIfNeeded() {
    final error = failNext;
    if (error == null) return;
    failNext = null;
    throw error;
  }

  @override
  Future<BlockModel> blockUser(String blockedUserId) async {
    _failIfNeeded();
    for (final block in blocks) {
      if (block.blockedUserId == blockedUserId) return block;
    }
    final block = BlockModel(
      id: 'block-${blocks.length + 1}',
      blockedUserId: blockedUserId,
      blockedUserName: null,
      createdAt: DateTime.utc(2026, 11, 16, 9),
    );
    blocks.add(block);
    return block;
  }

  @override
  Future<void> unblockUser(String blockedUserId) async {
    _failIfNeeded();
    blocks.removeWhere((block) => block.blockedUserId == blockedUserId);
  }

  @override
  Future<List<BlockModel>> listBlocks() async => List.of(blocks);

  @override
  Future<ReportModel> createReport(CreateReportRequest request) async {
    _failIfNeeded();
    final duplicated = reports.any(
      (report) =>
          report.reportedId == request.reportedId &&
          report.rideId == request.rideId &&
          report.reason == request.reason &&
          report.status == ReportStatus.pending,
    );
    if (duplicated) {
      throw const ApiException(
        code: ApiErrorCode.badRequest,
        message: 'Você já fez essa denúncia e ela ainda está em análise.',
        status: 400,
        fields: {'reportedId'},
      );
    }
    final report = ReportModel(
      id: 'report-${reports.length + 1}',
      reportedId: request.reportedId,
      rideId: request.rideId,
      reason: request.reason,
      description: request.description,
      status: ReportStatus.pending,
      createdAt: DateTime.utc(2026, 11, 16, 9),
    );
    reports.insert(0, report);
    return report;
  }

  @override
  Future<List<ReportModel>> listMyReports() async => List.of(reports);
}
