// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app_routes.dart';
import '../../../../core/config/styles.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../../ride/data/models/participant_status.dart';
import '../../../ride/state/ride_providers.dart';
import '../../data/models/driver_ride_request.dart';
import '../../state/driver_matches_provider.dart';
import '../../state/driver_ride_requests_provider.dart';
import '../../state/ride_request_actions_controller.dart';
import '../widgets/matching_message.dart';
import '../widgets/matching_section_title.dart';
import '../widgets/offer_matches_section.dart';
import '../widgets/ride_request_card.dart';

/// Painel do motorista: pedidos de vaga para aceitar ou recusar, passageiros já aceitos
/// (com o atalho para o chat) e passageiros compatíveis com os seus trajetos de oferta.
class RideRequestsPage extends ConsumerStatefulWidget {
  const RideRequestsPage({super.key});

  @override
  ConsumerState<RideRequestsPage> createState() => _RideRequestsPageState();
}

class _RideRequestsPageState extends ConsumerState<RideRequestsPage> {
  // Novos pedidos chegam sem aviso: a lista é recarregada enquanto a tela está aberta.
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

  Future<void> _refresh() async {
    ref.invalidate(myRidesProvider);
    ref.invalidate(myActiveOfferRoutesProvider);
    await ref.read(driverRideRequestsProvider.future);
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

    _showMessage(
      '${request.passenger.displayName} foi aceito. O chat está liberado.',
    );
    _openChat(request);
  }

  Future<void> _reject(DriverRideRequest request) async {
    final error = await ref
        .read(rideRequestActionsProvider.notifier)
        .reject(request);
    if (!mounted) return;

    _showMessage(
      error ?? 'Pedido de ${request.passenger.displayName} recusado.',
    );
  }

  void _openChat(DriverRideRequest request) {
    context.push(
      AppRoutes.chatOf(
        rideId: request.rideId,
        passengerId: request.passengerId,
      ),
    );
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(message),
          behavior: SnackBarBehavior.floating,
          backgroundColor: AppColors.primary,
        ),
      );
  }

  @override
  Widget build(BuildContext context) {
    final requests = ref.watch(driverRideRequestsProvider);
    final busy = ref.watch(rideRequestActionsProvider);
    final offers = ref.watch(myActiveOfferRoutesProvider);

    return Scaffold(
      backgroundColor: AppColors.scaffoldBackground,
      appBar: AppBar(
        backgroundColor: AppColors.cardBackground,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
        title: const Text(
          'Pedidos de vaga',
          style: TextStyle(fontWeight: FontWeight.w800, fontSize: 18),
        ),
      ),
      body: RefreshIndicator(
        color: AppColors.primary,
        onRefresh: _refresh,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 32),
          children: [
            ...requests.when(
              data: (list) => _requestSections(list, busy),
              loading: () => const [
                Padding(
                  padding: EdgeInsets.all(32),
                  child: Center(
                    child: CircularProgressIndicator(color: AppColors.primary),
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
          ],
        ),
      ),
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
