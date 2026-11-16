// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/route_estimate_model.dart';
import '../data/repository/route_repository_impl.dart';

/// Partida e destino de uma prévia de rota, em latitude e longitude.
typedef RouteEstimateKey = ({
  double originLatitude,
  double originLongitude,
  double destinationLatitude,
  double destinationLongitude,
});

/// Tempo e distância de carro entre a partida e o destino escolhidos (Routes API
/// do Google, pelo backend). Mostrado na etapa de horário, antes de publicar ou pedir.
final routeEstimateProvider = FutureProvider.autoDispose
    .family<RouteEstimateModel, RouteEstimateKey>(
      (ref, key) => ref
          .watch(routeRepositoryProvider)
          .estimate(
            originLatitude: key.originLatitude,
            originLongitude: key.originLongitude,
            destinationLatitude: key.destinationLatitude,
            destinationLongitude: key.destinationLongitude,
          ),
    );
