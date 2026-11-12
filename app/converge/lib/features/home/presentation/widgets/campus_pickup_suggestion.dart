// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:google_maps_flutter/google_maps_flutter.dart';

/// Ponto de embarque conhecido no campus, sugerido ao passageiro.
class CampusPickupSuggestion {
  final String title;
  final String subtitle;
  final LatLng position;

  const CampusPickupSuggestion({
    required this.title,
    required this.subtitle,
    required this.position,
  });
}
