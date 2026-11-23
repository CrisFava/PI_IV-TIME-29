// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../../core/config/styles.dart';
import '../destination_dot_badge.dart';
import '../origin_dot_badge.dart';
import 'ride_flow_sheet_frame.dart';

/// Confirmação do ponto escolhido (pela busca, pelo campus ou no mapa): o pino
/// fica no centro do mapa e o cartão pergunta "Saindo de X, confirma?". Dá para
/// arrastar o mapa e ajustar o ponto antes de confirmar.
class RideMapPickCard extends StatelessWidget {
  final bool isOrigin;
  final String address;
  final VoidCallback onConfirm;

  /// "Escolher outro": volta para a busca de endereços.
  final VoidCallback onBack;
  final double bottomSafeArea;

  const RideMapPickCard({
    super.key,
    required this.isOrigin,
    required this.address,
    required this.onConfirm,
    required this.onBack,
    this.bottomSafeArea = 0,
  });

  @override
  Widget build(BuildContext context) {
    // Painel arrastável aberto baixo: o pino no centro do mapa fica livre
    return RideFlowSheetFrame(
      initialSize: 0.375,
      bottomSafeArea: bottomSafeArea,
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.only(top: 2),
              child: isOrigin
                  ? const OriginDotBadge(size: 22)
                  : const DestinationDotBadge(size: 22),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    isOrigin ? 'Saindo de' : 'Indo para',
                    style: const TextStyle(
                      fontSize: 12.5,
                      fontWeight: FontWeight.w700,
                      color: AppColors.textDark,
                    ),
                  ),
                  Text(
                    address.isNotEmpty
                        ? address
                        : 'Arraste o mapa até o ponto...',
                    maxLines: 3,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w800,
                      color: AppColors.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 4),
                  const Text(
                    'É aqui mesmo? Arraste o mapa para ajustar o ponto.',
                    style: TextStyle(fontSize: 12.5, color: AppColors.textDark),
                  ),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),
        FilledButton.icon(
          key: const ValueKey('confirm_map_pick'),
          onPressed: address.isEmpty
              ? null
              : () {
                  HapticFeedback.mediumImpact();
                  onConfirm();
                },
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.primary,
            foregroundColor: AppColors.textLight,
            padding: const EdgeInsets.symmetric(vertical: 14),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
          ),
          icon: const Icon(Icons.check_rounded, size: 20),
          label: Text(
            isOrigin ? 'Confirmar partida' : 'Confirmar destino',
            style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
          ),
        ),
        const SizedBox(height: 8),
        TextButton(
          key: const ValueKey('choose_another_place'),
          onPressed: () {
            HapticFeedback.lightImpact();
            onBack();
          },
          style: TextButton.styleFrom(foregroundColor: AppColors.textPrimary),
          child: const Text(
            'Escolher outro',
            style: TextStyle(fontWeight: FontWeight.w700),
          ),
        ),
      ],
    );
  }
}
