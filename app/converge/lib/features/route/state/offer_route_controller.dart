// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../ride/state/payment_prompt_controller.dart';
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

  /// Publica a carona e devolve o trajeto salvo. Lança `ApiException` se não
  /// conseguir, inclusive quando já existe uma carona igual (409
  /// ROUTE_ALREADY_EXISTS): o motorista precisa ver o aviso, e uma segunda carona
  /// igual dividiria os passageiros.
  Future<RouteResponseModel> publish(CreateRouteRequest request) async {
    state = true;
    try {
      return await _routes.createRoute(request);
    } catch (e) {
      // Pagamento de uma carona em aberto: o pagamento volta a aparecer
      ref.read(paymentPromptProvider.notifier).handleBlockedAction(e);
      rethrow;
    } finally {
      if (ref.mounted) state = false;
    }
  }

  /// Cancela a carona oferecida: ela sai do matching e não recebe novos pedidos.
  /// Viagens que já têm passageiros confirmados continuam.
  Future<void> cancel(String routeId) => _routes.deleteRoute(routeId);
}
