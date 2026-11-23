// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

import '../../ride/data/models/participant_status.dart';
import '../../ride/data/models/ride_model.dart';
import '../../ride/data/models/ride_participant_model.dart';
import '../../ride/data/models/ride_status.dart';

/// Um aviso de cancelamento para mostrar no topo da tela.
@immutable
class RideCancellationNotice {
  final String key;
  final String title;
  final String message;

  /// O aviso é para quem dirige (abre "Minhas caronas" como motorista).
  final bool forDriver;

  const RideCancellationNotice({
    required this.key,
    required this.title,
    required this.message,
    required this.forDriver,
  });
}

/// O que mudou entre duas leituras das viagens de [viewerId] e merece aviso: a
/// carona ou a vaga cancelada pelo outro lado. O que o próprio usuário fez (sair
/// da viagem, tirar um passageiro, cancelar a carona) não gera aviso para ele.
List<RideCancellationNotice> rideCancellationNotices({
  required List<RideModel> before,
  required List<RideModel> after,
  required String viewerId,
}) {
  final notices = <RideCancellationNotice>[];
  final nowById = {for (final ride in after) ride.id: ride};

  for (final old in before) {
    final now = nowById[old.id];
    final when = _whenLabel(old.departureDateTime);

    if (old.isDrivenBy(viewerId)) {
      // A carona foi cancelada pelo sistema (o motorista não a cancelou)
      if (now == null || now.status == RideStatus.cancelled) {
        final wasOpen = old.status != RideStatus.cancelled;
        if (wasOpen && now != null && now.cancelledBy != viewerId) {
          notices.add(
            RideCancellationNotice(
              key: 'ride_cancelled_${old.id}',
              title: 'Carona cancelada',
              message: 'Sua carona $when foi cancelada.',
              forDriver: true,
            ),
          );
        }
        continue;
      }
      // Um passageiro saiu da viagem ou desistiu do pedido
      for (final passenger in old.participants) {
        if (!passenger.isPassenger || passenger.userId == null) continue;
        final was = passenger.status;
        if (was != ParticipantStatus.confirmed &&
            was != ParticipantStatus.requested) {
          continue;
        }
        final current = _participationOf(now, passenger.userId!, since: was);
        if (current == null ||
            current.status != ParticipantStatus.cancelled ||
            current.removedByDriver) {
          continue;
        }
        final name = passenger.displayName.split(' ').first;
        notices.add(
          RideCancellationNotice(
            key: 'passenger_left_${old.id}_${passenger.userId}',
            title: was == ParticipantStatus.confirmed
                ? 'Passageiro saiu da carona'
                : 'Pedido de vaga cancelado',
            message: was == ParticipantStatus.confirmed
                ? '$name saiu da sua carona $when. A vaga foi liberada.'
                : '$name desistiu do pedido para a sua carona $when.',
            forDriver: true,
          ),
        );
      }
      continue;
    }

    // Passageiro: a participação dele que estava valendo
    final mine = _activeParticipation(old, viewerId);
    if (mine == null) continue;
    final driver = old.driverName.split(' ').first;
    if (now == null ||
        (now.status == RideStatus.cancelled && now.cancelledBy != viewerId)) {
      notices.add(
        RideCancellationNotice(
          key: 'ride_cancelled_${old.id}',
          title: 'Carona cancelada',
          message: '$driver cancelou a carona $when.',
          forDriver: false,
        ),
      );
      continue;
    }
    final current = _participationOf(now, viewerId, since: mine.status);
    if (current == null) continue;
    if (current.status == ParticipantStatus.cancelled &&
        current.removedByDriver) {
      notices.add(
        RideCancellationNotice(
          key: 'removed_${old.id}',
          title: 'Você saiu da carona',
          message: '$driver tirou você da carona $when.',
          forDriver: false,
        ),
      );
    } else if (mine.status == ParticipantStatus.requested &&
        (current.status == ParticipantStatus.rejected ||
            current.status == ParticipantStatus.expired)) {
      notices.add(
        RideCancellationNotice(
          key: 'request_closed_${old.id}',
          title: 'Pedido não aceito',
          message: current.status == ParticipantStatus.expired
              ? 'Seu pedido para a carona $when expirou sem resposta.'
              : '$driver não aceitou seu pedido para a carona $when.',
          forDriver: false,
        ),
      );
    }
  }
  return notices;
}

RideParticipantModel? _activeParticipation(RideModel ride, String userId) {
  for (final p in ride.participants) {
    if (p.isPassenger &&
        p.userId == userId &&
        (p.status == ParticipantStatus.confirmed ||
            p.status == ParticipantStatus.requested)) {
      return p;
    }
  }
  return null;
}

// A mesma participação na leitura nova: quem saiu e pediu de novo aparece mais de
// uma vez, então vale a que não é a nova (a que veio do status [since])
RideParticipantModel? _participationOf(
  RideModel ride,
  String userId, {
  required ParticipantStatus since,
}) {
  RideParticipantModel? found;
  for (final p in ride.participants) {
    if (!p.isPassenger || p.userId != userId) continue;
    // Ainda valendo: nada mudou
    if (p.status == since) return p;
    found ??= p;
  }
  return found;
}

String _whenLabel(DateTime? departure) {
  if (departure == null) return '';
  final hh = departure.hour.toString().padLeft(2, '0');
  final mm = departure.minute.toString().padLeft(2, '0');
  final dd = departure.day.toString().padLeft(2, '0');
  final mo = departure.month.toString().padLeft(2, '0');
  final today = DateTime.now();
  final sameDay =
      departure.year == today.year &&
      departure.month == today.month &&
      departure.day == today.day;
  return sameDay ? 'das $hh:$mm' : 'de $dd/$mo às $hh:$mm';
}
