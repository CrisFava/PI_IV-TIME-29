// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../ride/data/models/ride_preview_model.dart';
import '../../ride/data/repository/ride_repository_impl.dart';

/// Como a viagem ficaria aceitando o pedido de um passageiro: os dois caminhos e o desvio.
final rideRequestPreviewProvider = FutureProvider.autoDispose
    .family<RidePreviewModel, ({String rideId, String passengerId})>(
      (ref, key) => ref
          .read(rideRepositoryProvider)
          .previewPassenger(key.rideId, key.passengerId),
    );
