// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../ride/data/models/participant_status.dart';
import '../../ride/state/ride_providers.dart';
import '../data/models/driver_ride_request.dart';
import 'driver_ride_requests_provider.dart';
import 'matching_viewer_provider.dart';

/// Pedido de vaga mostrado no aviso do motorista em expediente (ou `null`).
/// Só existe enquanto alguém o observa: a tela observa durante o expediente.
final incomingRideRequestProvider =
    NotifierProvider.autoDispose<
      IncomingRideRequestController,
      DriverRideRequest?
    >(IncomingRideRequestController.new);

/// Acompanha os pedidos de vaga durante o expediente e mostra um por vez,
/// do mais antigo para o mais novo, até o motorista responder ou dispensar.
class IncomingRideRequestController extends Notifier<DriverRideRequest?> {
  // Pedidos já respondidos ou dispensados neste expediente
  final Set<String> _handled = {};

  @override
  DriverRideRequest? build() {
    // Consulta as viagens de tempos em tempos para os pedidos novos chegarem sozinhos
    final timer = Timer.periodic(
      ref.read(matchingPollIntervalProvider),
      (_) => ref.invalidate(myRidesProvider),
    );
    ref.onDispose(timer.cancel);

    ref.listen(
      driverRideRequestsProvider,
      (_, next) => _showNext(next.value ?? const []),
    );
    return _nextPending(
      ref.read(driverRideRequestsProvider).value ?? const [],
      current: null,
    );
  }

  /// O motorista respondeu ou fechou o aviso: passa para o próximo pedido, se houver.
  void dismiss(DriverRideRequest request) {
    _handled.add(request.key);
    if (state?.key == request.key) state = null;
    _showNext(ref.read(driverRideRequestsProvider).value ?? const []);
  }

  void _showNext(List<DriverRideRequest> requests) {
    state = _nextPending(requests, current: state);
  }

  /// O pedido atual, enquanto continuar pendente; senão, o primeiro pendente não respondido.
  DriverRideRequest? _nextPending(
    List<DriverRideRequest> requests, {
    required DriverRideRequest? current,
  }) {
    final pending = [
      for (final request in requests)
        if (request.passenger.status == ParticipantStatus.requested &&
            !_handled.contains(request.key))
          request,
    ];

    if (current != null) {
      for (final request in pending) {
        if (request.key == current.key) return request;
      }
    }
    return pending.isEmpty ? null : pending.first;
  }
}
