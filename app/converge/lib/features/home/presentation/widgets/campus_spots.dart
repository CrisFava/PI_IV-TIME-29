// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:google_maps_flutter/google_maps_flutter.dart';

import 'campus_destination_suggestion.dart';
import 'campus_pickup_suggestion.dart';

/// Coordenadas centrais acadêmicas (Campus PUC-Campinas / Barão Geraldo)
const LatLng campusCoordinates = LatLng(-22.8338, -47.0519);

/// Pontos de embarque sugeridos no campus.
const List<CampusPickupSuggestion> campusPickupSuggestions = [
  CampusPickupSuggestion(
    title: 'PUC-Campinas - Portaria Principal',
    subtitle: 'Av. Rebouças - Portão 1 (Embarque geral)',
    position: LatLng(-22.8335, -47.0512),
  ),
  CampusPickupSuggestion(
    title: 'PUC-Campinas - Portaria 2 (Engenharia)',
    subtitle: 'Próximo ao Prédio H1 e Laboratórios',
    position: LatLng(-22.8315, -47.0490),
  ),
  CampusPickupSuggestion(
    title: 'PUC-Campinas - Estacionamento Central',
    subtitle: 'Vagas cobertas em frente ao Centro Esportivo',
    position: LatLng(-22.8350, -47.0530),
  ),
  CampusPickupSuggestion(
    title: 'Biblioteca Central / Centro Comunitário',
    subtitle: 'Praça de Convivência e Reitoria',
    position: LatLng(-22.8328, -47.0520),
  ),
];

/// Sugestões de destinos frequentes para o fluxo de busca
const List<CampusDestinationSuggestion> campusDestinationSuggestions = [
  CampusDestinationSuggestion(
    title: 'PUC-Campinas - Campus II (Saúde)',
    subtitle: 'Av. John Boyd Dunlop, s/n - Jd. Ipaussurama',
    position: LatLng(-22.9288, -47.0789),
  ),
  CampusDestinationSuggestion(
    title: 'Terminal Central de Barão Geraldo',
    subtitle: 'R. Luiz Vicentin, Barão Geraldo - Campinas',
    position: LatLng(-22.8228, -47.0864),
  ),
  CampusDestinationSuggestion(
    title: 'Parque Taquaral (Portão 1)',
    subtitle: 'Av. Dr. Heitor Penteado - Taquaral',
    position: LatLng(-22.8732, -47.0558),
  ),
  CampusDestinationSuggestion(
    title: 'Terminal Rodoviário Ramos de Azevedo',
    subtitle: 'Rua Dr. Pereira Lima, 85 - Vila Industrial',
    position: LatLng(-22.9038, -47.0658),
  ),
];
