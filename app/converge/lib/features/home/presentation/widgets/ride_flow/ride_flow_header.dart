// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../../core/config/styles.dart';

/// Título de cada etapa, com voltar (ou fechar, na primeira) e uma linha de apoio.
class RideFlowHeader extends StatelessWidget {
  final String title;
  final String subtitle;

  /// Volta para a etapa anterior; sem ele, o botão fecha o fluxo.
  final VoidCallback? onBack;
  final VoidCallback onClose;

  const RideFlowHeader({
    super.key,
    required this.title,
    required this.subtitle,
    required this.onClose,
    this.onBack,
  });

  @override
  Widget build(BuildContext context) {
    final back = onBack;
    return Row(
      children: [
        IconButton(
          key: const ValueKey('ride_flow_back'),
          tooltip: back == null ? 'Fechar' : 'Voltar',
          onPressed: () {
            HapticFeedback.lightImpact();
            (back ?? onClose)();
          },
          icon: Icon(
            back == null ? Icons.close_rounded : Icons.arrow_back_rounded,
            color: AppColors.textPrimary,
          ),
        ),
        const SizedBox(width: 4),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w800,
                  color: AppColors.textPrimary,
                  letterSpacing: -0.3,
                ),
              ),
              Text(
                subtitle,
                style: const TextStyle(
                  fontSize: 12.5,
                  color: AppColors.textDark,
                ),
              ),
            ],
          ),
        ),
        if (back != null)
          IconButton(
            tooltip: 'Fechar',
            onPressed: () {
              HapticFeedback.lightImpact();
              onClose();
            },
            icon: const Icon(
              Icons.close_rounded,
              size: 20,
              color: AppColors.textSecondary,
            ),
          ),
      ],
    );
  }
}
