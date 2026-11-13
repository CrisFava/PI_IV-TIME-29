// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:converge/features/home/presentation/widgets/home_driver_form_sheet.dart';
import 'package:converge/features/home/presentation/widgets/home_mode_selector_header.dart';
import 'package:converge/features/home/presentation/widgets/home_passenger_sheet.dart';
import 'package:converge/features/home/presentation/widgets/home_real_map_background.dart';

import '../widgets/become_driver_sheet.dart';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../../app_routes.dart';

import '../../../../core/network/api_error_code.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/services/auth/auth_providers.dart';
import '../../../route/data/models/create_route_request.dart';
import '../../../route/data/models/route_place_model.dart';
import '../../../route/data/models/route_response_model.dart';
import '../../../route/state/saved_routes_controller.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/services/maps/google_maps_service.dart';
import '../../../../core/widgets/ride_dock_navigation.dart';
import '../../../../core/widgets/sheet_drag_to_dismiss.dart';
import '../../../../core/widgets/show_app_notice.dart';
import '../../../ride/data/models/driver_shift_payload.dart';
import '../../../matching/data/models/match_model.dart';
import '../../../matching/presentation/widgets/ride_requests_sheet.dart';
import '../../../matching/state/matching_viewer_provider.dart';
import '../../../matching/state/passenger_match_controller.dart';
import '../../../matching/state/passenger_match_phase.dart';
import '../../../matching/state/passenger_match_state.dart';
import '../../../ride/presentation/widgets/ride_history_list.dart';
import '../../../vehicle/data/repository/vehicle_repository_impl.dart';
import '../../../vehicle/presentation/widgets/add_vehicle_sheet.dart';
import '../../../vehicle/presentation/widgets/my_vehicles_section.dart';
import '../../../vehicle/state/driver_access.dart';
import '../../../vehicle/state/driver_access_provider.dart';
import '../../../vehicle/state/my_vehicles_controller.dart';
import '../../../auth/presentation/widgets/edit_profile_sheet.dart';
import '../widgets/campus_spots.dart';
import '../widgets/passenger_ride_step.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../../auth/state/auth_controller.dart';
import '../../../auth/state/session/auth_session.dart';
import '../../../chat/presentation/widgets/conversations_tab.dart';

class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
  int _currentTabIndex = 0;
  bool _isCarMenuOpen = false;

  // Modo ativo na tela principal (Passageiro vs Motorista)
  AppRideMode _currentMode = AppRideMode.passenger;

  // No modo motorista: painel de pedidos de vaga no lugar do painel de expediente
  bool _showRideRequests = false;

  // Expediente ativo: o botão do carro fica verde e o menu mostra "Em expediente"
  bool _isOnShift = false;

  // Pedido de carona em andamento com o painel escondido: a busca continua e o
  // botão do carro fica azul; o menu do carro reabre os detalhes
  bool _passengerSheetHidden = false;

  // Destino do expediente, guardado à parte para o fluxo de pedir carona não apagá-lo
  String _shiftDestinationAddress = '';
  LatLng? _shiftDestination;
  List<LatLng>? _shiftRoutePoints;

  // Muda a cada expediente iniciado ou encerrado: a rota calculada para um
  // expediente anterior é descartada
  int _shiftGeneration = 0;

  // Muda sempre que o rascunho do pedido de carona é fechado: um cálculo de rota
  // que termine depois disso não reabre o painel
  int _draftGeneration = 0;

  // Estado do fluxo Uber-like do passageiro (etapas graduais)
  PassengerRideStep _passengerStep = PassengerRideStep.idle;
  LatLng _pickupLocation = campusCoordinates;
  String _pickupAddress = 'PUC-Campinas - Portaria Principal';
  LatLng? _destinationLocation;
  String _destinationAddress = '';

  // Rotas de carro pelas ruas reais
  List<LatLng>? _tripRoutePoints;
  List<LatLng>? _driverApproachRoutePoints;

  // Estado do raio de busca do motorista em KM (refletido no mapa)
  double _driverSearchRadiusKm = 5.0;

  void _onCameraCenterChanged(LatLng target) async {
    if (_passengerStep == PassengerRideStep.dragMapPickup ||
        _passengerStep == PassengerRideStep.selectPickup) {
      _pickupLocation = target;
      final addr = await GoogleMapsService.getAddressFromCoordinates(target);
      if (mounted &&
          (_passengerStep == PassengerRideStep.dragMapPickup ||
              _passengerStep == PassengerRideStep.selectPickup)) {
        setState(() => _pickupAddress = addr);
      }
    } else if (_passengerStep == PassengerRideStep.dragMapDestination ||
        _passengerStep == PassengerRideStep.searchDestination) {
      _destinationLocation = target;
      final addr = await GoogleMapsService.getAddressFromCoordinates(target);
      if (mounted &&
          (_passengerStep == PassengerRideStep.dragMapDestination ||
              _passengerStep == PassengerRideStep.searchDestination)) {
        setState(() => _destinationAddress = addr);
      }
    }
  }

  void _handleStartDragMapPickup() {
    setState(() {
      _passengerStep = PassengerRideStep.dragMapPickup;
    });
    _onCameraCenterChanged(_pickupLocation);
  }

  void _handlePickupChosen(String title, LatLng pos) {
    setState(() {
      _pickupAddress = title;
      _pickupLocation = pos;
      _passengerStep = PassengerRideStep.searchDestination;
    });
  }

  void _handleConfirmPickup() {
    setState(() {
      _passengerStep = PassengerRideStep.searchDestination;
    });
  }

  void _handleStartDragMapDestination() {
    setState(() {
      _passengerStep = PassengerRideStep.dragMapDestination;
      _destinationLocation ??= LatLng(
        _pickupLocation.latitude + 0.0072,
        _pickupLocation.longitude + 0.0052,
      );
    });
    if (_destinationLocation != null) {
      _onCameraCenterChanged(_destinationLocation!);
    }
  }

  Future<void> _handleDestinationChosen(String title, LatLng pos) async {
    setState(() {
      _destinationAddress = title;
      _destinationLocation = pos;
    });
    await _calculateRouteAndConfirm();
  }

  Future<void> _handleConfirmDestination() async {
    await _calculateRouteAndConfirm();
  }

  Future<void> _calculateRouteAndConfirm() async {
    final generation = _draftGeneration;
    final step = _passengerStep;
    // O rascunho foi fechado (ou mudou de etapa) enquanto a rota era calculada
    bool isStale() =>
        !mounted || generation != _draftGeneration || step != _passengerStep;

    List<LatLng>? routePoints;
    if (_destinationLocation != null) {
      try {
        routePoints = await GoogleMapsService.computeRoutes(
          origin: _pickupLocation,
          destination: _destinationLocation!,
        );
      } on RoutesException catch (e) {
        if (!isStale()) _showError(e.message);
        return;
      } catch (e) {
        if (!isStale()) {
          _showError('Não foi possível calcular o trajeto. Tente de novo.');
        }
        return;
      }
    }

    if (!isStale()) {
      setState(() {
        _tripRoutePoints = routePoints;
        _passengerStep = PassengerRideStep.routeConfirmation;
      });
    }
  }

  /// Salva (ou reaproveita) o trajeto de procura e busca os motoristas compatíveis.
  Future<void> _handleConfirmRideOrder() async {
    if (_destinationLocation == null) {
      _showError('Escolha o ponto de desembarque antes de buscar motoristas.');
      return;
    }

    HapticFeedback.mediumImpact();
    setState(() => _passengerStep = PassengerRideStep.driverList);

    final controller = ref.read(passengerMatchProvider.notifier);
    // Limpa a busca anterior: a lista mostra o carregamento até os novos motoristas chegarem
    controller.reset();
    final routeId = await controller.prepareSearchRoute(
      _buildRouteRequest(isDriver: false),
    );
    if (!mounted) return;

    if (routeId == null) {
      setState(() => _passengerStep = PassengerRideStep.routeConfirmation);
      return;
    }

    ref.read(savedRoutesProvider.notifier).loadMyRoutes();
    await controller.findDrivers(routeId);
  }

  /// Pede a vaga ao motorista escolhido e espera a resposta dele.
  Future<void> _handleSelectDriver(MatchModel driver) async {
    if (ref.read(passengerMatchProvider).isBusy) return;

    setState(() => _passengerStep = PassengerRideStep.waitingDriver);
    final requested = await ref
        .read(passengerMatchProvider.notifier)
        .requestSeat(driver);

    if (!requested && mounted) {
      setState(() => _passengerStep = PassengerRideStep.driverList);
    }
  }

  void _handleRetryMatches() {
    final routeId = ref.read(passengerMatchProvider).searchRouteId;
    if (routeId == null) return;

    HapticFeedback.lightImpact();
    ref.read(passengerMatchProvider.notifier).findDrivers(routeId);
  }

  Future<void> _handleCancelRequest() async {
    await ref.read(passengerMatchProvider.notifier).cancelRequest();
    if (mounted) {
      setState(() => _passengerStep = PassengerRideStep.driverList);
    }
  }

  /// Reage às mudanças do pedido de vaga: aceite, recusa e avisos.
  void _onPassengerMatchChanged(
    PassengerMatchState? previous,
    PassengerMatchState next,
  ) {
    final notice = next.notice;
    if (notice != null && notice != previous?.notice) {
      _showError(notice);
      Future.microtask(
        () => ref.read(passengerMatchProvider.notifier).clearNotice(),
      );
    }

    if (previous?.phase == next.phase) return;

    if (next.phase == PassengerMatchPhase.accepted) {
      _handleDriverAccepted(next.selected);
    } else if (next.phase == PassengerMatchPhase.ready &&
        _passengerStep == PassengerRideStep.waitingDriver) {
      setState(() => _passengerStep = PassengerRideStep.driverList);
    }
  }

  Future<void> _handleDriverAccepted(MatchModel? driver) async {
    HapticFeedback.mediumImpact();
    setState(() {
      _passengerStep = PassengerRideStep.driverAccepted;
      _passengerSheetHidden = false;
    });

    if (driver == null) return;
    try {
      final approachRoute = await GoogleMapsService.computeRoutes(
        origin: LatLng(driver.origin.latitude, driver.origin.longitude),
        destination: _pickupLocation,
      );
      if (mounted) setState(() => _driverApproachRoutePoints = approachRoute);
    } catch (e) {
      debugPrint('Erro ao obter o trajeto de aproximação do motorista: $e');
    }
  }

  /// Abre o chat da carona aceita (Parte B).
  void _handleSendMessageToDriver() {
    HapticFeedback.lightImpact();
    final rideId = ref.read(passengerMatchProvider).rideId;
    final viewerId = ref.read(matchingViewerIdProvider);

    if (rideId == null || viewerId == null) {
      _showError('Não foi possível abrir a conversa agora.');
      return;
    }

    context.push(AppRoutes.chatOf(rideId: rideId, passengerId: viewerId));
  }

  Future<void> _handleCancelFlow() async {
    final matchController = ref.read(passengerMatchProvider.notifier);
    if (_passengerStep == PassengerRideStep.waitingDriver) {
      await matchController.cancelRequest();
      if (!mounted) return;
    }

    setState(() {
      _draftGeneration++;
      _passengerSheetHidden = false;
      if (_passengerStep == PassengerRideStep.selectPickup) {
        _passengerStep = PassengerRideStep.chooseRouteOption;
      } else {
        _passengerStep = PassengerRideStep.idle;
        _destinationLocation = null;
        _destinationAddress = '';
        _tripRoutePoints = null;
        _driverApproachRoutePoints = null;
      }
    });

    if (_passengerStep == PassengerRideStep.idle) matchController.reset();
  }

  Future<void> _handleSelectSavedRoute(RouteResponseModel route) async {
    HapticFeedback.mediumImpact();
    setState(() {
      _pickupAddress = route.origin.address;
      _pickupLocation = LatLng(route.origin.latitude, route.origin.longitude);
      _destinationAddress = route.destination.address;
      _destinationLocation = LatLng(
        route.destination.latitude,
        route.destination.longitude,
      );
      if (route.encodedPolyline != null && route.encodedPolyline!.isNotEmpty) {
        _tripRoutePoints = GoogleMapsService.decodePolyline(
          route.encodedPolyline!,
        );
      } else {
        _tripRoutePoints = [];
      }
      _passengerStep = PassengerRideStep.driverList;
    });

    if (route.type == AppRouteType.search) {
      ref.read(passengerMatchProvider.notifier).findDrivers(route.id);
    } else {
      // Trajeto de oferta: quem pede carona é o passageiro. O motorista vê os pedidos no painel.
      setState(() => _passengerStep = PassengerRideStep.idle);
      return;
    }

    if ((_tripRoutePoints == null || _tripRoutePoints!.isEmpty) &&
        _destinationLocation != null) {
      try {
        final result = await GoogleMapsService.computeRoutes(
          origin: _pickupLocation,
          destination: _destinationLocation!,
        );
        if (mounted && result.isNotEmpty) {
          setState(() {
            _tripRoutePoints = result;
          });
        }
      } catch (_) {}
    }
  }

  void _onTabSelected(int index) {
    if (_isCarMenuOpen) {
      setState(() => _isCarMenuOpen = false);
    }
    if (_currentTabIndex == index) return;
    HapticFeedback.lightImpact();
    setState(() => _currentTabIndex = index);
  }

  void _onToggleCarMenu() {
    // Ao abrir o menu, fecha os painéis que estiverem abertos no mapa
    if (!_isCarMenuOpen) _closeOpenSheets();

    // Fora do mapa, o carro leva de volta à tela principal com o menu aberto
    if (_currentTabIndex != 0) {
      setState(() {
        _currentTabIndex = 0;
        _isCarMenuOpen = true;
      });
      return;
    }
    setState(() => _isCarMenuOpen = !_isCarMenuOpen);
  }

  void _showLockedDriverWarning() {
    HapticFeedback.mediumImpact();
    setState(() => _isCarMenuOpen = false);
    showBecomeDriverSheet(
      context: context,
      onSuccess: () {
        setState(() {
          _isCarMenuOpen = false;
          _currentMode = AppRideMode.driver;
          _currentTabIndex = 0;
        });
      },
    );
  }

  void _onRequestRide() {
    setState(() {
      _isCarMenuOpen = false;
      _currentMode = AppRideMode.passenger;
      _currentTabIndex = 0;
      _passengerStep = PassengerRideStep.chooseRouteOption;
      _passengerSheetHidden = false;
    });
    ref.read(savedRoutesProvider.notifier).loadMyRoutes();
  }

  void _onStartDriverShiftFromDock(bool canDrive) {
    setState(() => _isCarMenuOpen = false);
    if (_isDriverActionBlocked()) return;
    final latestSession = ref.read(authControllerProvider).value;
    final isDriverNow =
        canDrive ||
        (latestSession is Authenticated && latestSession.user.isDriver);

    if (isDriverNow) {
      setState(() {
        _currentMode = AppRideMode.driver;
        _currentTabIndex = 0;
        _showRideRequests = false;
      });
    } else {
      _showLockedDriverWarning();
    }
  }

  void _handleCloseDriverFormSheet() {
    setState(() {
      _currentMode = AppRideMode.passenger;
      _destinationLocation = null;
      _destinationAddress = '';
      _tripRoutePoints = null;
    });
  }

  Future<void> _handleDriverDestinationSelected(
    String address,
    LatLng position,
  ) async {
    setState(() {
      _destinationAddress = address;
      _destinationLocation = position;
    });
    try {
      final points = await GoogleMapsService.computeRoutes(
        origin: _pickupLocation,
        destination: position,
      );
      if (mounted && points.isNotEmpty) {
        setState(() {
          _tripRoutePoints = points;
        });
      }
    } catch (_) {}
  }

  /// Pedido de carona em andamento: procurando motorista, aguardando a resposta ou aceito.
  bool get _hasActiveRideRequest =>
      _passengerStep == PassengerRideStep.driverList ||
      _passengerStep == PassengerRideStep.waitingDriver ||
      _passengerStep == PassengerRideStep.driverAccepted;

  /// Fecha os painéis do mapa. Um pedido de carona em andamento só é escondido
  /// (a busca continua e volta pelo botão do carro). O expediente também continua.
  void _closeOpenSheets() {
    final keepRequest = _hasActiveRideRequest;
    final draftOpen = _passengerStep != PassengerRideStep.idle && !keepRequest;
    if (!_currentMode.isDriver && !draftOpen && !keepRequest) return;

    setState(() {
      if (_currentMode.isDriver) {
        _currentMode = AppRideMode.passenger;
        _showRideRequests = false;
      }
      if (keepRequest) {
        _passengerSheetHidden = true;
        return;
      }
      if (draftOpen) {
        _draftGeneration++;
        _passengerStep = PassengerRideStep.idle;
      }
      _destinationLocation = null;
      _destinationAddress = '';
      _tripRoutePoints = null;
    });
    if (draftOpen) ref.read(passengerMatchProvider.notifier).reset();
  }

  /// Reabre os detalhes do pedido de carona escondido.
  void _openRideSearch() {
    setState(() {
      _isCarMenuOpen = false;
      _currentTabIndex = 0;
      _currentMode = AppRideMode.passenger;
      _showRideRequests = false;
      _passengerSheetHidden = false;
    });
  }

  /// Cancela a busca de motorista (e o pedido de vaga, se já tiver sido feito).
  Future<void> _handleCancelRideSearch() async {
    HapticFeedback.mediumImpact();
    await _handleCancelFlow();
  }

  /// Arrastar o painel do passageiro para baixo: com um pedido em andamento só
  /// esconde (a busca continua); nas outras etapas, fecha como o X.
  void _handlePassengerSheetDismissed() {
    if (_hasActiveRideRequest) {
      setState(() => _passengerSheetHidden = true);
      return;
    }
    _handleCancelFlow();
  }

  /// O motorista não precisa de "expediente": com um trajeto de oferta ativo ele já aparece
  /// no matching. Aqui ele vai para o painel com os pedidos de vaga.

  Future<void> _handleStartDriverShift(DriverShiftPayload payload) async {
    HapticFeedback.mediumImpact();
    final generation = ++_shiftGeneration;
    setState(() {
      _isOnShift = true;
      _shiftDestinationAddress = _destinationAddress.isNotEmpty
          ? _destinationAddress
          : payload.destination;
      _shiftDestination = _destinationLocation;
      _shiftRoutePoints = _tripRoutePoints;
    });
    _openRideRequests();

    // Sem a rota ainda calculada, busca agora para desenhar o caminho até o destino
    final destination = _shiftDestination;
    if (destination != null &&
        (_shiftRoutePoints == null || _shiftRoutePoints!.isEmpty)) {
      try {
        final points = await GoogleMapsService.computeRoutes(
          origin: _pickupLocation,
          destination: destination,
        );
        if (mounted &&
            _isOnShift &&
            generation == _shiftGeneration &&
            points.isNotEmpty) {
          setState(() => _shiftRoutePoints = points);
        }
      } catch (e) {
        debugPrint('Erro ao calcular a rota do expediente: $e');
      }
    }
  }

  /// Encerra o expediente: fecha os painéis de motorista e volta ao mapa do passageiro.
  void _handleEndDriverShift() {
    setState(() {
      _shiftGeneration++;
      _isOnShift = false;
      _showRideRequests = false;
      _currentMode = AppRideMode.passenger;
      // Um pedido de carona em andamento usa o mesmo destino e a mesma rota
      if (!_hasActiveRideRequest) {
        _destinationLocation = null;
        _destinationAddress = '';
        _tripRoutePoints = null;
      }
      _shiftDestinationAddress = '';
      _shiftDestination = null;
      _shiftRoutePoints = null;
    });
  }

  /// Fechar os pedidos: em expediente, só esconde o painel (o expediente continua);
  /// fora dele, volta para o painel de expediente.
  void _handleCloseRideRequests() {
    setState(() {
      _showRideRequests = false;
      if (_isOnShift) _currentMode = AppRideMode.passenger;
    });
  }

  /// Cadastra um carro para liberar (ou manter) o acesso de motorista.
  Future<void> _handleAddVehicle() async {
    setState(() => _isCarMenuOpen = false);
    await showAddVehicleSheet(context);
  }

  /// Sem carro, o motorista perde o acesso: encerra o expediente e fecha os painéis.
  void _onDriverAccessChanged(DriverAccess? previous, DriverAccess next) {
    if (!next.isBlockedByVehicle || previous == next) return;
    setState(() {
      _shiftGeneration++;
      _isOnShift = false;
      _showRideRequests = false;
      _shiftDestinationAddress = '';
      _shiftDestination = null;
      _shiftRoutePoints = null;
      if (_currentMode.isDriver) _currentMode = AppRideMode.passenger;
    });
  }

  /// Bloqueia as ações de motorista sem carro confirmado: sem carro, abre o
  /// cadastro; com a lista de carros indisponível, avisa e tenta carregar de novo.
  bool _isDriverActionBlocked() {
    final access = ref.read(driverAccessProvider);
    if (access.isBlockedByVehicle) {
      _handleAddVehicle();
      return true;
    }
    if (access.isUnavailable) {
      _showError(
        'Não foi possível conferir seus carros agora. Tente de novo em instantes.',
      );
      if (ref.read(myVehiclesProvider).hasError) {
        ref.invalidate(myVehiclesProvider);
      }
      return true;
    }
    return false;
  }

  /// Mostra os pedidos de vaga no painel sobre o mapa, no modo motorista.
  void _openRideRequests() {
    if (_isDriverActionBlocked()) return;
    setState(() {
      _isCarMenuOpen = false;
      _currentMode = AppRideMode.driver;
      _currentTabIndex = 0;
      _showRideRequests = true;
    });
  }

  /// Dados do trajeto marcado no mapa. Os horários e os dias ainda são fixos neste fluxo.
  CreateRouteRequest _buildRouteRequest({
    required bool isDriver,
    String? vehicleId,
    int? totalSeats,
  }) {
    return CreateRouteRequest(
      type: isDriver ? AppRouteType.offer : AppRouteType.search,
      vehicleId: vehicleId,
      origin: RoutePlaceModel(
        address: _pickupAddress,
        neighborhood: 'Campus / Origem',
        city: 'Campinas',
        latitude: _pickupLocation.latitude,
        longitude: _pickupLocation.longitude,
      ),
      destination: RoutePlaceModel(
        address: _destinationAddress.isNotEmpty
            ? _destinationAddress
            : 'Destino Selecionado',
        neighborhood: 'Destino',
        city: 'Campinas',
        latitude: _destinationLocation!.latitude,
        longitude: _destinationLocation!.longitude,
      ),
      departureTime: '08:00',
      estimatedArrivalTime: '08:45',
      weekDays: const {'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'},
      maxDetourKm: 3.0,
      totalSeats: isDriver ? (totalSeats ?? 3) : 1,
    );
  }

  /// Veículo ativo do motorista, exigido em todo trajeto de oferta.
  Future<({String id, int seats})?> _findDriverVehicle() async {
    try {
      final vehicles = await ref
          .read(vehicleRepositoryProvider)
          .listMyVehicles();
      for (final vehicle in vehicles) {
        if (vehicle.active) {
          return (id: vehicle.id, seats: vehicle.passengerSeats);
        }
      }
    } catch (e) {
      debugPrint('Erro ao buscar os veículos do motorista: $e');
    }
    return null;
  }

  Future<void> _handleSaveRoute() async {
    HapticFeedback.lightImpact();
    if (_destinationLocation == null) {
      _showError('Selecione uma origem e destino antes de salvar o trajeto.');
      return;
    }

    final isDriver = _currentMode == AppRideMode.driver;
    final vehicle = isDriver ? await _findDriverVehicle() : null;
    if (!mounted) return;
    if (isDriver && vehicle == null) {
      _showError('Cadastre um veículo antes de salvar um trajeto de oferta.');
      return;
    }

    final request = _buildRouteRequest(
      isDriver: isDriver,
      vehicleId: vehicle?.id,
      totalSeats: vehicle?.seats,
    );

    final authService = ref.read(authServiceProvider);
    if (authService.currentUser == null) {
      ref.read(savedRoutesProvider.notifier).saveLocalRoute(request);
      await ref.read(authControllerProvider.notifier).signOut();
      return;
    }

    try {
      final saved = await ref
          .read(savedRoutesProvider.notifier)
          .saveRoute(request);
      if (saved.encodedPolyline != null && saved.encodedPolyline!.isNotEmpty) {
        final points = GoogleMapsService.decodePolyline(saved.encodedPolyline!);
        if (points.isNotEmpty && mounted) {
          setState(() {
            _tripRoutePoints = points;
          });
        }
      }
      if (mounted) {
        HapticFeedback.mediumImpact();
      }
    } on ApiException catch (e) {
      ref.read(savedRoutesProvider.notifier).saveLocalRoute(request);
      if (e.isUnauthorized || e.code == ApiErrorCode.invalidCredentials) {
        // Desloga o usuário imediatamente sem emitir alerta de sessão expirada
        await ref.read(authControllerProvider.notifier).signOut();
        return;
      }
      if (mounted) {
        _showError(e.message);
      }
    } catch (e) {
      ref.read(savedRoutesProvider.notifier).saveLocalRoute(request);
      if (mounted) {
        _showError(
          'Não foi possível salvar o trajeto no servidor. Ele ficou só neste aparelho.',
        );
      }
    }
  }

  Future<void> _confirmSignOut() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppColors.cardBackground,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Row(
          children: [
            Icon(Icons.logout_rounded, color: AppColors.error, size: 24),
            SizedBox(width: 10),
            Text(
              'Sair da Conta',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w800,
                color: AppColors.textPrimary,
              ),
            ),
          ],
        ),
        content: const Text(
          'Deseja realmente sair da sua conta Converge?',
          style: TextStyle(fontSize: 14, color: AppColors.textSecondary),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text(
              'Cancelar',
              style: TextStyle(
                fontWeight: FontWeight.w600,
                color: AppColors.textSecondary,
              ),
            ),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.error,
              elevation: 0,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text(
              'Sim, Sair',
              style: TextStyle(
                fontWeight: FontWeight.w700,
                color: AppColors.textLight,
              ),
            ),
          ),
        ],
      ),
    );

    if (confirmed == true && mounted) {
      ref.read(authActionProvider.notifier).signOut();
    }
  }

  /// Só erros viram aviso (no topo, com botão de fechar): os informativos
  /// atrapalhavam o fluxo de pedir carona.
  void _showError(String message) {
    showAppNotice(context, message, type: AppNoticeType.error);
  }

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(authControllerProvider).value;
    final user = session is Authenticated ? session.user : null;
    final bottomPadding = MediaQuery.paddingOf(context).bottom;
    final effectiveBottomOffset = bottomPadding > 0 ? bottomPadding : 16.0;

    // Verificação estrita de permissão de motorista através de participationRoles
    final canDrive = user?.isDriver ?? false;

    ref.listen(passengerMatchProvider, _onPassengerMatchChanged);
    ref.listen(driverAccessProvider, _onDriverAccessChanged);
    final driverAccess = ref.watch(driverAccessProvider);

    return Scaffold(
      backgroundColor: AppColors.scaffoldBackground,
      body: user == null
          ? const Center(
              child: CircularProgressIndicator(
                color: AppColors.primary,
                strokeWidth: 2.8,
              ),
            )
          : Stack(
              children: [
                // ============================================================
                // CAMADA 1: CONTEÚDO PRINCIPAL (MAPA OU ABAS SECUNDÁRIAS)
                // ============================================================
                Positioned.fill(
                  child: _currentTabIndex == 0
                      ? _buildUberHomeView(user, canDrive, bottomPadding)
                      : _buildSecondaryTabView(user, bottomPadding),
                ),

                // ============================================================
                // CAMADA 2: BARREIRA TRANSLÚCIDA DE DISMISS (DIM TOTAL)
                // ============================================================
                Positioned.fill(
                  key: const ValueKey('home_barrier_layer'),
                  child: IgnorePointer(
                    ignoring: !_isCarMenuOpen,
                    child: AnimatedOpacity(
                      opacity: _isCarMenuOpen ? 1.0 : 0.0,
                      duration: const Duration(milliseconds: 320),
                      curve: Curves.easeInOutCubic,
                      child: GestureDetector(
                        onTap: () => setState(() => _isCarMenuOpen = false),
                        behavior: HitTestBehavior.opaque,
                        child: Container(color: AppColors.barrier),
                      ),
                    ),
                  ),
                ),

                // ============================================================
                // CAMADA 3: DOCK FLUTUANTE DE NAVEGAÇÃO INFERIOR
                // ============================================================
                Positioned(
                  key: const ValueKey('home_dock_layer'),
                  left: 16,
                  right: 16,
                  bottom: effectiveBottomOffset,
                  child: RideDockNavigation(
                    key: const ValueKey('ride_dock_navigation'),
                    currentIndex: _currentTabIndex,
                    canDrive: canDrive,
                    onLockedDriverTap: _showLockedDriverWarning,
                    onTabSelected: _onTabSelected,
                    isOpen: _isCarMenuOpen,
                    onToggleOpen: _onToggleCarMenu,
                    onRequestRide: _onRequestRide,
                    onStartDriverShift: () => _isOnShift
                        ? _openRideRequests()
                        : _onStartDriverShiftFromDock(canDrive),
                    isOnShift: _isOnShift,
                    onEndDriverShift: _handleEndDriverShift,
                    needsVehicle: driverAccess.isBlockedByVehicle,
                    onAddVehicle: _handleAddVehicle,
                    // Pedido aceito também aparece: o menu reabre os detalhes da
                    // carona em vez de começar um pedido novo
                    isSearchingRide: _hasActiveRideRequest,
                    rideSearchTitle: switch (_passengerStep) {
                      PassengerRideStep.waitingDriver =>
                        'Aguardando o motorista',
                      PassengerRideStep.driverAccepted => 'Carona aceita',
                      _ => 'Procurando motorista',
                    },
                    rideSearchDestination: _destinationAddress,
                    onOpenRideSearch: _openRideSearch,
                    onCancelRideSearch: _handleCancelRideSearch,
                  ),
                ),
              ],
            ),
    );
  }

  // --- VIEW 0: TELA PRINCIPAL COM GOOGLE MAPS REAL ---
  Widget _buildUberHomeView(dynamic user, bool canDrive, double bottomPadding) {
    // Em expediente (sem um pedido de carona em andamento), o mapa mostra o caminho
    // até o destino do expediente, mesmo com o painel fechado
    final showShiftRoute =
        _isOnShift &&
        _passengerStep == PassengerRideStep.idle &&
        (_currentMode.isPassenger || _showRideRequests);
    // Os painéis do motorista abrem em 46% da tela: o mapa enquadra só a parte de cima
    final mapBottomPadding = _currentMode.isDriver
        ? MediaQuery.sizeOf(context).height * 0.46
        : 0.0;

    return Stack(
      key: const ValueKey('uber_map_home_view'),
      children: [
        // 1. Camada Base: Google Maps real em tela cheia com círculo dinâmico / marcadores / rotas
        Positioned.fill(
          child: HomeRealMapBackground(
            isDriverMode: _currentMode.isDriver,
            searchRadiusKm: _driverSearchRadiusKm,
            showDrivers:
                _currentMode.isPassenger &&
                _passengerStep == PassengerRideStep.driverList,
            availableDrivers: ref.watch(passengerMatchProvider).matches,
            acceptedDriver: _passengerStep == PassengerRideStep.driverAccepted
                ? ref.watch(passengerMatchProvider).selected
                : null,
            pickupLocation: _pickupLocation,
            destinationLocation: showShiftRoute
                ? _shiftDestination
                : _destinationLocation,
            pickupAddress: _pickupAddress,
            destinationAddress: showShiftRoute
                ? _shiftDestinationAddress
                : _destinationAddress,
            bottomPadding: mapBottomPadding,
            showTrajectory:
                (showShiftRoute && _shiftDestination != null) ||
                (_currentMode.isDriver && _destinationLocation != null) ||
                (_currentMode.isPassenger &&
                    (_passengerStep == PassengerRideStep.routeConfirmation ||
                        _passengerStep == PassengerRideStep.driverList ||
                        _passengerStep == PassengerRideStep.waitingDriver ||
                        _passengerStep == PassengerRideStep.driverAccepted)),
            tripRoutePoints: showShiftRoute
                ? _shiftRoutePoints
                : _tripRoutePoints,
            driverApproachRoutePoints: _driverApproachRoutePoints,
            isDraggingPin:
                _currentMode.isPassenger &&
                (_passengerStep == PassengerRideStep.dragMapPickup ||
                    _passengerStep == PassengerRideStep.dragMapDestination),
            centralPinType:
                _passengerStep == PassengerRideStep.dragMapDestination
                ? CentralPinType.destination
                : CentralPinType.pickup,
            pinLabel: _passengerStep == PassengerRideStep.dragMapDestination
                ? 'Ponto de Desembarque'
                : 'Ponto de Embarque',
            onCameraCenterChanged: _onCameraCenterChanged,
            universityName: user.academicRecord.university.toString(),
            onDriverSelected: _handleSelectDriver,
            onMapTap: () {
              if (_isCarMenuOpen) {
                setState(() => _isCarMenuOpen = false);
              }
            },
          ),
        ),

        // 2. Camada Intermediária: Painel Retrátil específico de acordo com o modo
        if (_currentMode.isDriver && _showRideRequests)
          Positioned.fill(
            child: SheetDragToDismiss(
              onDismiss: _handleCloseRideRequests,
              child: RideRequestsSheet(
                bottomSafeArea: bottomPadding,
                destinationAddress: _isOnShift
                    ? _shiftDestinationAddress
                    : null,
                onClose: _handleCloseRideRequests,
              ),
            ),
          )
        else if (_currentMode.isDriver)
          Positioned.fill(
            child: SheetDragToDismiss(
              onDismiss: _handleCloseDriverFormSheet,
              child: HomeDriverFormSheet(
                initialSearchRadiusKm: _driverSearchRadiusKm,
                bottomSafeArea: bottomPadding,
                onSearchRadiusChanged: (newRadius) {
                  setState(() => _driverSearchRadiusKm = newRadius);
                },
                onStartShift: _handleStartDriverShift,
                onSaveRoute: _handleSaveRoute,
                onClose: _handleCloseDriverFormSheet,
                onDestinationSelected: _handleDriverDestinationSelected,
              ),
            ),
          )
        else if (_passengerStep != PassengerRideStep.idle &&
            !_passengerSheetHidden)
          Positioned.fill(
            child: SheetDragToDismiss(
              onDismiss: _handlePassengerSheetDismissed,
              child: HomePassengerSheet(
                step: _passengerStep,
                pickupAddress: _pickupAddress,
                destinationAddress: _destinationAddress,
                pickupLocation: _pickupLocation,
                destinationLocation: _destinationLocation,
                bottomSafeArea: bottomPadding,
                onStartRideFlow: () {
                  setState(
                    () => _passengerStep = PassengerRideStep.chooseRouteOption,
                  );
                  ref.read(savedRoutesProvider.notifier).loadMyRoutes();
                },
                onChooseMapOption: () {
                  setState(
                    () => _passengerStep = PassengerRideStep.selectPickup,
                  );
                },
                onSelectSavedRoute: _handleSelectSavedRoute,
                onStartDragMapPickup: _handleStartDragMapPickup,
                onStartDragMapDestination: _handleStartDragMapDestination,
                onPickupChosen: _handlePickupChosen,
                onDestinationChosen: _handleDestinationChosen,
                onConfirmPickup: _handleConfirmPickup,
                onConfirmDestination: _handleConfirmDestination,
                onConfirmRideOrder: _handleConfirmRideOrder,
                onSelectDriver: _handleSelectDriver,
                onRetryMatches: _handleRetryMatches,
                onCancelRequest: _handleCancelRequest,
                onSendMessageToDriver: _handleSendMessageToDriver,
                onCancelFlow: _handleCancelFlow,
                onSaveRoute: _handleSaveRoute,
              ),
            ),
          ),
      ],
    );
  }

  // --- VIEW ALTERNATIVA: DEMAIS ABAS (BUSCAR, CHAT, PERFIL) ---
  Widget _buildSecondaryTabView(dynamic user, double bottomPadding) {
    return Container(
      key: ValueKey('tab_view_$_currentTabIndex'),
      color: AppColors.scaffoldBackground,
      child: SafeArea(
        bottom: false,
        child: Column(
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              color: AppColors.cardBackground,
              child: Row(
                children: [
                  IconButton(
                    tooltip: 'Voltar ao mapa',
                    onPressed: () => _onTabSelected(0),
                    icon: const Icon(
                      Icons.arrow_back_rounded,
                      color: AppColors.textPrimary,
                    ),
                  ),
                  const SizedBox(width: 4),
                  Text(
                    _getTabTitle(),
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w800,
                      color: AppColors.textPrimary,
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: switch (_currentTabIndex) {
                1 => _buildSearchAndHistoryTab(bottomPadding),
                2 => _buildChatTab(bottomPadding),
                3 => _buildProfileTab(user, bottomPadding),
                _ => const SizedBox.shrink(),
              },
            ),
          ],
        ),
      ),
    );
  }

  String _getTabTitle() => switch (_currentTabIndex) {
    1 => 'Histórico de caronas',
    2 => 'Mensagens',
    3 => 'Meu Perfil Acadêmico',
    _ => 'Início',
  };

  // --- ABA 1: HISTÓRICO ---
  // Só o histórico de viagens, com a etiqueta Motorista/Passageiro em cada uma.
  // Os pedidos de vaga ficam no botão do carro.
  Widget _buildSearchAndHistoryTab(double bottomPadding) {
    return RideHistoryList(bottomPadding: bottomPadding);
  }

  // --- ABA 2: CHAT ---
  // As conversas reais das caronas aceitas ficam na feature de chat.
  Widget _buildChatTab(double bottomPadding) {
    return ConversationsTab(bottomPadding: bottomPadding);
  }

  // --- ABA 3: PERFIL ---
  Widget _buildProfileTab(dynamic user, double bottomPadding) {
    return ListView(
      padding: EdgeInsets.fromLTRB(16, 16, 16, 120 + bottomPadding),
      children: [
        Container(
          padding: const EdgeInsets.all(22),
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: AppColors.borderSubtle),
          ),
          child: Column(
            children: [
              const CircleAvatar(
                radius: 38,
                backgroundColor: AppColors.neutralLight,
                child: Icon(
                  Icons.person_rounded,
                  size: 42,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 12),
              Text(
                user.name.toString(),
                style: const TextStyle(
                  fontSize: 19,
                  fontWeight: FontWeight.w700,
                  color: AppColors.textPrimary,
                  letterSpacing: -0.3,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                user.email.toString(),
                style: const TextStyle(
                  fontSize: 13,
                  color: AppColors.textSecondary,
                ),
              ),
              const SizedBox(height: 14),

              // Status das Permissões do Usuário (Passenger e Driver)
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.neutralLight,
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: [
                    _buildRoleBadge('Passageiro', true),
                    _buildRoleBadge(
                      ref.watch(driverAccessProvider).isBlockedByVehicle
                          ? 'Motorista (sem carro)'
                          : 'Motorista',
                      ref.watch(driverAccessProvider).canDrive,
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 16),

              const Divider(color: AppColors.neutralLight),
              const SizedBox(height: 10),
              _buildProfileRow('Telefone', user.phone.toString()),
              _buildProfileRow(
                'Universidade',
                user.academicRecord.university.toString(),
              ),
              _buildProfileRow('Curso', user.academicRecord.course.toString()),
              _buildProfileRow(
                'Matrícula',
                user.academicRecord.enrollmentNumber.toString(),
              ),
              if (user.pixKey != null)
                _buildProfileRow('Chave PIX', user.pixKey.toString()),

              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  key: const ValueKey('edit_profile_button'),
                  onPressed: _handleEditProfile,
                  icon: const Icon(
                    Icons.edit_rounded,
                    size: 18,
                    color: AppColors.textPrimary,
                  ),
                  label: const Text(
                    'Editar perfil',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                      color: AppColors.textPrimary,
                    ),
                  ),
                  style: OutlinedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    side: const BorderSide(color: AppColors.borderMedium),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                ),
              ),

              const SizedBox(height: 24),
              const Divider(color: AppColors.neutralLight),
              const SizedBox(height: 16),

              // Botão de Sair da Conta com Diálogo de Confirmação
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: _confirmSignOut,
                  icon: const Icon(
                    Icons.logout_rounded,
                    size: 18,
                    color: AppColors.error,
                  ),
                  label: const Text(
                    'Sair da Conta',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                      color: AppColors.error,
                    ),
                  ),
                  style: OutlinedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    side: const BorderSide(color: AppColors.errorBorder),
                    backgroundColor: AppColors.errorBackground,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
        // Motorista: carros cadastrados (sem nenhum, o acesso fica bloqueado)
        if (user.isDriver as bool) ...[
          const SizedBox(height: 16),
          const MyVehiclesSection(),
        ],
      ],
    );
  }

  Future<void> _handleEditProfile() async {
    final session = ref.read(authControllerProvider).value;
    if (session is! Authenticated) return;
    await showEditProfileSheet(context, session.user);
  }

  Widget _buildRoleBadge(String label, bool isEnabled) {
    return Row(
      children: [
        Icon(
          isEnabled ? Icons.check_circle_rounded : Icons.lock_rounded,
          size: 16,
          color: isEnabled ? AppColors.success : AppColors.textMuted,
        ),
        const SizedBox(width: 6),
        Text(
          label,
          style: TextStyle(
            fontSize: 12.5,
            fontWeight: isEnabled ? FontWeight.w700 : FontWeight.w500,
            color: isEnabled ? AppColors.textPrimary : AppColors.textMuted,
          ),
        ),
      ],
    );
  }

  Widget _buildProfileRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 7),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: const TextStyle(
              fontSize: 13,
              color: AppColors.textSecondary,
              fontWeight: FontWeight.w500,
            ),
          ),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.end,
              style: const TextStyle(
                fontSize: 13.5,
                color: AppColors.textPrimary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
