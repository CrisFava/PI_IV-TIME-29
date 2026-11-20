// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/more_info_hint.dart';
import '../../../ride/data/models/ride_status.dart';
import '../../../ride/presentation/widgets/ride_place_line.dart';
import '../../data/models/driver_offer.dart';
import 'driver_offer_title.dart';
import 'driver_ride_action_button.dart';

/// Uma carona em "Minhas caronas", resumida: quando sai, de onde para onde e como
/// estão os pedidos. Tocar abre os detalhes (passageiros, chat e cancelar).
/// "Iniciar trajeto" e "Concluir viagem" ficam à mão aqui mesmo.
class DriverOfferCard extends StatelessWidget {
  final DriverOffer offer;
  final Set<String> busyRideIds;
  final VoidCallback onOpenDetails;
  final ValueChanged<String> onStartRide;
  final ValueChanged<String> onCompleteRide;

  const DriverOfferCard({
    super.key,
    required this.offer,
    required this.onOpenDetails,
    required this.onStartRide,
    required this.onCompleteRide,
    this.busyRideIds = const {},
  });

  String _summary() {
    if (offer.rides.any((r) => r.status == RideStatus.inProgress)) {
      return 'Viagem em andamento';
    }
    final confirmed = offer.confirmedPassengers;
    final pending = offer.openRequests;
    return [
      if (confirmed > 0)
        confirmed == 1
            ? '1 passageiro confirmado'
            : '$confirmed passageiros confirmados',
      if (pending > 0)
        pending == 1 ? '1 pedido esperando' : '$pending pedidos esperando',
      if (confirmed == 0 && pending == 0) 'Ainda sem pedidos de vaga',
    ].join(' · ');
  }

  @override
  Widget build(BuildContext context) {
    final route = offer.route;
    final firstRide = offer.rides.isEmpty ? null : offer.rides.first;
    final origin = route?.origin.address ?? firstRide?.origin.address ?? '';
    final destination =
        route?.destination.address ?? firstRide?.destination.address ?? '';
    final action = offer.actionableRide;
    final inProgress = action?.status == RideStatus.inProgress;

    return Material(
      color: inProgress ? AppColors.successBackground : AppColors.cardSecondary,
      borderRadius: BorderRadius.circular(20),
      child: InkWell(
        key: ValueKey('offer_card_${offer.key}'),
        borderRadius: BorderRadius.circular(20),
        onTap: () {
          HapticFeedback.lightImpact();
          onOpenDetails();
        },
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(20),
            border: Border.all(
              color: inProgress
                  ? AppColors.successBorder
                  : AppColors.borderSubtle,
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  Expanded(child: DriverOfferTitle(offer: offer)),
                  if (offer.openRequests > 0) ...[
                    const SizedBox(width: 8),
                    _RequestsBadge(count: offer.openRequests),
                  ],
                ],
              ),
              const SizedBox(height: 8),
              RidePlaceLine(icon: Icons.trip_origin_rounded, text: origin),
              const SizedBox(height: 4),
              RidePlaceLine(icon: Icons.flag_rounded, text: destination),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: Text(
                      _summary(),
                      style: TextStyle(
                        fontSize: 12.5,
                        fontWeight: FontWeight.w600,
                        color: inProgress
                            ? AppColors.success
                            : AppColors.textSecondary,
                      ),
                    ),
                  ),
                  const MoreInfoHint(),
                ],
              ),
              if (action != null) ...[
                const SizedBox(height: 10),
                DriverRideActionButton(
                  ride: action,
                  isBusy: busyRideIds.contains(action.id),
                  onStart: () => onStartRide(action.id),
                  onComplete: () => onCompleteRide(action.id),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

/// Bolinha vermelha com o número de pedidos de vaga esperando resposta.
class _RequestsBadge extends StatelessWidget {
  final int count;

  const _RequestsBadge({required this.count});

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: count == 1 ? '1 pedido de vaga' : '$count pedidos de vaga',
      child: Container(
        key: const ValueKey('offer_requests_badge'),
        constraints: const BoxConstraints(minWidth: 22, minHeight: 22),
        padding: const EdgeInsets.symmetric(horizontal: 6),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: AppColors.error,
          borderRadius: BorderRadius.circular(11),
        ),
        child: Text(
          count > 99 ? '99+' : '$count',
          style: const TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w800,
            color: AppColors.textLight,
          ),
        ),
      ),
    );
  }
}
