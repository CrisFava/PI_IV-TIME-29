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
import '../../state/driver_matches_provider.dart';
import '../../state/driver_ride_requests_provider.dart';
import '../../state/ride_request_actions_controller.dart';
import 'matching_message.dart';
import 'matching_section_title.dart';
import 'offer_matches_section.dart';
import 'ride_request_card.dart';
import 'shift_destination_card.dart';

/// Painel retrátil do motorista sobre o mapa, no mesmo formato do painel de expediente:
/// pedidos de vaga para aceitar ou recusar, passageiros já aceitos (com o atalho
/// para o chat) e passageiros compatíveis com os seus trajetos de oferta.
class RideRequestsSheet extends ConsumerStatefulWidget {
  final VoidCallback onClose;
  final double bottomSafeArea;

  /// Destino do expediente em andamento, mostrado no topo do painel.
  final String? destinationAddress;

  const RideRequestsSheet({
    super.key,
    required this.onClose,
    this.bottomSafeArea = 0,
    this.destinationAddress,
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
    ref.invalidate(myActiveOfferRoutesProvider);
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

  void _openChat(DriverRideRequest request) {
    context.push(
      AppRoutes.chatOf(
        rideId: request.rideId,
        passengerId: request.passengerId,
      ),
    );
  }

  void _showMessage(String message) =>
      showAppNotice(context, message, type: AppNoticeType.error);

  @override
  Widget build(BuildContext context) {
    final requests = ref.watch(driverRideRequestsProvider);
    final busy = ref.watch(rideRequestActionsProvider);
    final offers = ref.watch(myActiveOfferRoutesProvider);

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
                    if (widget.destinationAddress?.isNotEmpty ?? false) ...[
                      const SizedBox(height: 8),
                      ShiftDestinationCard(address: widget.destinationAddress!),
                    ],
                    const SizedBox(height: 14),
                    ...requests.when(
                      data: (list) => _requestSections(list, busy),
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
                    const SizedBox(height: 24),
                    const MatchingSectionTitle(
                      title: 'Passageiros compatíveis',
                      subtitle: 'Quem procura carona no seu caminho e horário',
                    ),
                    ...offers.when(
                      data: (routes) => routes.isEmpty
                          ? const [
                              MatchingMessage(
                                text: 'Você não tem um trajeto de oferta ativo. Salve um trajeto como motorista para aparecer no matching.',
                              ),
                            ]
                          : [
                              for (final route in routes)
                                OfferMatchesSection(route: route),
                            ],
                      loading: () => const [SizedBox.shrink()],
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
        const Icon(Icons.inbox_rounded, size: 20, color: AppColors.textPrimary),
        const SizedBox(width: 8),
        const Expanded(
          child: Text(
            'Pedidos de vaga',
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
    final accepted = [
      for (final r in list)
        if (r.passenger.status.hasSeat) r,
    ];

    Widget card(DriverRideRequest request) => Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: RideRequestCard(
        request: request,
        isBusy: busy.contains(request.key),
        onAccept: () => _accept(request),
        onReject: () => _reject(request),
        onOpenChat: () => _openChat(request),
      ),
    );

    return [
      const MatchingSectionTitle(
        title: 'Aguardando resposta',
        subtitle: 'Aceite para liberar o chat com o passageiro',
      ),
      if (pending.isEmpty)
        const MatchingMessage(text: 'Nenhum pedido de vaga no momento.')
      else
        ...pending.map(card),
      if (accepted.isNotEmpty) ...[
        const SizedBox(height: 24),
        const MatchingSectionTitle(title: 'Passageiros aceitos'),
        ...accepted.map(card),
      ],
    ];
  }
}
