// Autor: Allan Giovanni Matias Paes - RA 25008211

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

import '../../../route/data/models/create_route_request.dart';
import '../../../route/data/models/route_place_model.dart';
import '../../../route/data/models/route_schedule.dart';
import '../../../route/state/offer_route_controller.dart';
import '../../../route/state/trip_estimate_controller.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/services/maps/google_maps_service.dart';
import '../../../../core/widgets/ride_dock_navigation.dart';
import '../../../../core/widgets/sheet_drag_to_dismiss.dart';
import '../../../../core/widgets/show_app_notice.dart';
import '../../../matching/data/models/match_model.dart';
import '../../../matching/presentation/widgets/ride_requests_sheet.dart';
import '../../../matching/state/driver_offers_provider.dart';
import '../../../matching/state/driver_ride_actions_controller.dart';
import '../../../matching/state/driver_rides_provider.dart';
import '../../../ride/data/models/participant_status.dart';
import '../../../ride/data/models/payment_status.dart';
import '../../../ride/data/models/ride_model.dart';
import '../../../ride/data/models/ride_status.dart';
import '../../../ride/presentation/widgets/pending_payment_prompt.dart';
import '../../../ride/state/pending_payments_provider.dart';
import '../../../ride/state/ride_providers.dart';
import '../../../review/presentation/widgets/ride_review_prompt.dart';
import '../../../review/state/pending_reviews.dart';
import '../../../review/state/ride_completion_watcher.dart';
import '../../../matching/presentation/widgets/driver_suggestion_alert.dart';
import '../../../matching/presentation/widgets/incoming_ride_request_alert.dart';
import '../../../matching/state/matching_viewer_provider.dart';
import '../../../matching/state/passenger_match_controller.dart';
import '../../../matching/state/passenger_match_phase.dart';
import '../../../matching/state/passenger_match_state.dart';
import '../../../matching/state/passenger_rides_provider.dart';
import '../../../ride/presentation/widgets/ride_history_list.dart';
import '../../../vehicle/data/repository/vehicle_repository_impl.dart';
import '../../../vehicle/presentation/widgets/add_vehicle_sheet.dart';
import '../../../vehicle/presentation/widgets/my_vehicles_section.dart';
import '../../../indicator/presentation/widgets/my_impact_card.dart';
import '../../../vehicle/state/driver_access.dart';
import '../../../vehicle/state/driver_access_provider.dart';
import '../../../vehicle/state/my_vehicles_controller.dart';
import '../../../auth/presentation/widgets/edit_profile_sheet.dart';
import '../widgets/campus_spots.dart';
import '../widgets/passenger_ride_step.dart';
import '../widgets/departure_time_sheet.dart';
import '../widgets/ride_departure_label.dart';
import '../widgets/ride_flow/ride_map_pick_card.dart';
import '../widgets/ride_flow/ride_place_step_sheet.dart';
import '../widgets/ride_flow/ride_review.dart';
import '../widgets/ride_flow/ride_schedule_step_sheet.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../../auth/state/auth_controller.dart';
import '../../../auth/state/session/auth_session.dart';
import '../../../chat/presentation/widgets/conversations_tab.dart';
import '../../../chat/state/chat_unread_provider.dart';

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

  // No modo motorista: painel de caronas e pedidos no lugar do formulário de oferta
  bool _showRideRequests = false;

  /// Viagens para as quais a tela de avaliação já abriu, para não abrir duas vezes.
  final Set<String> _reviewPrompted = {};

  // Viagens concluídas com a cobrança do passageiro ainda em aberto: a avaliação espera o pagamento
  final Set<String> _reviewAfterPayment = {};

  /// Última lista de viagens recebida, para notar quando uma delas é concluída.
  List<RideModel>? _lastRides;

  // Rota da viagem em andamento já decodificada (evita decodificar a cada build)
  String? _itineraryPolylineCache;
  List<LatLng>? _itineraryPointsCache;

  // Saída escolhida para a carona que o motorista vai publicar; null é "agora"
  DateTime? _offerDeparture;

  /// Carona oferecida se repete toda semana (por padrão, é de um dia só).
  bool _offerRepeatWeekly = false;

  // Publicando a carona (inclui a busca do carro antes do envio)
  bool _publishingOffer = false;

  // Pedido de carona em andamento com o painel escondido: a busca continua e o
  // botão do carro fica azul; o menu do carro reabre os detalhes
  bool _passengerSheetHidden = false;

  // Saída escolhida para o pedido de carona; null é "agora"
  DateTime? _rideDeparture;

  // Muda sempre que o rascunho do pedido de carona é fechado: um cálculo de rota
  // que termine depois disso não reabre o painel
  int _draftGeneration = 0;

  // Etapas de criar (motorista) ou pedir (passageiro) carona: partida, destino e
  // horário são as mesmas nos dois; depois, o passageiro segue para a busca
  PassengerRideStep _passengerStep = PassengerRideStep.idle;
  LatLng _pickupLocation = campusCoordinates;
  String _pickupAddress = '';
  LatLng? _destinationLocation;
  String _destinationAddress = '';

  // Rotas de carro pelas ruas reais
  List<LatLng>? _tripRoutePoints;
  List<LatLng>? _driverApproachRoutePoints;

  // Estado do raio de busca do motorista em KM (refletido no mapa)
  double _driverSearchRadiusKm = 3.0;

  void _onCameraCenterChanged(LatLng target) async {
    // Só na confirmação no mapa o ponto segue o centro. Se o pino continua no
    // ponto escolhido, o nome dele (ex.: "PUC-Campinas - Portaria 2") é mantido.
    if (_passengerStep == PassengerRideStep.dragMapPickup) {
      if (_samePoint(target, _pickupLocation) && _pickupAddress.isNotEmpty) {
        return;
      }
      // Sem endereço enquanto busca: o "Confirmar" fica desabilitado
      setState(() {
        _pickupLocation = target;
        _pickupAddress = '';
      });
      final addr = await GoogleMapsService.getAddressFromCoordinates(target);
      // Só vale se o pino ainda está nesse ponto (o mapa pode ter sido arrastado de novo)
      if (mounted &&
          _passengerStep == PassengerRideStep.dragMapPickup &&
          _samePoint(target, _pickupLocation)) {
        setState(() => _pickupAddress = addr);
      }
    } else if (_passengerStep == PassengerRideStep.dragMapDestination) {
      final destination = _destinationLocation;
      if (destination != null &&
          _samePoint(target, destination) &&
          _destinationAddress.isNotEmpty) {
        return;
      }
      setState(() {
        _destinationLocation = target;
        _destinationAddress = '';
      });
      final addr = await GoogleMapsService.getAddressFromCoordinates(target);
      final current = _destinationLocation;
      if (mounted &&
          _passengerStep == PassengerRideStep.dragMapDestination &&
          current != null &&
          _samePoint(target, current)) {
        setState(() => _destinationAddress = addr);
      }
    }
  }

  // Mesmo ponto no mapa (a câmera parou onde o ponto já estava)
  static bool _samePoint(LatLng a, LatLng b) =>
      (a.latitude - b.latitude).abs() < 0.00002 &&
      (a.longitude - b.longitude).abs() < 0.00002;

  void _handleStartDragMapPickup() {
    setState(() {
      _passengerStep = PassengerRideStep.dragMapPickup;
    });
    _onCameraCenterChanged(_pickupLocation);
  }

  /// Endereço escolhido na busca ou no campus: o mapa vai até ele e pergunta
  /// "Saindo de X, confirma?" antes de seguir.
  void _handlePickupChosen(String title, LatLng pos) {
    setState(() {
      _pickupAddress = title;
      _pickupLocation = pos;
      _passengerStep = PassengerRideStep.dragMapPickup;
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

  void _handleDestinationChosen(String title, LatLng pos) {
    setState(() {
      _destinationAddress = title;
      _destinationLocation = pos;
      _passengerStep = PassengerRideStep.dragMapDestination;
    });
  }

  void _handleConfirmDestination() => _goToSchedule();

  /// Etapa 3: mostra o horário na hora; o tempo e o desenho do caminho chegam
  /// do controlador (backend com a Routes API do Google).
  void _goToSchedule() {
    final destination = _destinationLocation;
    if (destination == null) return;

    setState(() => _passengerStep = PassengerRideStep.routeConfirmation);
    ref
        .read(tripEstimateProvider.notifier)
        .load(origin: _pickupLocation, destination: destination);
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
      // Sai agora, ou no dia e horário que o passageiro escolheu
      _buildRouteRequest(
        isDriver: false,
        schedule: RouteSchedule.startingAt(_rideDeparture ?? DateTime.now()),
      ),
    );
    if (!mounted) return;

    if (routeId == null) {
      setState(() => _passengerStep = PassengerRideStep.routeConfirmation);
      return;
    }

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
    // Na carona do motorista, fechar limpa o mapa
    if (_currentMode.isDriver) {
      _handleCloseDriverFormSheet();
      return;
    }

    final matchController = ref.read(passengerMatchProvider.notifier);
    if (_passengerStep == PassengerRideStep.waitingDriver) {
      await matchController.cancelRequest();
      if (!mounted) return;
    }

    setState(() {
      _draftGeneration++;
      _passengerSheetHidden = false;
      _passengerStep = PassengerRideStep.idle;
      _rideDeparture = null;
      _resetDraftPlaces();
      _driverApproachRoutePoints = null;
    });

    if (_passengerStep == PassengerRideStep.idle) matchController.reset();
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
          _currentTabIndex = 0;
        });
        _openOfferForm();
      },
    );
  }

  void _onRequestRide() {
    setState(() {
      _isCarMenuOpen = false;
      _currentMode = AppRideMode.passenger;
      _currentTabIndex = 0;
      _passengerStep = PassengerRideStep.selectPickup;
      _passengerSheetHidden = false;
      _resetDraftPlaces();
    });
  }

  /// Limpa a partida, o destino e o caminho do rascunho de carona.
  void _resetDraftPlaces() {
    _draftGeneration++;
    _pickupAddress = '';
    _pickupLocation = campusCoordinates;
    _destinationLocation = null;
    _destinationAddress = '';
    _tripRoutePoints = null;
    ref.read(tripEstimateProvider.notifier).reset();
  }

  void _onStartDriverShiftFromDock(bool canDrive) {
    setState(() => _isCarMenuOpen = false);
    final latestSession = ref.read(authControllerProvider).value;
    final isDriverNow =
        canDrive ||
        (latestSession is Authenticated && latestSession.user.isDriver);

    if (isDriverNow) {
      // "Minhas caronas": o painel com as caronas oferecidas e os pedidos
      _openRideRequests();
    } else {
      // Quem só pede carona também abre o painel, com as viagens em que vai
      _openRideRequests(asPassenger: true);
    }
  }

  /// Fechar a nova carona deixa o mapa limpo (sem voltar para "Minhas caronas").
  void _handleCloseDriverFormSheet() {
    setState(() {
      _currentMode = AppRideMode.passenger;
      _showRideRequests = false;
      _passengerStep = PassengerRideStep.idle;
      _offerDeparture = null;
      _offerRepeatWeekly = false;
      _resetDraftPlaces();
    });
  }

  /// Pedido de carona em andamento: procurando motorista, aguardando a resposta ou aceito.
  bool get _hasActiveRideRequest =>
      _passengerStep == PassengerRideStep.driverList ||
      _passengerStep == PassengerRideStep.waitingDriver ||
      _passengerStep == PassengerRideStep.driverAccepted;

  /// Fecha os painéis do mapa. Um pedido de carona em andamento só é escondido
  /// (a busca continua e volta pelo botão do carro). A viagem em andamento também continua.
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
        _rideDeparture = null;
      }
      _resetDraftPlaces();
    });
    if (draftOpen) ref.read(passengerMatchProvider.notifier).reset();
  }

  /// Escolhe outro dia e horário de saída (até 7 dias à frente), para o pedido de
  /// carona ou para a carona do motorista. O matching junta o motorista que sai de 30 min
  /// antes a 5 min depois do passageiro. [onPicked] recebe null quando a saída volta a ser "agora".
  Future<void> _pickDeparture({
    required DateTime? current,
    required void Function(DateTime? departure) onPicked,
  }) async {
    final now = DateTime.now();
    final initial = current ?? now;
    final today = DateTime(now.year, now.month, now.day);

    final date = await showDatePicker(
      context: context,
      initialDate: initial.isBefore(today) ? today : initial,
      firstDate: today,
      lastDate: today.add(const Duration(days: 6)),
      helpText: 'Dia da carona',
      builder: _pickerTheme,
    );
    if (date == null || !mounted) return;

    // Horário: atalhos (Agora, Em 30 min, Em 1 h) e a roda de 5 em 5 min
    final picked = await showDepartureTimeSheet(
      context,
      day: date,
      current: current,
    );
    if (picked == null || !mounted) return;

    final chosen = picked.departure;
    if (chosen == null) {
      HapticFeedback.selectionClick();
      setState(() => onPicked(null));
      return;
    }
    // Um horário que já passou vira "agora"
    if (!chosen.isAfter(DateTime.now())) {
      setState(() => onPicked(null));
      _showError('Esse horário já passou. A carona fica para agora.');
      return;
    }
    HapticFeedback.selectionClick();
    setState(() => onPicked(chosen));
  }

  Widget _pickerTheme(BuildContext context, Widget? child) {
    return Theme(
      data: ThemeData.light().copyWith(
        colorScheme: const ColorScheme.light(
          primary: AppColors.primary,
          onPrimary: AppColors.dockItemActive,
          surface: AppColors.cardBackground,
          onSurface: AppColors.textPrimary,
        ),
      ),
      child: child!,
    );
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

  /// Publica a carona agendada: um trajeto de oferta no dia e horário escolhidos
  /// ("agora" por padrão), com o raio de busca como desvio máximo. Os passageiros já
  /// podem pedir vaga; a viagem só começa no "Iniciar trajeto" da carona.
  Future<void> _handlePublishOffer() async {
    if (_publishingOffer || ref.read(offerRouteProvider)) return;
    if (_pickupAddress.isEmpty) {
      _showError('Escolha de onde a carona sai.');
      return;
    }
    if (_destinationLocation == null) {
      _showError('Escolha o destino antes de publicar a carona.');
      return;
    }

    HapticFeedback.mediumImpact();
    // O rascunho revisado: se ele for fechado ou trocado durante a busca do
    // carro, nada é publicado
    final generation = _draftGeneration;
    setState(() => _publishingOffer = true);
    final vehicle = await _findDriverVehicle();
    if (!mounted) return;
    if (generation != _draftGeneration) {
      setState(() => _publishingOffer = false);
      return;
    }
    if (vehicle == null) {
      setState(() => _publishingOffer = false);
      _showError('Cadastre um veículo antes de publicar a carona.');
      return;
    }

    final request = _buildRouteRequest(
      isDriver: true,
      vehicleId: vehicle.id,
      totalSeats: vehicle.seats,
      schedule: RouteSchedule.startingAt(
        _offerDeparture ?? DateTime.now().add(RouteSchedule.immediateOfferLead),
        repeatWeekly: _offerRepeatWeekly,
      ),
      maxDetourKm: _driverSearchRadiusKm,
    );
    try {
      await ref.read(offerRouteProvider.notifier).publish(request);
    } catch (e) {
      if (mounted) {
        setState(() => _publishingOffer = false);
        _showError(authErrorMessage(e));
      }
      return;
    }
    if (!mounted) return;
    setState(() => _publishingOffer = false);

    ref.invalidate(myOfferRoutesProvider);
    setState(() {
      _passengerStep = PassengerRideStep.idle;
      _offerDeparture = null;
      _offerRepeatWeekly = false;
      _resetDraftPlaces();
    });
    // Abre o painel: a carona aparece com os passageiros compatíveis
    _openRideRequests();
  }

  /// "Encerrar": conclui a viagem em andamento.
  Future<void> _handleEndDriverShift() async {
    final ride = ref.read(driverRideInProgressProvider);
    if (ride == null) return;

    final error = await ref
        .read(driverRideActionsProvider.notifier)
        .complete(ride.id);
    if (!mounted) return;
    if (error != null) {
      _showError(error);
      return;
    }
    setState(() {
      _showRideRequests = false;
      _currentMode = AppRideMode.passenger;
    });
  }

  /// "Oferecer carona" no painel: abre a folha para publicar uma carona nova.
  void _openOfferForm() {
    // Os dois fluxos usam as mesmas etapas: abrir a oferta apagaria o pedido
    if (_hasActiveRideRequest) {
      _showError(
        'Você tem um pedido de carona em andamento. Cancele o pedido antes de oferecer uma carona.',
      );
      return;
    }
    setState(() {
      _currentMode = AppRideMode.driver;
      _showRideRequests = false;
      _passengerStep = PassengerRideStep.selectPickup;
      _resetDraftPlaces();
    });
  }

  /// A viagem começou ("Iniciar trajeto"): fecha o painel e mostra a rota com as paradas.
  void _handleRideStarted() {
    HapticFeedback.mediumImpact();
    setState(() {
      _isCarMenuOpen = false;
      _showRideRequests = false;
      _currentMode = AppRideMode.passenger;
      _currentTabIndex = 0;
    });
  }

  /// Fechar o painel de caronas e pedidos volta ao mapa.
  void _handleCloseRideRequests() {
    setState(() {
      _showRideRequests = false;
      _currentMode = AppRideMode.passenger;
    });
  }

  /// Cadastra um carro para liberar (ou manter) o acesso de motorista.
  Future<void> _handleAddVehicle() async {
    setState(() => _isCarMenuOpen = false);
    await showAddVehicleSheet(context);
  }

  /// Sem carro, o motorista perde o acesso: fecha os painéis de motorista.
  void _onDriverAccessChanged(DriverAccess? previous, DriverAccess next) {
    if (!next.isBlockedByVehicle || previous == next) return;
    setState(() {
      _showRideRequests = false;
      if (_currentMode.isDriver) _currentMode = AppRideMode.passenger;
    });
  }

  /// Abre a avaliação quando uma viagem é concluída, por qualquer botão ("Encerrar",
  /// "Concluir viagem" no chat ou no cartão da carona) ou pelo motorista. Quando o app
  /// abre, lembra das viagens concluídas nas últimas 24 horas que ainda têm avaliação pendente.
  void _onRidesChanged(
    AsyncValue<List<RideModel>>? previous,
    AsyncValue<List<RideModel>> next,
  ) {
    final rides = next.value;
    if (rides == null) return;

    final before = _lastRides;
    _lastRides = rides;

    final toReview = before == null
        ? recentlyCompletedRides(rides: rides, now: DateTime.now())
        : newlyCompletedRides(previous: before, next: rides);

    // Primeiro o pagamento, depois a avaliação: com a cobrança em aberto, ela fica para depois
    for (final ride in toReview) {
      if (_awaitsPayment(ride)) _reviewAfterPayment.add(ride.id);
    }
    final ready = [
      for (final ride in toReview)
        if (!_reviewAfterPayment.contains(ride.id)) ride,
      for (final ride in rides)
        if (_reviewAfterPayment.contains(ride.id) && !_awaitsPayment(ride))
          ride,
    ];
    _reviewAfterPayment.removeAll(ready.map((ride) => ride.id));
    _promptReviews([
      for (final ride in ready)
        if (_reviewPrompted.add(ride.id)) ride,
    ]);

    _finishAcceptedRideIfDone();
  }

  /// O usuário é passageiro desta viagem, ela terminou para ele e a cobrança ainda está em aberto.
  bool _awaitsPayment(RideModel ride) {
    final viewerId = ref.read(matchingViewerIdProvider);
    final me = viewerId == null ? null : ride.passengerOf(viewerId);
    return me != null &&
        me.status == ParticipantStatus.completed &&
        me.paymentStatus == PaymentStatus.pending &&
        (me.finalAmount ?? 0) > 0;
  }

  /// O pedido aceito acabou: a viagem do passageiro terminou (ou foi cancelada) e não há
  /// cobrança em aberto. O pedido sai da tela e ele volta a poder pedir e oferecer carona.
  /// Com a cobrança em aberto, o card da carona fica, agora com o "Pagar".
  void _finishAcceptedRideIfDone() {
    if (_passengerStep != PassengerRideStep.driverAccepted) return;

    final rideId = ref.read(passengerMatchProvider).rideId;
    final rides = ref.read(myRidesProvider).value;
    final payments = ref.read(pendingPaymentsProvider);
    final pending = payments.value;
    if (payments.isLoading || payments.hasError) return;
    if (rideId == null || rides == null || pending == null) return;
    if (payments.isLoading) return;

    RideModel? ride;
    for (final candidate in rides) {
      if (candidate.id == rideId) ride = candidate;
    }
    final viewerId = ref.read(matchingViewerIdProvider);
    // Lista ainda sem a viagem (recém-aceita): não dá para concluir nada
    if (ride == null || viewerId == null) return;

    final me = ride.passengerOf(viewerId);
    if (me == null) return;
    final finished =
        me.status == ParticipantStatus.completed ||
        me.status == ParticipantStatus.cancelled ||
        me.status == ParticipantStatus.rejected ||
        me.status == ParticipantStatus.expired;
    if (!finished || me.paymentStatus == PaymentStatus.pending) return;
    final hasOpenCharge = pending.any(
      (payment) => payment.rideId == rideId && payment.isOpen,
    );
    if (hasOpenCharge) return;

    setState(() {
      _draftGeneration++;
      _passengerSheetHidden = false;
      _passengerStep = PassengerRideStep.idle;
      _rideDeparture = null;
      _resetDraftPlaces();
      _driverApproachRoutePoints = null;
    });
    ref.read(passengerMatchProvider.notifier).reset();
  }

  /// Abre a tela de avaliação de uma viagem por vez: a próxima só depois que a anterior fecha.
  Future<void> _promptReviews(List<RideModel> rides) async {
    for (final ride in rides) {
      if (!mounted) return;
      await promptRideReviews(context, ref, ride);
    }
  }

  static LatLng _latLngOf(RoutePlaceModel place) =>
      LatLng(place.latitude, place.longitude);

  /// Rota da viagem em andamento, decodificada uma vez por polyline.
  List<LatLng>? _itineraryPoints(RideModel ride) {
    final polyline = ride.itineraryPolyline;
    if (polyline == null || polyline.isEmpty) return null;
    if (_itineraryPolylineCache != polyline) {
      _itineraryPolylineCache = polyline;
      _itineraryPointsCache = GoogleMapsService.decodePolyline(polyline);
    }
    return _itineraryPointsCache;
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

  /// Mostra "Minhas caronas" sobre o mapa. O motorista precisa de um carro
  /// liberado; quem só pede carona ([asPassenger]) vê as viagens em que vai.
  void _openRideRequests({bool asPassenger = false}) {
    if (!asPassenger && _isDriverActionBlocked()) return;
    setState(() {
      _isCarMenuOpen = false;
      _currentMode = AppRideMode.driver;
      _currentTabIndex = 0;
      _showRideRequests = true;
    });
  }

  /// Dados do trajeto marcado no mapa, com o horário e os dias informados.
  CreateRouteRequest _buildRouteRequest({
    required bool isDriver,
    required RouteSchedule schedule,
    String? vehicleId,
    int? totalSeats,
    double maxDetourKm = 3.0,
  }) {
    // A partida escolhida na etapa 1 (carona do motorista ou embarque do passageiro)
    final from = _pickupLocation;
    final fromAddress = _pickupAddress;
    return CreateRouteRequest(
      type: isDriver ? AppRouteType.offer : AppRouteType.search,
      vehicleId: vehicleId,
      origin: RoutePlaceModel(
        address: fromAddress,
        neighborhood: 'Campus / Origem',
        city: 'Campinas',
        latitude: from.latitude,
        longitude: from.longitude,
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
      departureTime: schedule.departureTime,
      estimatedArrivalTime: schedule.estimatedArrivalTime,
      weekDays: schedule.weekDays,
      recurrence: schedule.recurrence,
      date: schedule.date,
      maxDetourKm: maxDetourKm,
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
    ref.listen(myRidesProvider, _onRidesChanged);
    // Pagou: o pedido aceito termina e a avaliação que esperava o pagamento pode abrir
    ref.listen(pendingPaymentsProvider, (_, _) => _finishAcceptedRideIfDone());
    // Enquanto há uma viagem a caminho, mantém a lista em dia para a avaliação abrir em qualquer tela
    ref.watch(rideCompletionWatcherProvider);
    final driverAccess = ref.watch(driverAccessProvider);
    // A viagem que o motorista está fazendo agora (botão verde e rota com paradas)
    final shiftRide = ref.watch(driverRideInProgressProvider);

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
                // Abre o pagamento sozinho quando há cobrança de carona em aberto.
                // Posicionado: um filho sem posição definiria o tamanho do Stack (0x0,
                // porque o ouvinte não desenha nada) e a Home inteira sumiria.
                const Positioned(
                  left: 0,
                  top: 0,
                  child: PendingPaymentPrompt(),
                ),

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
                    // Com a viagem em andamento, o menu abre as paradas; fora dela, a oferta de carona
                    onStartDriverShift: () => shiftRide != null
                        ? _openRideRequests()
                        : _onStartDriverShiftFromDock(canDrive),
                    isOnShift: shiftRide != null,
                    onEndDriverShift: () => _handleEndDriverShift(),
                    needsVehicle: driverAccess.isBlockedByVehicle,
                    onAddVehicle: _handleAddVehicle,
                    // Pedido aceito também aparece: o menu reabre os detalhes da
                    // carona em vez de começar um pedido novo
                    isSearchingRide: _hasActiveRideRequest,
                    chatUnreadCount: ref.watch(chatUnreadCountProvider),
                    // Anel verde na viagem em andamento, como motorista ou passageiro
                    isOnTrip:
                        shiftRide != null ||
                        (ref
                                .watch(myPassengerRidesProvider)
                                .value
                                ?.any(
                                  (ride) =>
                                      ride.status == RideStatus.inProgress,
                                ) ??
                            false),
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

                // ============================================================
                // CAMADA 4: AVISOS DE CARONA NO TOPO
                // ============================================================
                Positioned(
                  key: const ValueKey('home_ride_alerts_layer'),
                  top: MediaQuery.paddingOf(context).top + 8,
                  left: 12,
                  right: 12,
                  child: _buildRideAlert(driverAccess.canDrive),
                ),
              ],
            ),
    );
  }

  /// Avisos de carona no topo: o motorista vê os pedidos de vaga das suas caronas
  /// (eles só chegam antes de a viagem começar); o passageiro procurando carona vê
  /// o motorista mais compatível.
  Widget _buildRideAlert(bool canDrive) {
    final matchState = ref.watch(passengerMatchProvider);
    final suggestion = _passengerStep == PassengerRideStep.driverList
        ? matchState.suggestion
        : null;

    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // O pedido de vaga só vira aviso com "Minhas caronas" fechada: aberta,
        // ele já aparece na lista para aceitar ou recusar
        if (canDrive && !_showRideRequests) const IncomingRideRequestAlert(),
        if (suggestion != null)
          DriverSuggestionAlert(
            match: suggestion,
            isBusy: matchState.isBusy,
            onConfirm: () => _handleSelectDriver(suggestion),
            onDecline: () => ref
                .read(passengerMatchProvider.notifier)
                .declineSuggestion(suggestion),
          ),
      ],
    );
  }

  // --- VIEW 0: TELA PRINCIPAL COM GOOGLE MAPS REAL ---
  Widget _buildUberHomeView(dynamic user, bool canDrive, double bottomPadding) {
    // O desenho do caminho do rascunho vem do controlador da estimativa
    ref.listen(tripEstimateProvider, (_, next) {
      setState(
        () => _tripRoutePoints = next.points.isEmpty ? null : next.points,
      );
    });
    // Com a viagem em andamento (e sem um pedido de carona aberto), o mapa
    // mostra a rota da viagem com as paradas: buscar e deixar cada passageiro
    final shiftRide = ref.watch(driverRideInProgressProvider);
    final showShiftRoute =
        shiftRide != null &&
        _passengerStep == PassengerRideStep.idle &&
        (_currentMode.isPassenger || _showRideRequests);
    // Os painéis do motorista abrem em 52% da tela: o mapa enquadra só a parte de cima
    // Escolhendo no mapa, o mapa ocupa a tela toda (só o cartão fica embaixo)
    final pickingOnMap =
        _passengerStep == PassengerRideStep.dragMapPickup ||
        _passengerStep == PassengerRideStep.dragMapDestination;
    final mapBottomPadding = _currentMode.isDriver && !pickingOnMap
        ? MediaQuery.sizeOf(context).height * 0.52
        : 0.0;

    return Stack(
      key: const ValueKey('uber_map_home_view'),
      children: [
        // 1. Camada Base: Google Maps real em tela cheia com círculo dinâmico / marcadores / rotas
        Positioned.fill(
          child: HomeRealMapBackground(
            isDriverMode: _currentMode.isDriver,
            showDrivers:
                _currentMode.isPassenger &&
                _passengerStep == PassengerRideStep.driverList,
            availableDrivers: ref.watch(passengerMatchProvider).matches,
            acceptedDriver: _passengerStep == PassengerRideStep.driverAccepted
                ? ref.watch(passengerMatchProvider).selected
                : null,
            pickupLocation: showShiftRoute
                ? _latLngOf(shiftRide.origin)
                : _pickupLocation,
            destinationLocation: showShiftRoute
                ? _latLngOf(shiftRide.destination)
                : _destinationLocation,
            pickupAddress: showShiftRoute
                ? shiftRide.origin.address
                : _pickupAddress,
            destinationAddress: showShiftRoute
                ? shiftRide.destination.address
                : _destinationAddress,
            bottomPadding: mapBottomPadding,
            showTrajectory:
                showShiftRoute ||
                _passengerStep == PassengerRideStep.routeConfirmation ||
                (_currentMode.isPassenger &&
                    (_passengerStep == PassengerRideStep.driverList ||
                        _passengerStep == PassengerRideStep.waitingDriver ||
                        _passengerStep == PassengerRideStep.driverAccepted)),
            tripRoutePoints: showShiftRoute
                ? _itineraryPoints(shiftRide)
                : _tripRoutePoints,
            driverApproachRoutePoints: _driverApproachRoutePoints,
            isDraggingPin:
                _passengerStep == PassengerRideStep.dragMapPickup ||
                _passengerStep == PassengerRideStep.dragMapDestination,
            centralPinType:
                _passengerStep == PassengerRideStep.dragMapDestination
                ? CentralPinType.destination
                : CentralPinType.pickup,
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
                onClose: _handleCloseRideRequests,
                onRideStarted: _handleRideStarted,
                // Quem ainda não é motorista vê como liberar o perfil
                onNewOffer: canDrive
                    ? _openOfferForm
                    : _showLockedDriverWarning,
              ),
            ),
          )
        else if (_isDraftStep)
          Positioned.fill(
            child: SheetDragToDismiss(
              onDismiss: _handleCancelFlow,
              child: _buildRideFlowStep(bottomPadding),
            ),
          )
        else if (_currentMode.isPassenger &&
            _passengerStep != PassengerRideStep.idle &&
            !_passengerSheetHidden)
          Positioned.fill(
            child: SheetDragToDismiss(
              onDismiss: _handlePassengerSheetDismissed,
              child: HomePassengerSheet(
                step: _passengerStep,
                bottomSafeArea: bottomPadding,
                onSelectDriver: _handleSelectDriver,
                onRetryMatches: _handleRetryMatches,
                onCancelRequest: _handleCancelRequest,
                onSendMessageToDriver: _handleSendMessageToDriver,
                onCancelFlow: _handleCancelFlow,
              ),
            ),
          ),
      ],
    );
  }

  /// Revisão final com tudo o que foi escolhido; confirmando, publica a carona
  /// (motorista) ou busca os motoristas (passageiro).
  Future<void> _reviewAndSubmit() async {
    final isDriver = _currentMode.isDriver;
    final confirmed = await showRideReview(
      context,
      isDriver: isDriver,
      originAddress: _pickupAddress,
      destinationAddress: _destinationAddress,
      departureLabel: rideDepartureLabel(
        isDriver ? _offerDeparture : _rideDeparture,
        DateTime.now(),
      ),
      estimate: ref.read(tripEstimateProvider).estimate,
      repeatWeekly: _offerRepeatWeekly,
      searchRadiusKm: _driverSearchRadiusKm,
    );
    if (!confirmed || !mounted) return;
    if (isDriver) {
      await _handlePublishOffer();
    } else {
      await _handleConfirmRideOrder();
    }
  }

  /// Partida, destino ou horário: as etapas de criar ou pedir carona.
  bool get _isDraftStep => switch (_passengerStep) {
    PassengerRideStep.selectPickup ||
    PassengerRideStep.dragMapPickup ||
    PassengerRideStep.searchDestination ||
    PassengerRideStep.dragMapDestination ||
    PassengerRideStep.routeConfirmation => true,
    _ => false,
  };

  void _goToStep(PassengerRideStep step) =>
      setState(() => _passengerStep = step);

  /// As 3 etapas, iguais para motorista e passageiro: partida, destino e horário.
  Widget _buildRideFlowStep(double bottomPadding) {
    final isDriver = _currentMode.isDriver;
    final tripEstimate = ref.watch(tripEstimateProvider);
    return switch (_passengerStep) {
      PassengerRideStep.dragMapPickup => RideMapPickCard(
        isOrigin: true,
        address: _pickupAddress,
        onConfirm: _handleConfirmPickup,
        onBack: () => _goToStep(PassengerRideStep.selectPickup),
        bottomSafeArea: bottomPadding,
      ),
      PassengerRideStep.searchDestination => RidePlaceStepSheet(
        key: const ValueKey('ride_flow_destination'),
        isOrigin: false,
        isDriver: isDriver,
        selectedAddress: _destinationAddress,
        onPickOnMap: _handleStartDragMapDestination,
        onPlaceChosen: _handleDestinationChosen,
        onContinue: _handleConfirmDestination,
        onBack: () => _goToStep(PassengerRideStep.selectPickup),
        onClose: _handleCancelFlow,
        bottomSafeArea: bottomPadding,
      ),
      PassengerRideStep.dragMapDestination => RideMapPickCard(
        isOrigin: false,
        address: _destinationAddress,
        onConfirm: _handleConfirmDestination,
        onBack: () => _goToStep(PassengerRideStep.searchDestination),
        bottomSafeArea: bottomPadding,
      ),
      PassengerRideStep.routeConfirmation => RideScheduleStepSheet(
        isDriver: isDriver,
        originAddress: _pickupAddress,
        destinationAddress: _destinationAddress,
        estimate: tripEstimate.estimate,
        isEstimating: tripEstimate.isLoading,
        departureLabel: rideDepartureLabel(
          isDriver ? _offerDeparture : _rideDeparture,
          DateTime.now(),
        ),
        onChangeDeparture: () => isDriver
            ? _pickDeparture(
                current: _offerDeparture,
                onPicked: (departure) => _offerDeparture = departure,
              )
            : _pickDeparture(
                current: _rideDeparture,
                onPicked: (departure) => _rideDeparture = departure,
              ),
        repeatWeekly: _offerRepeatWeekly,
        onRepeatWeeklyChanged: isDriver
            ? (value) => setState(() => _offerRepeatWeekly = value)
            : null,
        searchRadiusKm: _driverSearchRadiusKm,
        onSearchRadiusChanged: isDriver
            ? (value) => setState(() => _driverSearchRadiusKm = value)
            : null,
        isSubmitting:
            isDriver && (_publishingOffer || ref.watch(offerRouteProvider)),
        onSubmit: _reviewAndSubmit,
        onEditOrigin: () => _goToStep(PassengerRideStep.selectPickup),
        onEditDestination: () => _goToStep(PassengerRideStep.searchDestination),
        onBack: () => _goToStep(PassengerRideStep.searchDestination),
        onClose: _handleCancelFlow,
        bottomSafeArea: bottomPadding,
      ),
      _ => RidePlaceStepSheet(
        key: const ValueKey('ride_flow_origin'),
        isOrigin: true,
        isDriver: isDriver,
        selectedAddress: _pickupAddress,
        onPickOnMap: _handleStartDragMapPickup,
        onPlaceChosen: _handlePickupChosen,
        onContinue: _handleConfirmPickup,
        onClose: _handleCancelFlow,
        bottomSafeArea: bottomPadding,
      ),
    };
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

              const SizedBox(height: 10),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  key: const ValueKey('blocked_users_button'),
                  onPressed: () => context.push(AppRoutes.blockedUsers),
                  icon: const Icon(
                    Icons.block_rounded,
                    size: 18,
                    color: AppColors.textPrimary,
                  ),
                  label: const Text(
                    'Usuários bloqueados',
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
        // Meu impacto: abre os indicadores de mobilidade e ambientais
        const SizedBox(height: 16),
        MyImpactCard(onOpen: () => context.push(AppRoutes.indicators)),
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
