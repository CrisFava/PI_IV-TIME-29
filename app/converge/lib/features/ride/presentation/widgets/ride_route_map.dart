// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/services/maps/google_maps_service.dart';
import '../../../../core/services/maps/map_marker_icons.dart';
import '../../../../core/services/maps/map_styles.dart';

/// Uma parada no mapa do percurso: embarque (pessoa) ou desembarque (bandeira).
@immutable
class RouteMapStop {
  final LatLng position;
  final String title;
  final bool isPickup;

  /// Em foco (preto); fora de foco fica cinza, apagado.
  final bool highlighted;

  const RouteMapStop({
    required this.position,
    required this.title,
    required this.isPickup,
    this.highlighted = true,
  });
}

/// Um pedaço do caminho: o destacado (preto e mais grosso) ou um dos outros (apagado),
/// como na viagem em andamento.
@immutable
class RouteMapSegment {
  final List<LatLng> points;
  final bool emphasized;

  const RouteMapSegment({required this.points, required this.emphasized});
}

/// O percurso completo de uma viagem num mapa pequeno, no padrão do app: estilo
/// monocromático, caminho preto, bolinha de saída, uma pessoa em cada embarque,
/// bandeira em cada desembarque e no destino. Dá para arrastar e aproximar.
///
/// Com [segments], desenha o caminho em pedaços (um destacado) no lugar de
/// [polyline]; com [focus], enquadra esses pontos sempre que eles mudam.
class RideRouteMap extends StatefulWidget {
  final String? polyline;
  final LatLng origin;
  final LatLng destination;
  final List<RouteMapStop> stops;
  final List<RouteMapSegment> segments;
  final List<LatLng>? focus;
  final double height;

  /// O começo é o embarque de um passageiro (pessoa), não a saída do carro.
  final bool originIsPickup;
  final String originTitle;
  final String destinationTitle;

  /// A bandeira do destino em foco (preta) ou apagada (cinza).
  final bool destinationHighlighted;

  const RideRouteMap({
    super.key,
    required this.polyline,
    required this.origin,
    required this.destination,
    this.stops = const [],
    this.segments = const [],
    this.focus,
    this.height = 240,
    this.originIsPickup = false,
    this.originTitle = 'Saída',
    this.destinationTitle = 'Destino',
    this.destinationHighlighted = true,
  });

  @override
  State<RideRouteMap> createState() => _RideRouteMapState();
}

class _RideRouteMapState extends State<RideRouteMap> {
  BitmapDescriptor? _origin;
  BitmapDescriptor? _pickup;
  BitmapDescriptor? _flag;
  BitmapDescriptor? _pickupLater;
  BitmapDescriptor? _flagLater;
  GoogleMapController? _controller;

  late List<LatLng> _path = _decode();

  List<LatLng> _decode() => widget.polyline == null || widget.polyline!.isEmpty
      ? [widget.origin, widget.destination]
      : GoogleMapsService.decodePolyline(widget.polyline!);

  @override
  void initState() {
    super.initState();
    Future.wait([
      MapMarkerIcons.origin(),
      MapMarkerIcons.pickup(),
      MapMarkerIcons.flag(),
      MapMarkerIcons.pickup(highlighted: false),
      MapMarkerIcons.flag(highlighted: false),
    ]).then((icons) {
      if (!mounted) return;
      setState(() {
        _origin = icons[0];
        _pickup = icons[1];
        _flag = icons[2];
        _pickupLater = icons[3];
        _flagLater = icons[4];
      });
    });
  }

  // Caminho novo (a viagem ganhou ou perdeu passageiro) ou outro passo escolhido
  @override
  void didUpdateWidget(covariant RideRouteMap oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.polyline != widget.polyline ||
        oldWidget.origin != widget.origin ||
        oldWidget.destination != widget.destination) {
      _path = _decode();
    }
    if (!listEquals(oldWidget.focus, widget.focus)) {
      final controller = _controller;
      if (controller != null) _fit(controller, animate: true);
    }
  }

  List<LatLng> get _pointsToFit {
    final focus = widget.focus;
    if (focus != null && focus.isNotEmpty) return focus;
    return [
      ..._path,
      for (final segment in widget.segments) ...segment.points,
      ...widget.stops.map((s) => s.position),
    ];
  }

  void _fit(GoogleMapController controller, {bool animate = false}) {
    final points = _pointsToFit;
    var south = points.first.latitude, north = south;
    var west = points.first.longitude, east = west;
    for (final p in points) {
      south = math.min(south, p.latitude);
      north = math.max(north, p.latitude);
      west = math.min(west, p.longitude);
      east = math.max(east, p.longitude);
    }
    // Um ponto só (parada no mesmo lugar): aproxima nele
    final update =
        (north - south).abs() < 0.0005 && (east - west).abs() < 0.0005
        ? CameraUpdate.newLatLngZoom(LatLng(south, west), 15)
        : CameraUpdate.newLatLngBounds(
            LatLngBounds(
              southwest: LatLng(south, west),
              northeast: LatLng(north, east),
            ),
            36,
          );
    animate ? controller.animateCamera(update) : controller.moveCamera(update);
  }

  @override
  Widget build(BuildContext context) {
    // Sem recorte arredondado: no iOS, recortar o mapa nativo some com o que vem depois
    return Container(
      height: widget.height,
      decoration: BoxDecoration(
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: GoogleMap(
        initialCameraPosition: CameraPosition(target: widget.origin, zoom: 12),
        style: monochromeMapStyle,
        onMapCreated: (controller) {
          _controller = controller;
          Future.delayed(const Duration(milliseconds: 250), () {
            if (mounted) _fit(controller);
          });
        },
        // Arrastar e aproximar o mapa sem rolar o painel junto
        gestureRecognizers: {
          Factory<OneSequenceGestureRecognizer>(EagerGestureRecognizer.new),
        },
        rotateGesturesEnabled: false,
        tiltGesturesEnabled: false,
        zoomControlsEnabled: false,
        myLocationButtonEnabled: false,
        mapToolbarEnabled: false,
        polylines: widget.segments.isEmpty
            ? {
                Polyline(
                  polylineId: const PolylineId('route'),
                  points: _path,
                  color: AppColors.primary,
                  width: 5,
                ),
              }
            : {
                for (var i = 0; i < widget.segments.length; i++)
                  Polyline(
                    polylineId: PolylineId('route_segment_$i'),
                    points: widget.segments[i].points,
                    color: widget.segments[i].emphasized
                        ? AppColors.primary
                        : AppColors.routeUpcoming,
                    width: widget.segments[i].emphasized ? 6 : 4,
                    zIndex: widget.segments[i].emphasized ? 2 : 1,
                  ),
              },
        markers: {
          Marker(
            markerId: const MarkerId('route_origin'),
            position: widget.origin,
            icon:
                (widget.originIsPickup ? _pickup : _origin) ??
                BitmapDescriptor.defaultMarker,
            anchor: const Offset(0.5, 0.5),
            infoWindow: InfoWindow(title: widget.originTitle),
          ),
          for (var i = 0; i < widget.stops.length; i++)
            Marker(
              markerId: MarkerId('route_stop_$i'),
              position: widget.stops[i].position,
              icon:
                  (widget.stops[i].highlighted
                      ? (widget.stops[i].isPickup ? _pickup : _flag)
                      : (widget.stops[i].isPickup
                            ? _pickupLater
                            : _flagLater)) ??
                  BitmapDescriptor.defaultMarker,
              // O pino em foco fica por cima dos apagados
              zIndexInt: widget.stops[i].highlighted ? 2 : 1,
              anchor: const Offset(0.5, 0.5),
              infoWindow: InfoWindow(title: widget.stops[i].title),
            ),
          Marker(
            markerId: const MarkerId('route_destination'),
            position: widget.destination,
            icon:
                (widget.destinationHighlighted ? _flag : _flagLater) ??
                BitmapDescriptor.defaultMarker,
            anchor: const Offset(0.5, 0.5),
            infoWindow: InfoWindow(title: widget.destinationTitle),
          ),
        },
      ),
    );
  }
}
