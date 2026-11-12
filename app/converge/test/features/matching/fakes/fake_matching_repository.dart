// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/core/network/api_error_code.dart';
import 'package:converge/core/network/api_exception.dart';
import 'package:converge/features/matching/data/models/match_model.dart';
import 'package:converge/features/matching/data/repository/matching_repository.dart';

/// Repositório de matching em memória.
class FakeMatchingRepository implements MatchingRepository {
  List<MatchModel> drivers;
  List<MatchModel> passengers;

  /// Quando preenchido, a próxima busca falha com esta mensagem.
  String? failNextWith;

  final List<String> calls = [];

  FakeMatchingRepository({this.drivers = const [], this.passengers = const []});

  @override
  Future<List<MatchModel>> findDriversFor(String searchRouteId) async {
    calls.add('findDriversFor:$searchRouteId');
    _maybeFail();
    return drivers;
  }

  @override
  Future<List<MatchModel>> findPassengersFor(String offerRouteId) async {
    calls.add('findPassengersFor:$offerRouteId');
    _maybeFail();
    return passengers;
  }

  void _maybeFail() {
    final message = failNextWith;
    if (message == null) return;
    failNextWith = null;
    throw ApiException(
      code: ApiErrorCode.badRequest,
      message: message,
      status: 400,
    );
  }
}
