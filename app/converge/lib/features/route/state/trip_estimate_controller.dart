// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../core/services/maps/google_maps_service.dart';
import '../data/models/route_estimate_model.dart';
import '../data/repository/route_repository_impl.dart';
import 'trip_estimate_state.dart';

/// Tempo, distância e desenho do caminho do rascunho de carona (etapa 3), pelo
/// backend (Routes API do Google). É o mesmo para motorista e passageiro.
final tripEstimateProvider =
    NotifierProvider<TripEstimateController, TripEstimateState>(
      TripEstimateController.new,
    );

class TripEstimateController extends Notifier<TripEstimateState> {
  // Muda a cada pedido: uma resposta antiga (rascunho fechado ou outro destino)
  // não sobrescreve a atual
  int _request = 0;

  @override
  TripEstimateState build() => const TripEstimateState();

  /// Calcula o caminho entre a partida e o destino.
  Future<void> load({
    required LatLng origin,
    required LatLng destination,
  }) async {
    final request = ++_request;
    state = const TripEstimateState(isLoading: true);

    RouteEstimateModel? estimate;
    try {
      estimate = await ref
          .read(routeRepositoryProvider)
          .estimate(
            originLatitude: origin.latitude,
            originLongitude: origin.longitude,
            destinationLatitude: destination.latitude,
            destinationLongitude: destination.longitude,
          );
    } catch (e) {
      debugPrint('Não foi possível estimar o trajeto: $e');
    }
    if (!ref.mounted || request != _request) return;

    var points = <LatLng>[];
    final polyline = estimate?.encodedPolyline;
    if (polyline != null && polyline.isNotEmpty) {
      points = GoogleMapsService.decodePolyline(polyline);
    } else {
      // Sem o desenho do backend, o app calcula a rota só para o mapa
      try {
        points = await GoogleMapsService.computeRoutes(
          origin: origin,
          destination: destination,
        );
      } catch (_) {}
      if (!ref.mounted || request != _request) return;
    }

    state = TripEstimateState(estimate: estimate, points: points);
  }

  /// Rascunho fechado ou partida/destino trocados: descarta o cálculo em andamento.
  void reset() {
    _request++;
    state = const TripEstimateState();
  }
}
