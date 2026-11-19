// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../auth/state/auth_controller.dart';
import '../../../auth/state/session/auth_session.dart';
import '../../../ride/data/models/ride_model.dart';
import '../../data/models/review_model.dart';
import '../../state/pending_reviews.dart';
import '../../state/ride_reviews_controller.dart';
import '../pages/ride_review_page.dart';

/// Abre a tela de avaliação de uma viagem que acabou de ser concluída, só com quem
/// o usuário ainda não avaliou. Se não há ninguém para avaliar, não abre nada.
Future<void> promptRideReviews(
  BuildContext context,
  WidgetRef ref,
  RideModel ride,
) async {
  final session = ref.read(authControllerProvider).value;
  if (session is! Authenticated) return;

  // O provider é autoDispose e quem chama (a home) não o observa: sem um ouvinte, ele
  // poderia ser descartado no meio da carga e o future nunca terminar.
  final subscription = ref.listenManual(
    rideReviewsProvider(ride.id),
    (_, _) {},
  );
  final List<ReviewModel> given;
  try {
    given = await ref.read(rideReviewsProvider(ride.id).future);
  } catch (_) {
    // Sem saber o que já foi avaliado, não abre sozinha: o botão "Avaliar" do chat continua lá.
    return;
  } finally {
    subscription.close();
  }

  final targets = pendingReviewTargets(
    ride: ride,
    viewerId: session.user.id,
    given: given,
  );
  if (targets.isEmpty || !context.mounted) return;

  await Navigator.of(context).push<bool>(
    MaterialPageRoute(
      fullscreenDialog: true,
      builder: (_) => RideReviewPage(rideId: ride.id, targets: targets),
    ),
  );
}
