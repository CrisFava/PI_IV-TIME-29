// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/state/auth_action_controller.dart';
import '../../../core/utils/currency_formatter.dart';
import '../../chat/data/models/chat_message_type.dart';
import '../../chat/state/ride_chat_announcer.dart';
import '../../ride/data/models/ride_model.dart';
import '../../ride/data/repository/ride_repository.dart';
import '../../ride/data/repository/ride_repository_impl.dart';
import '../../ride/state/ride_providers.dart';

final driverRideActionsProvider =
    NotifierProvider.autoDispose<DriverRideActionsController, Set<String>>(
      DriverRideActionsController.new,
    );

/// Iniciar, concluir e cancelar as viagens do motorista, e marcar ausentes. O estado são as viagens com uma
/// ação em andamento (para desabilitar os botões). Iniciar e concluir também avisam os
/// passageiros no chat, igual ao botão do próprio chat, para a tela deles se atualizar.
class DriverRideActionsController extends Notifier<Set<String>> {
  RideRepository get _rides => ref.read(rideRepositoryProvider);

  @override
  Set<String> build() => const {};

  /// "Iniciar trajeto": a viagem começa e o motorista entra em expediente.
  /// Devolve a mensagem de erro, ou `null` se deu certo.
  Future<String?> start(String rideId) => _run(
    rideId,
    () => _rides.startRide(rideId),
    announcement: RideChatAnnouncer.rideStarted,
  );

  /// Encerra o expediente concluindo a viagem. Devolve a mensagem de erro, ou `null`.
  Future<String?> complete(String rideId) => _run(
    rideId,
    () => _rides.completeRide(rideId),
    announcement: RideChatAnnouncer.rideCompleted,
  );

  /// "Cancelar esta viagem" (antes de ela começar): os confirmados são avisados.
  /// Devolve a mensagem de erro, ou `null`.
  Future<String?> cancel(String rideId) => _run(
    rideId,
    () => _rides.cancelRide(rideId, reason: 'O motorista cancelou a viagem.'),
  );

  /// Marca um passageiro confirmado como ausente na viagem em andamento.
  /// Devolve a mensagem de erro, ou `null`.
  Future<String?> markNoShow(String rideId, String passengerId) =>
      _run(rideId, () => _rides.markNoShow(rideId, passengerId));

  /// "Confirmar chegada": o passageiro desceu e já recebe a cobrança do rateio.
  /// Ele é avisado no chat com o valor. Devolve a mensagem de erro, ou `null`.
  Future<String?> arrive(String rideId, String passengerId) => _run(
    rideId,
    () => _rides.arrivePassenger(rideId, passengerId),
    afterSuccess: (ride) => _notifyCharged(ride, passengerId),
  );

  /// Executa a ação na API e, se [announcement] vier, avisa os passageiros no chat.
  Future<String?> _run(
    String rideId,
    Future<RideModel> Function() action, {
    String? announcement,
    Future<void> Function(RideModel ride)? afterSuccess,
  }) async {
    if (state.contains(rideId)) return null;
    state = {...state, rideId};

    // O "Encerrar" da home só lê o notifier: sem ninguém observando, o provider seria
    // descartado durante a chamada e o invalidate abaixo falharia depois de a API já ter
    // concluído a viagem, mostrando um erro falso ao motorista.
    final keepAlive = ref.keepAlive();
    try {
      final ride = await action();
      ref.invalidate(myRidesProvider);
      if (announcement != null) await _announce(ride, announcement);
      if (afterSuccess != null) await afterSuccess(ride);
      return null;
    } catch (e) {
      return authErrorMessage(e);
    } finally {
      state = {...state}..remove(rideId);
      keepAlive.close();
    }
  }

  // A cobrança já foi emitida pela API; o chat só avisa o passageiro na hora, com o valor.
  Future<void> _notifyCharged(RideModel ride, String passengerId) async {
    final amount = ride.passengerOf(passengerId)?.finalAmount;
    try {
      await ref.read(rideChatAnnouncerProvider).notifyPassenger(
        ride,
        passengerId,
        ChatMessageType.paymentRequest,
        amount == null
            ? 'Você chegou. A cobrança da carona foi enviada.'
            : 'Você chegou. A cobrança de ${CurrencyFormatter.format(amount)} foi enviada.',
        amount: amount,
      );
    } catch (_) {
      // Sem o aviso; o passageiro vê a cobrança ao abrir o app.
    }
  }

  // O aviso no chat é complementar: a viagem já foi gravada na API, então nenhuma
  // falha dele pode virar erro para o motorista.
  Future<void> _announce(RideModel ride, String text) async {
    try {
      await ref.read(rideChatAnnouncerProvider).announce(ride, text);
    } catch (_) {
      // Sem o aviso; a tela do passageiro se atualiza na próxima consulta.
    }
  }
}
