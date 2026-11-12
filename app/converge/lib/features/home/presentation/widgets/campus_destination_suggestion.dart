// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:google_maps_flutter/google_maps_flutter.dart';

/// Destino frequente sugerido ao passageiro.
class CampusDestinationSuggestion {
  final String title;
  final String subtitle;
  final LatLng position;

  const CampusDestinationSuggestion({
    required this.title,
    required this.subtitle,
    required this.position,
  });
}
