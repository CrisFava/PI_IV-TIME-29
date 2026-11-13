// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import '../models/match_model.dart';

/// Contrato com a API de matching (`/matchings`).
abstract class MatchingRepository {
  /// Motoristas compatíveis com um trajeto de procura do usuário, já ordenados.
  Future<List<MatchModel>> findDriversFor(String searchRouteId);

  /// Passageiros compatíveis com um trajeto de oferta do usuário, já ordenados.
  Future<List<MatchModel>> findPassengersFor(String offerRouteId);
}
