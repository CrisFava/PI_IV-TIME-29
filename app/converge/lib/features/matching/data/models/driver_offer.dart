// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

import '../../../ride/data/models/ride_model.dart';
import '../../../route/data/models/route_response_model.dart';

/// Uma carona que o motorista oferece: o trajeto de oferta publicado e as viagens
/// que já saíram dele (uma por data em que alguém pediu vaga).
/// Sem [route], é uma viagem cuja oferta foi cancelada mas que ainda tem passageiros.
@immutable
class DriverOffer {
  final RouteResponseModel? route;
  final List<RideModel> rides;

  DriverOffer({required this.route, required List<RideModel> rides})
    : rides = List.unmodifiable(rides);

  String get key => route?.id ?? 'ride:${rides.first.id}';
}
