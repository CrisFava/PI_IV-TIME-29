// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/confirm_dialog.dart';
import '../../../../core/widgets/ride_dock_navigation.dart';
import '../../../../core/widgets/sheet_drag_to_dismiss.dart';
import '../../../../core/widgets/show_app_notice.dart';
import '../../../auth/data/models/user_model.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../../auth/state/auth_controller.dart';
import '../../../auth/state/session/auth_session.dart';
import '../../../chat/presentation/widgets/conversations_tab.dart';
import '../../../chat/state/chat_unread_provider.dart';
import '../../../matching/data/models/match_model.dart';
import '../../../matching/presentation/widgets/ride_requests_sheet.dart';
import '../../../matching/state/driver_offers_provider.dart';
import '../../../matching/state/driver_ride_actions_controller.dart';
import '../../../matching/state/driver_rides_provider.dart';
import '../../../matching/state/matching_viewer_provider.dart';
import '../../../matching/state/passenger_match_controller.dart';
import '../../../matching/state/passenger_match_phase.dart';
import '../../../matching/state/passenger_match_state.dart';
import '../../../matching/state/passenger_rides_provider.dart';
import '../../../review/presentation/widgets/ride_review_prompt.dart';
import '../../../review/state/pending_reviews.dart';
import '../../../review/state/ride_completion_watcher.dart';
import '../../../ride/data/models/participant_status.dart';
import '../../../ride/data/models/payment_status.dart';
import '../../../ride/data/models/ride_model.dart';
import '../../../ride/data/models/ride_status.dart';
import '../../../ride/presentation/widgets/pending_payment_prompt.dart';
import '../../../ride/presentation/widgets/ride_history_list.dart';
import '../../../ride/state/ride_providers.dart';
import '../../../route/data/models/create_route_request.dart';
import '../../../route/data/models/route_place_model.dart';
import '../../../route/data/models/route_schedule.dart';
import '../../../route/state/offer_route_controller.dart';
import '../../../route/state/trip_estimate_controller.dart';
import '../../../vehicle/data/models/vehicle_model.dart';
import '../../../vehicle/data/repository/vehicle_repository_impl.dart';
import '../../../vehicle/presentation/widgets/add_vehicle_sheet.dart';
import '../../../vehicle/state/driver_access.dart';
import '../../../vehicle/state/driver_access_provider.dart';
import '../../../vehicle/state/my_vehicles_controller.dart';
import '../widgets/become_driver_sheet.dart';
import '../widgets/campus_spots.dart';
import '../widgets/departure_time_sheet.dart';
import '../widgets/home_map_viewport.dart';
import '../widgets/home_mode_selector_header.dart';
import '../widgets/home_passenger_sheet.dart';
import '../widgets/home_profile_tab.dart';
import '../widgets/home_ride_alerts_overlay.dart';
import '../widgets/home_trip_mode_overlay.dart';
import '../widgets/passenger_ride_step.dart';
import '../widgets/ride_departure_label.dart';
import '../widgets/ride_flow/discard_ride_dialog.dart';
import '../widgets/ride_flow/home_ride_flow_coordinator.dart';
import '../widgets/ride_flow/ride_review.dart';

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

  // Modo viagem: com uma viagem em andamento, só ela aparece, com as ações de quem olha
  bool _tripModeOpen = false;

  // Saída escolhida para a carona que o motorista vai publicar; null é "agora"
  DateTime? _offerDeparture;

  /// Carona oferecida se repete toda semana (por padrão, é de um dia só).
  bool _offerRepeatWeekly = false;

  // Publicando a carona (inclui a busca do carro antes do envio)
  bool _publishingOffer = false;

  // Aviso de carona confirmada (o passageiro escolhe se abre "Caronas ativas")
  ({String key, String? driverName})? _acceptedNotice;

  // Carona que o painel abre assim que aparecer (tocou no aviso de pedido de vaga)
  String? _openOfferKey;

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

  // Carro e vagas da carona que o motorista vai publicar (nulos: o primeiro carro, todas as vagas)
  String? _offerVehicleId;
  int? _offerSeats;

  void _handleStartDragMapPickup() {
    setState(() {
      _passengerStep = PassengerRideStep.dragMapPickup;
    });
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
  }

  void _handleDestinationChosen(String title, LatLng pos) {
    setState(() {
      _destinationAddress = title;
      _destinationLocation = pos;
      _passengerStep = PassengerRideStep.dragMapDestination;
    });
  }

  void _handleConfirmDestination() => _goToRoutePreview();

  /// Etapa do trajeto: o mapa em foco com o caminho inteiro; o tempo e o desenho
  /// chegam do controlador (backend com a Routes API do Google).
  void _goToRoutePreview() {
    final destination = _destinationLocation;
    if (destination == null) return;

    setState(() => _passengerStep = PassengerRideStep.routePreview);
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
    setState(() {
      _passengerStep = PassengerRideStep.driverList;
    });

    final controller = ref.read(passengerMatchProvider.notifier);
    // Limpa a busca anterior: a lista mostra o carregamento até os novos motoristas chegarem
    controller.reset();
    final routeId = await controller.prepareSearchRoute(
      // No dia e horário que o passageiro escolheu
      _buildRouteRequest(
        isDriver: false,
        schedule: RouteSchedule.startingAt(_rideDeparture!),
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

  /// "Continuar procurando depois": fecha o painel sem cancelar o pedido. A busca
  /// segue ativa e volta pelo aviso "Procurando motorista" ou pelo botão do carro.
  void _handleKeepSearching() {
    HapticFeedback.lightImpact();
    setState(() => _passengerSheetHidden = true);
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

  /// O motorista aceitou: o pedido termina e a viagem passa a aparecer em
  /// "Caronas ativas". O app não troca de tela: mostra um aviso no topo, e o
  /// toque nele abre "Caronas ativas".
  void _handleDriverAccepted(MatchModel? driver) {
    HapticFeedback.mediumImpact();
    ref.invalidate(myRidesProvider);
    setState(() {
      _passengerStep = PassengerRideStep.idle;
      _passengerSheetHidden = false;
      _rideDeparture = null;
      _driverApproachRoutePoints = null;
      _resetDraftPlaces();
      _acceptedNotice = (
        key: 'accepted_${DateTime.now().microsecondsSinceEpoch}',
        driverName: driver?.otherUserName,
      );
    });
    ref.read(passengerMatchProvider.notifier).reset();
  }

  /// X do pedido de carona ou da carona do motorista: com a partida já confirmada,
  /// pergunta antes de descartar o que foi escolhido. Na primeira etapa ainda não
  /// há nada a perder, então fecha direto.
  Future<void> _handleCloseRideFlow() async {
    if (_passengerStep.hasConfirmedPickup) {
      final discard = await showDiscardRideDialog(
        context,
        isDriver: _currentMode.isDriver,
      );
      if (!discard || !mounted) return;
    }
    await _handleCancelFlow();
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
      // "Caronas ativas": o painel com as caronas oferecidas e os pedidos
      _openRideRequests();
    } else {
      // Quem só pede carona também abre o painel, com as viagens em que vai
      _openRideRequests(asPassenger: true);
    }
  }

  /// Fechar a nova carona deixa o mapa limpo (sem voltar para "Caronas ativas").
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

  /// Escolhe o dia e o horário de saída (até 7 dias à frente), para o pedido de
  /// carona ou para a carona do motorista. Não existe "agora": o horário é sempre
  /// escolhido. O matching junta o motorista que sai de 5 min antes a 30 min depois
  /// do passageiro.
  Future<void> _pickDeparture({
    required DateTime? current,
    required void Function(DateTime departure) onPicked,
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

    // Horário: a roda de hora e minuto
    final chosen = await showDepartureTimeSheet(
      context,
      day: date,
      current: current,
    );
    if (chosen == null || !mounted) return;

    // Um horário que já passou não vale: continua o que estava escolhido
    if (!chosen.isAfter(DateTime.now())) {
      _showError('Esse horário já passou. Escolha um horário depois de agora.');
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
    // O carro escolhido na etapa "Sua carona", conferido na lista atual
    final preferredId = _offerVehicles().selected?.id;
    final vehicle = await _findDriverVehicle(preferredId: preferredId);
    if (!mounted) return;
    if (generation != _draftGeneration) {
      setState(() => _publishingOffer = false);
      return;
    }
    if (vehicle == null && preferredId == null) {
      setState(() => _publishingOffer = false);
      _showError('Cadastre um veículo antes de publicar a carona.');
      return;
    }
    if (vehicle == null) {
      setState(() {
        _publishingOffer = false;
        _offerVehicleId = null;
      });
      ref.invalidate(myVehiclesProvider);
      _goToStep(PassengerRideStep.offerDetails);
      _showError('Esse carro não está mais disponível. Escolha outro.');
      return;
    }

    final maxSeats = vehicle.seats;
    final request = _buildRouteRequest(
      isDriver: true,
      vehicleId: vehicle.id,
      totalSeats: (_offerSeats ?? maxSeats).clamp(1, maxSeats),
      schedule: RouteSchedule.startingAt(
        _offerDeparture!,
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

    final confirmed = await showConfirmDialog(
      context,
      title: 'Concluir a viagem?',
      message: 'Os valores finais serão calculados com quem foi. Marque antes quem não apareceu.',
      confirmLabel: 'Concluir',
    );
    if (!confirmed || !mounted) return;

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
      _tripModeOpen = true;
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

    _noticeTripsStarted(before, rides);

    final viewerId = ref.read(matchingViewerIdProvider);
    final toReview = before == null
        ? recentlyCompletedRides(rides: rides, now: DateTime.now())
        : [
            ...newlyCompletedRides(previous: before, next: rides),
            // O passageiro avalia quando ele desce, sem esperar os outros
            if (viewerId != null)
              ...newlyArrivedRides(
                previous: before,
                next: rides,
                viewerId: viewerId,
              ),
          ];

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

  /// Avisa o passageiro quando o motorista inicia uma viagem em que ele está.
  void _noticeTripsStarted(List<RideModel>? before, List<RideModel> rides) {
    final viewerId = ref.read(matchingViewerIdProvider);
    if (before == null || viewerId == null) return;
    final wasInProgress = {
      for (final ride in before)
        if (ride.status == RideStatus.inProgress) ride.id,
    };
    for (final ride in rides) {
      if (ride.status != RideStatus.inProgress ||
          wasInProgress.contains(ride.id) ||
          ride.isDrivenBy(viewerId) ||
          ride.passengerOf(viewerId) == null) {
        continue;
      }
      HapticFeedback.mediumImpact();
      showAppNotice(
        context,
        'Sua viagem com ${ride.driverName} começou.',
        type: AppNoticeType.success,
      );
      return;
    }
  }

  /// Abre a tela de avaliação de uma viagem por vez: a próxima só depois que a anterior fecha.
  Future<void> _promptReviews(List<RideModel> rides) async {
    for (final ride in rides) {
      if (!mounted) return;
      await promptRideReviews(context, ref, ride);
    }
  }

  /// Botão verde da dock: abre o modo viagem (só a viagem em andamento).
  void _openTripMode() {
    HapticFeedback.mediumImpact();
    setState(() {
      _tripModeOpen = true;
      _isCarMenuOpen = false;
      _showRideRequests = false;
      _currentMode = AppRideMode.passenger;
      _currentTabIndex = 0;
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

  /// Mostra "Caronas ativas" sobre o mapa. O motorista precisa de um carro
  /// liberado; quem só pede carona ([asPassenger]) vê as viagens em que vai.
  void _openRideRequests({bool asPassenger = false, String? offerKey}) {
    if (!asPassenger && _isDriverActionBlocked()) return;
    setState(() {
      _tripModeOpen = false;
      _openOfferKey = offerKey;
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
      // Sem bairro: o app não o conhece, então não inventa um texto fixo. O histórico
      // mostra o começo do endereço no lugar.
      origin: RoutePlaceModel(
        address: fromAddress,
        city: 'Campinas',
        latitude: from.latitude,
        longitude: from.longitude,
      ),
      destination: RoutePlaceModel(
        address: _destinationAddress.isNotEmpty
            ? _destinationAddress
            : 'Destino Selecionado',
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

  /// Veículo ativo do motorista, exigido em todo trajeto de oferta. Com
  /// [preferredId], só serve esse carro (sem trocar por outro).
  Future<({String id, int seats})?> _findDriverVehicle({
    String? preferredId,
  }) async {
    try {
      final vehicles = await ref
          .read(vehicleRepositoryProvider)
          .listMyVehicles();
      for (final vehicle in vehicles) {
        if (vehicle.active &&
            (preferredId == null || vehicle.id == preferredId)) {
          return (id: vehicle.id, seats: vehicle.passengerSeats);
        }
      }
    } catch (e) {
      debugPrint('Erro ao buscar os veículos do motorista: $e');
    }
    return null;
  }

  /// Só erros viram aviso (no topo, com botão de fechar): os informativos
  /// atrapalhavam o fluxo de pedir carona.
  void _showError(String message) {
    if (!mounted) return;
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
                    // Com a viagem em andamento, o botão verde abre o modo viagem
                    onTripTap: _openTripMode,
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
                    // Anel azul só enquanto procura ou espera a resposta do motorista
                    isLookingForDriver:
                        _passengerStep == PassengerRideStep.driverList ||
                        _passengerStep == PassengerRideStep.waitingDriver,
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
                  child: HomeRideAlertsOverlay(
                    canDrive: driverAccess.canDrive,
                    passengerStep: _passengerStep,
                    currentTabIndex: _currentTabIndex,
                    passengerSheetHidden: _passengerSheetHidden,
                    isPassengerMode: _currentMode.isPassenger,
                    showRideRequests: _showRideRequests,
                    acceptedNotice: _acceptedNotice,
                    onOpenRideSearch: _openRideSearch,
                    onOpenOffer: (offerKey) =>
                        _openRideRequests(offerKey: offerKey),
                    onOpenRidesAsPassenger: () =>
                        _openRideRequests(asPassenger: true),
                    onDismissAcceptedNotice: () =>
                        setState(() => _acceptedNotice = null),
                  ),
                ),
              ],
            ),
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
    // O passageiro também vê o caminho (com as paradas) da viagem em que está
    final passengerTrip =
        (ref.watch(myPassengerRidesProvider).value ?? const [])
            .where((ride) => ride.status == RideStatus.inProgress)
            .firstOrNull;
    final shiftRide = ref.watch(driverRideInProgressProvider) ?? passengerTrip;
    // Durante toda a viagem o caminho fica no mapa (some quando ela termina),
    // a não ser que o usuário esteja montando um pedido novo
    final showShiftRoute =
        shiftRide != null && _passengerStep == PassengerRideStep.idle;
    final tripModeVisible = _tripModeOpen && showShiftRoute;

    return Stack(
      key: const ValueKey('uber_map_home_view'),
      children: [
        // 1. Camada Base: Google Maps real em tela cheia com círculo dinâmico / marcadores / rotas
        Positioned.fill(
          child: HomeMapViewport(
            isDriverMode: _currentMode.isDriver,
            passengerStep: _passengerStep,
            shiftRide: shiftRide,
            pickupLocation: _pickupLocation,
            pickupAddress: _pickupAddress,
            destinationLocation: _destinationLocation,
            destinationAddress: _destinationAddress,
            tripRoutePoints: _tripRoutePoints,
            driverApproachRoutePoints: _driverApproachRoutePoints,
            universityName: user.academicRecord.university.toString(),
            onDriverSelected: _handleSelectDriver,
            onMapTap: () {
              if (_isCarMenuOpen) {
                setState(() => _isCarMenuOpen = false);
              }
            },
            onPickupLocationChanged: (location, address) {
              setState(() {
                _pickupLocation = location;
                _pickupAddress = address;
              });
            },
            onDestinationLocationChanged: (location, address) {
              setState(() {
                _destinationLocation = location;
                _destinationAddress = address;
              });
            },
          ),
        ),

        // 2. Camada Intermediária: Painel Retrátil específico de acordo com o modo
        // Modo viagem: só a viagem em andamento, acima da dock
        if (tripModeVisible)
          HomeTripModeOverlay(
            ride: shiftRide,
            bottomPadding: bottomPadding,
            onClose: () => setState(() => _tripModeOpen = false),
            onError: _showError,
          )
        else if (_currentMode.isDriver && _showRideRequests)
          Positioned.fill(
            child: SheetDragToDismiss(
              onDismiss: _handleCloseRideRequests,
              child: RideRequestsSheet(
                bottomSafeArea: bottomPadding,
                onClose: _handleCloseRideRequests,
                onRideStarted: _handleRideStarted,
                openOfferKey: _openOfferKey,
                // Só limpa: o painel já está abrindo a carona
                onOfferOpened: () => _openOfferKey = null,
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
              child: HomeRideFlowCoordinator(
                step: _passengerStep,
                isDriver: _currentMode.isDriver,
                bottomPadding: bottomPadding,
                pickupAddress: _pickupAddress,
                destinationAddress: _destinationAddress,
                onConfirmPickup: _handleConfirmPickup,
                onBackToPickup: () =>
                    _goToStep(PassengerRideStep.selectPickup),
                onStartDragMapPickup: _handleStartDragMapPickup,
                onPickupChosen: _handlePickupChosen,
                onStartDragMapDestination: _handleStartDragMapDestination,
                onDestinationChosen: _handleDestinationChosen,
                onConfirmDestination: _handleConfirmDestination,
                onBackToDestinationSearch: () =>
                    _goToStep(PassengerRideStep.searchDestination),
                onCancelFlow: _handleCloseRideFlow,
                onRetryEstimate: _goToRoutePreview,
                onConfirmRoutePreview: () =>
                    _goToStep(PassengerRideStep.routeConfirmation),
                onEditDestination: () =>
                    _goToStep(PassengerRideStep.searchDestination),
                offerDeparture: _offerDeparture,
                rideDeparture: _rideDeparture,
                onChangeDeparture: () => _currentMode.isDriver
                    ? _pickDeparture(
                        current: _offerDeparture,
                        onPicked: (departure) => _offerDeparture = departure,
                      )
                    : _pickDeparture(
                        current: _rideDeparture,
                        onPicked: (departure) => _rideDeparture = departure,
                      ),
                offerRepeatWeekly: _offerRepeatWeekly,
                onRepeatWeeklyChanged: _currentMode.isDriver
                    ? (value) => setState(() => _offerRepeatWeekly = value)
                    : null,
                onGoToOfferDetails: _goToOfferDetails,
                onReviewAndSubmit: _reviewAndSubmit,
                onBackToRoutePreview: () =>
                    _goToStep(PassengerRideStep.routePreview),
                vehicles: _offerVehicles().vehicles,
                selectedVehicleId: _offerVehicles().selected?.id,
                onVehicleChanged: (vehicle) => setState(() {
                  _offerVehicleId = vehicle.id;
                  _offerSeats = vehicle.passengerSeats;
                }),
                seats: (_offerSeats ??
                        _offerVehicles().selected?.passengerSeats ??
                        1)
                    .clamp(
                  1,
                  _offerVehicles().selected?.passengerSeats ?? 1,
                ),
                onSeatsChanged: (value) => setState(() => _offerSeats = value),
                searchRadiusKm: _driverSearchRadiusKm,
                onSearchRadiusChanged: (value) =>
                    setState(() => _driverSearchRadiusKm = value),
                isSubmitting:
                    _publishingOffer || ref.watch(offerRouteProvider),
                onAddVehicle: _handleAddVehicle,
                onBackToRouteConfirmation: () =>
                    _goToStep(PassengerRideStep.routeConfirmation),
              ),
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
                onCancelFlow: _handleCancelFlow,
                onKeepSearching: _handleKeepSearching,
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
    // O horário é sempre escolhido (não existe "agora")
    if ((isDriver ? _offerDeparture : _rideDeparture) == null) {
      _showError('Escolha o dia e o horário de saída.');
      return;
    }
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
    // O tempo passou durante a revisão: o horário precisa continuar valendo (o
    // motorista, com pelo menos 5 min de antecedência para receber pedidos)
    final departure = isDriver ? _offerDeparture : _rideDeparture;
    final earliest = isDriver
        ? DateTime.now().add(const Duration(minutes: 5))
        : DateTime.now();
    if (departure == null || !departure.isAfter(earliest)) {
      _goToStep(PassengerRideStep.routeConfirmation);
      _showError(
        isDriver
            ? 'Esse horário está muito perto ou já passou. Escolha outro.'
            : 'Esse horário já passou. Escolha outro.',
      );
      return;
    }
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
        PassengerRideStep.routePreview ||
        PassengerRideStep.routeConfirmation ||
        PassengerRideStep.offerDetails =>
          true,
        _ => false,
      };

  void _goToStep(PassengerRideStep step) =>
      setState(() => _passengerStep = step);

  /// Motorista: do horário para o carro, as vagas e o raio. Precisa do horário.
  void _goToOfferDetails() {
    if (_offerDeparture == null) {
      _showError('Escolha o dia e o horário de saída.');
      return;
    }
    _goToStep(PassengerRideStep.offerDetails);
  }

  /// Os carros ativos do motorista, com o escolhido (o primeiro, se nenhum foi).
  ({List<VehicleModel> vehicles, VehicleModel? selected}) _offerVehicles() {
    final vehicles = [
      for (final vehicle
          in ref.watch(myVehiclesProvider).value ?? const <VehicleModel>[])
        if (vehicle.active) vehicle,
    ];
    final selected =
        vehicles
            .where((vehicle) => vehicle.id == _offerVehicleId)
            .firstOrNull ??
        vehicles.firstOrNull;
    return (vehicles: vehicles, selected: selected);
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
                3 => HomeProfileTab(
                    user: user as UserModel,
                    bottomPadding: bottomPadding,
                  ),
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
}
