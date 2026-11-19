// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

import '../models/block_model.dart';
import '../models/create_report_request.dart';
import '../models/report_model.dart';

/// Contrato com a API de bloqueios (`/blocks`) e denúncias (`/reports`).
abstract class SecurityRepository {
  /// Bloqueia um usuário. Repetir o pedido devolve o mesmo bloqueio.
  Future<BlockModel> blockUser(String blockedUserId);

  /// Desbloqueia um usuário. Não dá erro se ele não estava bloqueado.
  Future<void> unblockUser(String blockedUserId);

  /// Usuários que o usuário logado bloqueou.
  Future<List<BlockModel>> listBlocks();

  /// Denuncia outro usuário.
  Future<ReportModel> createReport(CreateReportRequest request);

  /// Denúncias feitas pelo usuário logado, da mais recente para a mais antiga.
  Future<List<ReportModel>> listMyReports();
}
