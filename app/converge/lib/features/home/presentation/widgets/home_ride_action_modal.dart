import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';

/// ============================================================================
/// COMPONENTE: HomeRideActionModal (Modal de Ações Centrais de Carona)
/// ============================================================================
/// Apresenta o fluxo contextual de caronas com as 2 opções táteis:
/// 1. "Pedir ride"
/// 2. "Iniciar expediente de motorista"
/// ============================================================================
Future<void> showRideActionModal({
  required BuildContext context,
  required VoidCallback onRequestRide,
  required VoidCallback onStartDriverShift,
}) {
  HapticFeedback.mediumImpact();
  return showModalBottomSheet(
    context: context,
    backgroundColor: Colors.transparent,
    barrierColor: AppColors.barrier,
    isScrollControlled: true,
    builder: (context) => HomeRideActionModal(
      onRequestRide: () {
        Navigator.of(context).pop();
        onRequestRide();
      },
      onStartDriverShift: () {
        Navigator.of(context).pop();
        onStartDriverShift();
      },
    ),
  );
}

class HomeRideActionModal extends StatelessWidget {
  final VoidCallback onRequestRide;
  final VoidCallback onStartDriverShift;

  const HomeRideActionModal({
    super.key,
    required this.onRequestRide,
    required this.onStartDriverShift,
  });

  @override
  Widget build(BuildContext context) {
    final bottomPadding = MediaQuery.paddingOf(context).bottom;

    return Container(
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
        border: Border.all(color: AppColors.borderSubtle, width: 1),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.16),
            blurRadius: 30,
            offset: const Offset(0, -8),
          ),
        ],
      ),
      padding: EdgeInsets.fromLTRB(20, 12, 20, 24 + bottomPadding),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Alça central
          Center(
            child: Container(
              width: 36,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.borderMedium,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 18),

          // Título e Fechar
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Caronas Converge',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w800,
                  color: AppColors.textPrimary,
                  letterSpacing: -0.3,
                ),
              ),
              IconButton(
                visualDensity: VisualDensity.compact,
                onPressed: () => Navigator.of(context).pop(),
                icon: const Icon(
                  Icons.close_rounded,
                  size: 20,
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),

          const SizedBox(height: 16),

          // Card 1: Pedir Carona
          _buildActionCard(
            icon: Icons.hail_rounded,
            title: 'Pedir ride',
            subtitle: 'Encontre motoristas universitários com rotas para o seu campus.',
            onTap: onRequestRide,
          ),

          const SizedBox(height: 12),

          // Card 2: Iniciar Expediente de Motorista
          _buildActionCard(
            icon: Icons.airline_seat_recline_normal_rounded,
            title: 'Iniciar expediente de motorista',
            subtitle: 'Disponibilize assentos no seu carro e divida os custos do percurso.',
            badgeText: 'Disponível',
            onTap: onStartDriverShift,
          ),
        ],
      ),
    );
  }

  Widget _buildActionCard({
    required IconData icon,
    required String title,
    required String subtitle,
    String? badgeText,
    required VoidCallback onTap,
  }) {
    return Material(
      color: AppColors.cardSecondary,
      borderRadius: BorderRadius.circular(20),
      child: InkWell(
        borderRadius: BorderRadius.circular(20),
        onTap: () {
          HapticFeedback.lightImpact();
          onTap();
        },
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: AppColors.borderSubtle),
          ),
          child: Row(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: const BoxDecoration(
                  color: AppColors.primary,
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  icon,
                  color: Colors.white,
                  size: 22,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Flexible(
                          child: Text(
                            title,
                            style: const TextStyle(
                              fontSize: 14.5,
                              fontWeight: FontWeight.w700,
                              color: AppColors.textPrimary,
                            ),
                          ),
                        ),
                        if (badgeText != null) ...[
                          const SizedBox(width: 8),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 7,
                              vertical: 2,
                            ),
                            decoration: BoxDecoration(
                              color: AppColors.successBackground,
                              borderRadius: BorderRadius.circular(6),
                              border: Border.all(color: AppColors.successBorder),
                            ),
                            child: Text(
                              badgeText,
                              style: const TextStyle(
                                fontSize: 10,
                                fontWeight: FontWeight.w700,
                                color: AppColors.success,
                              ),
                            ),
                          ),
                        ],
                      ],
                    ),
                    const SizedBox(height: 3),
                    Text(
                      subtitle,
                      style: const TextStyle(
                        fontSize: 12,
                        color: AppColors.textSecondary,
                        height: 1.3,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              const Icon(
                Icons.chevron_right_rounded,
                size: 20,
                color: AppColors.textMuted,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
