// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../route/data/models/route_estimate_model.dart';
import '../../route/data/models/route_place_model.dart';
import '../../route/data/repository/route_repository_impl.dart';

/// O caminho do passageiro, do embarque ao desembarque, calculado pelo backend
/// (Routes API do Google). É só a parte dele: as paradas dos outros não entram.
final passengerSegmentProvider = FutureProvider.autoDispose
    .family<RouteEstimateModel, ({RoutePlaceModel from, RoutePlaceModel to})>(
      (ref, segment) => ref
          .read(routeRepositoryProvider)
          .estimate(
            originLatitude: segment.from.latitude,
            originLongitude: segment.from.longitude,
            destinationLatitude: segment.to.latitude,
            destinationLongitude: segment.to.longitude,
          ),
    );
