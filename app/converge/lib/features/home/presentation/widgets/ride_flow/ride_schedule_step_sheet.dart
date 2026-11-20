// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../../core/config/styles.dart';
import '../repeat_weekly_checkbox.dart';
import '../ride_departure_tile.dart';
import 'ride_flow_header.dart';
import 'ride_flow_sheet_frame.dart';

/// Etapa de criar ou pedir carona depois do trajeto: o dia e o horário. O motorista
/// também escolhe se a carona se repete toda semana (o carro, as vagas e o raio vêm
/// na etapa seguinte).
class RideScheduleStepSheet extends StatelessWidget {
  final bool isDriver;
  final String departureLabel;

  /// O dia e o horário já foram escolhidos; sem isso o botão fica desligado.
  final bool hasDeparture;
  final VoidCallback onChangeDeparture;

  // Só do motorista
  final bool repeatWeekly;
  final ValueChanged<bool>? onRepeatWeeklyChanged;

  final bool isSubmitting;
  final VoidCallback onSubmit;
  final VoidCallback onBack;
  final VoidCallback onClose;
  final double bottomSafeArea;

  const RideScheduleStepSheet({
    super.key,
    required this.isDriver,
    required this.departureLabel,
    required this.hasDeparture,
    required this.onChangeDeparture,
    required this.isSubmitting,
    required this.onSubmit,
    required this.onBack,
    required this.onClose,
    this.repeatWeekly = false,
    this.onRepeatWeeklyChanged,
    this.bottomSafeArea = 0,
  });

  @override
  Widget build(BuildContext context) {
    final repeatChanged = onRepeatWeeklyChanged;

    return RideFlowSheetFrame(
      initialSize: isDriver ? 0.45 : 0.4,
      bottomSafeArea: bottomSafeArea,
      children: [
        RideFlowHeader(
          title: 'Quando você vai?',
          subtitle: isDriver
              ? 'Dia, horário e se a carona se repete toda semana.'
              : 'Dia e horário que você quer sair.',
          onBack: onBack,
          onClose: onClose,
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

        const SizedBox(height: 18),
        SizedBox(
          height: 52,
          child: FilledButton.icon(
            key: const ValueKey('ride_flow_submit'),
            onPressed: isSubmitting || !hasDeparture
                ? null
                : () {
                    HapticFeedback.mediumImpact();
                    onSubmit();
                  },
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: AppColors.textLight,
              // Enviando, o botão segue escuro com o progresso; sem horário, fica cinza
              disabledBackgroundColor: isSubmitting
                  ? AppColors.primary
                  : AppColors.borderMedium,
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
                : Icon(
                    isDriver
                        ? Icons.arrow_forward_rounded
                        : Icons.fact_check_rounded,
                    size: 20,
                  ),
            label: Text(
              // O motorista ainda escolhe o carro, as vagas e o raio
              isDriver ? 'Continuar' : 'Revisar pedido',
              style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
            ),
          ),
        ),
        if (!hasDeparture) ...[
          const SizedBox(height: 8),
          const Text(
            'Escolha o dia e o horário para continuar.',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 12.5, color: AppColors.textSecondary),
          ),
        ],
      ],
    );
  }
}
