// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../data/models/create_route_request.dart';
import '../data/models/route_response_model.dart';
import '../data/repository/route_repository.dart';
import '../data/repository/route_repository_impl.dart';

/// `true` enquanto uma carona está sendo publicada (o botão fica desabilitado).
final offerRouteProvider = NotifierProvider<OfferRouteController, bool>(
  OfferRouteController.new,
);

/// Publica as caronas agendadas do motorista: cada uma é um trajeto de oferta
/// com dia e horário, e é com ele que o motorista aparece no matching.
class OfferRouteController extends Notifier<bool> {
  RouteRepository get _routes => ref.read(routeRepositoryProvider);

  @override
  bool build() => false;

  /// Publica a carona e devolve o trajeto salvo. Lança `ApiException` se não conseguir.
  Future<RouteResponseModel> publish(CreateRouteRequest request) async {
    state = true;
    try {
      return await _routes.createRoute(request);
    } on ApiException catch (e) {
      // A mesma carona já estava publicada (mesma origem, destino e horário): usa a que existe
      if (!e.isConflict) rethrow;
      final existing = _findSame(await _routes.listMyRoutes(), request);
      if (existing == null) rethrow;
      return existing;
    } finally {
      if (ref.mounted) state = false;
    }
  }

  /// Cancela a carona oferecida: ela sai do matching e não recebe novos pedidos.
  /// Viagens que já têm passageiros confirmados continuam.
  Future<void> cancel(String routeId) => _routes.deleteRoute(routeId);

  static RouteResponseModel? _findSame(
    List<RouteResponseModel> routes,
    CreateRouteRequest request,
  ) {
    for (final route in routes) {
      if (route.type == request.type &&
          route.status == 'ACTIVE' &&
          route.origin.address == request.origin.address &&
          route.destination.address == request.destination.address &&
          route.departureTime == request.departureTime) {
        return route;
      }
    }
    return null;
  }
}
