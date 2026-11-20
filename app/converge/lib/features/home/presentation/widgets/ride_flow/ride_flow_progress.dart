// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:flutter/material.dart';

import '../../../../../core/config/styles.dart';
import '../passenger_ride_step.dart';

/// Onde a tela está no caminho de pedir (ou oferecer) uma carona: a etapa [step]
/// de [total].
typedef RideFlowStepPosition = ({int step, int total});

/// A posição de cada etapa do fluxo.
///
/// O passageiro passa por 5 etapas (partida, destino, trajeto, horário e revisão) e
/// o motorista por 6 (o carro e as vagas vêm antes da revisão). A revisão abre num
/// aviso por cima da tela, então não desenha barra. As telas de arrastar o mapa
/// contam como a etapa a que pertencem.
///
/// Devolve `null` fora do fluxo (sem barra).
RideFlowStepPosition? rideFlowStepPosition(
  PassengerRideStep step, {
  required bool isDriver,
}) {
  final position = switch (step) {
    PassengerRideStep.selectPickup || PassengerRideStep.dragMapPickup => 1,
    PassengerRideStep.searchDestination ||
    PassengerRideStep.dragMapDestination => 2,
    PassengerRideStep.routePreview => 3,
    PassengerRideStep.routeConfirmation => 4,
    // Só o motorista passa pelo carro e pelas vagas
    PassengerRideStep.offerDetails => isDriver ? 5 : null,
    PassengerRideStep.idle ||
    PassengerRideStep.driverList ||
    PassengerRideStep.waitingDriver ||
    PassengerRideStep.driverAccepted => null,
  };
  if (position == null) return null;
  return (step: position, total: isDriver ? 6 : 5);
}

/// A etapa e o quanto da barra está preenchido neste instante ([fraction], de 0 a 1).
typedef RideFlowProgress = ({int step, int total, double fraction});

/// Leva a etapa até o painel das etapas (`RideFlowSheetFrame`), que desenha a barra.
/// Sem este widget acima, o painel não mostra barra nenhuma.
///
/// Cada etapa é uma tela nova, mas este widget fica no coordenador e não troca de
/// lugar entre elas. Por isso o preenchimento anda aos poucos até a etapa nova (para
/// a frente ou para trás) e a barra da tela que abre já começa de onde a anterior parou.
class RideFlowProgressScope extends StatelessWidget {
  final RideFlowStepPosition? position;
  final Widget child;

  const RideFlowProgressScope({
    super.key,
    required this.position,
    required this.child,
  });

  /// A etapa em que o fluxo está, ou `null` se não há barra para mostrar.
  static RideFlowProgress? maybeOf(BuildContext context) => context
      .dependOnInheritedWidgetOfExactType<_RideFlowProgressValue>()
      ?.progress;

  @override
  Widget build(BuildContext context) {
    final current = position;
    return TweenAnimationBuilder<double>(
      tween: Tween<double>(
        end: current == null ? 0 : current.step / current.total,
      ),
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeOutCubic,
      builder: (context, fraction, child) => _RideFlowProgressValue(
        progress: current == null
            ? null
            : (step: current.step, total: current.total, fraction: fraction),
        child: child!,
      ),
      child: child,
    );
  }
}

class _RideFlowProgressValue extends InheritedWidget {
  final RideFlowProgress? progress;

  const _RideFlowProgressValue({required this.progress, required super.child});

  @override
  bool updateShouldNotify(_RideFlowProgressValue oldWidget) =>
      progress != oldWidget.progress;
}

/// Linha fina que vai enchendo conforme as etapas avançam.
class RideFlowProgressBar extends StatelessWidget {
  final RideFlowProgress progress;

  const RideFlowProgressBar({super.key, required this.progress});

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Etapa ${progress.step} de ${progress.total}',
      child: ExcludeSemantics(
        child: ClipRRect(
          borderRadius: BorderRadius.circular(2),
          child: Container(
            height: 3,
            color: AppColors.progressTrack,
            alignment: Alignment.centerLeft,
            child: FractionallySizedBox(
              key: const ValueKey('ride_flow_progress_fill'),
              widthFactor: progress.fraction.clamp(0.0, 1.0),
              heightFactor: 1,
              child: const ColoredBox(color: AppColors.primary),
            ),
          ),
        ),
      ),
    );
  }
}
