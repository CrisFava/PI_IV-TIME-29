// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/services/maps/google_maps_service.dart';
import '../../../ride/data/models/ride_model.dart';
import '../../../ride/presentation/widgets/ride_route_map.dart';
import '../../../ride/presentation/widgets/route_step_card.dart';
import '../../../ride/state/trip_progress.dart';
import '../../../route/data/models/route_place_model.dart';
import '../../state/passenger_segment_provider.dart';

/// O caminho do passageiro em dois passos, no mesmo padrão do trajeto do motorista:
/// "Motorista indo até você" (do início dele até o embarque) e "Até o seu destino"
/// (do embarque ao desembarque). O mapa desenha um caminho limpo entre esses pontos,
/// sem os desvios até as paradas dos outros (que mostrariam onde eles embarcam e
/// descem); as paradas deles aparecem só contadas, e os tempos são os reais, com elas.
class PassengerRoutePreview extends ConsumerStatefulWidget {
  final RoutePlaceModel driverOrigin;
  final RoutePlaceModel pickup;
  final RoutePlaceModel dropOff;

  /// Paradas de outros passageiros antes do embarque e durante o trecho dele.
  final int? stopsBeforePickup;
  final int? stopsDuringRide;

  /// Tempos reais (com as paradas): até o motorista chegar e com ele no carro.
  final int? pickupMinutes;
  final int? rideMinutes;

  /// Quando o motorista sai: com os tempos, vira o horário em que ele chega até o
  /// passageiro e em que o passageiro chega ao destino.
  final DateTime? departure;

  const PassengerRoutePreview({
    super.key,
    required this.driverOrigin,
    required this.pickup,
    required this.dropOff,
    this.stopsBeforePickup,
    this.stopsDuringRide,
    this.pickupMinutes,
    this.rideMinutes,
    this.departure,
  });

  /// O passageiro numa viagem em que já tem vaga.
  factory PassengerRoutePreview.ofRide(
    RideModel ride, {
    required RoutePlaceModel pickup,
    required RoutePlaceModel dropOff,
  }) {
    final pickupAt = ride.itineraryMyPickupStop;
    final dropOffAt = ride.itineraryMyDropOffStop;
    return PassengerRoutePreview(
      driverOrigin: ride.origin,
      pickup: pickup,
      dropOff: dropOff,
      stopsBeforePickup: pickupAt,
      stopsDuringRide: pickupAt == null || dropOffAt == null
          ? null
          : dropOffAt - pickupAt - 1,
      pickupMinutes: ride.itineraryMyPickupMinutes,
      rideMinutes: ride.itineraryMyRideMinutes,
      departure: ride.departureDateTime,
    );
  }

  @override
  ConsumerState<PassengerRoutePreview> createState() =>
      _PassengerRoutePreviewState();
}

class _PassengerRoutePreviewState extends ConsumerState<PassengerRoutePreview> {
  // 0: motorista indo até você; 1: até o seu destino
  late int _selected = _approachIsEmpty ? 1 : 0;

  bool get _approachIsEmpty =>
      TripProgress.sameSpot(widget.driverOrigin, widget.pickup);

  static LatLng _latLng(RoutePlaceModel place) =>
      LatLng(place.latitude, place.longitude);

  // Paradas de outros passageiros, em texto
  static String? _stopsLabel(int? count, String where) => switch (count) {
    null || 0 => null,
    1 => '1 parada $where',
    _ => '$count paradas $where',
  };

  /// O horário de chegada (saída + minutos até o embarque [+ do trecho]); sem a
  /// saída, só a duração.
  String? _timeLabel(
    int? untilPickup,
    int? onBoard, {
    required String arriving,
  }) {
    final departure = widget.departure;
    if (untilPickup == null) return null;
    final minutes = untilPickup + (onBoard ?? 0);
    if (departure == null) return 'cerca de ${onBoard ?? untilPickup} min';
    final at = departure.add(Duration(minutes: minutes));
    final hhmm =
        '${at.hour.toString().padLeft(2, '0')}:${at.minute.toString().padLeft(2, '0')}';
    return '$arriving $hhmm';
  }

  /// O aviso das paradas de outros passageiros: quantas, e quando (antes de
  /// buscar o passageiro e com ele no carro).
  String _stopsNotice() {
    final before = widget.stopsBeforePickup ?? 0;
    final during = widget.stopsDuringRide ?? 0;
    final total = before + during;
    if (total == 0) {
      return 'Por enquanto não há paradas de outros passageiros. O trajeto pode mudar se alguém entrar na carona.';
    }
    String count(int n, String what) =>
        n == 1 ? '1 $what' : '$n ${what.replaceFirst('parada', 'paradas')}';
    final when = [
      if (before > 0) '${count(before, 'parada')} antes de te buscar',
      if (during > 0) '${count(during, 'parada')} no caminho',
    ].join(' e ');
    final stops = total == 1
        ? '1 parada de outro passageiro'
        : '$total paradas de outros passageiros';
    // Dos dois tipos, detalha quantas de cada; de um só, diz quando
    final detail = before > 0 && during > 0
        ? ' ($when)'
        : before > 0
        ? ' antes de te buscar'
        : ' no caminho';
    return 'O trajeto real pode mudar: o motorista faz $stops$detail. '
        'Os horários já contam com elas.';
  }

  void _select(int index) {
    if (index == _selected) return;
    HapticFeedback.selectionClick();
    setState(() => _selected = index);
  }

  @override
  Widget build(BuildContext context) {
    // Caminhos limpos entre os pontos dele (sem passar pelas paradas dos outros)
    final approach = _approachIsEmpty
        ? null
        : ref
              .watch(
                passengerSegmentProvider((
                  from: widget.driverOrigin,
                  to: widget.pickup,
                )),
              )
              .value;
    final onBoard = ref
        .watch(
          passengerSegmentProvider((from: widget.pickup, to: widget.dropOff)),
        )
        .value;

    List<LatLng> points(
      String? polyline,
      RoutePlaceModel a,
      RoutePlaceModel b,
    ) => polyline == null || polyline.isEmpty
        ? [_latLng(a), _latLng(b)]
        : GoogleMapsService.decodePolyline(polyline);

    final steps = [
      (
        icon: Icons.person_rounded,
        title: 'Motorista indo até você',
        from: widget.driverOrigin,
        to: widget.pickup,
        points: _approachIsEmpty
            ? const <LatLng>[]
            : points(
                approach?.encodedPolyline,
                widget.driverOrigin,
                widget.pickup,
              ),
        minutes: widget.pickupMinutes ?? approach?.durationMinutes ?? 0,
        stops: _stopsLabel(widget.stopsBeforePickup, 'antes de você'),
      ),
      (
        icon: Icons.flag_rounded,
        title: 'Até o seu destino',
        from: widget.pickup,
        to: widget.dropOff,
        points: points(onBoard?.encodedPolyline, widget.pickup, widget.dropOff),
        minutes: widget.rideMinutes ?? onBoard?.durationMinutes,
        stops: _stopsLabel(widget.stopsDuringRide, 'no caminho'),
      ),
    ];
    final selected = steps[_selected];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        RideRouteMap(
          polyline: null,
          origin: _latLng(widget.driverOrigin),
          destination: _latLng(widget.dropOff),
          originTitle: 'Saída do motorista',
          destinationTitle: 'Seu desembarque',
          stops: [
            RouteMapStop(
              position: _latLng(widget.pickup),
              title: 'Seu embarque',
              isPickup: true,
            ),
          ],
          segments: [
            for (var i = 0; i < steps.length; i++)
              if (steps[i].points.isNotEmpty)
                RouteMapSegment(
                  points: steps[i].points,
                  emphasized: i == _selected,
                ),
          ],
          focus: [
            _latLng(selected.from),
            _latLng(selected.to),
            ...selected.points,
          ],
        ),
        const SizedBox(height: 12),
        // Os dois passos, lado a lado: tocar mostra o passo no mapa
        Row(
          children: [
            for (var i = 0; i < steps.length; i++) ...[
              if (i > 0) const SizedBox(width: 8),
              Expanded(
                child: _StepTab(
                  key: ValueKey('passenger_route_step_$i'),
                  icon: steps[i].icon,
                  title: steps[i].title,
                  selected: i == _selected,
                  onTap: () => _select(i),
                ),
              ),
            ],
          ],
        ),
        const SizedBox(height: 10),
        RouteStepCard(
          title: selected.title,
          from: selected.from,
          to: selected.to,
          toIcon: selected.icon,
          samePlaceLabel: 'O motorista já sai de',
          info: [
            ?_timeLabel(
              steps[0].minutes,
              _selected == 0 ? null : steps[1].minutes,
              arriving: _selected == 0
                  ? 'O motorista chega por volta das'
                  : 'Você chega por volta das',
            ),
            ?selected.stops,
          ].join(' · '),
        ),
        const SizedBox(height: 10),
        // As paradas dos outros, contadas: o mapa mostra um caminho direto
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Icon(
              Icons.info_outline_rounded,
              size: 16,
              color: AppColors.textSecondary,
            ),
            const SizedBox(width: 6),
            Expanded(
              child: Text(
                _stopsNotice(),
                style: const TextStyle(
                  fontSize: 12.5,
                  color: AppColors.textSecondary,
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }
}

/// Um dos dois passos: o ícone (pessoa no embarque, bandeira no destino) e o nome.
class _StepTab extends StatelessWidget {
  final IconData icon;
  final String title;
  final bool selected;
  final VoidCallback onTap;

  const _StepTab({
    super.key,
    required this.icon,
    required this.title,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      selected: selected,
      child: Material(
        color: selected ? AppColors.primary : AppColors.cardBackground,
        borderRadius: BorderRadius.circular(14),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(14),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(14),
              border: Border.all(
                color: selected ? AppColors.primary : AppColors.borderMedium,
              ),
            ),
            child: Row(
              children: [
                Icon(
                  icon,
                  size: 18,
                  color: selected ? AppColors.textLight : AppColors.textPrimary,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    title,
                    maxLines: 2,
                    style: TextStyle(
                      fontSize: 12.5,
                      fontWeight: FontWeight.w800,
                      color: selected
                          ? AppColors.textLight
                          : AppColors.textPrimary,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
