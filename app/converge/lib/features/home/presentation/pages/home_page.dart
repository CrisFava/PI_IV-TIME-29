import 'package:converge/features/home/presentation/widgets/home_driver_form_sheet.dart';
import 'package:converge/features/home/presentation/widgets/home_mode_selector_header.dart';
import 'package:converge/features/home/presentation/widgets/home_passenger_sheet.dart';
import 'package:converge/features/home/presentation/widgets/home_real_map_background.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

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
import '../../../ride/data/models/driver_shift_payload.dart';
import '../../../ride/data/models/mock_driver_model.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../../auth/state/auth_controller.dart';
import '../../../auth/state/session/auth_session.dart';

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

  // Estado do fluxo Uber-like do passageiro (etapas graduais)
  PassengerRideStep _passengerStep = PassengerRideStep.idle;
  LatLng _pickupLocation = mockCampusCoordinates;
  String _pickupAddress = 'PUC-Campinas - Portaria Principal';
  LatLng? _destinationLocation;
  String _destinationAddress = '';

  // Lista dinâmica de motoristas e seleção
  List<MockNearbyDriver> _availableDrivers = List.from(mockNearbyDriversList);
  MockNearbyDriver? _selectedDriver;

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
    _showFeedback('Embarque: $title. Agora escolha o ponto de desembarque.');
  }

  void _handleConfirmPickup() {
    setState(() {
      _passengerStep = PassengerRideStep.searchDestination;
    });
    _showFeedback('Embarque definido! Agora escolha o ponto de desembarque.');
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
    _showFeedback('Calculando trajeto pelas ruas da cidade...');
    List<LatLng>? routePoints;
    if (_destinationLocation != null) {
      try {
        routePoints = await GoogleMapsService.computeRoutes(
          origin: _pickupLocation,
          destination: _destinationLocation!,
        );
      } on RoutesException catch (e) {
        _showFeedback(e.message);
        return;
      } catch (e) {
        _showFeedback('Não foi possível calcular o trajeto viário: ');
        return;
      }
    }

    if (mounted) {
      setState(() {
        _tripRoutePoints = routePoints;
        _passengerStep = PassengerRideStep.routeConfirmation;
      });
    }
  }

  void _handleConfirmRideOrder() {
    setState(() {
      _passengerStep = PassengerRideStep.driverList;
    });
    _showFeedback('Selecione um motorista para solicitar a carona.');
  }

  void _handleSelectDriver(MockNearbyDriver driver) {
    setState(() {
      _selectedDriver = driver;
      _passengerStep = PassengerRideStep.waitingDriver;
    });
  }

  Future<void> _handleSimulateDriverResponse(bool accepted) async {
    final driver = _selectedDriver;
    if (driver == null) return;

    if (accepted) {
      // Motorista aceitou!
      _showFeedback(
        'O motorista ${driver.name} aceitou a corrida! Ele está a caminho.',
      );
      List<LatLng>? approachRoute;
      try {
        approachRoute = await GoogleMapsService.computeRoutes(
          origin: driver.position,
          destination: _pickupLocation,
        );
      } catch (e) {
        debugPrint('Erro ao obter trajeto de aproximação do motorista: $e');
      }

      if (mounted) {
        setState(() {
          _driverApproachRoutePoints = approachRoute;
          _passengerStep = PassengerRideStep.driverAccepted;
        });
      }
    } else {
      // Motorista recusou! Volta pra lista SEM esse motorista
      HapticFeedback.mediumImpact();
      setState(() {
        _availableDrivers.removeWhere((d) => d.id == driver.id);
        _selectedDriver = null;
        _passengerStep = PassengerRideStep.driverList;
      });
      _showFeedback(
        'O motorista ${driver.name} não pôde aceitar. Selecione outro motorista.',
      );
    }
  }

  void _handleSendMessageToDriver() {
    HapticFeedback.lightImpact();
    final driver = _selectedDriver;
    _showFeedback('Iniciando conversa com ${driver?.name ?? "o motorista"}...');
    _onTabSelected(2); // Redireciona para a aba de Chat
  }

  void _handleCancelFlow() {
    setState(() {
      if (_passengerStep == PassengerRideStep.selectPickup) {
        _passengerStep = PassengerRideStep.chooseRouteOption;
      } else {
        _passengerStep = PassengerRideStep.idle;
        _destinationLocation = null;
        _destinationAddress = '';
        _selectedDriver = null;
        _tripRoutePoints = null;
        _driverApproachRoutePoints = null;
        _availableDrivers = List.from(mockNearbyDriversList);
      }
    });
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
        _tripRoutePoints =
            GoogleMapsService.decodePolyline(route.encodedPolyline!);
      } else {
        _tripRoutePoints = [];
      }
      _passengerStep = PassengerRideStep.driverList;
    });

    _showFeedback('Trajeto selecionado! Buscando motoristas disponíveis...');

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
    setState(() => _isCarMenuOpen = !_isCarMenuOpen);
  }

  void _showLockedDriverWarning() {
    HapticFeedback.mediumImpact();
    _showFeedback(
      'Apenas usuários habilitados como motoristas podem iniciar expediente. Redirecionando para o seu perfil...',
    );
    _onTabSelected(3); // Redireciona imediatamente para a aba de Perfil
  }

  void _onRequestRide() {
    setState(() {
      _isCarMenuOpen = false;
      _currentMode = AppRideMode.passenger;
      _currentTabIndex = 0;
      _passengerStep = PassengerRideStep.chooseRouteOption;
    });
    ref.read(savedRoutesProvider.notifier).loadMyRoutes();
    _showFeedback('Escolha como deseja definir seu trajeto.');
  }

  void _onStartDriverShiftFromDock(bool canDrive) {
    setState(() => _isCarMenuOpen = false);
    if (canDrive) {
      setState(() {
        _currentMode = AppRideMode.driver;
        _currentTabIndex = 0;
      });
      _showFeedback(
        'Modo Motorista ativo. Preencha os dados do seu expediente.',
      );
    } else {
      _showLockedDriverWarning();
    }
  }

  Future<void> _handleStartDriverShift(DriverShiftPayload payload) async {
    _showFeedback(
      'Expediente de motorista iniciado com sucesso! Buscando passageiros no raio de ${payload.searchRadiusKm.toStringAsFixed(1)} km até às ${payload.endSearchTime.format(context)}.',
    );

    // ========================================================================
    // TODO: Enviar informações coletadas do motorista para o backend (API Converge):
    // Endpoint previsto: POST /api/v1/rides/work
    // Payload preparado: payload.toJson()
    // Exemplo:
    // await ref.read(caronaRepositoryProvider).iniciarExpediente(payload.toJson());
    // ========================================================================
    debugPrint(
      'Dados do expediente de motorista prontos para o backend: ${payload.toJson()}',
    );
  }

  Future<void> _handleSaveRoute() async {
    HapticFeedback.lightImpact();
    if (_destinationLocation == null) {
      _showFeedback(
        'Selecione uma origem e destino antes de salvar o trajeto.',
      );
      return;
    }

    final isDriver = _currentMode == AppRideMode.driver;
    final request = CreateRouteRequest(
      type: isDriver ? AppRouteType.offer : AppRouteType.search,
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
      totalSeats: isDriver ? 3 : 1,
    );

    final authService = ref.read(authServiceProvider);
    if (authService.currentUser == null) {
      ref.read(savedRoutesProvider.notifier).saveLocalRoute(request);
      await ref.read(authControllerProvider.notifier).signOut();
      return;
    }

    _showFeedback('Salvando trajeto...');

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
        _showFeedback('Trajeto salvo com sucesso no backend!');
      }
    } on ApiException catch (e) {
      ref.read(savedRoutesProvider.notifier).saveLocalRoute(request);
      if (e.isUnauthorized || e.code == ApiErrorCode.invalidCredentials) {
        // Desloga o usuário imediatamente sem emitir alerta de sessão expirada
        await ref.read(authControllerProvider.notifier).signOut();
        return;
      }
      if (mounted) {
        _showFeedback(e.message);
      }
    } catch (e) {
      ref.read(savedRoutesProvider.notifier).saveLocalRoute(request);
      if (mounted) {
        _showFeedback('Trajeto salvo localmente nos seus favoritos.');
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

  void _showFeedback(String message) {
    ScaffoldMessenger.of(context).hideCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Row(
          children: [
            const Icon(
              Icons.check_circle_rounded,
              color: AppColors.textLight,
              size: 18,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                message,
                style: const TextStyle(
                  fontWeight: FontWeight.w600,
                  fontSize: 13.5,
                  color: AppColors.textLight,
                ),
              ),
            ),
          ],
        ),
        backgroundColor: AppColors.primary,
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        margin: const EdgeInsets.only(top: 96, left: 16, right: 16),
        duration: const Duration(seconds: 4),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(authControllerProvider).value;
    final user = session is Authenticated ? session.user : null;
    final bottomPadding = MediaQuery.paddingOf(context).bottom;
    final effectiveBottomOffset = bottomPadding > 0 ? bottomPadding : 16.0;

    // Verificação estrita de permissão de motorista através de participationRoles
    final canDrive = user?.isDriver ?? false;

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
                    onStartDriverShift: () =>
                        _onStartDriverShiftFromDock(canDrive),
                  ),
                ),
              ],
            ),
    );
  }

  // --- VIEW 0: TELA PRINCIPAL COM GOOGLE MAPS REAL ---
  Widget _buildUberHomeView(dynamic user, bool canDrive, double bottomPadding) {
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
            availableDrivers: _availableDrivers,
            acceptedDriver: _passengerStep == PassengerRideStep.driverAccepted
                ? _selectedDriver
                : null,
            pickupLocation: _pickupLocation,
            destinationLocation: _destinationLocation,
            pickupAddress: _pickupAddress,
            destinationAddress: _destinationAddress,
            showTrajectory:
                _currentMode.isPassenger &&
                (_passengerStep == PassengerRideStep.routeConfirmation ||
                    _passengerStep == PassengerRideStep.driverList ||
                    _passengerStep == PassengerRideStep.waitingDriver ||
                    _passengerStep == PassengerRideStep.driverAccepted),
            tripRoutePoints: _tripRoutePoints,
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
        if (_currentMode.isDriver)
          Positioned.fill(
            child: HomeDriverFormSheet(
              initialSearchRadiusKm: _driverSearchRadiusKm,
              bottomSafeArea: bottomPadding,
              onSearchRadiusChanged: (newRadius) {
                setState(() => _driverSearchRadiusKm = newRadius);
              },
              onStartShift: _handleStartDriverShift,
              onSaveRoute: _handleSaveRoute,
            ),
          )
        else if (_passengerStep != PassengerRideStep.idle)
          Positioned.fill(
            child: HomePassengerSheet(
              step: _passengerStep,
              pickupAddress: _pickupAddress,
              destinationAddress: _destinationAddress,
              pickupLocation: _pickupLocation,
              destinationLocation: _destinationLocation,
              availableDrivers: _availableDrivers,
              selectedDriver: _selectedDriver,
              bottomSafeArea: bottomPadding,
              onStartRideFlow: () {
                setState(() => _passengerStep = PassengerRideStep.chooseRouteOption);
                ref.read(savedRoutesProvider.notifier).loadMyRoutes();
              },
              onChooseMapOption: () {
                setState(() => _passengerStep = PassengerRideStep.selectPickup);
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
              onSimulateDriverResponse: _handleSimulateDriverResponse,
              onSendMessageToDriver: _handleSendMessageToDriver,
              onCancelFlow: _handleCancelFlow,
              onSaveRoute: _handleSaveRoute,
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
    1 => 'Buscar e Histórico',
    2 => 'Mensagens',
    3 => 'Meu Perfil Acadêmico',
    _ => 'Início',
  };

  // --- ABA 1: BUSCAR E HISTÓRICO ---
  Widget _buildSearchAndHistoryTab(double bottomPadding) {
    return ListView(
      padding: EdgeInsets.fromLTRB(16, 16, 16, 120 + bottomPadding),
      children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: AppColors.borderSubtle),
          ),
          child: const Row(
            children: [
              Icon(
                Icons.search_rounded,
                color: AppColors.textPrimary,
                size: 22,
              ),
              SizedBox(width: 12),
              Expanded(
                child: Text(
                  'Buscar destinos ou motoristas...',
                  style: TextStyle(
                    color: AppColors.textMuted,
                    fontSize: 14,
                    fontWeight: FontWeight.w400,
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 24),
        const Text(
          'Motoristas Ativos no Campus',
          style: TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w800,
            color: AppColors.textPrimary,
            letterSpacing: -0.2,
          ),
        ),
        const SizedBox(height: 12),
        ...mockNearbyDriversList.map((driver) {
          return Container(
            margin: const EdgeInsets.only(bottom: 10),
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: AppColors.cardBackground,
              borderRadius: BorderRadius.circular(18),
              border: Border.all(color: AppColors.borderSubtle),
            ),
            child: Row(
              children: [
                const CircleAvatar(
                  backgroundColor: AppColors.neutralLight,
                  child: Icon(
                    Icons.person_rounded,
                    color: AppColors.textPrimary,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        driver.name,
                        style: const TextStyle(
                          fontWeight: FontWeight.w700,
                          fontSize: 14,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      Text(
                        '${driver.vehicleModel} • ${driver.currentRoute}',
                        style: const TextStyle(
                          fontSize: 12,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
                Text(
                  'R\$ ${driver.contributionAmount.toStringAsFixed(2)}',
                  style: const TextStyle(
                    fontWeight: FontWeight.w700,
                    fontSize: 14,
                    color: AppColors.textPrimary,
                  ),
                ),
              ],
            ),
          );
        }),
      ],
    );
  }

  // --- ABA 2: CHAT ---
  Widget _buildChatTab(double bottomPadding) {
    return ListView(
      padding: EdgeInsets.fromLTRB(16, 16, 16, 120 + bottomPadding),
      children: [
        const Text(
          'Mensagens das Caronas',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w700,
            color: AppColors.textPrimary,
          ),
        ),
        const SizedBox(height: 12),
        _buildChatThread(
          nome: 'Lucas Silveira (Motorista)',
          ultimaMensagem: 'Chegando no ponto de encontro em 3 minutos!',
          horario: '07:42',
          unread: true,
        ),
      ],
    );
  }

  Widget _buildChatThread({
    required String nome,
    required String ultimaMensagem,
    required String horario,
    required bool unread,
  }) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Row(
        children: [
          const CircleAvatar(
            radius: 20,
            backgroundColor: AppColors.neutralLight,
            child: Icon(
              Icons.forum_rounded,
              color: AppColors.textPrimary,
              size: 20,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      nome,
                      style: TextStyle(
                        fontWeight: unread ? FontWeight.w800 : FontWeight.w600,
                        fontSize: 14,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    Text(
                      horario,
                      style: const TextStyle(
                        fontSize: 11,
                        color: AppColors.textMuted,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  ultimaMensagem,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: 12.5,
                    color: unread
                        ? AppColors.textPrimary
                        : AppColors.textSecondary,
                    fontWeight: unread ? FontWeight.w600 : FontWeight.w400,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
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
                    _buildRoleBadge('Motorista', user.isDriver as bool),
                  ],
                ),
              ),

              const SizedBox(height: 18),
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
      ],
    );
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
