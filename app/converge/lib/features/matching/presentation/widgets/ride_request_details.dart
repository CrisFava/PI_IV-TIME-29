// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/services/maps/google_maps_service.dart';
import '../../../../core/services/maps/map_marker_icons.dart';
import '../../../../core/services/maps/map_styles.dart';
import '../../../../core/widgets/details_section.dart';
import '../../../../core/widgets/details_stat_tile.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../../ride/data/models/ride_preview_model.dart';
import '../../../ride/presentation/widgets/ride_place_line.dart';
import '../../data/models/driver_ride_request.dart';
import '../../state/ride_request_actions_controller.dart';
import '../../state/ride_request_preview_provider.dart';

/// Detalhes de um pedido de vaga: de onde o passageiro sai, para onde vai, a hora que
/// ele quer sair, o desvio que ele acrescenta e o mapa da viagem com e sem ele.
/// No mapa, o caminho de agora fica cinza por cima do caminho com o passageiro, em
/// preto: o que aparece em preto é exatamente o desvio.
class RideRequestDetails extends ConsumerWidget {
  final DriverRideRequest request;
  final VoidCallback onAccept;
  final VoidCallback onReject;
  final VoidCallback onOpenChat;

  const RideRequestDetails({
    super.key,
    required this.request,
    required this.onAccept,
    required this.onReject,
    required this.onOpenChat,
  });

  static String _hhmm(DateTime time) =>
      '${time.hour.toString().padLeft(2, '0')}:'
      '${time.minute.toString().padLeft(2, '0')}';

  /// O desvio em tempo, que é o principal: o Google sempre escolhe o caminho mais
  /// rápido, então a distância só aparece de apoio, embaixo.
  static String _detourText(RidePreviewModel preview) {
    final minutes = preview.extraMinutes;
    if (minutes == null) return 'Não foi possível calcular o tempo a mais.';
    if (minutes <= 0) return 'Ele está no seu caminho: sem tempo a mais.';
    return 'Desvio: +$minutes min no seu tempo de viagem.';
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final passenger = request.passenger;
    final preview = ref.watch(
      rideRequestPreviewProvider((
        rideId: request.rideId,
        passengerId: request.passengerId,
      )),
    );
    final busy = ref.watch(rideRequestActionsProvider).contains(request.key);
    final departure = request.ride.departureDateTime;
    final boarding = passenger.boardingLocation;
    final dropOff = passenger.dropOffLocation;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          passenger.displayName,
          style: const TextStyle(
            fontSize: 17,
            fontWeight: FontWeight.w800,
            color: AppColors.textPrimary,
          ),
        ),
        // O desvio que ele acrescenta (o mapa, embaixo, mostra em preto)
        if (preview.value case final value?)
          Padding(
            padding: const EdgeInsets.only(top: 6),
            // O tempo a mais é o principal; a distância vem de apoio, menor
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  _detourText(value),
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w800,
                    color: AppColors.textPrimary,
                  ),
                ),
                if (value.withPassengerDistanceKm > 0)
                  Text(
                    'Com ele, a viagem fica com '
                    '${value.withPassengerDistanceKm.toStringAsFixed(1).replaceAll('.', ',')} km.',
                    style: const TextStyle(
                      fontSize: 12.5,
                      color: AppColors.textSecondary,
                    ),
                  ),
              ],
            ),
          ),
        DetailsSection(
          title: 'Embarque',
          child: RidePlaceLine(
            icon: Icons.person_pin_circle_rounded,
            text: boarding?.address ?? 'Não informado',
            maxLines: null,
            emphasized: true,
          ),
        ),
        DetailsSection(
          title: 'Desembarque',
          child: RidePlaceLine(
            icon: Icons.flag_rounded,
            text: dropOff?.address ?? 'Não informado',
            maxLines: null,
            emphasized: true,
          ),
        ),
        Padding(
          padding: const EdgeInsets.only(top: 12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: DetailsStatTile(
                  icon: Icons.schedule_rounded,
                  value: passenger.requestedDepartureTime ?? '—',
                  label: 'Quer sair às',
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: DetailsStatTile(
                  icon: Icons.directions_car_rounded,
                  value: departure == null ? '—' : _hhmm(departure),
                  label: 'Sua saída',
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 18),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                key: const ValueKey('request_details_reject'),
                onPressed: busy
                    ? null
                    : () {
                        HapticFeedback.lightImpact();
                        onReject();
                      },
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppColors.textPrimary,
                  side: const BorderSide(color: AppColors.borderMedium),
                  padding: const EdgeInsets.symmetric(vertical: 13),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                ),
                child: const Text(
                  'Recusar',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: FilledButton(
                key: const ValueKey('request_details_accept'),
                onPressed: busy
                    ? null
                    : () {
                        HapticFeedback.mediumImpact();
                        onAccept();
                      },
                style: FilledButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  foregroundColor: AppColors.textLight,
                  padding: const EdgeInsets.symmetric(vertical: 13),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                ),
                child: busy
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: AppColors.textLight,
                        ),
                      )
                    : const Text(
                        'Aceitar',
                        style: TextStyle(fontWeight: FontWeight.w700),
                      ),
              ),
            ),
          ],
        ),
        TextButton.icon(
          onPressed: () {
            HapticFeedback.lightImpact();
            onOpenChat();
          },
          style: TextButton.styleFrom(foregroundColor: AppColors.textPrimary),
          icon: const Icon(Icons.chat_bubble_outline_rounded, size: 18),
          label: Text(
            'Conversar com ${passenger.displayName.split(' ').first}',
          ),
        ),
        const SizedBox(height: 16),
        const Text(
          'Como fica a viagem',
          style: TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w800,
            color: AppColors.textSecondary,
          ),
        ),
        const SizedBox(height: 6),
        // Sem recorte arredondado: no iOS, recortar o mapa nativo some com o que vem depois
        Container(
          height: 220,
          decoration: BoxDecoration(
            border: Border.all(color: AppColors.borderSubtle),
          ),
          child: switch (preview) {
            AsyncData(:final value) => _DetourMap(
              preview: value,
              boarding: boarding == null
                  ? null
                  : LatLng(boarding.latitude, boarding.longitude),
              dropOff: dropOff == null
                  ? null
                  : LatLng(dropOff.latitude, dropOff.longitude),
            ),
            AsyncError(:final error) => _MapMessage(
              text: authErrorMessage(error),
            ),
            _ => const ColoredBox(
              color: AppColors.neutralLight,
              child: Center(
                child: CircularProgressIndicator(color: AppColors.primary),
              ),
            ),
          },
        ),
        const SizedBox(height: 4),
        const Text(
          'Cinza: o seu caminho. Preto: o desvio para buscar e deixar o passageiro.',
          style: TextStyle(fontSize: 12, color: AppColors.textSecondary),
        ),
      ],
    );
  }
}

class _MapMessage extends StatelessWidget {
  final String text;

  const _MapMessage({required this.text});

  @override
  Widget build(BuildContext context) => ColoredBox(
    color: AppColors.neutralLight,
    child: Center(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Text(
          text,
          textAlign: TextAlign.center,
          style: const TextStyle(fontSize: 13, color: AppColors.textSecondary),
        ),
      ),
    ),
  );
}

/// O mapa da viagem com e sem o passageiro, no estilo dos outros mapas. Dá para
/// arrastar e aproximar: os gestos ficam com o mapa, não com o painel.
class _DetourMap extends StatefulWidget {
  final RidePreviewModel preview;
  final LatLng? boarding;
  final LatLng? dropOff;

  const _DetourMap({
    required this.preview,
    required this.boarding,
    required this.dropOff,
  });

  @override
  State<_DetourMap> createState() => _DetourMapState();
}

class _DetourMapState extends State<_DetourMap> {
  BitmapDescriptor? _originIcon;
  BitmapDescriptor? _flagIcon;

  @override
  void initState() {
    super.initState();
    // Os mesmos ícones dos outros mapas: bolinha de partida e bandeira de chegada
    // Embarque com a pessoa e desembarque com a bandeira, como no mapa da viagem
    Future.wait([MapMarkerIcons.pickup(), MapMarkerIcons.flag()]).then((icons) {
      if (mounted) {
        setState(() {
          _originIcon = icons[0];
          _flagIcon = icons[1];
        });
      }
    });
  }

  late final List<LatLng> _current = _decode(widget.preview.currentPolyline);
  late final List<LatLng> _withPassenger = _decode(
    widget.preview.withPassengerPolyline,
  );

  static List<LatLng> _decode(String? polyline) =>
      polyline == null || polyline.isEmpty
      ? const []
      : GoogleMapsService.decodePolyline(polyline);

  List<LatLng> get _allPoints => [
    ..._withPassenger,
    ..._current,
    ?widget.boarding,
    ?widget.dropOff,
  ];

  void _fit(GoogleMapController controller) {
    final points = _allPoints;
    if (points.isEmpty) return;
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
        32,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final points = _allPoints;
    if (points.isEmpty) {
      return const _MapMessage(text: 'Não foi possível desenhar o caminho.');
    }
    return GoogleMap(
      initialCameraPosition: CameraPosition(target: points.first, zoom: 12),
      onMapCreated: (controller) =>
          Future.delayed(const Duration(milliseconds: 250), () {
            if (mounted) _fit(controller);
          }),
      style: monochromeMapStyle,
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
        // Com o passageiro, em preto, por baixo: só o desvio fica visível em preto
        if (_withPassenger.isNotEmpty)
          Polyline(
            polylineId: const PolylineId('with_passenger'),
            points: _withPassenger,
            color: AppColors.primary,
            width: 6,
            zIndex: 1,
          ),
        if (_current.isNotEmpty)
          Polyline(
            polylineId: const PolylineId('current'),
            points: _current,
            color: AppColors.routeUpcoming,
            width: 6,
            zIndex: 2,
          ),
      },
      markers: {
        if (widget.boarding != null)
          Marker(
            markerId: const MarkerId('request_boarding'),
            position: widget.boarding!,
            icon: _originIcon ?? BitmapDescriptor.defaultMarker,
            anchor: const Offset(0.5, 0.5),
            infoWindow: const InfoWindow(title: 'Embarque'),
          ),
        if (widget.dropOff != null)
          Marker(
            markerId: const MarkerId('request_drop_off'),
            position: widget.dropOff!,
            icon: _flagIcon ?? BitmapDescriptor.defaultMarker,
            anchor: const Offset(0.5, 0.5),
            infoWindow: const InfoWindow(title: 'Desembarque'),
          ),
      },
    );
  }
}
