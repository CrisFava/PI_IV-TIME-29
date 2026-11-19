// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/utils/currency_formatter.dart';
import '../../../matching/data/models/match_model.dart';
import '../../../matching/presentation/widgets/match_driver_card.dart';
import '../../../matching/presentation/widgets/match_waiting_card.dart';
import '../../../matching/state/passenger_match_controller.dart';
import '../../../matching/state/passenger_match_phase.dart';
import '../../../matching/state/passenger_match_state.dart';
import '../../../ride/data/models/ride_payment_model.dart';
import '../../../ride/presentation/widgets/payment_prompt_sheet.dart';
import '../../../ride/state/pending_payments_provider.dart';
import 'passenger_ride_step.dart';

/// ============================================================================
/// COMPONENTE: HomePassengerSheet (Pedido de carona depois da busca)
/// ============================================================================
/// Partida, destino e horário ficam nas etapas de `ride_flow/`, iguais para
/// motorista e passageiro. Este painel cuida do que vem depois:
/// 1. Lista de motoristas compatíveis (matching real)
/// 2. Aguardando o motorista aceitar ou recusar o pedido de vaga
/// 3. Carona aceita (atalho para o chat)
/// ============================================================================
class HomePassengerSheet extends ConsumerStatefulWidget {
  final PassengerRideStep step;
  final ValueChanged<MatchModel> onSelectDriver;
  final VoidCallback onRetryMatches;
  final VoidCallback onCancelRequest;
  final VoidCallback onSendMessageToDriver;
  final VoidCallback onCancelFlow;
  final double bottomSafeArea;

  const HomePassengerSheet({
    super.key,
    required this.step,
    required this.onSelectDriver,
    required this.onRetryMatches,
    required this.onCancelRequest,
    required this.onSendMessageToDriver,
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
      case PassengerRideStep.routeConfirmation:
        return const SizedBox.shrink();
      case PassengerRideStep.driverList:
        return _buildDriverListSheet();
      case PassengerRideStep.waitingDriver:
        return _buildWaitingDriverSheet();
      case PassengerRideStep.driverAccepted:
        final matchState = ref.watch(passengerMatchProvider);
        return _DriverAcceptedPipSheet(
          driver: matchState.selected,
          pendingPayment: _openChargeOf(matchState.rideId),
          onSendMessage: widget.onSendMessageToDriver,
          onPay: (payment) {
            HapticFeedback.mediumImpact();
            showPaymentPromptSheet(context, payment);
          },
          onCancel: widget.onCancelFlow,
          bottomSafeArea: widget.bottomSafeArea,
        );
    }
  }

  /// Cobrança em aberto da viagem do pedido aceito: a viagem do passageiro já terminou.
  RidePaymentModel? _openChargeOf(String? rideId) {
    if (rideId == null) return null;
    final pending = ref.watch(pendingPaymentsProvider).value ?? const [];
    for (final payment in pending) {
      if (payment.rideId == rideId && payment.isOpen) return payment;
    }
    return null;
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

class _DriverAcceptedPipSheet extends StatefulWidget {
  final MatchModel? driver;

  /// Cobrança da viagem que acabou de terminar: o card vira o atalho para pagar.
  final RidePaymentModel? pendingPayment;
  final VoidCallback onSendMessage;
  final ValueChanged<RidePaymentModel> onPay;
  final VoidCallback onCancel;
  final double bottomSafeArea;

  const _DriverAcceptedPipSheet({
    required this.driver,
    required this.pendingPayment,
    required this.onSendMessage,
    required this.onPay,
    required this.onCancel,
    required this.bottomSafeArea,
  });

  @override
  State<_DriverAcceptedPipSheet> createState() =>
      _DriverAcceptedPipSheetState();
}

class _DriverAcceptedPipSheetState extends State<_DriverAcceptedPipSheet>
    with SingleTickerProviderStateMixin {
  late final AnimationController _animController;
  late final Animation<double> _slideAnimation;
  late final Animation<double> _fadeAnimation;
  late final Animation<double> _scaleAnimation;

  bool _isCollapsed = false;
  bool _manuallyOpened = false;
  Offset? _bubblePosition;

  @override
  void didUpdateWidget(covariant _DriverAcceptedPipSheet oldWidget) {
    super.didUpdateWidget(oldWidget);
    // A cobrança chegou: o card abre sozinho para o passageiro pagar
    if (oldWidget.pendingPayment == null && widget.pendingPayment != null) {
      setState(() {
        _isCollapsed = false;
        _manuallyOpened = true;
      });
    }
  }

  @override
  void initState() {
    super.initState();
    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 500),
    );

    _slideAnimation = Tween<double>(begin: 60.0, end: 0.0).animate(
      CurvedAnimation(parent: _animController, curve: Curves.easeOutCubic),
    );
    _fadeAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(parent: _animController, curve: Curves.easeOut));
    _scaleAnimation = Tween<double>(begin: 0.92, end: 1.0).animate(
      CurvedAnimation(parent: _animController, curve: Curves.easeOutBack),
    );

    _animController.forward();

    // Aguarda 3 segundos após a confirmação e transforma suavemente o modal
    // em um ponto flutuante à direita da tela
    Future.delayed(const Duration(seconds: 3), () {
      if (mounted && !_manuallyOpened && !_isCollapsed) {
        setState(() {
          _isCollapsed = true;
        });
      }
    });
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_bubblePosition == null) {
      final size = MediaQuery.sizeOf(context);
      _bubblePosition = Offset(size.width - 76, size.height * 0.40);
    }
  }

  @override
  void dispose() {
    _animController.dispose();
    super.dispose();
  }

  Offset _clampPosition(Offset raw, Size size) {
    const bubbleSize = 58.0;
    const padding = 16.0;
    final topLimit = MediaQuery.paddingOf(context).top + 40;
    final bottomLimit = size.height - 120 - widget.bottomSafeArea;

    final dx = raw.dx.clamp(padding, size.width - bubbleSize - padding);
    final dy = raw.dy.clamp(topLimit, bottomLimit);
    return Offset(dx, dy);
  }

  @override
  Widget build(BuildContext context) {
    final screenSize = MediaQuery.sizeOf(context);
    final pos =
        _bubblePosition ??
        Offset(screenSize.width - 76, screenSize.height * 0.40);

    return Stack(
      children: [
        // 1. PONTO FLUTUANTE À DIREITA (DRAGGABLE PiP BUBBLE)
        AnimatedPositioned(
          duration: const Duration(milliseconds: 320),
          curve: Curves.easeOutBack,
          left: pos.dx,
          top: pos.dy,
          child: AnimatedScale(
            scale: _isCollapsed ? 1.0 : 0.0,
            duration: const Duration(milliseconds: 280),
            curve: Curves.easeInOutBack,
            child: AnimatedOpacity(
              opacity: _isCollapsed ? 1.0 : 0.0,
              duration: const Duration(milliseconds: 220),
              child: _isCollapsed
                  ? GestureDetector(
                      onPanUpdate: (details) {
                        setState(() {
                          _bubblePosition = _clampPosition(
                            pos + details.delta,
                            screenSize,
                          );
                        });
                      },
                      onTap: () {
                        HapticFeedback.mediumImpact();
                        setState(() {
                          _isCollapsed = false;
                          _manuallyOpened = true;
                        });
                      },
                      child: Container(
                        width: 58,
                        height: 58,
                        decoration: BoxDecoration(
                          color: AppColors.dockBackground,
                          shape: BoxShape.circle,
                          border: Border.all(color: Colors.white, width: 2),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withValues(alpha: 0.35),
                              blurRadius: 18,
                              offset: const Offset(0, 6),
                            ),
                          ],
                        ),
                        child: Stack(
                          alignment: Alignment.center,
                          children: [
                            const Icon(
                              Icons.directions_car_rounded,
                              color: Colors.white,
                              size: 28,
                            ),
                            // Indicador verde pulsante de corrida em andamento
                            Positioned(
                              top: 4,
                              right: 4,
                              child: Container(
                                width: 12,
                                height: 12,
                                decoration: BoxDecoration(
                                  color: AppColors.success,
                                  shape: BoxShape.circle,
                                  border: Border.all(
                                    color: Colors.white,
                                    width: 2,
                                  ),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    )
                  : const SizedBox.shrink(),
            ),
          ),
        ),

        // 2. MODAL EXPANDIDO COMPLETO (COM ANIMAÇÃO DE ENTRADA E RETRAÇÃO)
        Align(
          alignment: Alignment.bottomCenter,
          child: AnimatedSlide(
            offset: _isCollapsed ? const Offset(0, 1.2) : Offset.zero,
            duration: const Duration(milliseconds: 380),
            curve: Curves.easeInOutCubic,
            child: AnimatedOpacity(
              opacity: _isCollapsed ? 0.0 : 1.0,
              duration: const Duration(milliseconds: 280),
              child: AnimatedBuilder(
                animation: _animController,
                builder: (context, child) {
                  return Transform.translate(
                    offset: Offset(0, _slideAnimation.value),
                    child: Transform.scale(
                      scale: _scaleAnimation.value,
                      child: Opacity(
                        opacity: _fadeAnimation.value,
                        child: child,
                      ),
                    ),
                  );
                },
                child: Padding(
                  padding: EdgeInsets.fromLTRB(
                    16,
                    0,
                    16,
                    96 + widget.bottomSafeArea,
                  ),
                  child: Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: AppColors.cardBackground,
                      borderRadius: BorderRadius.circular(26),
                      border: Border.all(
                        color: AppColors.borderSubtle,
                        width: 1,
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withValues(alpha: 0.18),
                          blurRadius: 28,
                          offset: const Offset(0, 8),
                        ),
                      ],
                    ),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(10),
                              decoration: BoxDecoration(
                                color: AppColors.successBackground,
                                shape: BoxShape.circle,
                                border: Border.all(
                                  color: AppColors.successBorder,
                                ),
                              ),
                              child: const Icon(
                                Icons.check_rounded,
                                color: AppColors.success,
                                size: 24,
                              ),
                            ),
                            const SizedBox(width: 14),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    widget.pendingPayment != null
                                        ? 'Viagem concluída'
                                        : 'Carona Confirmada!',
                                    style: TextStyle(
                                      fontSize: 16,
                                      fontWeight: FontWeight.w800,
                                      color: AppColors.textPrimary,
                                    ),
                                  ),
                                  Text(
                                    widget.pendingPayment != null
                                        ? 'Pague a carona para pedir ou oferecer outra'
                                        : widget.driver != null
                                        ? '${widget.driver!.otherUserName} aceitou o seu pedido'
                                        : 'O motorista aceitou o seu pedido',
                                    style: const TextStyle(
                                      fontSize: 12.5,
                                      color: AppColors.textSecondary,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            IconButton(
                              onPressed: () {
                                HapticFeedback.lightImpact();
                                setState(() {
                                  _isCollapsed = true;
                                });
                              },
                              icon: const Icon(
                                Icons.close_fullscreen_rounded,
                                size: 20,
                                color: AppColors.textSecondary,
                              ),
                              tooltip: 'Minimizar para botão flutuante',
                            ),
                          ],
                        ),
                        const SizedBox(height: 14),

                        if (widget.driver != null) ...[
                          Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: AppColors.neutralLight,
                              borderRadius: BorderRadius.circular(14),
                            ),
                            child: Row(
                              children: [
                                const Icon(
                                  Icons.directions_car_rounded,
                                  size: 20,
                                  color: AppColors.primary,
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Text(
                                    widget.driver!.vehicle?.description ??
                                        'Sai às ${widget.driver!.departureTime}',
                                    style: const TextStyle(
                                      fontSize: 13,
                                      fontWeight: FontWeight.w700,
                                      color: AppColors.textPrimary,
                                    ),
                                  ),
                                ),
                                if (widget.driver!.estimatedCostPerPassenger !=
                                    null)
                                  Text(
                                    CurrencyFormatter.format(
                                      widget.driver!.estimatedCostPerPassenger!,
                                    ),
                                    style: const TextStyle(
                                      fontSize: 15,
                                      fontWeight: FontWeight.w800,
                                      color: AppColors.primary,
                                    ),
                                  ),
                              ],
                            ),
                          ),
                          const SizedBox(height: 16),
                        ],

                        if (widget.pendingPayment case final payment?)
                          ElevatedButton.icon(
                            onPressed: () => widget.onPay(payment),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: AppColors.primary,
                              foregroundColor: AppColors.textLight,
                              padding: const EdgeInsets.symmetric(vertical: 14),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(16),
                              ),
                              elevation: 0,
                            ),
                            icon: const Icon(Icons.pix_rounded, size: 19),
                            label: Text(
                              payment.amount == null
                                  ? 'Pagar a carona'
                                  : 'Pagar ${CurrencyFormatter.format(payment.amount!)}',
                              style: const TextStyle(
                                fontSize: 14.5,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          )
                        else
                          ElevatedButton.icon(
                            onPressed: widget.onSendMessage,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: AppColors.primary,
                              foregroundColor: AppColors.textLight,
                              padding: const EdgeInsets.symmetric(vertical: 14),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(16),
                              ),
                              elevation: 0,
                            ),
                            icon: const Icon(
                              Icons.chat_bubble_rounded,
                              size: 19,
                            ),
                            label: const Text(
                              'Enviar mensagem ao motorista',
                              style: TextStyle(
                                fontSize: 14.5,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ),
                        const SizedBox(height: 8),

                        // Com a cobrança em aberto, o pedido só termina depois do pagamento
                        if (widget.pendingPayment == null)
                          TextButton(
                            onPressed: widget.onCancel,
                            style: TextButton.styleFrom(
                              foregroundColor: AppColors.textSecondary,
                              padding: const EdgeInsets.symmetric(vertical: 8),
                            ),
                            child: const Text(
                              'Fechar',
                              style: TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}
