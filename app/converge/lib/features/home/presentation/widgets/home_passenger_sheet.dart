// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../matching/data/models/match_model.dart';
import '../../../matching/presentation/widgets/match_driver_card.dart';
import '../../../matching/presentation/widgets/match_waiting_card.dart';
import '../../../matching/state/passenger_match_controller.dart';
import '../../../matching/state/passenger_match_phase.dart';
import '../../../matching/state/passenger_match_state.dart';
import 'passenger_ride_step.dart';

/// ============================================================================
/// COMPONENTE: HomePassengerSheet (Pedido de carona depois da busca)
/// ============================================================================
/// Partida, destino e horário ficam nas etapas de `ride_flow/`, iguais para
/// motorista e passageiro. Este painel cuida do que vem depois:
/// 1. Lista de motoristas compatíveis (matching real)
/// 2. Aguardando o motorista aceitar ou recusar o pedido de vaga
/// ============================================================================
class HomePassengerSheet extends ConsumerStatefulWidget {
  final PassengerRideStep step;
  final ValueChanged<MatchModel> onSelectDriver;
  final VoidCallback onRetryMatches;
  final VoidCallback onCancelRequest;
  final VoidCallback onCancelFlow;
  final double bottomSafeArea;

  const HomePassengerSheet({
    super.key,
    required this.step,
    required this.onSelectDriver,
    required this.onRetryMatches,
    required this.onCancelRequest,
    required this.onCancelFlow,
    this.bottomSafeArea = 0,
  });

  @override
  ConsumerState<HomePassengerSheet> createState() => _HomePassengerSheetState();
}

class _HomePassengerSheetState extends ConsumerState<HomePassengerSheet> {
  @override
  Widget build(BuildContext context) {
    switch (widget.step) {
      // Partida, destino e horário ficam nas etapas de RideFlow (home/widgets/ride_flow)
      case PassengerRideStep.idle:
      case PassengerRideStep.selectPickup:
      case PassengerRideStep.dragMapPickup:
      case PassengerRideStep.searchDestination:
      case PassengerRideStep.dragMapDestination:
      case PassengerRideStep.routePreview:
      case PassengerRideStep.routeConfirmation:
      case PassengerRideStep.offerDetails:
        return const SizedBox.shrink();
      case PassengerRideStep.driverList:
        return _buildDriverListSheet();
      case PassengerRideStep.waitingDriver:
        return _buildWaitingDriverSheet();
      // Aceito: o app avisa e abre "Minhas caronas" (sem a bolinha flutuante)
      case PassengerRideStep.driverAccepted:
        return const SizedBox.shrink();
    }
  }

  // ==========================================================================
  // PASSO 6: LISTA DE MOTORISTAS DISPONÍVEIS
  // ==========================================================================
  Widget _buildDriverListSheet() {
    final matchState = ref.watch(passengerMatchProvider);

    return DraggableScrollableSheet(
      initialChildSize: 0.5,
      minChildSize: 0.16,
      maxChildSize: 0.85,
      snap: true,
      snapSizes: const [0.28, 0.5, 0.85],
      builder: (context, scrollController) {
        return Container(
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: const BorderRadius.vertical(top: Radius.circular(26)),
            border: Border.all(color: AppColors.borderSubtle, width: 1),
          ),
          child: CustomScrollView(
            controller: scrollController,
            slivers: [
              SliverToBoxAdapter(
                child: Center(
                  child: Container(
                    margin: const EdgeInsets.symmetric(vertical: 10),
                    width: 38,
                    height: 4,
                    decoration: BoxDecoration(
                      color: AppColors.borderMedium,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
              ),
              SliverPadding(
                padding: EdgeInsets.fromLTRB(
                  16,
                  4,
                  16,
                  120 + widget.bottomSafeArea,
                ),
                sliver: SliverList(
                  delegate: SliverChildListDelegate([
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Motoristas Compatíveis',
                                style: TextStyle(
                                  fontSize: 17,
                                  fontWeight: FontWeight.w800,
                                  color: AppColors.textPrimary,
                                ),
                              ),
                              SizedBox(height: 2),
                              Text(
                                'Ordenados pela compatibilidade com o seu trajeto',
                                style: TextStyle(
                                  fontSize: 12,
                                  color: AppColors.textSecondary,
                                ),
                              ),
                            ],
                          ),
                        ),
                        IconButton(
                          icon: const Icon(Icons.refresh_rounded),
                          onPressed:
                              matchState.phase == PassengerMatchPhase.loading ||
                                  matchState.isBusy
                              ? null
                              : widget.onRetryMatches,
                          tooltip: 'Buscar de novo',
                        ),
                        IconButton(
                          icon: const Icon(Icons.close_rounded),
                          onPressed: widget.onCancelFlow,
                          tooltip: 'Fechar',
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),
                    ..._buildDriverListContent(matchState),
                  ]),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  List<Widget> _buildDriverListContent(PassengerMatchState matchState) {
    final phase = matchState.phase;
    final drivers = matchState.matches;

    if (phase == PassengerMatchPhase.loading ||
        phase == PassengerMatchPhase.idle) {
      return const [
        Padding(
          padding: EdgeInsets.all(28),
          child: Center(
            child: CircularProgressIndicator(color: AppColors.primary),
          ),
        ),
      ];
    }

    if (phase == PassengerMatchPhase.failed) {
      return [
        _buildDriverListMessage(
          matchState.errorMessage ?? 'Não foi possível buscar os motoristas.',
        ),
      ];
    }

    if (drivers.isEmpty) {
      return [
        _buildDriverListMessage(
          'Nenhum motorista compatível com o seu trajeto agora. '
          'Tente outro horário ou toque em atualizar mais tarde.',
        ),
      ];
    }

    final selected = matchState.selected;
    return [
      for (final driver in drivers)
        Padding(
          padding: const EdgeInsets.only(bottom: 10),
          child: MatchDriverCard(
            match: driver,
            isRequesting:
                phase == PassengerMatchPhase.requesting &&
                selected?.driverRouteId == driver.driverRouteId,
            isDisabled: matchState.isBusy,
            onRequest: () => widget.onSelectDriver(driver),
          ),
        ),
    ];
  }

  Widget _buildDriverListMessage(String text) {
    return Container(
      padding: const EdgeInsets.all(20),
      alignment: Alignment.center,
      child: Text(
        text,
        textAlign: TextAlign.center,
        style: const TextStyle(
          fontSize: 14,
          color: AppColors.textSecondary,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }

  // ==========================================================================
  // PASSO 7: AGUARDANDO O MOTORISTA ACEITAR OU RECUSAR O PEDIDO DE VAGA
  // ==========================================================================
  Widget _buildWaitingDriverSheet() {
    final matchState = ref.watch(passengerMatchProvider);

    return Align(
      alignment: Alignment.bottomCenter,
      child: Padding(
        padding: EdgeInsets.fromLTRB(16, 0, 16, 96 + widget.bottomSafeArea),
        child: MatchWaitingCard(
          match: matchState.selected,
          isCancelling: matchState.isCancelling,
          onCancel: widget.onCancelRequest,
        ),
      ),
    );
  }

  // ==========================================================================
  // PASSO 8: MOTORISTA ACEITOU! (MODAL ANIMADO COM RECOLHIMENTO PARA BUBBLE PIP)
  // ==========================================================================
}
