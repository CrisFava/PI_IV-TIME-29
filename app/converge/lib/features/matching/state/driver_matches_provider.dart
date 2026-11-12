// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../route/data/models/create_route_request.dart';
import '../../route/data/models/route_response_model.dart';
import '../../route/data/repository/route_repository_impl.dart';
import '../data/models/match_model.dart';
import '../data/repository/matching_repository_impl.dart';

/// Trajetos de oferta ativos do motorista logado: é com eles que ele aparece no matching.
final myActiveOfferRoutesProvider =
    FutureProvider.autoDispose<List<RouteResponseModel>>((ref) async {
      final routes = await ref.watch(routeRepositoryProvider).listMyRoutes();
      return [
        for (final route in routes)
          if (route.type == AppRouteType.offer && route.status == 'ACTIVE')
            route,
      ];
    });

/// Passageiros compatíveis com um trajeto de oferta do motorista ("N passageiros x 1 motorista").
final driverMatchesProvider = FutureProvider.autoDispose
    .family<List<MatchModel>, String>(
      (ref, offerRouteId) =>
          ref.watch(matchingRepositoryProvider).findPassengersFor(offerRouteId),
    );
