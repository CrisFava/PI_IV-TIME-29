// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../data/models/route_estimate_model.dart';

/// Prévia do caminho do rascunho de carona: o tempo e a distância (nulos se não
/// deu para calcular) e o desenho para o mapa.
@immutable
class TripEstimateState {
  final RouteEstimateModel? estimate;
  final List<LatLng> points;
  final bool isLoading;

  const TripEstimateState({
    this.estimate,
    this.points = const [],
    this.isLoading = false,
  });
}
