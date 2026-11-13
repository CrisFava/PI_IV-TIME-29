// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app_routes.dart';
import '../../../../core/config/styles.dart';
import '../../../../core/widgets/show_app_notice.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../../ride/data/models/participant_status.dart';
import '../../../ride/state/ride_providers.dart';
import '../../data/models/driver_ride_request.dart';
import '../../state/driver_ride_actions_controller.dart';
import '../../state/driver_ride_requests_provider.dart';
import '../../state/driver_offers_provider.dart';
import '../../state/ride_request_actions_controller.dart';
import '../../../route/state/offer_route_controller.dart';
import 'driver_offer_card.dart';
import 'matching_message.dart';
import 'matching_section_title.dart';
import 'ride_request_card.dart';

/// Painel retrátil do motorista sobre o mapa: as caronas dele (com quem vai, o chat
/// e "Iniciar trajeto") e os pedidos de vaga para aceitar ou recusar.
class RideRequestsSheet extends ConsumerStatefulWidget {
  final VoidCallback onClose;
  final double bottomSafeArea;

  /// A viagem começou: o motorista entra em expediente.
  final VoidCallback? onRideStarted;

  /// "Oferecer carona": abre a folha para publicar uma carona nova.
  final VoidCallback? onNewOffer;

  const RideRequestsSheet({
    super.key,
    required this.onClose,
    this.bottomSafeArea = 0,
    this.onRideStarted,
    this.onNewOffer,
  });

  @override
  ConsumerState<RideRequestsSheet> createState() => _RideRequestsSheetState();
}

class _RideRequestsSheetState extends ConsumerState<RideRequestsSheet> {
  // Novos pedidos chegam sem aviso: a lista é recarregada enquanto o painel está aberto.
  static const _refreshInterval = Duration(seconds: 10);

  Timer? _refreshTimer;

  @override
  void initState() {
    super.initState();
    _refreshTimer = Timer.periodic(
      _refreshInterval,
      (_) => ref.invalidate(myRidesProvider),
    );
  }

  @override
  void dispose() {
    _refreshTimer?.cancel();
    super.dispose();
  }

  void _refresh() {
    HapticFeedback.lightImpact();
    ref.invalidate(myRidesProvider);
    ref.invalidate(myOfferRoutesProvider);
  }

  // Ofertas sendo canceladas (o botão vira progresso)
  final Set<String> _cancellingOfferIds = {};

  Future<void> _cancelOffer(String routeId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppColors.cardBackground,
        title: const Text('Cancelar esta carona?'),
        content: const Text(
          'Ela deixa de receber pedidos de vaga. Quem já foi confirmado continua na viagem.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Voltar'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: TextButton.styleFrom(foregroundColor: AppColors.error),
            child: const Text('Cancelar carona'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    setState(() => _cancellingOfferIds.add(routeId));
    try {
      await ref.read(offerRouteProvider.notifier).cancel(routeId);
      ref.invalidate(myOfferRoutesProvider);
    } catch (e) {
      if (mounted) _showMessage(authErrorMessage(e));
    } finally {
      if (mounted) setState(() => _cancellingOfferIds.remove(routeId));
    }
  }

  Future<void> _accept(DriverRideRequest request) async {
    final error = await ref
        .read(rideRequestActionsProvider.notifier)
        .accept(request);
    if (!mounted) return;

    if (error != null) {
      _showMessage(error);
      return;
    }

    _openChat(request);
  }

  Future<void> _reject(DriverRideRequest request) async {
    final error = await ref
        .read(rideRequestActionsProvider.notifier)
        .reject(request);
    if (!mounted) return;

    if (error != null) _showMessage(error);
  }

  void _openChat(DriverRideRequest request) =>
      _openChatWith(request.rideId, request.passengerId);

  void _openChatWith(String rideId, String passengerId) {
    context.push(AppRoutes.chatOf(rideId: rideId, passengerId: passengerId));
  }

  Future<void> _startRide(String rideId) async {
    final error = await ref
        .read(driverRideActionsProvider.notifier)
        .start(rideId);
    if (!mounted) return;
    if (error != null) {
      _showMessage(error);
      return;
    }
    widget.onRideStarted?.call();
  }

  Future<void> _completeRide(String rideId) async {
    final error = await ref
        .read(driverRideActionsProvider.notifier)
        .complete(rideId);
    if (mounted && error != null) _showMessage(error);
  }

  void _showMessage(String message) =>
      showAppNotice(context, message, type: AppNoticeType.error);

  @override
  Widget build(BuildContext context) {
    final requests = ref.watch(driverRideRequestsProvider);
    final busy = ref.watch(rideRequestActionsProvider);
    final offers = ref.watch(myDriverOffersProvider);
    final rideActions = ref.watch(driverRideActionsProvider);

    return DraggableScrollableSheet(
      initialChildSize: 0.46,
      minChildSize: 0.10,
      maxChildSize: 0.88,
      snap: true,
      snapSizes: const [0.22, 0.46, 0.88],
      builder: (context, scrollController) {
        return Container(
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: const BorderRadius.vertical(top: Radius.circular(26)),
            border: Border.all(color: AppColors.borderSubtle, width: 1),
            boxShadow: [
              BoxShadow(
                color: AppColors.primary.withValues(alpha: 0.12),
                blurRadius: 24,
                offset: const Offset(0, -6),
              ),
            ],
          ),
          child: CustomScrollView(
            controller: scrollController,
            slivers: [
              // Alça de arraste
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
                    _buildHeader(),
                    if (widget.onNewOffer != null) ...[
                      const SizedBox(height: 10),
                      FilledButton.icon(
                        key: const ValueKey('new_offer_button'),
                        onPressed: () {
                          HapticFeedback.mediumImpact();
                          widget.onNewOffer!();
                        },
                        style: FilledButton.styleFrom(
                          backgroundColor: AppColors.primary,
                          foregroundColor: AppColors.textLight,
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(14),
                          ),
                        ),
                        icon: const Icon(Icons.add_rounded, size: 20),
                        label: const Text(
                          'Oferecer carona',
                          style: TextStyle(fontWeight: FontWeight.w700),
                        ),
                      ),
                    ],
                    const SizedBox(height: 18),
                    // Pedidos de vaga primeiro: é o que pede uma resposta
                    ...requests.when(
                      data: (list) => _requestSections(list, busy),
                      loading: () => const [SizedBox.shrink()],
                      error: (error, _) => [
                        MatchingMessage(text: authErrorMessage(error)),
                      ],
                    ),
                    const MatchingSectionTitle(title: 'Caronas oferecidas'),
                    ...offers.when(
                      data: (list) => list.isEmpty
                          ? const [
                              MatchingMessage(
                                text: 'Você ainda não ofereceu nenhuma carona.',
                              ),
                            ]
                          : [
                              for (final offer in list)
                                Padding(
                                  padding: const EdgeInsets.only(bottom: 12),
                                  child: DriverOfferCard(
                                    offer: offer,
                                    busyRideIds: rideActions,
                                    isCancelling: _cancellingOfferIds.contains(
                                      offer.route?.id,
                                    ),
                                    onStartRide: _startRide,
                                    onCompleteRide: _completeRide,
                                    onOpenChat: _openChatWith,
                                    onCancelOffer: () =>
                                        _cancelOffer(offer.route!.id),
                                  ),
                                ),
                            ],
                      loading: () => const [
                        Padding(
                          padding: EdgeInsets.all(28),
                          child: Center(
                            child: CircularProgressIndicator(
                              color: AppColors.primary,
                            ),
                          ),
                        ),
                      ],
                      error: (error, _) => [
                        MatchingMessage(text: authErrorMessage(error)),
                      ],
                    ),
                  ]),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildHeader() {
    return Row(
      children: [
        const Icon(
          Icons.directions_car_rounded,
          size: 20,
          color: AppColors.textPrimary,
        ),
        const SizedBox(width: 8),
        const Expanded(
          child: Text(
            'Minhas caronas',
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w800,
              color: AppColors.textPrimary,
              letterSpacing: -0.3,
            ),
          ),
        ),
        IconButton(
          tooltip: 'Atualizar pedidos',
          visualDensity: VisualDensity.compact,
          onPressed: _refresh,
          icon: const Icon(
            Icons.refresh_rounded,
            size: 20,
            color: AppColors.textPrimary,
          ),
        ),
        IconButton(
          key: const ValueKey('close_ride_requests_sheet'),
          tooltip: 'Fechar pedidos de vaga',
          visualDensity: VisualDensity.compact,
          onPressed: () {
            HapticFeedback.lightImpact();
            widget.onClose();
          },
          icon: const Icon(
            Icons.close_rounded,
            size: 20,
            color: AppColors.textPrimary,
          ),
        ),
      ],
    );
  }

  List<Widget> _requestSections(
    List<DriverRideRequest> list,
    Set<String> busy,
  ) {
    final pending = [
      for (final r in list)
        if (r.passenger.status == ParticipantStatus.requested) r,
    ];

    if (pending.isEmpty) return const [];

    return [
      const MatchingSectionTitle(title: 'Pedidos de vaga'),
      for (final request in pending)
        Padding(
          padding: const EdgeInsets.only(bottom: 10),
          child: RideRequestCard(
            request: request,
            isBusy: busy.contains(request.key),
            onAccept: () => _accept(request),
            onReject: () => _reject(request),
            onOpenChat: () => _openChat(request),
          ),
        ),
      const SizedBox(height: 14),
    ];
  }
}
