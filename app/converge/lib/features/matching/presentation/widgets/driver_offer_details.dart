// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/details_danger_button.dart';
import '../../../../core/widgets/details_section.dart';
import '../../../ride/presentation/widgets/ride_place_line.dart';
import '../../../route/data/models/route_recurrence.dart';
import '../../data/models/driver_offer.dart';
import '../../state/driver_offers_provider.dart';
import '../../state/driver_ride_actions_controller.dart';
import 'driver_offer_title.dart';
import 'driver_ride_card.dart';
import 'matching_message.dart';

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

  const DriverOfferDetails({
    super.key,
    required this.offerKey,
    required this.onStartRide,
    required this.onCompleteRide,
    required this.onOpenChat,
    required this.onCancelOffer,
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

  Future<void> _cancel(String routeId) async {
    setState(() => _cancelling = true);
    final cancelled = await widget.onCancelOffer(routeId);
    if (!mounted) return;
    if (cancelled) {
      Navigator.of(context).pop();
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
          title: 'Trajeto',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              RidePlaceLine(
                icon: Icons.trip_origin_rounded,
                text: route?.origin.address ?? firstRide?.origin.address ?? '',
                maxLines: null,
                emphasized: true,
              ),
              const SizedBox(height: 6),
              RidePlaceLine(
                icon: Icons.flag_rounded,
                text:
                    route?.destination.address ??
                    firstRide?.destination.address ??
                    '',
                maxLines: null,
                emphasized: true,
              ),
              if (route?.routeDistanceKm != null)
                _info(
                  Icons.route_rounded,
                  '${_km(route!.routeDistanceKm!)} km de percurso',
                ),
              if (route?.maxDetourKm != null)
                _info(
                  Icons.radar_rounded,
                  'Passageiros a até ${_km(route!.maxDetourKm!)} km do caminho',
                ),
              if (route?.totalSeats != null)
                _info(
                  Icons.event_seat_rounded,
                  route!.totalSeats == 1
                      ? '1 vaga'
                      : '${route.totalSeats} vagas',
                ),
            ],
          ),
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
                        ),
                      ),
                  ],
                ),
        ),
        if (route != null)
          DetailsDangerButton(
            key: ValueKey('cancel_offer_${route.id}'),
            label: 'Cancelar carona',
            isBusy: _cancelling,
            onPressed: () => _cancel(route.id),
          ),
      ],
    );
  }

  Widget _info(IconData icon, String text) => Padding(
    padding: const EdgeInsets.only(top: 10),
    child: Row(
      children: [
        Icon(icon, size: 17, color: AppColors.textPrimary),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            text,
            style: const TextStyle(
              fontSize: 13.5,
              fontWeight: FontWeight.w600,
              color: AppColors.textPrimary,
            ),
          ),
        ),
      ],
    ),
  );
}
