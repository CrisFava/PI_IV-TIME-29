import 'package:google_maps_flutter/google_maps_flutter.dart';

/// Passos do fluxo de solicitação de carona pelo passageiro (Uber-like).
enum PassengerRideStep {
  /// Estado de repouso: mapa limpo, sem motoristas, botão "Pedir carona".
  idle,

  /// Passo 1: Seleção do Ponto de Embarque (Origem).
  selectPickup,

  /// Passo 2: Arrasto do mapa estilo Uber com pino fixo para definir o Embarque.
  dragMapPickup,

  /// Passo 3: Busca/Seleção do Ponto de Desembarque (Destino).
  searchDestination,

  /// Passo 4: Arrasto do mapa estilo Uber com pino fixo para definir o Desembarque.
  dragMapDestination,

  /// Passo 5: Confirmação prévia com o trajeto desenhado no mapa antes de confirmar.
  routeConfirmation,

  /// Passo 6: Pedido confirmado! Abre a lista de motoristas disponíveis na área.
  driverList,

  /// Passo 7: Aguardando aceite do motorista selecionado (com simulação).
  waitingDriver,

  /// Passo 8: Motorista aceitou! Exibe localização dele, rota em cinza claro e botão de mensagem.
  driverAccepted,
}

/// Modelo de motorista próximo para o fluxo de passageiro.
class MockNearbyDriver {
  final String id;
  final String name;
  final String vehicleModel;
  final String licensePlate;
  final LatLng position;
  final String currentRoute;
  final double rating;
  final int availableSeats;
  final double contributionAmount;

  const MockNearbyDriver({
    required this.id,
    required this.name,
    required this.vehicleModel,
    required this.licensePlate,
    required this.position,
    required this.currentRoute,
    required this.rating,
    required this.availableSeats,
    required this.contributionAmount,
  });
}

/// Sugestão de local de embarque acadêmico / campus mockado.
class MockPickupSuggestion {
  final String title;
  final String subtitle;
  final LatLng position;

  const MockPickupSuggestion({
    required this.title,
    required this.subtitle,
    required this.position,
  });
}

const List<MockPickupSuggestion> mockPickupSuggestionsList = [
  MockPickupSuggestion(
    title: 'PUC-Campinas - Portaria Principal',
    subtitle: 'Av. Rebouças - Portão 1 (Embarque geral)',
    position: LatLng(-22.8335, -47.0512),
  ),
  MockPickupSuggestion(
    title: 'PUC-Campinas - Portaria 2 (Engenharia)',
    subtitle: 'Próximo ao Prédio H1 e Laboratórios',
    position: LatLng(-22.8315, -47.0490),
  ),
  MockPickupSuggestion(
    title: 'PUC-Campinas - Estacionamento Central',
    subtitle: 'Vagas cobertas em frente ao Centro Esportivo',
    position: LatLng(-22.8350, -47.0530),
  ),
  MockPickupSuggestion(
    title: 'Biblioteca Central / Centro Comunitário',
    subtitle: 'Praça de Convivência e Reitoria',
    position: LatLng(-22.8328, -47.0520),
  ),
];

/// Sugestão de destino acadêmico / frequente mockado.
class MockDestinationSuggestion {
  final String title;
  final String subtitle;
  final LatLng position;

  const MockDestinationSuggestion({
    required this.title,
    required this.subtitle,
    required this.position,
  });
}

/// Coordenadas centrais acadêmicas (Campus PUC-Campinas / Barão Geraldo)
const LatLng mockCampusCoordinates = LatLng(-22.8338, -47.0519);

/// Sugestões de destinos frequentes para o fluxo de busca
const List<MockDestinationSuggestion> mockDestinationsList = [
  MockDestinationSuggestion(
    title: 'PUC-Campinas - Campus II (Saúde)',
    subtitle: 'Av. John Boyd Dunlop, s/n - Jd. Ipaussurama',
    position: LatLng(-22.9288, -47.0789),
  ),
  MockDestinationSuggestion(
    title: 'Terminal Central de Barão Geraldo',
    subtitle: 'R. Luiz Vicentin, Barão Geraldo - Campinas',
    position: LatLng(-22.8228, -47.0864),
  ),
  MockDestinationSuggestion(
    title: 'Parque Taquaral (Portão 1)',
    subtitle: 'Av. Dr. Heitor Penteado - Taquaral',
    position: LatLng(-22.8732, -47.0558),
  ),
  MockDestinationSuggestion(
    title: 'Terminal Rodoviário Ramos de Azevedo',
    subtitle: 'Rua Dr. Pereira Lima, 85 - Vila Industrial',
    position: LatLng(-22.9038, -47.0658),
  ),
];

/// Lista restrita de poucos motoristas mockados na área para seleção pelo passageiro
final List<MockNearbyDriver> mockNearbyDriversList = [
  const MockNearbyDriver(
    id: 'mock_driver_1',
    name: 'Lucas Silveira',
    vehicleModel: 'Onix Preto',
    licensePlate: 'ABC-4E21',
    position: LatLng(-22.8315, -47.0495),
    currentRoute: 'Barão Geraldo → Campus Central',
    rating: 4.9,
    availableSeats: 3,
    contributionAmount: 6.00,
  ),
  const MockNearbyDriver(
    id: 'mock_driver_2',
    name: 'Mariana Costa',
    vehicleModel: 'HB20 Prata',
    licensePlate: 'XYZ-8K90',
    position: LatLng(-22.8360, -47.0545),
    currentRoute: 'Taquaral → Campus Central',
    rating: 5.0,
    availableSeats: 2,
    contributionAmount: 5.50,
  ),
];
