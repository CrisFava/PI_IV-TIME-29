// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app_routes.dart';
import '../../../../core/config/styles.dart';
import '../../../../core/widgets/confirm_dialog.dart';
import '../../../../core/utils/currency_formatter.dart';
import '../../../../core/widgets/details_sheet.dart';
import '../../../../core/widgets/show_app_notice.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../../ride/data/models/participant_status.dart';
import '../../../ride/data/models/ride_model.dart';
import '../../../ride/data/models/ride_participant_model.dart';
import '../../../ride/data/models/ride_status.dart';
import '../../../ride/state/ride_providers.dart';
import '../../data/models/driver_offer.dart';
import '../../data/models/driver_ride_request.dart';
import '../../state/driver_ride_actions_controller.dart';
import '../../state/driver_offers_provider.dart';
import '../../state/driver_rides_provider.dart';
import '../../state/matching_viewer_provider.dart';
import '../../state/passenger_ride_actions_controller.dart';
import '../../state/passenger_rides_provider.dart';
import '../../state/ride_request_actions_controller.dart';
import '../../../route/state/offer_route_controller.dart';
import 'driver_offer_card.dart';
import 'driver_offer_details.dart';
import 'match_date_label.dart';
import 'matching_message.dart';
import 'matching_section_title.dart';
import 'passenger_ride_card.dart';
import 'passenger_ride_details.dart';

/// Painel retrátil do motorista sobre o mapa: as caronas dele (com quem vai, o chat
/// e "Iniciar trajeto") e os pedidos de vaga para aceitar ou recusar.
class RideRequestsSheet extends ConsumerStatefulWidget {
  final VoidCallback onClose;
  final double bottomSafeArea;

  /// A viagem começou: o motorista entra em expediente.
  final VoidCallback? onRideStarted;

  /// "Oferecer carona": abre a folha para publicar uma carona nova.
  final VoidCallback? onNewOffer;

  /// Carona a abrir assim que o painel tiver a lista (ex.: tocou no aviso de
  /// pedido de vaga). [onOfferOpened] avisa que já abriu, para não abrir de novo.
  final String? openOfferKey;
  final VoidCallback? onOfferOpened;

  const RideRequestsSheet({
    super.key,
    required this.onClose,
    this.bottomSafeArea = 0,
    this.onRideStarted,
    this.onNewOffer,
    this.openOfferKey,
    this.onOfferOpened,
  });

  @override
  ConsumerState<RideRequestsSheet> createState() => _RideRequestsSheetState();
}

class _RideRequestsSheetState extends ConsumerState<RideRequestsSheet> {
  // Novos pedidos chegam sem aviso: a lista é recarregada enquanto o painel está aberto.
  static const _refreshInterval = Duration(seconds: 10);

  Timer? _refreshTimer;

  // Aba escolhida quando há caronas nos dois papéis (nulo: a automática)
  bool? _showPassengerTab;

  @override
  void initState() {
    super.initState();
    // As caronas também: o backend encerra sozinho a de um dia só
    _refreshTimer = Timer.periodic(_refreshInterval, (_) {
      ref.invalidate(myRidesProvider);
      ref.invalidate(myOfferRoutesProvider);
    });
    _openRequestedOffer();
  }

  @override
  void didUpdateWidget(covariant RideRequestsSheet oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.openOfferKey != oldWidget.openOfferKey) _openRequestedOffer();
  }

  /// Abre os detalhes da carona pedida em [RideRequestsSheet.openOfferKey].
  Future<void> _openRequestedOffer() async {
    final key = widget.openOfferKey;
    if (key == null) return;
    widget.onOfferOpened?.call();
    try {
      final offers = await ref.read(myDriverOffersProvider.future);
      final offer = offers.where((o) => o.key == key).firstOrNull;
      if (mounted && offer != null) _openDetails(offer);
    } catch (_) {
      // Sem a lista agora: o painel mostra o erro, e o motorista abre a carona por ele
    }
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

  void _openDetails(DriverOffer offer) {
    showDetailsSheet<void>(
      context,
      title: 'Detalhes da carona',
      builder: (sheetContext) => DriverOfferDetails(
        offerKey: offer.key,
        // Iniciar ou concluir fecha os detalhes e volta para o mapa
        onStartRide: (rideId) {
          Navigator.of(sheetContext).pop();
          _startRide(rideId);
        },
        onCompleteRide: (rideId) {
          Navigator.of(sheetContext).pop();
          _completeRide(rideId);
        },
        onOpenChat: _openChatWith,
        onCancelOffer: _cancelOffer,
        onMarkNoShow: _markNoShow,
        onAcceptRequest: _accept,
        onRejectRequest: _reject,
        onOpenRequestChat: _openChat,
        onArrive: _arrivePassenger,
        onRemovePassenger: _removePassenger,
      ),
    );
  }

  /// Confirmação padrão das ações que não dá para desfazer.
  Future<bool> _confirm({
    required String title,
    required String message,
    required String confirmLabel,
    bool destructive = true,
  }) async {
    final confirmed = await showConfirmDialog(
      context,
      title: title,
      message: message,
      confirmLabel: confirmLabel,
      destructive: destructive,
    );
    return confirmed && mounted;
  }

  /// O motorista cancela só uma viagem (os confirmados são avisados).
  /// Durante a viagem, o motorista marca quem não apareceu.
  Future<void> _markNoShow(String rideId, String passengerId) async {
    final confirmed = await _confirm(
      title: 'Marcar como ausente?',
      message: 'A vaga é liberada e o passageiro não é cobrado nem entra nos indicadores.',
      confirmLabel: 'Marcar ausente',
    );
    if (!confirmed) return;
    final error = await ref
        .read(driverRideActionsProvider.notifier)
        .markNoShow(rideId, passengerId);
    if (mounted && error != null) _showMessage(error);
  }

  /// Tira um passageiro confirmado da viagem, antes de ela começar. Pede confirmação
  /// com o nome e a viagem; o passageiro é avisado pelo backend.
  Future<void> _removePassenger(String rideId, String passengerId) async {
    final ride = (ref.read(myRidesProvider).value ?? const <RideModel>[])
        .where((r) => r.id == rideId)
        .firstOrNull;
    final passenger = ride?.passengerOf(passengerId);
    if (ride == null || passenger == null) return;
    final departure = ride.departureDateTime;
    final confirmed = await _confirm(
      title: 'Remover ${passenger.displayName}?',
      message:
          '${passenger.displayName} sai da viagem'
          '${departure == null ? '' : ' de ${matchDateTimeLabel(departure)}'}'
          ' e é avisado(a). Remover um passageiro confirmado conta na sua reputação.',
      confirmLabel: 'Remover',
    );
    if (!confirmed) return;
    final error = await ref
        .read(rideRequestActionsProvider.notifier)
        .reject(DriverRideRequest(ride: ride, passenger: passenger));
    if (!mounted) return;
    if (error != null) {
      _showMessage(error);
      return;
    }
    showAppNotice(
      context,
      '${passenger.displayName} foi removido(a) da viagem.',
      type: AppNoticeType.success,
    );
  }

  /// "Chegou": o passageiro desceu, a viagem dele termina e ele recebe a cobrança do rateio.
  Future<void> _arrivePassenger(String rideId, String passengerId) async {
    final rides = ref.read(myRidesProvider).value ?? const <RideModel>[];
    RideParticipantModel? passenger;
    for (final ride in rides) {
      if (ride.id == rideId) passenger = ride.passengerOf(passengerId);
    }
    final estimate = passenger?.estimatedAmount;
    final amountHint = estimate != null && estimate > 0
        ? ' (cerca de ${CurrencyFormatter.format(estimate)})'
        : '';

    final confirmed = await _confirm(
      title:
          'Confirmar a chegada de ${passenger?.displayName ?? 'o passageiro'}?',
      message:
          'A viagem dele termina aqui e ele recebe a cobrança do rateio pelo PIX$amountHint.',
      confirmLabel: 'Confirmar chegada',
      destructive: false,
    );
    if (!confirmed) return;
    final error = await ref
        .read(driverRideActionsProvider.notifier)
        .arrive(rideId, passengerId);
    if (mounted && error != null) _showMessage(error);
  }

  /// Detalhes de uma viagem em que o usuário vai como passageiro.
  void _openPassengerDetails(RideModel ride, String viewerId) {
    showDetailsSheet<void>(
      context,
      title: 'Detalhes da viagem',
      builder: (_) => PassengerRideDetails(
        rideId: ride.id,
        viewerId: viewerId,
        onOpenChat: (rideId) => _openChatWith(rideId, viewerId),
        onLeave: _leaveRide,
      ),
    );
  }

  /// O passageiro sai da viagem antes de ela começar. A menos de 2 h da saída,
  /// conta como cancelamento tardio.
  Future<bool> _leaveRide(RideModel ride) async {
    final departure = ride.departureDateTime;
    final late =
        departure != null &&
        departure.difference(DateTime.now()) < const Duration(hours: 2);
    final confirmed = await _confirm(
      title: 'Sair da viagem?',
      message: late
          ? 'Faltam menos de 2 h para a saída: sair agora conta como cancelamento tardio. O motorista será avisado.'
          : 'Sua vaga será liberada e o motorista será avisado.',
      confirmLabel: 'Sair da viagem',
    );
    if (!confirmed) return false;
    final error = await ref
        .read(passengerRideActionsProvider.notifier)
        .leave(ride.id);
    if (!mounted) return false;
    if (error != null) {
      _showMessage(error);
      return false;
    }
    return true;
  }

  /// Pede a confirmação e cancela a carona; devolve se cancelou.
  /// Pede a confirmação e cancela a carona: as viagens marcadas dela (os
  /// confirmados são avisados) e a oferta, se ainda existir. Devolve se cancelou.
  Future<bool> _cancelOffer(String offerKey) async {
    final offer =
        (ref.read(myDriverOffersProvider).value ?? const <DriverOffer>[])
            .where((o) => o.key == offerKey)
            .firstOrNull;
    if (offer == null) return false;
    final hasConfirmed = offer.confirmedPassengers > 0;
    final confirmed = await _confirm(
      title: 'Cancelar esta carona?',
      message: hasConfirmed
          ? 'Ela deixa de receber pedidos, e os passageiros confirmados são avisados. Cancelar com passageiro confirmado conta na sua reputação.'
          : 'Ela deixa de receber pedidos de vaga.',
      confirmLabel: 'Cancelar carona',
    );
    if (!confirmed) return false;

    // A carona primeiro: cancelar a viagem de uma carona de um dia só já a
    // encerra no backend, e cancelar a carona depois daria "não encontrado"
    final routeId = offer.route?.id;
    if (routeId != null) {
      try {
        await ref.read(offerRouteProvider.notifier).cancel(routeId);
      } catch (e) {
        // Já encerrada por outro caminho: a lista recarrega e ela some
        ref.invalidate(myOfferRoutesProvider);
        if (mounted) _showMessage(authErrorMessage(e));
        return false;
      }
    }

    String? rideError;
    for (final ride in offer.openRides) {
      final error = await ref
          .read(driverRideActionsProvider.notifier)
          .cancel(ride.id);
      if (error != null) {
        rideError ??= error;
        // Sem a carona cancelada, nada mudou ainda: para aqui
        if (routeId == null) break;
      }
    }

    ref.invalidate(myOfferRoutesProvider);
    ref.invalidate(myRidesProvider);
    if (rideError != null && mounted) _showMessage(rideError);
    // A carona já foi cancelada: os detalhes fecham mesmo com uma viagem pendente
    return routeId != null || rideError == null;
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

    // Fica no painel: o chat com o passageiro está a um toque, no card
    showAppNotice(
      context,
      '${request.passenger.displayName} confirmado(a) na viagem.',
      type: AppNoticeType.success,
    );
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

  /// Nomes dos passageiros confirmados numa viagem do motorista ("Ana e Bruno").
  String _confirmedNames(String rideId) {
    final ride = (ref.read(myDrivenRidesProvider).value ?? const <RideModel>[])
        .where((r) => r.id == rideId)
        .firstOrNull;
    final names = [
      for (final p in ride?.participants ?? const <RideParticipantModel>[])
        if (p.isPassenger && p.status == ParticipantStatus.confirmed)
          p.displayName,
    ];
    if (names.length <= 1) return names.join();
    return '${names.sublist(0, names.length - 1).join(', ')} e ${names.last}';
  }

  Future<void> _startRide(String rideId) async {
    final names = _confirmedNames(rideId);
    final confirmed = await _confirm(
      title: 'Iniciar a viagem?',
      message: names.isEmpty
          ? 'Os pedidos que ainda esperam resposta serão recusados.'
          : 'Você vai buscar $names. Os pedidos que ainda esperam resposta serão recusados.',
      confirmLabel: 'Iniciar',
      destructive: false,
    );
    if (!confirmed) return;
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
    final confirmed = await _confirm(
      title: 'Concluir a viagem?',
      message: 'Os valores finais serão calculados com quem foi. Marque antes quem não apareceu.',
      confirmLabel: 'Concluir',
      destructive: false,
    );
    if (!confirmed) return;
    final error = await ref
        .read(driverRideActionsProvider.notifier)
        .complete(rideId);
    if (mounted && error != null) _showMessage(error);
  }

  void _showMessage(String message) =>
      showAppNotice(context, message, type: AppNoticeType.error);

  @override
  Widget build(BuildContext context) {
    final offers = ref.watch(myDriverOffersProvider);
    final passengerRides = ref.watch(myPassengerRidesProvider);
    final viewerId = ref.watch(matchingViewerIdProvider);
    final rideActions = ref.watch(driverRideActionsProvider);

    return DraggableScrollableSheet(
      // Abre um pouco acima da metade, para o conteúdo não ficar atrás da navbar
      initialChildSize: 0.52,
      minChildSize: 0.10,
      maxChildSize: 0.88,
      snap: true,
      snapSizes: const [0.22, 0.52, 0.88],
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
                    // Uma lista só: as viagens como passageiro e as caronas oferecidas
                    ..._ridesList(
                      offers,
                      passengerRides,
                      viewerId,
                      rideActions,
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

  List<Widget> _ridesList(
    AsyncValue<List<DriverOffer>> offers,
    AsyncValue<List<RideModel>> passengerRides,
    String? viewerId,
    Set<String> rideActions,
  ) {
    final offerList = offers.value;
    final passengerList = passengerRides.value;
    if (offerList == null && passengerList == null) {
      if (offers.hasError) {
        return [MatchingMessage(text: authErrorMessage(offers.error!))];
      }
      return const [
        Padding(
          padding: EdgeInsets.all(28),
          child: Center(
            child: CircularProgressIndicator(color: AppColors.primary),
          ),
        ),
      ];
    }

    final asPassenger = passengerList ?? const <RideModel>[];
    final asDriver = offerList ?? const <DriverOffer>[];
    if (asPassenger.isEmpty && asDriver.isEmpty) {
      return const [MatchingMessage(text: 'Você não tem caronas ativas no momento.')];
    }

    // A viagem em andamento como passageiro vem primeiro; as caronas oferecidas
    // já chegam ordenadas (em andamento, depois pela próxima viagem)
    final inProgress = [
      for (final ride in asPassenger)
        if (ride.status == RideStatus.inProgress) ride,
    ];
    final upcoming = [
      for (final ride in asPassenger)
        if (ride.status != RideStatus.inProgress) ride,
    ];

    Widget passengerCard(RideModel ride) => Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: PassengerRideCard(
        ride: ride,
        viewerId: viewerId ?? '',
        onOpenDetails: () => _openPassengerDetails(ride, viewerId ?? ''),
      ),
    );

    final driverCards = [
      for (final offer in asDriver)
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: DriverOfferCard(
            offer: offer,
            busyRideIds: rideActions,
            onOpenDetails: () => _openDetails(offer),
            onStartRide: _startRide,
            onCompleteRide: _completeRide,
          ),
        ),
    ];
    final passengerCards = [
      for (final ride in inProgress) passengerCard(ride),
      for (final ride in upcoming) passengerCard(ride),
    ];

    // Um papel só: a lista dele, com o título. Os dois: abas lado a lado
    if (asPassenger.isEmpty) {
      return [
        const MatchingSectionTitle(title: 'Como motorista'),
        ...driverCards,
      ];
    }
    if (asDriver.isEmpty) {
      return [
        const MatchingSectionTitle(title: 'Como passageiro'),
        ...passengerCards,
      ];
    }

    // Abre na aba da viagem em andamento como passageiro; senão, na de motorista
    final showPassenger = _showPassengerTab ?? inProgress.isNotEmpty;
    final openRequests = asDriver.fold(0, (sum, o) => sum + o.openRequests);
    void select(bool passenger) {
      if (passenger == showPassenger) return;
      HapticFeedback.lightImpact();
      setState(() => _showPassengerTab = passenger);
    }

    return [
      Row(
        children: [
          Expanded(
            child: _RoleTabButton(
              label: 'Como motorista',
              icon: Icons.directions_car_rounded,
              selected: !showPassenger,
              badgeCount: openRequests,
              onTap: () => select(false),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: _RoleTabButton(
              label: 'Como passageiro',
              icon: Icons.person_rounded,
              selected: showPassenger,
              onTap: () => select(true),
            ),
          ),
        ],
      ),
      const SizedBox(height: 14),
      ...(showPassenger ? passengerCards : driverCards),
    ];
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
            'Caronas ativas',
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
}

/// Aba de "Caronas ativas" (motorista ou passageiro), no mesmo estilo das abas
/// dos indicadores. [badgeCount] mostra os pedidos de vaga esperando resposta.
class _RoleTabButton extends StatelessWidget {
  final String label;
  final IconData icon;
  final bool selected;
  final int badgeCount;
  final VoidCallback onTap;

  const _RoleTabButton({
    required this.label,
    required this.icon,
    required this.selected,
    required this.onTap,
    this.badgeCount = 0,
  });

  @override
  Widget build(BuildContext context) {
    final color = selected ? AppColors.dockItemActive : AppColors.textPrimary;
    return Material(
      color: selected ? AppColors.primary : AppColors.cardBackground,
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        key: ValueKey('my_rides_tab_$label'),
        borderRadius: BorderRadius.circular(14),
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: AppColors.primary),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, size: 18, color: color),
              const SizedBox(width: 6),
              Flexible(
                child: Text(
                  label,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: 13.5,
                    fontWeight: FontWeight.w700,
                    color: color,
                  ),
                ),
              ),
              if (badgeCount > 0) ...[
                const SizedBox(width: 6),
                Container(
                  constraints: const BoxConstraints(minWidth: 20),
                  padding: const EdgeInsets.symmetric(
                    horizontal: 5,
                    vertical: 1,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.error,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    badgeCount > 99 ? '99+' : '$badgeCount',
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w800,
                      color: AppColors.textLight,
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
