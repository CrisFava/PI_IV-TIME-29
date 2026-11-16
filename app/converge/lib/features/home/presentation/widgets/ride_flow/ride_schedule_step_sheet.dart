// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../../core/config/styles.dart';
import '../../../../route/data/models/route_estimate_model.dart';
import '../destination_dot_badge.dart';
import '../origin_dot_badge.dart';
import '../repeat_weekly_checkbox.dart';
import '../ride_departure_tile.dart';
import 'ride_flow_format.dart';
import 'ride_flow_header.dart';
import 'ride_flow_sheet_frame.dart';

/// Etapa 3 de criar ou pedir carona: o trajeto escolhido, o tempo estimado pelo
/// Google, o dia e o horário. O motorista também escolhe se a carona se repete
/// toda semana e até onde busca passageiros.
class RideScheduleStepSheet extends StatelessWidget {
  final bool isDriver;
  final String originAddress;
  final String destinationAddress;

  /// Tempo e distância de carro; nulo enquanto calcula ou se não deu para calcular.
  final RouteEstimateModel? estimate;
  final bool isEstimating;

  final String departureLabel;
  final VoidCallback onChangeDeparture;

  // Só do motorista
  final bool repeatWeekly;
  final ValueChanged<bool>? onRepeatWeeklyChanged;
  final double searchRadiusKm;
  final ValueChanged<double>? onSearchRadiusChanged;

  final bool isSubmitting;
  final VoidCallback onSubmit;
  final VoidCallback onEditOrigin;
  final VoidCallback onEditDestination;
  final VoidCallback onBack;
  final VoidCallback onClose;
  final double bottomSafeArea;

  const RideScheduleStepSheet({
    super.key,
    required this.isDriver,
    required this.originAddress,
    required this.destinationAddress,
    required this.estimate,
    required this.isEstimating,
    required this.departureLabel,
    required this.onChangeDeparture,
    required this.isSubmitting,
    required this.onSubmit,
    required this.onEditOrigin,
    required this.onEditDestination,
    required this.onBack,
    required this.onClose,
    this.repeatWeekly = false,
    this.onRepeatWeeklyChanged,
    this.searchRadiusKm = 3.0,
    this.onSearchRadiusChanged,
    this.bottomSafeArea = 0,
  });

  String get _estimateText {
    final value = estimate;
    if (value == null) {
      return isEstimating
          ? 'Calculando o tempo de viagem...'
          : 'Não foi possível calcular o tempo de viagem.';
    }
    final minutes = value.durationMinutes;
    final distance = '${formatRideKm(value.distanceKm)} km';
    return minutes == null
        ? distance
        : '~${formatTripDuration(minutes)} de viagem · $distance';
  }

  @override
  Widget build(BuildContext context) {
    final radiusChanged = onSearchRadiusChanged;
    final repeatChanged = onRepeatWeeklyChanged;

    return RideFlowSheetFrame(
      initialSize: isDriver ? 0.775 : 0.575,
      bottomSafeArea: bottomSafeArea,
      children: [
        RideFlowHeader(
          title: 'Quando você vai?',
          subtitle: isDriver
              ? 'Dia, horário e quem pode pedir vaga.'
              : 'Dia e horário que você quer sair.',
          onBack: onBack,
          onClose: onClose,
        ),
        const SizedBox(height: 14),

        // O trajeto escolhido: tocar volta para trocar a partida ou o destino
        // (travado enquanto envia)
        AbsorbPointer(
          absorbing: isSubmitting,
          child: Container(
            decoration: BoxDecoration(
              color: AppColors.neutralLight,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppColors.borderSubtle),
            ),
            child: Column(
              children: [
                _placeRow(
                  badge: const OriginDotBadge(size: 16),
                  label: 'Saindo de',
                  address: originAddress,
                  onTap: onEditOrigin,
                ),
                const Divider(height: 1, color: AppColors.borderSubtle),
                _placeRow(
                  badge: const DestinationDotBadge(size: 16),
                  label: 'Indo para',
                  address: destinationAddress,
                  onTap: onEditDestination,
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 10),

        // Tempo estimado pelo Google
        Row(
          children: [
            isEstimating && estimate == null
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: AppColors.primary,
                    ),
                  )
                : const Icon(
                    Icons.schedule_rounded,
                    size: 18,
                    color: AppColors.textPrimary,
                  ),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                _estimateText,
                key: const ValueKey('ride_flow_estimate'),
                style: const TextStyle(
                  fontSize: 13.5,
                  fontWeight: FontWeight.w700,
                  color: AppColors.textPrimary,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 14),

        AbsorbPointer(
          absorbing: isSubmitting,
          child: RideDepartureTile(
            label: departureLabel,
            onTap: onChangeDeparture,
          ),
        ),

        if (isDriver && repeatChanged != null) ...[
          const SizedBox(height: 4),
          RepeatWeeklyCheckbox(
            value: repeatWeekly,
            onChanged: repeatChanged,
            enabled: !isSubmitting,
          ),
        ],

        if (isDriver && radiusChanged != null) ...[
          const SizedBox(height: 10),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Buscar passageiros a até',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w700,
                  color: AppColors.textPrimary,
                ),
              ),
              Text(
                '${formatRideKm(searchRadiusKm)} km',
                style: const TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w800,
                  color: AppColors.primary,
                ),
              ),
            ],
          ),
          Slider(
            value: searchRadiusKm,
            min: 1.0,
            max: 10.0,
            divisions: 18,
            activeColor: AppColors.primary,
            inactiveColor: AppColors.borderSubtle,
            onChanged: isSubmitting ? null : radiusChanged,
          ),
          const Text(
            'Do seu caminho até onde o passageiro embarca, e do seu destino até o dele.',
            style: TextStyle(fontSize: 12, color: AppColors.textDark),
          ),
        ],

        const SizedBox(height: 18),
        SizedBox(
          height: 52,
          child: FilledButton.icon(
            key: const ValueKey('ride_flow_submit'),
            onPressed: isSubmitting
                ? null
                : () {
                    HapticFeedback.mediumImpact();
                    onSubmit();
                  },
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: AppColors.textLight,
              disabledBackgroundColor: AppColors.primary,
              disabledForegroundColor: AppColors.textLight,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
            icon: isSubmitting
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: AppColors.textLight,
                    ),
                  )
                : const Icon(Icons.fact_check_rounded, size: 20),
            label: Text(
              isDriver ? 'Revisar carona' : 'Revisar pedido',
              style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
            ),
          ),
        ),
      ],
    );
  }

  Widget _placeRow({
    required Widget badge,
    required String label,
    required String address,
    required VoidCallback onTap,
  }) {
    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: () {
        HapticFeedback.lightImpact();
        onTap();
      },
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Row(
          children: [
            badge,
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    label,
                    style: const TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                      color: AppColors.textDark,
                    ),
                  ),
                  Text(
                    address,
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                      color: AppColors.textPrimary,
                    ),
                  ),
                ],
              ),
            ),
            const Icon(
              Icons.edit_rounded,
              size: 18,
              color: AppColors.textSecondary,
            ),
          ],
        ),
      ),
    );
  }
}
