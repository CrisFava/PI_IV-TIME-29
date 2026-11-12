// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../review/data/models/review_model.dart';
import '../../../review/presentation/widgets/review_sheet.dart';
import '../../../review/state/ride_reviews_controller.dart';
import '../../../ride/data/models/ride_status.dart';
import '../../data/models/chat_conversation.dart';
import '../../state/chat_actions_controller.dart';
import 'chat_panel_button.dart';
import 'chat_panel_card.dart';

/// Faixa da viagem na conversa: mostra a situação e a próxima ação.
///
/// Motorista: iniciar e concluir a viagem. Depois de concluída, os dois
/// podem se avaliar.
class ChatRidePanel extends ConsumerWidget {
  final ChatConversation conversation;

  const ChatRidePanel({super.key, required this.conversation});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ride = conversation.ride;
    final busy = ref.watch(chatActionsProvider(conversation.thread)).isLoading;
    final route = '${ride.origin.address} → ${ride.destination.address}';

    return switch (ride.status) {
      RideStatus.confirmed => ChatPanelCard(
        icon: Icons.directions_car_rounded,
        title: 'Carona confirmada',
        subtitle: route,
        actions: [
          if (conversation.viewerIsDriver)
            ChatPanelButton(
              label: 'Iniciar viagem',
              icon: Icons.play_arrow_rounded,
              busy: busy,
              onPressed: () => _startRide(ref),
            ),
        ],
      ),
      RideStatus.inProgress => ChatPanelCard(
        icon: Icons.near_me_rounded,
        title: 'Viagem em andamento',
        subtitle: route,
        actions: [
          if (conversation.viewerIsDriver)
            ChatPanelButton(
              label: 'Concluir viagem',
              icon: Icons.flag_rounded,
              busy: busy,
              onPressed: () => _completeRide(context, ref),
            ),
        ],
      ),
      RideStatus.completed => _completed(context, ref, route),
      _ => ChatPanelCard(
        icon: Icons.directions_car_rounded,
        title: ride.status.label,
        subtitle: route,
      ),
    };
  }

  Widget _completed(BuildContext context, WidgetRef ref, String route) {
    final reviewedId = conversation.viewerIsDriver
        ? conversation.thread.passengerId
        : conversation.ride.driverId;
    final reviews = ref.watch(rideReviewsProvider(conversation.ride.id));
    final ReviewModel? given = reviews.value
        ?.where((review) => review.reviewedId == reviewedId)
        .firstOrNull;

    if (given != null) {
      return ChatPanelCard(
        icon: Icons.star_rounded,
        success: true,
        title: 'Viagem concluída',
        subtitle:
            'Você avaliou ${conversation.otherPartyName} com ${given.rating} de 5 estrelas.',
      );
    }

    return ChatPanelCard(
      icon: Icons.flag_rounded,
      title: 'Viagem concluída',
      subtitle: route,
      actions: [
        ChatPanelButton(
          label: conversation.viewerIsDriver
              ? 'Avaliar passageiro'
              : 'Avaliar motorista',
          icon: Icons.star_rounded,
          busy: reviews.isLoading,
          onPressed: () {
            HapticFeedback.lightImpact();
            showReviewSheet(
              context,
              rideId: conversation.ride.id,
              reviewedId: reviewedId,
              reviewedName: conversation.otherPartyName,
              reviewedIsDriver: !conversation.viewerIsDriver,
            );
          },
        ),
      ],
    );
  }

  Future<void> _startRide(WidgetRef ref) async {
    HapticFeedback.mediumImpact();
    await ref
        .read(chatActionsProvider(conversation.thread).notifier)
        .startRide();
  }

  Future<void> _completeRide(BuildContext context, WidgetRef ref) async {
    HapticFeedback.lightImpact();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppColors.cardBackground,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text(
          'Concluir viagem',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w800,
            color: AppColors.textPrimary,
          ),
        ),
        content: const Text(
          'Todos os passageiros já desembarcaram? Concluir encerra a viagem '
          'para todos e libera as avaliações.',
          style: TextStyle(fontSize: 14, color: AppColors.textSecondary),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text(
              'Ainda não',
              style: TextStyle(
                fontWeight: FontWeight.w600,
                color: AppColors.textSecondary,
              ),
            ),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: AppColors.textLight,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text(
              'Concluir',
              style: TextStyle(fontWeight: FontWeight.w700),
            ),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    HapticFeedback.mediumImpact();
    await ref
        .read(chatActionsProvider(conversation.thread).notifier)
        .completeRide();
  }
}
