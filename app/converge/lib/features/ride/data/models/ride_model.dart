// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/foundation.dart';

import '../../../route/data/models/route_place_model.dart';
import 'ride_participant_model.dart';
import 'ride_stop_model.dart';
import 'ride_status.dart';

/// Uma viagem concreta, conforme `RideResponseDTO`.
@immutable
///
/// O que cada usuário recebe depende de quem ele é na viagem: o backend esconde dados dos outros
/// passageiros e as paradas que não são do usuário logado.
class RideModel {
  /// Id da viagem no MongoDB.
  final String id;

  /// Número amigável da viagem por motorista (`Ride #042`).
  final int sequentialNumber;

  /// Id do motorista.
  final String driverId;

  /// Trajeto de oferta do motorista que gerou a viagem.
  final String driverRouteId;

  /// Origem da viagem (cópia do trajeto na criação).
  final RoutePlaceModel origin;

  /// Destino da viagem (cópia do trajeto na criação).
  final RoutePlaceModel destination;

  /// Data e hora de saída; nula se não puder ser lida.
  final DateTime? departureDateTime;

  /// Quando a viagem foi concluída; nula até lá.
  final DateTime? completedAt;

  /// Situação da viagem.
  final RideStatus status;

  /// Vagas para passageiros (sem contar o motorista).
  final int totalSeats;

  /// Vagas ainda livres.
  final int availableSeats;

  /// Sala do chat no Firebase Realtime Database. Fica `null` até o motorista
  /// aceitar o primeiro passageiro.
  final String? chatRoomId;

  /// Quem cancelou a viagem (o id do usuário); nulo quando o sistema cancelou.
  final String? cancelledBy;

  /// Modelo do veículo, copiado na criação da viagem.
  final String? vehicleModel;

  /// Placa do veículo, copiada na criação da viagem.
  final String? vehicleLicensePlate;

  /// Cor do veículo, copiada na criação da viagem (ajuda o passageiro a achar o carro).
  final String? vehicleColor;

  /// Lista imutável: quem chamou o construtor não consegue alterá-la depois.
  final List<RideParticipantModel> participants;

  /// Paradas na ordem: buscar e deixar cada passageiro confirmado. O motorista
  /// vê todas; o passageiro, só as próprias.
  final List<RideStopModel> itinerary;

  /// Rota passando pelas paradas (só o motorista recebe).
  final String? itineraryPolyline;

  /// O mesmo caminho em trechos: um até cada parada e o último até o destino.
  /// Vazio sem o Google; aí o app desenha o caminho inteiro de uma vez.
  final List<String> itineraryLegPolylines;

  /// Só para o motorista: o tempo de cada trecho, em segundos, na mesma ordem.
  final List<int> itineraryLegSeconds;

  /// Viagem em andamento: a parada em que ela está. O trecho atual é
  /// `itineraryLegPolylines[itineraryNextStop]` (o último leva ao destino).
  final int? itineraryNextStop;

  /// Para o passageiro: onde ficam o embarque e o desembarque dele no itinerário.
  final int? itineraryMyPickupStop;

  /// Para o passageiro: posição do desembarque dele no itinerário.
  final int? itineraryMyDropOffStop;

  /// Para o passageiro, com as paradas dos outros: quanto o motorista leva até ele
  /// e quanto dura o trecho com ele no carro.
  final int? itineraryMyPickupMinutes;
  final int? itineraryMyRideMinutes;

  /// Distância da rota com as paradas, em km.
  final double? itineraryDistanceKm;

  /// Chegada estimada ao destino, já contando o tempo das paradas.
  final DateTime? estimatedArrivalDateTime;

  RideModel({
    required this.id,
    required this.sequentialNumber,
    required this.driverId,
    required this.driverRouteId,
    required this.origin,
    required this.destination,
    required this.departureDateTime,
    required this.completedAt,
    required this.status,
    required this.totalSeats,
    required this.availableSeats,
    required this.chatRoomId,
    this.cancelledBy,
    required this.vehicleModel,
    required this.vehicleLicensePlate,
    this.vehicleColor,
    required List<RideParticipantModel> participants,
    List<RideStopModel> itinerary = const [],
    this.itineraryPolyline,
    this.itineraryDistanceKm,
    this.estimatedArrivalDateTime,
    List<String> itineraryLegPolylines = const [],
    List<int> itineraryLegSeconds = const [],
    this.itineraryNextStop,
    this.itineraryMyPickupStop,
    this.itineraryMyDropOffStop,
    this.itineraryMyPickupMinutes,
    this.itineraryMyRideMinutes,
  }) : participants = List.unmodifiable(participants),
       itinerary = List.unmodifiable(itinerary),
       itineraryLegPolylines = List.unmodifiable(itineraryLegPolylines),
       itineraryLegSeconds = List.unmodifiable(itineraryLegSeconds);

  /// Se o usuário é o motorista da viagem.
  bool isDrivenBy(String userId) => driverId == userId;

  /// Nome do motorista; "Motorista" se a API não mandou o nome.
  String get driverName => participants
      .where((participant) => participant.isDriver)
      .map((participant) => participant.displayName)
      .firstWhere((_) => true, orElse: () => 'Motorista');

  /// Passageiros com quem o chat está liberado: os que o motorista aceitou.
  /// Só traz quem o usuário logado pode identificar (o backend esconde o id dos demais).
  List<RideParticipantModel> get passengersWithSeat => [
    for (final participant in participants)
      if (participant.isPassenger &&
          participant.status.hasSeat &&
          participant.userId != null)
        participant,
  ];

  /// Todos os passageiros com vaga, inclusive os que vêm sem id (para contar e
  /// mostrar os nomes do grupo).
  List<RideParticipantModel> get seatedPassengers => [
    for (final participant in participants)
      if (participant.isPassenger && participant.status.hasSeat) participant,
  ];

  /// A participação de um passageiro. Quem saiu e pediu vaga de novo aparece
  /// mais de uma vez: vale a participação que está com a vaga.
  RideParticipantModel? passengerOf(String userId) {
    final own = participants
        .where((p) => p.isPassenger && p.userId == userId)
        .toList();
    if (own.isEmpty) return null;
    return own.firstWhere((p) => p.status.hasSeat, orElse: () => own.last);
  }

  /// Lê a viagem do JSON da API. Campos ausentes viram valores vazios e itens malformados das listas
  /// são ignorados, para uma resposta incompleta não derrubar a tela.
  factory RideModel.fromJson(Map<String, dynamic> json) {
    final vehicle = json['vehicleSnapshot'];
    final participants = json['participants'];
    final itinerary = json['itinerary'];
    return RideModel(
      id: json['id'] as String? ?? '',
      sequentialNumber: (json['sequentialNumber'] as num?)?.toInt() ?? 0,
      driverId: json['driverId'] as String? ?? '',
      driverRouteId: json['driverRouteId'] as String? ?? '',
      origin: RoutePlaceModel.fromJson(
        (json['origin'] as Map<String, dynamic>?) ?? const {},
      ),
      destination: RoutePlaceModel.fromJson(
        (json['destination'] as Map<String, dynamic>?) ?? const {},
      ),
      departureDateTime: DateTime.tryParse(
        json['departureDateTime'] as String? ?? '',
      ),
      completedAt: DateTime.tryParse(json['completedAt'] as String? ?? ''),
      status: RideStatus.fromJson(json['status']),
      totalSeats: (json['totalSeats'] as num?)?.toInt() ?? 0,
      availableSeats: (json['availableSeats'] as num?)?.toInt() ?? 0,
      chatRoomId: json['chatRoomId'] as String?,
      cancelledBy: json['cancelledBy'] as String?,
      vehicleModel: vehicle is Map<String, dynamic>
          ? vehicle['model'] as String?
          : null,
      vehicleLicensePlate: vehicle is Map<String, dynamic>
          ? vehicle['licensePlate'] as String?
          : null,
      vehicleColor: vehicle is Map<String, dynamic>
          ? vehicle['color'] as String?
          : null,
      participants: [
        if (participants is List)
          for (final participant in participants)
            if (participant is Map<String, dynamic>)
              RideParticipantModel.fromJson(participant),
      ],
      itinerary: [
        if (itinerary is List)
          for (final stop in itinerary)
            if (stop is Map<String, dynamic>) RideStopModel.fromJson(stop),
      ],
      itineraryPolyline: json['itineraryPolyline'] as String?,
      itineraryDistanceKm: (json['itineraryDistanceKm'] as num?)?.toDouble(),
      estimatedArrivalDateTime: DateTime.tryParse(
        json['estimatedArrivalDateTime'] as String? ?? '',
      ),
      itineraryNextStop: (json['itineraryNextStop'] as num?)?.toInt(),
      itineraryMyPickupStop: (json['itineraryMyPickupStop'] as num?)?.toInt(),
      itineraryMyDropOffStop: (json['itineraryMyDropOffStop'] as num?)?.toInt(),
      itineraryMyPickupMinutes: (json['itineraryMyPickupMinutes'] as num?)
          ?.toInt(),
      itineraryMyRideMinutes: (json['itineraryMyRideMinutes'] as num?)?.toInt(),
      itineraryLegPolylines: [
        if (json['itineraryLegPolylines'] case final List legs)
          for (final leg in legs)
            if (leg is String) leg,
      ],
      itineraryLegSeconds: [
        if (json['itineraryLegSeconds'] case final List seconds)
          for (final value in seconds)
            if (value is num) value.toInt(),
      ],
    );
  }
}
