// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';
import '../../../route/data/models/route_place_model.dart';
import '../../state/trip_progress.dart';

/// O passo escolhido de um trajeto: o que fazer, de onde sai e para onde vai.
/// É o mesmo cartão no "Trajeto completo" do motorista (com as setas para trocar
/// de passo) e no trajeto do passageiro.
class RouteStepCard extends StatelessWidget {
  final String title;
  final RoutePlaceModel from;
  final RoutePlaceModel to;

  /// Ícone do lugar de chegada (pessoa no embarque, bandeira no desembarque).
  final IconData toIcon;

  /// Linha pequena em cima do título (ex.: "Passo 2 de 5 · ~10:12").
  final String? header;

  /// Linha embaixo dos lugares (ex.: horário e paradas); vazia esconde.
  final String? info;

  /// Quando o passo começa e termina no mesmo lugar.
  final String samePlaceLabel;

  /// Setas para o passo anterior e o próximo; nulas desligam, sem [onNext] e
  /// [onPrevious] as setas não aparecem.
  final VoidCallback? onPrevious;
  final VoidCallback? onNext;
  final bool showArrows;

  const RouteStepCard({
    super.key,
    required this.title,
    required this.from,
    required this.to,
    required this.toIcon,
    this.header,
    this.info,
    this.samePlaceLabel = 'No mesmo ponto',
    this.onPrevious,
    this.onNext,
    this.showArrows = false,
  });

  @override
  Widget build(BuildContext context) {
    final samePlace = TripProgress.sameSpot(from, to);
    final headerText = header;
    final infoText = info;
    return Container(
      padding: EdgeInsets.fromLTRB(14, showArrows ? 10 : 14, 6, 12),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.primary, width: 1.5),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (headerText != null || showArrows)
            Row(
              children: [
                Expanded(
                  child: Text(
                    headerText ?? '',
                    style: const TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w800,
                      color: AppColors.textSecondary,
                    ),
                  ),
                ),
                if (showArrows) ...[
                  IconButton(
                    key: const ValueKey('route_step_previous'),
                    tooltip: 'Passo anterior',
                    visualDensity: VisualDensity.compact,
                    onPressed: onPrevious,
                    icon: const Icon(Icons.chevron_left_rounded),
                  ),
                  IconButton(
                    key: const ValueKey('route_step_next'),
                    tooltip: 'Próximo passo',
                    visualDensity: VisualDensity.compact,
                    onPressed: onNext,
                    icon: const Icon(Icons.chevron_right_rounded),
                  ),
                ],
              ],
            ),
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: Text(
              title,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w800,
                color: AppColors.textPrimary,
              ),
            ),
          ),
          const SizedBox(height: 8),
          if (samePlace)
            _PlaceLine(
              icon: Icons.place_rounded,
              label: samePlaceLabel,
              address: to.address,
            )
          else ...[
            _PlaceLine(
              icon: Icons.trip_origin_rounded,
              label: 'Saindo de',
              address: from.address,
            ),
            const SizedBox(height: 6),
            _PlaceLine(icon: toIcon, label: 'Indo para', address: to.address),
          ],
          if (infoText != null && infoText.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(
              infoText,
              style: const TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w800,
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _PlaceLine extends StatelessWidget {
  final IconData icon;
  final String label;
  final String address;

  const _PlaceLine({
    required this.icon,
    required this.label,
    required this.address,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 17, color: AppColors.textPrimary),
          const SizedBox(width: 8),
          Expanded(
            child: Text.rich(
              TextSpan(
                children: [
                  TextSpan(
                    text: '$label  ',
                    style: const TextStyle(
                      fontWeight: FontWeight.w600,
                      color: AppColors.textSecondary,
                    ),
                  ),
                  TextSpan(
                    text: address,
                    style: const TextStyle(
                      fontWeight: FontWeight.w700,
                      color: AppColors.textPrimary,
                    ),
                  ),
                ],
              ),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 13.5),
            ),
          ),
        ],
      ),
    );
  }
}
