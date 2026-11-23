// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/create_report_request.dart';
import '../data/models/report_model.dart';
import '../data/models/report_reason.dart';
import '../data/repository/security_repository.dart';
import '../data/repository/security_repository_impl.dart';

/// Denúncias que o usuário logado já fez.
final reportsProvider =
    AsyncNotifierProvider.autoDispose<ReportController, List<ReportModel>>(
      ReportController.new,
    );

class ReportController extends AsyncNotifier<List<ReportModel>> {
  SecurityRepository get _repository => ref.read(securityRepositoryProvider);

  @override
  Future<List<ReportModel>> build() => _repository.listMyReports();

  /// Envia a denúncia e a inclui na lista. Erros são propagados para a tela exibir a mensagem.
  Future<ReportModel> submit({
    required String reportedId,
    required ReportReason reason,
    String? description,
    String? rideId,
  }) async {
    final report = await _repository.createReport(
      CreateReportRequest(
        reportedId: reportedId,
        reason: reason,
        description: description,
        rideId: rideId,
      ),
    );
    if (ref.mounted) {
      state = AsyncData([report, ...state.value ?? const <ReportModel>[]]);
    }
    return report;
  }
}
