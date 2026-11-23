// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/services/maps/google_maps_service.dart';
import '../../../route/data/models/route_place_model.dart';
import '../../../route/data/models/route_response_model.dart';
import '../../data/models/ride_model.dart';
import '../../data/models/ride_preview_model.dart';
import '../../data/models/ride_stop_model.dart';
import '../../data/models/ride_stop_type.dart';
import '../../state/trip_progress.dart';
import 'ride_route_map.dart';
import 'route_step_card.dart';

/// Um passo do trajeto: de onde sai, para onde vai e o que faz lá (buscar ou
/// deixar passageiros, ou chegar ao destino).
@immutable
class RideRouteStep {
  final String title;
  final RoutePlaceModel from;
  final RoutePlaceModel to;
  final bool isPickup;

  /// Os trechos do caminho (índices de `itineraryLegPolylines`) deste passo.
  final List<int> legs;

  /// O passo é do passageiro que pediu vaga (na simulação do pedido).
  final bool isNew;

  const RideRouteStep._({
    required this.title,
    required this.from,
    required this.to,
    required this.isPickup,
    required this.legs,
    this.isNew = false,
  });
}

/// O trajeto completo de uma carona do motorista, em passos: "Passo 1: saindo de
/// X, indo para Y". O mapa destaca o passo escolhido (como na viagem em
/// andamento) e as setas ou a lista trocam de passo. Sem viagem ainda (ninguém
/// aceito), mostra só o caminho da carona.
class RideRouteOverview extends StatefulWidget {
  final RoutePlaceModel origin;
  final RoutePlaceModel destination;
  final String? polyline;
  final List<String> legPolylines;
  final double? distanceKm;
  final String departureLabel;
  final String arrivalLabel;
  final int? minutes;
  final List<RideRouteStep> steps;

  /// O tempo de cada trecho em segundos (mesma ordem dos trechos) e a saída: viram
  /// o horário em que o carro chega a cada passo.
  final List<int> legSeconds;
  final DateTime? departure;

  /// Um aviso embaixo do resumo (ex.: como a chegada muda aceitando o pedido).
  final String? note;

  const RideRouteOverview._({
    required this.origin,
    required this.destination,
    required this.polyline,
    required this.legPolylines,
    required this.distanceKm,
    required this.departureLabel,
    required this.arrivalLabel,
    required this.minutes,
    required this.steps,
    this.legSeconds = const [],
    this.departure,
    this.note,
  });

  /// A próxima viagem da carona, com as paradas de quem foi aceito.
  factory RideRouteOverview.ofRide(
    RideModel ride, {
    String? fallbackPolyline,
    double? fallbackDistanceKm,
  }) {
    final departure = ride.departureDateTime;
    final arrival = ride.estimatedArrivalDateTime;
    final legs = ride.itineraryLegPolylines;
    final hasLegs = legs.length == ride.itinerary.length + 1;
    return RideRouteOverview._(
      origin: ride.origin,
      destination: ride.destination,
      polyline: ride.itineraryPolyline ?? fallbackPolyline,
      legPolylines: hasLegs ? legs : const [],
      distanceKm: ride.itineraryDistanceKm ?? fallbackDistanceKm,
      departureLabel: departure == null ? '—' : _hhmm(departure),
      arrivalLabel: arrival == null ? '—' : '~${_hhmm(arrival)}',
      minutes: departure != null && arrival != null
          ? arrival.difference(departure).inMinutes
          : null,
      steps: _stepsFrom(
        origin: ride.origin,
        destination: ride.destination,
        stops: ride.itinerary,
        names: _namesOf(ride),
      ),
      legSeconds: ride.itineraryLegSeconds.length == ride.itinerary.length + 1
          ? ride.itineraryLegSeconds
          : const [],
      departure: departure,
    );
  }

  /// A viagem como ficaria aceitando o pedido de [passengerId]: os passos com ele
  /// (os dele em destaque), o caminho e como muda a chegada.
  factory RideRouteOverview.ofPreview(
    RideModel ride,
    RidePreviewModel preview, {
    required String passengerId,
    required String passengerName,
  }) {
    final departure = ride.departureDateTime;
    final stops = preview.withPassengerStops;
    final legs = preview.withPassengerLegPolylines;
    final seconds = preview.withPassengerLegSeconds;
    final total = preview.withPassengerDurationMinutes;
    final extra = preview.extraMinutes;
    final newArrival = departure == null || total == null
        ? null
        : departure.add(Duration(minutes: total));
    final oldArrival = newArrival == null || extra == null
        ? null
        : newArrival.subtract(Duration(minutes: extra));
    return RideRouteOverview._(
      origin: ride.origin,
      destination: ride.destination,
      polyline: preview.withPassengerPolyline,
      legPolylines: legs.length == stops.length + 1 ? legs : const [],
      distanceKm: preview.withPassengerDistanceKm,
      departureLabel: departure == null ? '—' : _hhmm(departure),
      arrivalLabel: newArrival == null ? '—' : '~${_hhmm(newArrival)}',
      minutes: total,
      steps: _stepsFrom(
        origin: ride.origin,
        destination: ride.destination,
        stops: stops,
        names: {..._namesOf(ride), passengerId: passengerName},
        newPassengerId: passengerId,
      ),
      legSeconds: seconds.length == stops.length + 1 ? seconds : const [],
      departure: departure,
      note: oldArrival == null || newArrival == null
          ? null
          : oldArrival == newArrival
          ? 'A chegada não muda: continua por volta das ${_hhmm(newArrival)}.'
          : 'A chegada passa de ~${_hhmm(oldArrival)} para ~${_hhmm(newArrival)}.',
    );
  }

  static Map<String, String> _namesOf(RideModel ride) => {
    for (final p in ride.participants)
      if (p.isPassenger && p.userId != null) p.userId!: p.displayName,
  };

  /// A carona sem ninguém aceito: só o caminho dela.
  factory RideRouteOverview.ofRoute(RouteResponseModel route) =>
      RideRouteOverview._(
        origin: route.origin,
        destination: route.destination,
        polyline: route.encodedPolyline,
        legPolylines: const [],
        distanceKm: route.routeDistanceKm,
        departureLabel: route.departureTime.isEmpty ? '—' : route.departureTime,
        arrivalLabel: route.estimatedArrivalTime.isEmpty
            ? '—'
            : '~${route.estimatedArrivalTime}',
        minutes: null,
        steps: [
          RideRouteStep._(
            title: 'Ir até o destino',
            from: route.origin,
            to: route.destination,
            isPickup: false,
            legs: const [],
          ),
        ],
      );

  /// As paradas agrupadas como na viagem em andamento (vários passageiros no
  /// mesmo ponto, um logo depois do outro, viram um passo só), mais a chegada ao
  /// destino. Com [newPassengerId], marca os passos do passageiro que pediu vaga.
  static List<RideRouteStep> _stepsFrom({
    required RoutePlaceModel origin,
    required RoutePlaceModel destination,
    required List<RideStopModel> stops,
    required Map<String, String> names,
    String? newPassengerId,
  }) {
    // Grupos: [primeira parada, última parada, passageiros]
    final groups = <({int first, int last, List<String> ids})>[];
    for (var i = 0; i < stops.length; i++) {
      final previous = groups.isEmpty ? null : groups.last;
      if (previous != null &&
          previous.last == i - 1 &&
          stops[previous.first].type == stops[i].type &&
          TripProgress.sameSpot(stops[previous.first].place, stops[i].place)) {
        groups.last = (
          first: previous.first,
          last: i,
          ids: [...previous.ids, stops[i].passengerId],
        );
      } else {
        groups.add((first: i, last: i, ids: [stops[i].passengerId]));
      }
    }

    final steps = <RideRouteStep>[];
    var previous = origin;
    var arrivalMerged = false;
    for (var k = 0; k < groups.length; k++) {
      final group = groups[k];
      final stop = stops[group.first];
      final isPickup = stop.type == RideStopType.pickup;
      final who = _names([for (final id in group.ids) names[id] ?? '']);
      // Última parada já no destino: deixar e chegar é um passo só
      final endsTrip =
          k == groups.length - 1 &&
          !isPickup &&
          TripProgress.sameSpot(stop.place, destination);
      arrivalMerged = endsTrip;
      steps.add(
        RideRouteStep._(
          title: isPickup
              ? 'Buscar $who'
              : endsTrip
              ? 'Deixar $who no destino'
              : 'Deixar $who',
          from: previous,
          to: stop.place,
          isPickup: isPickup,
          legs: [
            for (var i = group.first; i <= group.last; i++) i,
            if (endsTrip) stops.length,
          ],
          isNew: newPassengerId != null && group.ids.contains(newPassengerId),
        ),
      );
      previous = stop.place;
    }

    if (!arrivalMerged) {
      steps.add(
        RideRouteStep._(
          title: groups.isEmpty ? 'Ir até o destino' : 'Chegar ao destino',
          from: previous,
          to: destination,
          isPickup: false,
          legs: [stops.length],
        ),
      );
    }
    return steps;
  }

  // "Ana", "Ana e Bruno", "Ana, Bruno e Carla"
  static String _names(List<String> fullNames) {
    final names = [
      for (final name in fullNames)
        if (name.trim().isNotEmpty) name.trim().split(' ').first,
    ];
    if (names.isEmpty) return 'passageiro';
    if (names.length == 1) return names.first;
    return '${names.sublist(0, names.length - 1).join(', ')} e ${names.last}';
  }

  static String _hhmm(DateTime time) =>
      '${time.hour.toString().padLeft(2, '0')}:'
      '${time.minute.toString().padLeft(2, '0')}';

  @override
  State<RideRouteOverview> createState() => _RideRouteOverviewState();
}

class _RideRouteOverviewState extends State<RideRouteOverview> {
  int _selected = 0;
  final Map<String, List<LatLng>> _decoded = {};
  final ScrollController _stripController = ScrollController();

  @override
  void dispose() {
    _stripController.dispose();
    super.dispose();
  }

  List<RideRouteStep> get _steps => widget.steps;

  bool get _hasLegs => widget.legPolylines.isNotEmpty;

  static String _km(double km) => km.toStringAsFixed(1).replaceAll('.', ',');

  static LatLng _latLng(RoutePlaceModel place) =>
      LatLng(place.latitude, place.longitude);

  List<LatLng> _stepPoints(RideRouteStep step) => [
    for (final i in step.legs)
      if (i < widget.legPolylines.length)
        ..._decoded.putIfAbsent(
          widget.legPolylines[i],
          () => GoogleMapsService.decodePolyline(widget.legPolylines[i]),
        ),
  ];

  /// Por volta de que horas o carro chega ao fim de [step]: a saída mais os
  /// trechos até ele; nulo sem os tempos.
  String? _etaOf(RideRouteStep step) {
    final departure = widget.departure;
    final seconds = widget.legSeconds;
    if (departure == null || seconds.isEmpty || step.legs.isEmpty) return null;
    final last = step.legs.reduce((a, b) => a > b ? a : b);
    if (last >= seconds.length) return null;
    var total = 0;
    for (var i = 0; i <= last; i++) {
      total += seconds[i];
    }
    final at = departure.add(Duration(seconds: total));
    return '~${at.hour.toString().padLeft(2, '0')}:'
        '${at.minute.toString().padLeft(2, '0')}';
  }

  void _select(int index) {
    if (index < 0 || index >= _steps.length || index == _selected) return;
    HapticFeedback.selectionClick();
    setState(() => _selected = index);
    // A faixa acompanha: o passo escolhido fica no meio
    if (_stripController.hasClients) {
      final position = _stripController.position;
      final target =
          index * _StepStrip.itemExtent -
          (position.viewportDimension - _StepStrip.dotSize) / 2;
      _stripController.animateTo(
        target.clamp(0.0, position.maxScrollExtent),
        duration: const Duration(milliseconds: 250),
        curve: Curves.easeOut,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final selected = _steps[_selected.clamp(0, _steps.length - 1)];
    final selectedPoints = _hasLegs ? _stepPoints(selected) : const <LatLng>[];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _Summary(
          schedule: '${widget.departureLabel} → ${widget.arrivalLabel}',
          minutes: widget.minutes,
          distance: widget.distanceKm == null
              ? null
              : '${_km(widget.distanceKm!)} km',
        ),
        if (widget.note != null) ...[
          const SizedBox(height: 8),
          Text(
            widget.note!,
            style: const TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w700,
              color: AppColors.textPrimary,
            ),
          ),
        ],
        const SizedBox(height: 12),
        RideRouteMap(
          polyline: widget.polyline,
          origin: _latLng(widget.origin),
          destination: _latLng(widget.destination),
          // Um marcador por passo, no lugar onde ele termina
          // Só o pino do passo escolhido em foco; os outros, cinza
          stops: [
            for (var i = 0; i < _steps.length; i++)
              if (!TripProgress.sameSpot(_steps[i].to, widget.destination))
                RouteMapStop(
                  position: _latLng(_steps[i].to),
                  title: _steps[i].title,
                  isPickup: _steps[i].isPickup,
                  highlighted: !_hasLegs || i == _selected,
                ),
          ],
          destinationHighlighted: !_hasLegs || _selected == _steps.length - 1,
          segments: _hasLegs
              ? [
                  for (var i = 0; i < _steps.length; i++)
                    RouteMapSegment(
                      points: _stepPoints(_steps[i]),
                      emphasized: i == _selected,
                    ),
                ]
              : const [],
          focus: _hasLegs
              ? [
                  _latLng(selected.from),
                  _latLng(selected.to),
                  ...selectedPoints,
                ]
              : null,
        ),
        // Todos os passos numa faixa só, que rola de lado: cabe com 12 ou mais
        if (_steps.length > 1) ...[
          const SizedBox(height: 10),
          _StepStrip(
            steps: _steps,
            selected: _selected,
            controller: _stripController,
            onSelect: _select,
          ),
        ],
        const SizedBox(height: 10),
        RouteStepCard(
          header: [
            'Passo ${_selected + 1} de ${_steps.length}',
            ?_etaOf(selected),
            if (selected.isNew) 'novo passageiro',
          ].join(' · '),
          title: selected.title,
          from: selected.from,
          to: selected.to,
          toIcon: selected.isPickup
              ? Icons.person_pin_circle_rounded
              : Icons.flag_rounded,
          showArrows: _steps.length > 1,
          onPrevious: _selected > 0 ? () => _select(_selected - 1) : null,
          onNext: _selected < _steps.length - 1
              ? () => _select(_selected + 1)
              : null,
        ),
        if (_steps.length == 1 && _steps.first.legs.isEmpty) ...[
          const SizedBox(height: 10),
          const Text(
            'Ainda sem passageiros: os passos aparecem quando você aceitar alguém.',
            style: TextStyle(fontSize: 12.5, color: AppColors.textSecondary),
          ),
        ],
      ],
    );
  }
}

/// Saída e chegada, duração e distância numa linha só.
class _Summary extends StatelessWidget {
  final String schedule;
  final int? minutes;
  final String? distance;

  const _Summary({
    required this.schedule,
    required this.minutes,
    required this.distance,
  });

  @override
  Widget build(BuildContext context) {
    final items = [
      (Icons.schedule_rounded, schedule),
      if (minutes != null) (Icons.timer_outlined, '$minutes min'),
      if (distance != null) (Icons.route_rounded, distance!),
    ];
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: AppColors.inputBackground,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          for (final (icon, text) in items)
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(icon, size: 16, color: AppColors.textPrimary),
                const SizedBox(width: 5),
                Text(
                  text,
                  style: const TextStyle(
                    fontSize: 13.5,
                    fontWeight: FontWeight.w800,
                    color: AppColors.textPrimary,
                  ),
                ),
              ],
            ),
        ],
      ),
    );
  }
}

/// Os passos numa faixa horizontal: um número por passo (o escolhido em
/// destaque), ligados por uma linha, com o ícone de embarque ou desembarque embaixo.
class _StepStrip extends StatelessWidget {
  static const double dotSize = 30;
  static const double gap = 18;
  static const double itemExtent = dotSize + gap;

  final List<RideRouteStep> steps;
  final int selected;
  final ScrollController controller;
  final ValueChanged<int> onSelect;

  const _StepStrip({
    required this.steps,
    required this.selected,
    required this.controller,
    required this.onSelect,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 50,
      child: ListView.builder(
        controller: controller,
        scrollDirection: Axis.horizontal,
        itemCount: steps.length,
        itemBuilder: (context, i) {
          final isSelected = i == selected;
          final done = i < selected;
          return SizedBox(
            width: i == steps.length - 1 ? dotSize : itemExtent,
            child: Stack(
              clipBehavior: Clip.none,
              children: [
                // A linha até o próximo passo
                if (i < steps.length - 1)
                  Positioned(
                    left: dotSize,
                    top: dotSize / 2 - 1,
                    width: gap,
                    height: 2,
                    child: ColoredBox(
                      color: done || isSelected
                          ? AppColors.primary
                          : AppColors.borderMedium,
                    ),
                  ),
                Semantics(
                  button: true,
                  selected: isSelected,
                  label: 'Passo ${i + 1}: ${steps[i].title}',
                  child: GestureDetector(
                    key: ValueKey('route_step_$i'),
                    behavior: HitTestBehavior.opaque,
                    onTap: () => onSelect(i),
                    child: SizedBox(
                      width: dotSize,
                      child: Column(
                        children: [
                          AnimatedContainer(
                            duration: const Duration(milliseconds: 200),
                            width: dotSize,
                            height: dotSize,
                            alignment: Alignment.center,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: isSelected
                                  ? AppColors.primary
                                  : AppColors.cardBackground,
                              border: Border.all(
                                // Os passos do passageiro novo, em verde
                                color: steps[i].isNew && !isSelected
                                    ? AppColors.success
                                    : isSelected || done
                                    ? AppColors.primary
                                    : AppColors.borderMedium,
                                width: steps[i].isNew ? 2.5 : 1.5,
                              ),
                            ),
                            child: Text(
                              '${i + 1}',
                              style: TextStyle(
                                fontSize: 12.5,
                                fontWeight: FontWeight.w800,
                                color: isSelected
                                    ? AppColors.textLight
                                    : AppColors.textPrimary,
                              ),
                            ),
                          ),
                          const SizedBox(height: 3),
                          Icon(
                            steps[i].isPickup
                                ? Icons.person_rounded
                                : Icons.flag_rounded,
                            size: 14,
                            color: isSelected
                                ? AppColors.textPrimary
                                : AppColors.iconMuted,
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
