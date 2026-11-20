// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../../core/config/styles.dart';
import '../../../../route/data/models/route_estimate_model.dart';
import '../destination_dot_badge.dart';
import '../origin_dot_badge.dart';
import 'ride_flow_format.dart';
import 'ride_flow_sheet_frame.dart';

/// Etapa do trajeto: com partida e destino escolhidos, o mapa fica em foco com o
/// caminho inteiro (bolinha de partida, caminho preto e bandeira de chegada), e o
/// cartão pequeno embaixo pede "Confirmar trajeto". Igual para motorista e passageiro.
class RideRoutePreviewCard extends StatelessWidget {
  final String originAddress;
  final String destinationAddress;
  final RouteEstimateModel? estimate;
  final bool isEstimating;
  final VoidCallback onConfirm;

  /// O cálculo falhou: tenta de novo (o trajeto só é confirmado com ele pronto).
  final VoidCallback? onRetry;

  /// "Alterar": volta para escolher o destino.
  final VoidCallback onEdit;
  final double bottomSafeArea;

  const RideRoutePreviewCard({
    super.key,
    required this.originAddress,
    required this.destinationAddress,
    required this.estimate,
    required this.isEstimating,
    required this.onConfirm,
    required this.onEdit,
    this.onRetry,
    this.bottomSafeArea = 0,
  });

  String get _estimateText {
    final value = estimate;
    if (value == null) {
      return isEstimating ? 'Calculando o caminho...' : 'Tempo indisponível';
    }
    final minutes = value.durationMinutes;
    final distance = '${formatRideKm(value.distanceKm)} km';
    return minutes == null ? distance : '$distance · cerca de $minutes min';
  }

  @override
  Widget build(BuildContext context) {
    final failed = estimate == null && !isEstimating;
    return RideFlowSheetFrame(
      initialSize: 0.415,
      bottomSafeArea: bottomSafeArea,
      children: [
        const Text(
          'Seu trajeto',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w800,
            color: AppColors.textPrimary,
          ),
        ),
        const SizedBox(height: 10),
        _place(const OriginDotBadge(size: 16), originAddress),
        const SizedBox(height: 6),
        _place(const DestinationDotBadge(size: 16), destinationAddress),
        const SizedBox(height: 10),
        Row(
          children: [
            const Icon(
              Icons.schedule_rounded,
              size: 17,
              color: AppColors.textPrimary,
            ),
            const SizedBox(width: 6),
            Text(
              _estimateText,
              key: const ValueKey('route_preview_estimate'),
              style: const TextStyle(
                fontSize: 13.5,
                fontWeight: FontWeight.w700,
                color: AppColors.textPrimary,
              ),
            ),
          ],
        ),
        const SizedBox(height: 14),
        if (failed && onRetry != null)
          TextButton.icon(
            key: const ValueKey('retry_route'),
            onPressed: () {
              HapticFeedback.lightImpact();
              onRetry!();
            },
            style: TextButton.styleFrom(foregroundColor: AppColors.textPrimary),
            icon: const Icon(Icons.refresh_rounded, size: 18),
            label: const Text(
              'Tentar de novo',
              style: TextStyle(fontWeight: FontWeight.w700),
            ),
          ),
        FilledButton.icon(
          key: const ValueKey('confirm_route'),
          // Só com o caminho calculado
          onPressed: estimate == null
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
          label: const Text(
            'Confirmar trajeto',
            style: TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
          ),
        ),
        TextButton(
          key: const ValueKey('edit_route'),
          onPressed: () {
            HapticFeedback.lightImpact();
            onEdit();
          },
          style: TextButton.styleFrom(foregroundColor: AppColors.textPrimary),
          child: const Text(
            'Alterar',
            style: TextStyle(fontWeight: FontWeight.w700),
          ),
        ),
      ],
    );
  }

  static Widget _place(Widget badge, String address) => Row(
    children: [
      badge,
      const SizedBox(width: 10),
      Expanded(
        child: Text(
          address,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w700,
            color: AppColors.textPrimary,
          ),
        ),
      ),
    ],
  );
}
