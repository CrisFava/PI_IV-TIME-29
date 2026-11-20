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

  const RouteMapStop({
    required this.position,
    required this.title,
    required this.isPickup,
  });
}

/// O percurso completo de uma viagem num mapa pequeno, no padrão do app: estilo
/// monocromático, caminho preto, bolinha de saída, uma pessoa em cada embarque,
/// bandeira em cada desembarque e no destino. Dá para arrastar e aproximar.
class RideRouteMap extends StatefulWidget {
  final String? polyline;
  final LatLng origin;
  final LatLng destination;
  final List<RouteMapStop> stops;
  final double height;

  const RideRouteMap({
    super.key,
    required this.polyline,
    required this.origin,
    required this.destination,
    this.stops = const [],
    this.height = 240,
  });

  @override
  State<RideRouteMap> createState() => _RideRouteMapState();
}

class _RideRouteMapState extends State<RideRouteMap> {
  BitmapDescriptor? _origin;
  BitmapDescriptor? _pickup;
  BitmapDescriptor? _flag;

  late List<LatLng> _path = _decode();

  List<LatLng> _decode() => widget.polyline == null || widget.polyline!.isEmpty
      ? [widget.origin, widget.destination]
      : GoogleMapsService.decodePolyline(widget.polyline!);

  // Caminho novo (a viagem ganhou ou perdeu passageiro): redesenha
  @override
  void didUpdateWidget(covariant RideRouteMap oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.polyline != widget.polyline ||
        oldWidget.origin != widget.origin ||
        oldWidget.destination != widget.destination) {
      _path = _decode();
    }
  }

  @override
  void initState() {
    super.initState();
    Future.wait([
      MapMarkerIcons.origin(),
      MapMarkerIcons.pickup(),
      MapMarkerIcons.flag(),
    ]).then((icons) {
      if (!mounted) return;
      setState(() {
        _origin = icons[0];
        _pickup = icons[1];
        _flag = icons[2];
      });
    });
  }

  void _fit(GoogleMapController controller) {
    final points = [..._path, ...widget.stops.map((s) => s.position)];
    var south = points.first.latitude, north = south;
    var west = points.first.longitude, east = west;
    for (final p in points) {
      south = math.min(south, p.latitude);
      north = math.max(north, p.latitude);
      west = math.min(west, p.longitude);
      east = math.max(east, p.longitude);
    }
    controller.moveCamera(
      CameraUpdate.newLatLngBounds(
        LatLngBounds(
          southwest: LatLng(south, west),
          northeast: LatLng(north, east),
        ),
        36,
      ),
    );
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
        onMapCreated: (controller) =>
            Future.delayed(const Duration(milliseconds: 250), () {
              if (mounted) _fit(controller);
            }),
        // Arrastar e aproximar o mapa sem rolar o painel junto
        gestureRecognizers: {
          Factory<OneSequenceGestureRecognizer>(EagerGestureRecognizer.new),
        },
        rotateGesturesEnabled: false,
        tiltGesturesEnabled: false,
        zoomControlsEnabled: false,
        myLocationButtonEnabled: false,
        mapToolbarEnabled: false,
        polylines: {
          Polyline(
            polylineId: const PolylineId('route'),
            points: _path,
            color: AppColors.primary,
            width: 5,
          ),
        },
        markers: {
          Marker(
            markerId: const MarkerId('route_origin'),
            position: widget.origin,
            icon: _origin ?? BitmapDescriptor.defaultMarker,
            anchor: const Offset(0.5, 0.5),
            infoWindow: const InfoWindow(title: 'Saída'),
          ),
          for (var i = 0; i < widget.stops.length; i++)
            Marker(
              markerId: MarkerId('route_stop_$i'),
              position: widget.stops[i].position,
              icon:
                  (widget.stops[i].isPickup ? _pickup : _flag) ??
                  BitmapDescriptor.defaultMarker,
              anchor: const Offset(0.5, 0.5),
              infoWindow: InfoWindow(title: widget.stops[i].title),
            ),
          Marker(
            markerId: const MarkerId('route_destination'),
            position: widget.destination,
            icon: _flag ?? BitmapDescriptor.defaultMarker,
            anchor: const Offset(0.5, 0.5),
            infoWindow: const InfoWindow(title: 'Destino'),
          ),
        },
      ),
    );
  }
}
