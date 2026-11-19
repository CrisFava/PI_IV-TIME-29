// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/details_danger_button.dart';
import '../../../../core/widgets/details_section.dart';
import '../../../../core/widgets/details_stat_tile.dart';
import '../../../ride/presentation/widgets/ride_place_line.dart';
import '../../../route/data/models/route_recurrence.dart';
import '../../data/models/driver_offer.dart';
import '../../data/models/driver_ride_request.dart';
import '../../state/driver_offers_provider.dart';
import '../../state/driver_ride_actions_controller.dart';
import 'driver_offer_title.dart';
import 'driver_ride_card.dart';
import 'matching_message.dart';
import 'offer_ride_requests.dart';

/// Detalhes de uma carona de "Minhas caronas": o trajeto completo, as vagas, cada
/// viagem com os passageiros (e o chat) e "Cancelar carona". Acompanha a lista,
/// então reflete na hora um passageiro aceito ou uma viagem iniciada.
class DriverOfferDetails extends ConsumerStatefulWidget {
  final String offerKey;
  final ValueChanged<String> onStartRide;
  final ValueChanged<String> onCompleteRide;
  final void Function(String rideId, String passengerId) onOpenChat;

  /// Pede a confirmação e cancela; devolve se cancelou.
  final Future<bool> Function(String routeId) onCancelOffer;

  /// Marca um passageiro como ausente na viagem em andamento.
  final void Function(String rideId, String passengerId) onMarkNoShow;

  /// Respostas aos pedidos de vaga desta carona.
  final ValueChanged<DriverRideRequest> onAcceptRequest;
  final ValueChanged<DriverRideRequest> onRejectRequest;
  final ValueChanged<DriverRideRequest> onOpenRequestChat;

  /// Confirma a chegada de um passageiro na viagem em andamento (ele recebe a cobrança).
  final void Function(String rideId, String passengerId) onArrive;

  const DriverOfferDetails({
    super.key,
    required this.offerKey,
    required this.onStartRide,
    required this.onCompleteRide,
    required this.onOpenChat,
    required this.onCancelOffer,
    required this.onMarkNoShow,
    required this.onAcceptRequest,
    required this.onRejectRequest,
    required this.onOpenRequestChat,
    required this.onArrive,
  });

  @override
  ConsumerState<DriverOfferDetails> createState() => _DriverOfferDetailsState();
}

class _DriverOfferDetailsState extends ConsumerState<DriverOfferDetails> {
  bool _cancelling = false;

  // A última versão vista: a lista recarrega sozinha e não deve piscar aqui
  DriverOffer? _offer;

  static String _km(double value) =>
      value.toStringAsFixed(1).replaceAll('.', ',');

  Future<void> _cancel(String offerKey) async {
    // A folha de detalhes antes da espera: depois dela, só fecha se ainda for a da frente
    final sheet = ModalRoute.of(context);
    setState(() => _cancelling = true);
    final cancelled = await widget.onCancelOffer(offerKey);
    if (!mounted) return;
    if (cancelled) {
      if (sheet != null && sheet.isCurrent) Navigator.of(context).pop();
      return;
    }
    setState(() => _cancelling = false);
  }

  @override
  Widget build(BuildContext context) {
    final offers = ref.watch(myDriverOffersProvider).value;
    if (offers != null) {
      _offer = offers.where((o) => o.key == widget.offerKey).firstOrNull;
    }
    final busyRideIds = ref.watch(driverRideActionsProvider);
    final offer = _offer;

    if (offer == null) {
      return const MatchingMessage(
        text: 'Esta carona não está mais em "Minhas caronas".',
      );
    }

    final route = offer.route;
    final firstRide = offer.rides.isEmpty ? null : offer.rides.first;
    final distanceKm = route?.routeDistanceKm;
    final detourKm = route?.maxDetourKm;
    final seats = route?.totalSeats;
    final stats = [
      if (distanceKm != null)
        DetailsStatTile(
          icon: Icons.route_rounded,
          value: '${_km(distanceKm)} km',
          label: 'Percurso',
        ),
      if (detourKm != null)
        DetailsStatTile(
          icon: Icons.radar_rounded,
          value: '${_km(detourKm)} km',
          label: 'Raio de busca',
        ),
      if (seats != null)
        DetailsStatTile(
          icon: Icons.event_seat_rounded,
          value: '$seats',
          label: seats == 1 ? 'Vaga' : 'Vagas',
        ),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        DriverOfferTitle(offer: offer, fontSize: 16),
        if (route != null) ...[
          const SizedBox(height: 4),
          Text(
            route.recurrence == RouteRecurrence.weekly
                ? 'Repete toda semana, no mesmo dia e horário.'
                : 'Carona de um dia só: some depois da viagem.',
            style: const TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: AppColors.textDark,
            ),
          ),
        ],
        DetailsSection(
          title: 'Saindo de',
          child: RidePlaceLine(
            icon: Icons.trip_origin_rounded,
            text: route?.origin.address ?? firstRide?.origin.address ?? '',
            maxLines: null,
            emphasized: true,
          ),
        ),
        DetailsSection(
          title: 'Indo para',
          child: RidePlaceLine(
            icon: Icons.flag_rounded,
            text:
                route?.destination.address ??
                firstRide?.destination.address ??
                '',
            maxLines: null,
            emphasized: true,
          ),
        ),
        if (stats.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 12),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                for (var i = 0; i < stats.length; i++) ...[
                  if (i > 0) const SizedBox(width: 8),
                  Expanded(child: stats[i]),
                ],
              ],
            ),
          ),
        // Pedidos esperando resposta: só aqui, dentro da carona
        OfferRideRequests(
          offer: offer,
          onAccept: widget.onAcceptRequest,
          onReject: widget.onRejectRequest,
          onOpenChat: widget.onOpenRequestChat,
        ),
        DetailsSection(
          title: 'Viagens',
          child: offer.rides.isEmpty
              ? const Text(
                  'Ainda sem pedidos de vaga. Quando alguém pedir, a viagem aparece aqui.',
                  style: TextStyle(fontSize: 13.5, color: AppColors.textDark),
                )
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    for (final ride in offer.rides)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 10),
                        child: DriverRideCard(
                          ride: ride,
                          isBusy: busyRideIds.contains(ride.id),
                          onStart: () => widget.onStartRide(ride.id),
                          onComplete: () => widget.onCompleteRide(ride.id),
                          onOpenChat: (passengerId) =>
                              widget.onOpenChat(ride.id, passengerId),
                          onMarkNoShow: (passengerId) =>
                              widget.onMarkNoShow(ride.id, passengerId),
                          onArrive: (passengerId) =>
                              widget.onArrive(ride.id, passengerId),
                        ),
                      ),
                  ],
                ),
        ),
        // Um cancelar só: a carona e as viagens marcadas dela
        if (route != null || offer.hasOpenRides)
          DetailsDangerButton(
            key: ValueKey('cancel_offer_${offer.key}'),
            label: 'Cancelar carona',
            isBusy: _cancelling,
            onPressed: () => _cancel(offer.key),
          ),
      ],
    );
  }
}
