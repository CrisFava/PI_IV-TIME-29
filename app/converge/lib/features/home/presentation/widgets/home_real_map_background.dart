import 'dart:math' as math;
import 'dart:ui' as ui;
import 'package:converge/features/home/presentation/widgets/home_map_background.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../../core/config/styles.dart';
import '../../../ride/data/models/mock_driver_model.dart';

/// Tipo de pino central para o modo de arrasto estilo Uber
enum CentralPinType {
  destination,
  pickup,
}

/// Estilo JSON Monocromático de Alto Contraste (Prédios e áreas não-viárias mais escuras)
const String _monochromeMapStyle = '''[
  {
    "elementType": "geometry",
    "stylers": [{"color": "#e2e4e8"}]
  },
  {
    "elementType": "labels.icon",
    "stylers": [{"visibility": "off"}]
  },
  {
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#4b5563"}]
  },
  {
    "elementType": "labels.text.stroke",
    "stylers": [{"color": "#e2e4e8"}]
  },
  {
    "featureType": "administrative.land_parcel",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#9ca3af"}]
  },
  {
    "featureType": "landscape.man_made",
    "elementType": "geometry",
    "stylers": [{"color": "#cbcfd6"}]
  },
  {
    "featureType": "landscape.natural",
    "elementType": "geometry",
    "stylers": [{"color": "#d5d9df"}]
  },
  {
    "featureType": "poi",
    "elementType": "geometry",
    "stylers": [{"color": "#cbcfd6"}]
  },
  {
    "featureType": "poi",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#4b5563"}]
  },
  {
    "featureType": "poi.park",
    "elementType": "geometry",
    "stylers": [{"color": "#c5cad2"}]
  },
  {
    "featureType": "poi.park",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#6b7280"}]
  },
  {
    "featureType": "road",
    "elementType": "geometry",
    "stylers": [{"color": "#ffffff"}]
  },
  {
    "featureType": "road",
    "elementType": "geometry.stroke",
    "stylers": [{"color": "#b8bec8"}]
  },
  {
    "featureType": "road.arterial",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#374151"}]
  },
  {
    "featureType": "road.highway",
    "elementType": "geometry",
    "stylers": [{"color": "#ffffff"}]
  },
  {
    "featureType": "road.highway",
    "elementType": "geometry.stroke",
    "stylers": [{"color": "#9ca3af"}]
  },
  {
    "featureType": "road.highway",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#1f2937"}]
  },
  {
    "featureType": "road.local",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#4b5563"}]
  },
  {
    "featureType": "transit.line",
    "elementType": "geometry",
    "stylers": [{"color": "#c0c5cd"}]
  },
  {
    "featureType": "transit.station",
    "elementType": "geometry",
    "stylers": [{"color": "#cbcfd6"}]
  },
  {
    "featureType": "water",
    "elementType": "geometry",
    "stylers": [{"color": "#b4bac4"}]
  },
  {
    "featureType": "water",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#4b5563"}]
  }
]''';

/// ============================================================================
/// COMPONENTE: HomeRealMapBackground (Google Maps Monocromático & Elegante)
/// ============================================================================
/// 1. Mapa com estilização monocromática elegante (Google Maps JSON).
/// 2. Destino: bolinha quadriculada, remetendo à chegada.
/// 3. Origem: bolinha preta opaca, remetendo à origem.
/// 4. Motorista: carrinho preto filled.
/// 5. Seleção: pino monocromático preto não totalmente filled (vazado no centro).
/// ============================================================================
class HomeRealMapBackground extends StatefulWidget {
  final bool isDriverMode;
  final double searchRadiusKm;
  final bool showDrivers;
  final List<MockNearbyDriver> availableDrivers;
  final MockNearbyDriver? acceptedDriver;
  final LatLng? pickupLocation;
  final LatLng? destinationLocation;
  final String pickupAddress;
  final String destinationAddress;
  final bool showTrajectory;
  final List<LatLng>? tripRoutePoints;
  final List<LatLng>? driverApproachRoutePoints;

  // Modo arrasto de mapa estilo Uber com pino fixo
  final bool isDraggingPin;
  final CentralPinType centralPinType;
  final String pinLabel;
  final ValueChanged<LatLng>? onCameraCenterChanged;

  final ValueChanged<MockNearbyDriver>? onDriverSelected;
  final VoidCallback? onMapTap;
  final String universityName;

  const HomeRealMapBackground({
    super.key,
    required this.isDriverMode,
    this.searchRadiusKm = 5.0,
    this.showDrivers = false,
    this.availableDrivers = const [],
    this.acceptedDriver,
    this.pickupLocation,
    this.destinationLocation,
    this.pickupAddress = '',
    this.destinationAddress = '',
    this.showTrajectory = false,
    this.tripRoutePoints,
    this.driverApproachRoutePoints,
    this.isDraggingPin = false,
    this.centralPinType = CentralPinType.destination,
    this.pinLabel = '',
    this.onCameraCenterChanged,
    this.onDriverSelected,
    this.onMapTap,
    this.universityName = 'PUC-Campinas',
  });

  @override
  State<HomeRealMapBackground> createState() => _HomeRealMapBackgroundState();
}

class _HomeRealMapBackgroundState extends State<HomeRealMapBackground> {
  GoogleMapController? _mapController;
  final bool _hasMapInitError = false;
  LatLng? _currentCameraTarget;

  // Cache estático dos marcadores customizados desenhados no canvas
  static BitmapDescriptor? _originDotIcon;
  static BitmapDescriptor? _checkeredDestIcon;
  static BitmapDescriptor? _blackCarIcon;

  @override
  void initState() {
    super.initState();
    _initCustomMonochromeIcons();
  }

  Future<void> _initCustomMonochromeIcons() async {
    try {
      final origin = _originDotIcon ?? await _generateOriginDotDescriptor();
      final dest = _checkeredDestIcon ?? await _generateCheckeredDestDescriptor();
      final car = _blackCarIcon ?? await _generateBlackCarDescriptor();
      _originDotIcon = origin;
      _checkeredDestIcon = dest;
      _blackCarIcon = car;
      if (mounted) {
        setState(() {});
      }
    } catch (e) {
      debugPrint('Erro ao gerar ícones monocromáticos: $e');
    }
  }

  void _onMapCreated(GoogleMapController controller) {
    _mapController = controller;
    _adjustCameraForCurrentState();
  }

  @override
  void didUpdateWidget(covariant HomeRealMapBackground oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.pickupLocation != oldWidget.pickupLocation ||
        widget.destinationLocation != oldWidget.destinationLocation ||
        widget.acceptedDriver != oldWidget.acceptedDriver ||
        widget.tripRoutePoints != oldWidget.tripRoutePoints ||
        widget.driverApproachRoutePoints != oldWidget.driverApproachRoutePoints ||
        widget.showTrajectory != oldWidget.showTrajectory) {
      _adjustCameraForCurrentState();
    }
  }

  void _adjustCameraForCurrentState() {
    if (_mapController == null) return;

    // Se o motorista aceitou, enquadra motorista, embarque e destino
    if (widget.acceptedDriver != null && widget.pickupLocation != null) {
      final points = <LatLng>[
        widget.acceptedDriver!.position,
        widget.pickupLocation!,
      ];
      if (widget.destinationLocation != null) {
        points.add(widget.destinationLocation!);
      }
      _animateToBounds(points);
      return;
    }

    // Se há trajeto entre embarque e destino
    if (widget.showTrajectory &&
        widget.pickupLocation != null &&
        widget.destinationLocation != null) {
      _animateToBounds([widget.pickupLocation!, widget.destinationLocation!]);
      Future.delayed(const Duration(milliseconds: 500), () {
        if (mounted) {
          _mapController?.showMarkerInfoWindow(const MarkerId('destination_marker'));
        }
      });
      return;
    }

    if (widget.pickupLocation != null && !widget.isDraggingPin) {
      _mapController?.animateCamera(
        CameraUpdate.newLatLng(widget.pickupLocation!),
      );
    }
  }

  void _animateToBounds(List<LatLng> points) {
    if (points.isEmpty) return;

    double minLat = points.first.latitude;
    double maxLat = points.first.latitude;
    double minLng = points.first.longitude;
    double maxLng = points.first.longitude;

    for (final p in points) {
      minLat = math.min(minLat, p.latitude);
      maxLat = math.max(maxLat, p.latitude);
      minLng = math.min(minLng, p.longitude);
      maxLng = math.max(maxLng, p.longitude);
    }

    _mapController?.animateCamera(
      CameraUpdate.newLatLngBounds(
        LatLngBounds(
          southwest: LatLng(minLat, minLng),
          northeast: LatLng(maxLat, maxLng),
        ),
        85.0,
      ),
    );
  }

  void _recenterMap() {
    HapticFeedback.lightImpact();
    final target = widget.pickupLocation ?? mockCampusCoordinates;
    _mapController?.animateCamera(
      CameraUpdate.newCameraPosition(
        CameraPosition(
          target: target,
          zoom: 15.0,
        ),
      ),
    );
  }

  Set<Circle> _buildCircles() {
    if (!widget.isDriverMode) return const {};

    return {
      Circle(
        circleId: const CircleId('driver_acceptance_radius'),
        center: widget.pickupLocation ?? mockCampusCoordinates,
        radius: widget.searchRadiusKm * 1000,
        strokeWidth: 1,
        strokeColor: AppColors.primary.withValues(alpha: 0.35),
        fillColor: AppColors.primary.withValues(alpha: 0.04),
      ),
    };
  }

  Set<Marker> _buildMarkers() {
    final markers = <Marker>{};

    // 1. Marcador de Origem (Bolinha preta opaca remetendo a "origem")
    final isDraggingPickup = widget.isDraggingPin &&
        widget.centralPinType == CentralPinType.pickup;
    if (!isDraggingPickup) {
      final pickupPos = widget.pickupLocation ?? mockCampusCoordinates;
      final pickupTitle = widget.pickupAddress.isNotEmpty
          ? 'Origem: ${widget.pickupAddress}'
          : 'Origem: Ponto de Embarque';

      markers.add(
        Marker(
          markerId: const MarkerId('pickup_marker'),
          position: pickupPos,
          icon: _originDotIcon ??
              BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueViolet),
          anchor: const Offset(0.5, 0.5),
          infoWindow: InfoWindow(
            title: pickupTitle,
            snippet: 'Ponto de partida da viagem',
          ),
        ),
      );
    }

    // 2. Marcador de Destino (Bolinha quadriculada remetendo à chegada)
    final isDraggingDest = widget.isDraggingPin &&
        widget.centralPinType == CentralPinType.destination;
    if (widget.destinationLocation != null && !isDraggingDest) {
      final destTitle = widget.destinationAddress.isNotEmpty
          ? 'Destino: ${widget.destinationAddress}'
          : 'Destino Selecionado';

      markers.add(
        Marker(
          markerId: const MarkerId('destination_marker'),
          position: widget.destinationLocation!,
          icon: _checkeredDestIcon ??
              BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueRed),
          anchor: const Offset(0.5, 0.5),
          infoWindow: InfoWindow(
            title: destTitle,
            snippet: 'Ponto de chegada da viagem',
          ),
        ),
      );
    }

    // 3. Marcador do Motorista Aceito (Carrinho preto filled)
    if (widget.acceptedDriver != null) {
      final driver = widget.acceptedDriver!;
      markers.add(
        Marker(
          markerId: MarkerId('accepted_driver_${driver.id}'),
          position: driver.position,
          icon: _blackCarIcon ??
              BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueAzure),
          anchor: const Offset(0.5, 0.5),
          infoWindow: InfoWindow(
            title: '${driver.name} (A caminho)',
            snippet: driver.vehicleModel,
          ),
        ),
      );
    }
    // 4. Marcadores de Motoristas Disponíveis (Carrinho preto filled)
    else if (!widget.isDriverMode && widget.showDrivers) {
      final driversToShow = widget.availableDrivers.isNotEmpty
          ? widget.availableDrivers
          : mockNearbyDriversList;

      for (final driver in driversToShow) {
        markers.add(
          Marker(
            markerId: MarkerId(driver.id),
            position: driver.position,
            icon: _blackCarIcon ??
                BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueAzure),
            anchor: const Offset(0.5, 0.5),
            infoWindow: InfoWindow(
              title: driver.name,
              snippet:
                  '${driver.vehicleModel} • R\$ ${driver.contributionAmount.toStringAsFixed(2)}',
              onTap: () => widget.onDriverSelected?.call(driver),
            ),
            onTap: () => widget.onDriverSelected?.call(driver),
          ),
        );
      }
    }

    return markers;
  }

  Set<Polyline> _buildPolylines() {
    final polylines = <Polyline>{};

    // 1. Rota do Motorista até o Ponto de Embarque (EM CINZA CLARO ELEGANTE)
    if (widget.acceptedDriver != null && widget.pickupLocation != null) {
      final approachPoints = widget.driverApproachRoutePoints;
      if (approachPoints != null && approachPoints.isNotEmpty) {
        polylines.add(
          Polyline(
            polylineId: const PolylineId('driver_approach_route'),
            points: approachPoints,
            color: AppColors.driverApproachRoute, // Cinza claro elegante
            width: 5,
            startCap: Cap.roundCap,
            endCap: Cap.roundCap,
            jointType: JointType.round,
          ),
        );
      }
    }

    // 2. Rota Principal da Viagem (Origem → Destino) em Preto Monocromático
    if (widget.showTrajectory &&
        widget.pickupLocation != null &&
        widget.destinationLocation != null) {
      final mainTripPoints = widget.tripRoutePoints;
      if (mainTripPoints != null && mainTripPoints.isNotEmpty) {
        polylines.add(
          Polyline(
            polylineId: const PolylineId('ride_active_route'),
            points: mainTripPoints,
            color: AppColors.primary, // Preto monocromático elegante
            width: 5,
            startCap: Cap.roundCap,
            endCap: Cap.roundCap,
            jointType: JointType.round,
          ),
        );
      }
    }

    return polylines;
  }

  @override
  Widget build(BuildContext context) {
    final isUnsupportedPlatform = kIsWeb ||
        (defaultTargetPlatform != TargetPlatform.android &&
            defaultTargetPlatform != TargetPlatform.iOS);

    if (_hasMapInitError || isUnsupportedPlatform) {
      return Stack(
        children: [
          Positioned.fill(
            child: HomeMapBackground(
              universityName: widget.universityName,
              onMapTap: widget.onMapTap,
            ),
          ),
          _buildRecenterFab(),
        ],
      );
    }

    final currentDragAddress = widget.centralPinType == CentralPinType.destination
        ? widget.destinationAddress
        : widget.pickupAddress;

    return Stack(
      children: [
        Positioned.fill(
          child: GoogleMap(
            style: _monochromeMapStyle,
            initialCameraPosition: CameraPosition(
              target: widget.pickupLocation ?? mockCampusCoordinates,
              zoom: 15.0,
            ),
            onMapCreated: _onMapCreated,
            onTap: (_) => widget.onMapTap?.call(),
            onCameraMove: (pos) {
              _currentCameraTarget = pos.target;
            },
            onCameraIdle: () {
              if (_currentCameraTarget != null && widget.isDraggingPin) {
                widget.onCameraCenterChanged?.call(_currentCameraTarget!);
              }
            },
            myLocationEnabled: false,
            myLocationButtonEnabled: false,
            zoomControlsEnabled: false,
            compassEnabled: false,
            mapToolbarEnabled: false,
            trafficEnabled: false,
            circles: _buildCircles(),
            markers: _buildMarkers(),
            polylines: _buildPolylines(),
          ),
        ),

        // Pino Central Fixo estilo Uber:
        // Monocromático preto e NÃO totalmente filled (vazado no centro)
        if (widget.isDraggingPin)
          Positioned.fill(
            child: IgnorePointer(
              child: Center(
                child: _UberStyleCentralPin(
                  type: widget.centralPinType,
                  label: widget.pinLabel,
                  address: currentDragAddress,
                ),
              ),
            ),
          ),

        _buildRecenterFab(),
      ],
    );
  }

  Widget _buildRecenterFab() {
    return Positioned(
      right: 16,
      bottom: 270,
      child: Material(
        color: AppColors.cardBackground,
        elevation: 4,
        shadowColor: Colors.black.withValues(alpha: 0.15),
        shape: const CircleBorder(),
        child: InkWell(
          customBorder: const CircleBorder(),
          onTap: _recenterMap,
          child: Container(
            width: 44,
            height: 44,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              border: Border.all(color: AppColors.borderSubtle, width: 1),
            ),
            child: const Icon(
              Icons.my_location_rounded,
              size: 20,
              color: AppColors.textPrimary,
            ),
          ),
        ),
      ),
    );
  }

  // --- GERADORES DE BITMAP DESCRIPTOR NO CANVAS ---

  /// Bolinha preta opaca remetendo a "origem"
  static Future<BitmapDescriptor> _generateOriginDotDescriptor() async {
    const size = 64.0;
    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder, const Rect.fromLTWH(0, 0, size, size));
    const center = Offset(size / 2, size / 2);

    // Sombra suave
    final shadowPaint = Paint()
      ..color = Colors.black.withValues(alpha: 0.28)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 4);
    canvas.drawCircle(center + const Offset(0, 2), 16, shadowPaint);

    // Borda/halo externa branca
    final haloPaint = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.fill;
    canvas.drawCircle(center, 16, haloPaint);

    // Bolinha preta opaca principal (AppColors.primary)
    final dotPaint = Paint()
      ..color = AppColors.primary
      ..style = PaintingStyle.fill;
    canvas.drawCircle(center, 12, dotPaint);

    // Centro branco fino para contraste e estilo de pino de partida
    final innerDot = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.fill;
    canvas.drawCircle(center, 3.5, innerDot);

    final picture = recorder.endRecording();
    final image = await picture.toImage(size.toInt(), size.toInt());
    final bytes = await image.toByteData(format: ui.ImageByteFormat.png);
    return BitmapDescriptor.bytes(bytes!.buffer.asUint8List());
  }

  /// Bolinha quadriculada remetendo à "chegada"
  static Future<BitmapDescriptor> _generateCheckeredDestDescriptor() async {
    const size = 64.0;
    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder, const Rect.fromLTWH(0, 0, size, size));
    const center = Offset(size / 2, size / 2);
    const radius = 16.0;

    // Sombra suave
    final shadowPaint = Paint()
      ..color = Colors.black.withValues(alpha: 0.28)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 4);
    canvas.drawCircle(center + const Offset(0, 2), radius + 2, shadowPaint);

    // Halo externo branco
    final haloPaint = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.fill;
    canvas.drawCircle(center, radius + 2, haloPaint);

    // Clip circular para o padrão quadriculado
    canvas.save();
    final clipPath = Path()..addOval(Rect.fromCircle(center: center, radius: radius));
    canvas.clipPath(clipPath);

    // Base branca
    final whitePaint = Paint()..color = Colors.white;
    canvas.drawRect(Rect.fromCircle(center: center, radius: radius), whitePaint);

    // Grade quadriculada 4x4 (preto e branco)
    final blackPaint = Paint()..color = AppColors.primary;
    const gridSize = 4;
    final cellSize = (radius * 2) / gridSize;
    final startX = center.dx - radius;
    final startY = center.dy - radius;

    for (int row = 0; row < gridSize; row++) {
      for (int col = 0; col < gridSize; col++) {
        if ((row + col) % 2 == 0) {
          canvas.drawRect(
            Rect.fromLTWH(
              startX + (col * cellSize),
              startY + (row * cellSize),
              cellSize,
              cellSize,
            ),
            blackPaint,
          );
        }
      }
    }
    canvas.restore();

    // Borda preta de acabamento
    final borderPaint = Paint()
      ..color = AppColors.primary
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.0;
    canvas.drawCircle(center, radius, borderPaint);

    final picture = recorder.endRecording();
    final image = await picture.toImage(size.toInt(), size.toInt());
    final bytes = await image.toByteData(format: ui.ImageByteFormat.png);
    return BitmapDescriptor.bytes(bytes!.buffer.asUint8List());
  }

  /// Carrinho preto filled
  static Future<BitmapDescriptor> _generateBlackCarDescriptor() async {
    const size = 64.0;
    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder, const Rect.fromLTWH(0, 0, size, size));
    const center = Offset(size / 2, size / 2);

    // Sombra suave sob o badge do veículo
    final shadowPaint = Paint()
      ..color = Colors.black.withValues(alpha: 0.28)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 4);
    canvas.drawCircle(center + const Offset(0, 2), 18, shadowPaint);

    // Badge circular branco para contraste garantido sobre ruas
    final badgePaint = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.fill;
    canvas.drawCircle(center, 18, badgePaint);

    final badgeBorder = Paint()
      ..color = AppColors.borderSubtle
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.2;
    canvas.drawCircle(center, 18, badgeBorder);

    // Ícone do carrinho preto filled usando Icons.directions_car_rounded
    final textPainter = TextPainter(textDirection: TextDirection.ltr);
    textPainter.text = TextSpan(
      text: String.fromCharCode(Icons.directions_car_rounded.codePoint),
      style: const TextStyle(
        fontSize: 24,
        fontFamily: 'MaterialIcons',
        color: AppColors.primary,
      ),
    );
    textPainter.layout();
    textPainter.paint(
      canvas,
      Offset(
        center.dx - (textPainter.width / 2),
        center.dy - (textPainter.height / 2),
      ),
    );

    final picture = recorder.endRecording();
    final image = await picture.toImage(size.toInt(), size.toInt());
    final bytes = await image.toByteData(format: ui.ImageByteFormat.png);
    return BitmapDescriptor.bytes(bytes!.buffer.asUint8List());
  }
}

/// ============================================================================
/// PINO CENTRAL FIXO: MONOCROMÁTICO PRETO NÃO TOTALMENTE FILLED
/// ============================================================================
class _UberStyleCentralPin extends StatelessWidget {
  final CentralPinType type;
  final String label;
  final String address;

  const _UberStyleCentralPin({
    required this.type,
    required this.label,
    this.address = '',
  });

  @override
  Widget build(BuildContext context) {
    final isDest = type == CentralPinType.destination;
    final typeName = isDest ? 'Destino' : 'Origem';
    final displayText = address.isNotEmpty
        ? '$typeName: $address'
        : (label.isNotEmpty ? label : typeName);

    return Transform.translate(
      offset: const Offset(0, -28),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Balão monocromático elegante acima do pinpoint
          Container(
            constraints: const BoxConstraints(maxWidth: 270),
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
            decoration: BoxDecoration(
              color: AppColors.dockBackground,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppColors.borderSubtle, width: 0.8),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.30),
                  blurRadius: 14,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (isDest)
                  const _MiniCheckeredIcon()
                else
                  const _MiniOriginIcon(),
                const SizedBox(width: 8),
                Flexible(
                  child: Text(
                    displayText,
                    style: const TextStyle(
                      color: AppColors.textLight,
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 6),

          // Pino monocromático preto não totalmente filled (vazado no centro)
          const _MonochromeHollowPin(size: 46),
        ],
      ),
    );
  }
}

/// Widget de pino monocromático preto não totalmente filled (vazado no centro)
class _MonochromeHollowPin extends StatelessWidget {
  final double size;

  const _MonochromeHollowPin({this.size = 46});

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      size: Size(size * 0.72, size),
      painter: _HollowPinPainter(),
    );
  }
}

class _HollowPinPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final width = size.width;
    final height = size.height;
    final headRadius = width / 2;
    final headCenter = Offset(width / 2, headRadius);

    // 1. Sombra sob a ponta do pino no solo
    final groundShadow = Paint()
      ..color = Colors.black.withValues(alpha: 0.28)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 3);
    canvas.drawOval(
      Rect.fromCenter(
        center: Offset(width / 2, height + 2),
        width: 14,
        height: 5,
      ),
      groundShadow,
    );

    // 2. Traçado do pino tipo gota em preto monocromático (AppColors.primary)
    final pinPath = Path();
    pinPath.moveTo(width / 2, height);
    pinPath.cubicTo(
      width * 0.08,
      height * 0.65,
      0,
      headRadius * 1.4,
      0,
      headRadius,
    );
    pinPath.arcToPoint(
      Offset(width, headRadius),
      radius: Radius.circular(headRadius),
      clockwise: true,
    );
    pinPath.cubicTo(
      width,
      headRadius * 1.4,
      width * 0.92,
      height * 0.65,
      width / 2,
      height,
    );
    pinPath.close();

    final fillPaint = Paint()
      ..color = AppColors.primary
      ..style = PaintingStyle.fill;
    canvas.drawPath(pinPath, fillPaint);

    // 3. ABERTURA VAZADA / NÃO TOTALMENTE FILLED
    // Centro vazado em branco/abertura circular elegante
    final hollowCenterPaint = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.fill;
    canvas.drawCircle(headCenter, headRadius * 0.44, hollowCenterPaint);

    // Ponto central de precisão
    final innerDotPaint = Paint()
      ..color = AppColors.primary
      ..style = PaintingStyle.fill;
    canvas.drawCircle(headCenter, headRadius * 0.16, innerDotPaint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

/// Mini bolinha quadriculada para o balão de destino
class _MiniCheckeredIcon extends StatelessWidget {
  const _MiniCheckeredIcon();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 14,
      height: 14,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        border: Border.all(color: Colors.white, width: 1.2),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        children: [
          Expanded(
            child: Row(
              children: [
                Expanded(child: Container(color: Colors.white)),
                Expanded(child: Container(color: AppColors.primary)),
              ],
            ),
          ),
          Expanded(
            child: Row(
              children: [
                Expanded(child: Container(color: AppColors.primary)),
                Expanded(child: Container(color: Colors.white)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Mini bolinha preta opaca para o balão de origem
class _MiniOriginIcon extends StatelessWidget {
  const _MiniOriginIcon();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 14,
      height: 14,
      decoration: BoxDecoration(
        color: Colors.white,
        shape: BoxShape.circle,
        border: Border.all(color: Colors.white, width: 1),
      ),
      alignment: Alignment.center,
      child: Container(
        width: 8,
        height: 8,
        decoration: const BoxDecoration(
          color: AppColors.primary,
          shape: BoxShape.circle,
        ),
      ),
    );
  }
}
