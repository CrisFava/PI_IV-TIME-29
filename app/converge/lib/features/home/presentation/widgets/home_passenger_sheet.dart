// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../route/data/models/place_suggestion_model.dart';
import '../../../route/state/places_autocomplete_controller.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/show_app_notice.dart';
import '../../../../core/utils/currency_formatter.dart';
import '../../../matching/data/models/match_model.dart';
import '../../../matching/presentation/widgets/match_driver_card.dart';
import '../../../matching/presentation/widgets/match_waiting_card.dart';
import '../../../matching/state/passenger_match_controller.dart';
import '../../../matching/state/passenger_match_phase.dart';
import '../../../matching/state/passenger_match_state.dart';
import '../../../route/data/models/route_response_model.dart';
import '../../../route/state/saved_routes_controller.dart';
import 'campus_spots.dart';
import 'home_driver_form_sheet.dart';
import 'passenger_ride_step.dart';

/// ============================================================================
/// COMPONENTE: HomePassengerSheet (Fluxo de Solicitação do Passageiro)
/// ============================================================================
/// Ordem do fluxo invertida conforme solicitado:
/// 1. Primeiro: Seleção do Ponto de Embarque (Origem)
/// 2. Segundo: Seleção do Ponto de Desembarque (Destino)
/// 3. Terceiro: Confirmação de Rota (com resumo Origem e Destino)
/// 4. Quarto: Lista de Motoristas Compatíveis (matching real)
/// 5. Quinto: Aguardando o motorista aceitar ou recusar o pedido de vaga
/// 6. Sexto: Carona Aceita (atalho para o Chat)
/// ============================================================================
class HomePassengerSheet extends ConsumerStatefulWidget {
  final PassengerRideStep step;
  final String pickupAddress;
  final String destinationAddress;
  final LatLng pickupLocation;
  final LatLng? destinationLocation;

  final VoidCallback onStartRideFlow;
  final VoidCallback onStartDragMapPickup;
  final VoidCallback onStartDragMapDestination;
  final void Function(String title, LatLng position) onPickupChosen;
  final void Function(String title, LatLng position) onDestinationChosen;
  final VoidCallback onConfirmPickup;
  final VoidCallback onConfirmDestination;
  final VoidCallback onConfirmRideOrder;
  final ValueChanged<MatchModel> onSelectDriver;
  final VoidCallback onRetryMatches;
  final VoidCallback onCancelRequest;
  final VoidCallback onSendMessageToDriver;
  final VoidCallback onCancelFlow;
  final VoidCallback onSaveRoute;
  final ValueChanged<RouteResponseModel>? onSelectSavedRoute;
  final VoidCallback? onChooseMapOption;
  final double bottomSafeArea;

  const HomePassengerSheet({
    super.key,
    required this.step,
    required this.pickupAddress,
    required this.destinationAddress,
    required this.pickupLocation,
    this.destinationLocation,
    required this.onStartRideFlow,
    required this.onStartDragMapPickup,
    required this.onStartDragMapDestination,
    required this.onPickupChosen,
    required this.onDestinationChosen,
    required this.onConfirmPickup,
    required this.onConfirmDestination,
    required this.onConfirmRideOrder,
    required this.onSelectDriver,
    required this.onRetryMatches,
    required this.onCancelRequest,
    required this.onSendMessageToDriver,
    required this.onCancelFlow,
    required this.onSaveRoute,
    this.onSelectSavedRoute,
    this.onChooseMapOption,
    this.bottomSafeArea = 0,
  });

  @override
  ConsumerState<HomePassengerSheet> createState() => _HomePassengerSheetState();
}

class _HomePassengerSheetState extends ConsumerState<HomePassengerSheet> {
  final _pickupSearchController = TextEditingController();
  final _searchController = TextEditingController();

  Future<void> _handlePickupSuggestionSelected(
    PlaceSuggestionModel suggestion,
  ) async {
    HapticFeedback.lightImpact();
    FocusScope.of(context).unfocus();
    final details = await ref
        .read(placesAutocompleteProvider.notifier)
        .getPlaceDetails(suggestion.placeId);
    if (details != null && mounted) {
      _pickupSearchController.clear();
      ref.read(placesAutocompleteProvider.notifier).clear();
      widget.onPickupChosen(
        details.address,
        LatLng(details.latitude, details.longitude),
      );
    } else if (mounted) {
      showAppNotice(
        context,
        'Não foi possível obter detalhes deste endereço.',
        type: AppNoticeType.error,
      );
    }
  }

  Future<void> _handleDestinationSuggestionSelected(
    PlaceSuggestionModel suggestion,
  ) async {
    HapticFeedback.lightImpact();
    FocusScope.of(context).unfocus();
    final details = await ref
        .read(placesAutocompleteProvider.notifier)
        .getPlaceDetails(suggestion.placeId);
    if (details != null && mounted) {
      _searchController.clear();
      ref.read(placesAutocompleteProvider.notifier).clear();
      widget.onDestinationChosen(
        details.address,
        LatLng(details.latitude, details.longitude),
      );
    } else if (mounted) {
      showAppNotice(
        context,
        'Não foi possível obter detalhes deste endereço.',
        type: AppNoticeType.error,
      );
    }
  }

  @override
  void dispose() {
    _pickupSearchController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    switch (widget.step) {
      case PassengerRideStep.idle:
        return const SizedBox.shrink();
      case PassengerRideStep.chooseRouteOption:
        return _buildChooseRouteOptionSheet();
      case PassengerRideStep.selectPickup:
        return _buildSelectPickupSheet();
      case PassengerRideStep.dragMapPickup:
        return _buildDragMapPickupSheet();
      case PassengerRideStep.searchDestination:
        return _buildSearchDestinationSheet();
      case PassengerRideStep.dragMapDestination:
        return _buildDragMapDestinationSheet();
      case PassengerRideStep.routeConfirmation:
        return _buildRouteConfirmationSheet();
      case PassengerRideStep.driverList:
        return _buildDriverListSheet();
      case PassengerRideStep.waitingDriver:
        return _buildWaitingDriverSheet();
      case PassengerRideStep.driverAccepted:
        return _DriverAcceptedPipSheet(
          driver: ref.watch(passengerMatchProvider).selected,
          onSendMessage: widget.onSendMessageToDriver,
          onCancel: widget.onCancelFlow,
          bottomSafeArea: widget.bottomSafeArea,
        );
    }
  }

  // ==========================================================================
  // PASSO 0: ESCOLHA ENTRE ROTA SALVA OU SELEÇÃO NO MAPA
  // ==========================================================================
  Widget _buildChooseRouteOptionSheet() {
    final savedRoutesState = ref.watch(savedRoutesProvider);
    final savedRoutes = savedRoutesState.routes;

    return DraggableScrollableSheet(
      initialChildSize: 0.62,
      minChildSize: 0.26,
      maxChildSize: 0.92,
      snap: true,
      snapSizes: const [0.38, 0.62, 0.92],
      builder: (context, scrollController) {
        return Container(
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: const BorderRadius.vertical(top: Radius.circular(26)),
            border: Border.all(color: AppColors.borderSubtle, width: 1),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.14),
                blurRadius: 24,
                offset: const Offset(0, -6),
              ),
            ],
          ),
          child: CustomScrollView(
            controller: scrollController,
            slivers: [
              // Alça de arraste superior
              SliverToBoxAdapter(
                child: Center(
                  child: Container(
                    margin: const EdgeInsets.symmetric(vertical: 10),
                    width: 38,
                    height: 4,
                    decoration: BoxDecoration(
                      color: AppColors.borderMedium,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
              ),

              SliverPadding(
                padding: EdgeInsets.fromLTRB(
                  16,
                  4,
                  16,
                  120 + widget.bottomSafeArea,
                ),
                sliver: SliverList(
                  delegate: SliverChildListDelegate([
                    Row(
                      children: [
                        IconButton(
                          onPressed: widget.onCancelFlow,
                          icon: const Icon(
                            Icons.close_rounded,
                            color: AppColors.textPrimary,
                          ),
                          tooltip: 'Fechar',
                        ),
                        const SizedBox(width: 4),
                        const Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Iniciar Carona',
                              style: TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.w800,
                                color: AppColors.textPrimary,
                                letterSpacing: -0.3,
                              ),
                            ),
                            Text(
                              'Como deseja definir seu trajeto hoje?',
                              style: TextStyle(
                                fontSize: 12,
                                color: AppColors.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),

                    // OPÇÃO 1: SELECIONAR NO MAPA
                    Material(
                      color: AppColors.neutralLight,
                      borderRadius: BorderRadius.circular(18),
                      child: InkWell(
                        onTap: () {
                          HapticFeedback.lightImpact();
                          if (widget.onChooseMapOption != null) {
                            widget.onChooseMapOption!();
                          } else {
                            widget.onStartRideFlow();
                          }
                        },
                        borderRadius: BorderRadius.circular(18),
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.all(12),
                                decoration: BoxDecoration(
                                  color: AppColors.cardBackground,
                                  shape: BoxShape.circle,
                                  border: Border.all(color: AppColors.borderSubtle),
                                ),
                                child: const Icon(
                                  Icons.map_rounded,
                                  color: AppColors.textPrimary,
                                  size: 22,
                                ),
                              ),
                              const SizedBox(width: 14),
                              const Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      'Selecionar no Mapa',
                                      style: TextStyle(
                                        fontSize: 15,
                                        fontWeight: FontWeight.w800,
                                        color: AppColors.textPrimary,
                                      ),
                                    ),
                                    SizedBox(height: 3),
                                    Text(
                                      'Definir novo ponto de embarque e destino',
                                      style: TextStyle(
                                        fontSize: 12,
                                        color: AppColors.textSecondary,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              const Icon(
                                Icons.arrow_forward_ios_rounded,
                                size: 14,
                                color: AppColors.textMuted,
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),

                    const SizedBox(height: 22),

                    // OPÇÃO 2: USAR UMA ROTA SALVA
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Row(
                          children: [
                            Icon(
                              Icons.bookmark_rounded,
                              size: 18,
                              color: AppColors.primary,
                            ),
                            SizedBox(width: 8),
                            Text(
                              'Usar uma Rota Salva',
                              style: TextStyle(
                                fontSize: 15,
                                fontWeight: FontWeight.w800,
                                color: AppColors.textPrimary,
                                letterSpacing: -0.2,
                              ),
                            ),
                          ],
                        ),
                        if (savedRoutes.isNotEmpty)
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                            decoration: BoxDecoration(
                              color: AppColors.neutralLight,
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: Text(
                              '${savedRoutes.length} salvas',
                              style: const TextStyle(
                                fontSize: 11.5,
                                fontWeight: FontWeight.w700,
                                color: AppColors.textPrimary,
                              ),
                            ),
                          ),
                      ],
                    ),
                    const SizedBox(height: 12),

                    if (savedRoutesState.isLoading && savedRoutes.isEmpty)
                      const Center(
                        child: Padding(
                          padding: EdgeInsets.symmetric(vertical: 32),
                          child: CircularProgressIndicator(
                            color: AppColors.primary,
                            strokeWidth: 2.8,
                          ),
                        ),
                      )
                    else if (savedRoutes.isEmpty)
                      Container(
                        padding: const EdgeInsets.all(20),
                        decoration: BoxDecoration(
                          color: AppColors.cardSecondary,
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: AppColors.borderSubtle),
                        ),
                        child: Column(
                          children: [
                            const Icon(
                              Icons.bookmark_border_rounded,
                              size: 36,
                              color: AppColors.iconMuted,
                            ),
                            const SizedBox(height: 10),
                            const Text(
                              'Nenhuma rota salva encontrada',
                              style: TextStyle(
                                fontWeight: FontWeight.w700,
                                fontSize: 14,
                                color: AppColors.textPrimary,
                              ),
                            ),
                            const SizedBox(height: 4),
                            const Text(
                              'Você pode selecionar no mapa e salvar seu trajeto frequente para pedir carona em 1 toque.',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                fontSize: 12,
                                color: AppColors.textSecondary,
                              ),
                            ),
                            const SizedBox(height: 14),
                            FilledButton.icon(
                              style: FilledButton.styleFrom(
                                backgroundColor: AppColors.primary,
                                foregroundColor: AppColors.textLight,
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(12),
                                ),
                              ),
                              onPressed: () {
                                HapticFeedback.lightImpact();
                                if (widget.onChooseMapOption != null) {
                                  widget.onChooseMapOption!();
                                } else {
                                  widget.onStartRideFlow();
                                }
                              },
                              icon: const Icon(Icons.add_location_alt_rounded, size: 16),
                              label: const Text('Definir Rota no Mapa'),
                            ),
                          ],
                        ),
                      )
                    else
                      ...savedRoutes.map((route) {
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: Material(
                            color: AppColors.cardBackground,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(18),
                              side: const BorderSide(color: AppColors.borderSubtle),
                            ),
                            child: InkWell(
                              onTap: () {
                                HapticFeedback.mediumImpact();
                                widget.onSelectSavedRoute?.call(route);
                              },
                              borderRadius: BorderRadius.circular(18),
                              child: Padding(
                                padding: const EdgeInsets.all(16),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    // Linha do topo: Horário e Dias
                                    Row(
                                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                      children: [
                                        Container(
                                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                          decoration: BoxDecoration(
                                            color: AppColors.neutralLight,
                                            borderRadius: BorderRadius.circular(8),
                                          ),
                                          child: Row(
                                            mainAxisSize: MainAxisSize.min,
                                            children: [
                                              const Icon(
                                                Icons.schedule_rounded,
                                                size: 13,
                                                color: AppColors.textSecondary,
                                              ),
                                              const SizedBox(width: 5),
                                              Text(
                                                '${route.departureTime} → ${route.estimatedArrivalTime}',
                                                style: const TextStyle(
                                                  fontSize: 11.5,
                                                  fontWeight: FontWeight.w700,
                                                  color: AppColors.textPrimary,
                                                ),
                                              ),
                                            ],
                                          ),
                                        ),
                                        Text(
                                          _formatDaysSummary(route.weekDays),
                                          style: const TextStyle(
                                            fontSize: 11.5,
                                            color: AppColors.textSecondary,
                                            fontWeight: FontWeight.w600,
                                          ),
                                        ),
                                      ],
                                    ),
                                    const SizedBox(height: 12),

                                    // Origem
                                    Row(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        const Icon(
                                          Icons.radio_button_checked_rounded,
                                          size: 15,
                                          color: AppColors.success,
                                        ),
                                        const SizedBox(width: 10),
                                        Expanded(
                                          child: Text(
                                            route.origin.address,
                                            maxLines: 1,
                                            overflow: TextOverflow.ellipsis,
                                            style: const TextStyle(
                                              fontSize: 13.5,
                                              fontWeight: FontWeight.w700,
                                              color: AppColors.textPrimary,
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),
                                    Padding(
                                      padding: const EdgeInsets.only(left: 7),
                                      child: Container(
                                        width: 1.5,
                                        height: 14,
                                        color: AppColors.borderMedium,
                                      ),
                                    ),
                                    // Destino
                                    Row(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        const Icon(
                                          Icons.location_on_rounded,
                                          size: 15,
                                          color: AppColors.primary,
                                        ),
                                        const SizedBox(width: 10),
                                        Expanded(
                                          child: Text(
                                            route.destination.address,
                                            maxLines: 1,
                                            overflow: TextOverflow.ellipsis,
                                            style: const TextStyle(
                                              fontSize: 13.5,
                                              fontWeight: FontWeight.w700,
                                              color: AppColors.textPrimary,
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),

                                    const SizedBox(height: 14),
                                    const Divider(color: AppColors.borderSubtle, height: 1),
                                    const SizedBox(height: 10),

                                    // Rodapé com "Ver detalhes" e Ação
                                    Row(
                                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                      children: [
                                        OutlinedButton.icon(
                                          onPressed: () {
                                            HapticFeedback.lightImpact();
                                            _showRouteDetailsModal(context, route);
                                          },
                                          icon: const Icon(
                                            Icons.info_outline_rounded,
                                            size: 15,
                                            color: AppColors.textPrimary,
                                          ),
                                          label: const Text(
                                            'Ver detalhes',
                                            style: TextStyle(
                                              fontSize: 12.5,
                                              fontWeight: FontWeight.w700,
                                              color: AppColors.textPrimary,
                                            ),
                                          ),
                                          style: OutlinedButton.styleFrom(
                                            visualDensity: VisualDensity.compact,
                                            side: const BorderSide(color: AppColors.borderSubtle),
                                            shape: RoundedRectangleBorder(
                                              borderRadius: BorderRadius.circular(10),
                                            ),
                                            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                          ),
                                        ),
                                        FilledButton.icon(
                                          onPressed: () {
                                            HapticFeedback.mediumImpact();
                                            widget.onSelectSavedRoute?.call(route);
                                          },
                                          icon: const Icon(
                                            Icons.directions_car_rounded,
                                            size: 15,
                                            color: AppColors.textLight,
                                          ),
                                          label: const Text(
                                            'Pedir carona',
                                            style: TextStyle(
                                              fontSize: 12,
                                              fontWeight: FontWeight.w700,
                                              color: AppColors.textLight,
                                            ),
                                          ),
                                          style: FilledButton.styleFrom(
                                            backgroundColor: AppColors.primary,
                                            visualDensity: VisualDensity.compact,
                                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                                            shape: RoundedRectangleBorder(
                                              borderRadius: BorderRadius.circular(10),
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        );
                      }),
                  ]),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  void _showRouteDetailsModal(BuildContext context, RouteResponseModel route) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (modalContext) {
        return Container(
          decoration: const BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: BorderRadius.vertical(top: Radius.circular(26)),
          ),
          padding: EdgeInsets.fromLTRB(
            20,
            12,
            20,
            24 + MediaQuery.paddingOf(modalContext).bottom,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 38,
                  height: 4,
                  decoration: BoxDecoration(
                    color: AppColors.borderMedium,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 14),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Row(
                    children: [
                      Icon(
                        Icons.alt_route_rounded,
                        color: AppColors.primary,
                        size: 22,
                      ),
                      SizedBox(width: 8),
                      Text(
                        'Detalhes da Rota Salva',
                        style: TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.w800,
                          color: AppColors.textPrimary,
                          letterSpacing: -0.3,
                        ),
                      ),
                    ],
                  ),
                  IconButton(
                    onPressed: () => Navigator.pop(modalContext),
                    icon: const Icon(
                      Icons.close_rounded,
                      color: AppColors.textPrimary,
                    ),
                    tooltip: 'Fechar',
                  ),
                ],
              ),
              const SizedBox(height: 14),
              _buildDetailItem(
                'Origem (Local de Embarque)',
                route.origin.address,
                subtitle: '${route.origin.neighborhood} - ${route.origin.city}',
                icon: Icons.radio_button_checked_rounded,
                iconColor: AppColors.success,
              ),
              const SizedBox(height: 8),
              _buildDetailItem(
                'Destino (Local de Desembarque)',
                route.destination.address,
                subtitle: '${route.destination.neighborhood} - ${route.destination.city}',
                icon: Icons.location_on_rounded,
                iconColor: AppColors.primary,
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: _buildStatBadge(
                      'Saída',
                      route.departureTime,
                      Icons.schedule_rounded,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: _buildStatBadge(
                      'Chegada estimada',
                      route.estimatedArrivalTime,
                      Icons.flag_rounded,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: _buildStatBadge(
                      'Distância',
                      route.routeDistanceKm != null && route.routeDistanceKm! > 0
                          ? '${route.routeDistanceKm!.toStringAsFixed(1)} km'
                          : 'Calculada no mapa',
                      Icons.straighten_rounded,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: _buildStatBadge(
                      'Desvio tolerado',
                      'Até ${route.maxDetourKm ?? 3.0} km',
                      Icons.navigation_rounded,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              _buildDetailItem(
                'Frequência Semanal',
                _formatFullWeekDays(route.weekDays),
                icon: Icons.calendar_month_rounded,
                iconColor: AppColors.textSecondary,
              ),
              const SizedBox(height: 20),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  style: FilledButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: AppColors.textLight,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(14),
                    ),
                  ),
                  onPressed: () {
                    HapticFeedback.mediumImpact();
                    Navigator.pop(modalContext);
                    widget.onSelectSavedRoute?.call(route);
                  },
                  icon: const Icon(
                    Icons.directions_car_rounded,
                    size: 18,
                  ),
                  label: const Text(
                    'Usar esta rota e buscar motoristas',
                    style: TextStyle(
                      fontSize: 14.5,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildDetailItem(
    String title,
    String mainValue, {
    String? subtitle,
    IconData? icon,
    Color? iconColor,
  }) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.neutralLight,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (icon != null) ...[
            Icon(
              icon,
              size: 18,
              color: iconColor ?? AppColors.textPrimary,
            ),
            const SizedBox(width: 10),
          ],
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                    color: AppColors.textSecondary,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  mainValue,
                  style: const TextStyle(
                    fontSize: 13.5,
                    fontWeight: FontWeight.w700,
                    color: AppColors.textPrimary,
                  ),
                ),
                if (subtitle != null && subtitle.isNotEmpty) ...[
                  const SizedBox(height: 2),
                  Text(
                    subtitle,
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatBadge(String label, String value, IconData icon) {
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: AppColors.neutralLight,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, size: 14, color: AppColors.textSecondary),
              const SizedBox(width: 6),
              Text(
                label,
                style: const TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.w500,
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            value,
            style: const TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w700,
              color: AppColors.textPrimary,
            ),
          ),
        ],
      ),
    );
  }

  String _formatDaysSummary(Set<String> weekDays) {
    if (weekDays.isEmpty) return 'Todos os dias';
    const map = {
      'MONDAY': 'Seg',
      'TUESDAY': 'Ter',
      'WEDNESDAY': 'Qua',
      'THURSDAY': 'Qui',
      'FRIDAY': 'Sex',
      'SATURDAY': 'Sáb',
      'SUNDAY': 'Dom',
    };
    if (weekDays.length == 5 &&
        weekDays.contains('MONDAY') &&
        weekDays.contains('FRIDAY')) {
      return 'Segunda a Sexta';
    }
    return weekDays.map((d) => map[d.toUpperCase()] ?? d).join(', ');
  }

  String _formatFullWeekDays(Set<String> weekDays) {
    if (weekDays.isEmpty) return 'Segunda a Sexta-feira';
    const map = {
      'MONDAY': 'Segunda-feira',
      'TUESDAY': 'Terça-feira',
      'WEDNESDAY': 'Quarta-feira',
      'THURSDAY': 'Quinta-feira',
      'FRIDAY': 'Sexta-feira',
      'SATURDAY': 'Sábado',
      'SUNDAY': 'Domingo',
    };
    return weekDays.map((d) => map[d.toUpperCase()] ?? d).join(', ');
  }

  // ==========================================================================
  // PASSO 1: SELEÇÃO DO PONTO DE EMBARQUE (ORIGEM)
  // ==========================================================================
  Widget _buildSelectPickupSheet() {
    final autocompleteState = ref.watch(placesAutocompleteProvider);

    return DraggableScrollableSheet(
      initialChildSize: 0.54,
      minChildSize: 0.22,
      maxChildSize: 0.88,
      snap: true,
      snapSizes: const [0.34, 0.54, 0.88],
      builder: (context, scrollController) {
        return Container(
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: const BorderRadius.vertical(top: Radius.circular(26)),
            border: Border.all(color: AppColors.borderSubtle, width: 1),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.14),
                blurRadius: 24,
                offset: const Offset(0, -6),
              ),
            ],
          ),
          child: CustomScrollView(
            controller: scrollController,
            slivers: [
              // Alça de arraste
              SliverToBoxAdapter(
                child: Center(
                  child: Container(
                    margin: const EdgeInsets.symmetric(vertical: 10),
                    width: 38,
                    height: 4,
                    decoration: BoxDecoration(
                      color: AppColors.borderMedium,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
              ),

              SliverPadding(
                padding: EdgeInsets.fromLTRB(16, 4, 16, 120 + widget.bottomSafeArea),
                sliver: SliverList(
                  delegate: SliverChildListDelegate([
                    Row(
                      children: [
                        IconButton(
                          onPressed: widget.onCancelFlow,
                          icon: const Icon(
                            Icons.close_rounded,
                            color: AppColors.textPrimary,
                          ),
                          tooltip: 'Fechar',
                        ),
                        const SizedBox(width: 4),
                        const Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Ponto de Embarque',
                              style: TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.w800,
                                color: AppColors.textPrimary,
                                letterSpacing: -0.3,
                              ),
                            ),
                            Text(
                              'Onde o motorista deve encontrar você?',
                              style: TextStyle(
                                fontSize: 12,
                                color: AppColors.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),

                    // Card de Embarque Atual
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: AppColors.neutralLight,
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: AppColors.borderSubtle),
                      ),
                      child: Row(
                        children: [
                          const _OriginOpaqueDotBadge(size: 18),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Text(
                                  'Local de Embarque Selecionado',
                                  style: TextStyle(
                                    fontSize: 11,
                                    fontWeight: FontWeight.w700,
                                    color: AppColors.textSecondary,
                                  ),
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  widget.pickupAddress,
                                  style: const TextStyle(
                                    fontSize: 13.5,
                                    fontWeight: FontWeight.w700,
                                    color: AppColors.textPrimary,
                                  ),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),

                    // Campo de Busca de Embarque com Autocomplete (debounce de 1s) e Lupa Animada
                    Container(
                      decoration: BoxDecoration(
                        color: AppColors.neutralLight,
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: AppColors.borderSubtle),
                      ),
                      child: TextField(
                        controller: _pickupSearchController,
                        autofocus: false,
                        style: const TextStyle(
                          fontSize: 14.5,
                          fontWeight: FontWeight.w600,
                          color: AppColors.textPrimary,
                        ),
                        decoration: InputDecoration(
                          prefixIcon: const Icon(
                            Icons.pin_drop_rounded,
                            color: AppColors.textPrimary,
                            size: 20,
                          ),
                          suffixIcon: autocompleteState.isLoading
                              ? const FloatingSearchLensIcon(
                                  key: ValueKey('pickup_floating_search_lens'),
                                )
                              : _pickupSearchController.text.isNotEmpty
                                  ? IconButton(
                                      icon: const Icon(
                                        Icons.clear_rounded,
                                        size: 18,
                                        color: AppColors.textSecondary,
                                      ),
                                      onPressed: () {
                                        _pickupSearchController.clear();
                                        ref
                                            .read(
                                              placesAutocompleteProvider
                                                  .notifier,
                                            )
                                            .clear();
                                        setState(() {});
                                      },
                                    )
                                  : const Icon(
                                      Icons.search_rounded,
                                      size: 20,
                                      color: AppColors.textMuted,
                                    ),
                          hintText: 'Pesquisar outro endereço de embarque...',
                          hintStyle: const TextStyle(
                            fontSize: 13.5,
                            color: AppColors.textPlaceholder,
                          ),
                          border: InputBorder.none,
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: 14,
                            vertical: 14,
                          ),
                        ),
                        onChanged: (text) {
                          setState(() {});
                          ref
                              .read(placesAutocompleteProvider.notifier)
                              .onQueryChanged(text);
                        },
                      ),
                    ),
                    const SizedBox(height: 12),

                    if (autocompleteState.suggestions.isNotEmpty) ...[
                      const Text(
                        'Sugestões de Endereço',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w800,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 8),
                      ...autocompleteState.suggestions.map((suggestion) {
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 8),
                          child: Material(
                            color: AppColors.cardBackground,
                            borderRadius: BorderRadius.circular(14),
                            child: InkWell(
                              borderRadius: BorderRadius.circular(14),
                              onTap: () =>
                                  _handlePickupSuggestionSelected(suggestion),
                              child: Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 14,
                                  vertical: 12,
                                ),
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(14),
                                  border: Border.all(
                                    color: AppColors.borderSubtle,
                                  ),
                                ),
                                child: Row(
                                  children: [
                                    const Icon(
                                      Icons.location_on_rounded,
                                      color: AppColors.primary,
                                      size: 20,
                                    ),
                                    const SizedBox(width: 12),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            suggestion.mainText,
                                            style: const TextStyle(
                                              fontSize: 14,
                                              fontWeight: FontWeight.w700,
                                              color: AppColors.textPrimary,
                                            ),
                                          ),
                                          if (suggestion
                                              .secondaryText
                                              .isNotEmpty) ...[
                                            const SizedBox(height: 2),
                                            Text(
                                              suggestion.secondaryText,
                                              style: const TextStyle(
                                                fontSize: 12,
                                                color: AppColors.textSecondary,
                                              ),
                                              maxLines: 1,
                                              overflow: TextOverflow.ellipsis,
                                            ),
                                          ],
                                        ],
                                      ),
                                    ),
                                    const Icon(
                                      Icons.chevron_right_rounded,
                                      color: AppColors.iconMuted,
                                      size: 20,
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        );
                      }),
                      const SizedBox(height: 8),
                    ],

                    // Botão estilo Uber: "Ajustar embarque no mapa"
                    Material(
                      color: AppColors.cardBackground,
                      borderRadius: BorderRadius.circular(16),
                      child: InkWell(
                        borderRadius: BorderRadius.circular(16),
                        onTap: () {
                          HapticFeedback.lightImpact();
                          widget.onStartDragMapPickup();
                        },
                        child: Container(
                          padding: const EdgeInsets.symmetric(vertical: 13, horizontal: 14),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(color: AppColors.borderSubtle),
                          ),
                          child: const Row(
                            children: [
                              Icon(
                                Icons.my_location_rounded,
                                color: AppColors.primary,
                                size: 22,
                              ),
                              SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      'Ajustar embarque no mapa',
                                      style: TextStyle(
                                        fontSize: 14,
                                        fontWeight: FontWeight.w700,
                                        color: AppColors.textPrimary,
                                      ),
                                    ),
                                    Text(
                                      'Arraste o mapa até o local exato de partida',
                                      style: TextStyle(
                                        fontSize: 11.5,
                                        color: AppColors.textSecondary,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              Icon(
                                Icons.chevron_right_rounded,
                                color: AppColors.iconMuted,
                                size: 20,
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),

                    const Text(
                      'Pontos de Encontro no Campus',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w800,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 8),

                    // Lista de Pontos de Embarque no Campus
                    ...campusPickupSuggestions.map((spot) {
                      final isSelected = widget.pickupAddress == spot.title;
                      return Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: Material(
                          color: isSelected
                              ? AppColors.neutralLight
                              : AppColors.cardBackground,
                          borderRadius: BorderRadius.circular(14),
                          child: InkWell(
                            borderRadius: BorderRadius.circular(14),
                            onTap: () {
                              HapticFeedback.lightImpact();
                              widget.onPickupChosen(spot.title, spot.position);
                            },
                            child: Container(
                              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                              decoration: BoxDecoration(
                                borderRadius: BorderRadius.circular(14),
                                border: Border.all(
                                  color: isSelected
                                      ? AppColors.primary
                                      : AppColors.borderSubtle,
                                  width: isSelected ? 1.5 : 1,
                                ),
                              ),
                              child: Row(
                                children: [
                                  const _OriginOpaqueDotBadge(size: 16),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          spot.title,
                                          style: const TextStyle(
                                            fontSize: 13.5,
                                            fontWeight: FontWeight.w700,
                                            color: AppColors.textPrimary,
                                          ),
                                        ),
                                        const SizedBox(height: 2),
                                        Text(
                                          spot.subtitle,
                                          style: const TextStyle(
                                            fontSize: 11.5,
                                            color: AppColors.textSecondary,
                                          ),
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                      ],
                                    ),
                                  ),
                                  const Icon(
                                    Icons.chevron_right_rounded,
                                    color: AppColors.iconMuted,
                                    size: 20,
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      );
                    }),
                    const SizedBox(height: 10),

                    // Botão de Avanço para Escolha do Desembarque
                    ElevatedButton(
                      onPressed: () {
                        HapticFeedback.mediumImpact();
                        widget.onConfirmPickup();
                      },
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.primary,
                        foregroundColor: AppColors.textLight,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(16),
                        ),
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        elevation: 0,
                      ),
                      child: const Text(
                        'Confirmar Ponto de Embarque',
                        style: TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
                      ),
                    ),
                  ]),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  // ==========================================================================
  // PASSO 2: ARRASTO DO MAPA PARA EMBARQUE ESTILO UBER (PINO FIXO)
  // ==========================================================================
  Widget _buildDragMapPickupSheet() {
    return Align(
      alignment: Alignment.bottomCenter,
      child: Padding(
        padding: EdgeInsets.fromLTRB(16, 0, 16, 96 + widget.bottomSafeArea),
        child: Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: AppColors.borderSubtle, width: 1),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.14),
                blurRadius: 20,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  const _OriginOpaqueDotBadge(size: 22),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Ponto de Embarque',
                          style: TextStyle(
                            fontSize: 11.5,
                            fontWeight: FontWeight.w700,
                            color: AppColors.textSecondary,
                          ),
                        ),
                        Text(
                          widget.pickupAddress.isNotEmpty
                              ? widget.pickupAddress
                              : 'Arraste o mapa para definir o embarque...',
                          style: const TextStyle(
                            fontSize: 14.5,
                            fontWeight: FontWeight.w800,
                            color: AppColors.textPrimary,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    onPressed: widget.onCancelFlow,
                    icon: const Icon(
                      Icons.close_rounded,
                      color: AppColors.textSecondary,
                    ),
                    tooltip: 'Cancelar',
                  ),
                ],
              ),
              const SizedBox(height: 16),

              ElevatedButton(
                onPressed: () {
                  HapticFeedback.mediumImpact();
                  widget.onConfirmPickup();
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  foregroundColor: AppColors.textLight,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  elevation: 0,
                ),
                child: const Text(
                  'Confirmar Ponto de Embarque',
                  style: TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // ==========================================================================
  // PASSO 3: BUSCA / SELEÇÃO DO PONTO DE DESEMBARQUE (DESTINO)
  // ==========================================================================
  Widget _buildSearchDestinationSheet() {
    final query = _searchController.text.trim().toLowerCase();
    final autocompleteState = ref.watch(placesAutocompleteProvider);
    final filteredDestinations = campusDestinationSuggestions.where((dest) {
      if (query.isEmpty) return true;
      return dest.title.toLowerCase().contains(query) ||
          dest.subtitle.toLowerCase().contains(query);
    }).toList();

    return DraggableScrollableSheet(
      initialChildSize: 0.54,
      minChildSize: 0.22,
      maxChildSize: 0.90,
      snap: true,
      snapSizes: const [0.34, 0.54, 0.90],
      builder: (context, scrollController) {
        return Container(
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: const BorderRadius.vertical(top: Radius.circular(26)),
            border: Border.all(color: AppColors.borderSubtle, width: 1),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.14),
                blurRadius: 24,
                offset: const Offset(0, -6),
              ),
            ],
          ),
          child: CustomScrollView(
            controller: scrollController,
            slivers: [
              // Alça de arraste
              SliverToBoxAdapter(
                child: Center(
                  child: Container(
                    margin: const EdgeInsets.symmetric(vertical: 10),
                    width: 38,
                    height: 4,
                    decoration: BoxDecoration(
                      color: AppColors.borderMedium,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
              ),

              SliverPadding(
                padding: EdgeInsets.fromLTRB(16, 4, 16, 120 + widget.bottomSafeArea),
                sliver: SliverList(
                  delegate: SliverChildListDelegate([
                    // Cabeçalho com botão Voltar
                    Row(
                      children: [
                        IconButton(
                          onPressed: widget.onCancelFlow,
                          icon: const Icon(
                            Icons.arrow_back_rounded,
                            color: AppColors.textPrimary,
                          ),
                          tooltip: 'Voltar',
                        ),
                        const SizedBox(width: 4),
                        const Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Para onde você vai?',
                              style: TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.w800,
                                color: AppColors.textPrimary,
                                letterSpacing: -0.3,
                              ),
                            ),
                            Text(
                              'Defina o ponto de desembarque',
                              style: TextStyle(
                                fontSize: 12,
                                color: AppColors.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),

                    // Campo de Busca com Autocomplete (debounce de 1s)
                    Container(
                      decoration: BoxDecoration(
                        color: AppColors.neutralLight,
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: AppColors.borderSubtle),
                      ),
                      child: TextField(
                        controller: _searchController,
                        autofocus: false,
                        style: const TextStyle(
                          fontSize: 14.5,
                          fontWeight: FontWeight.w600,
                          color: AppColors.textPrimary,
                        ),
                        decoration: InputDecoration(
                          prefixIcon: const Icon(
                            Icons.pin_drop_rounded,
                            color: AppColors.textPrimary,
                            size: 20,
                          ),
                          suffixIcon: autocompleteState.isLoading
                              ? const FloatingSearchLensIcon(
                                  key: ValueKey('passenger_floating_search_lens'),
                                )
                              : _searchController.text.isNotEmpty
                                  ? IconButton(
                                      icon: const Icon(
                                        Icons.clear_rounded,
                                        size: 18,
                                        color: AppColors.textSecondary,
                                      ),
                                      onPressed: () {
                                        _searchController.clear();
                                        ref.read(placesAutocompleteProvider.notifier).clear();
                                        setState(() {});
                                      },
                                    )
                                  : const Icon(
                                      Icons.search_rounded,
                                      size: 20,
                                      color: AppColors.textMuted,
                                    ),
                          hintText: 'Digite o endereço ou destino...',
                          hintStyle: const TextStyle(
                            fontSize: 13.5,
                            color: AppColors.textPlaceholder,
                          ),
                          border: InputBorder.none,
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: 14,
                            vertical: 14,
                          ),
                        ),
                        onChanged: (text) {
                          setState(() {});
                          ref.read(placesAutocompleteProvider.notifier).onQueryChanged(text);
                        },
                      ),
                    ),
                    const SizedBox(height: 12),

                    // Botão: Definir desembarque no mapa
                    Material(
                      color: AppColors.cardBackground,
                      borderRadius: BorderRadius.circular(16),
                      child: InkWell(
                        borderRadius: BorderRadius.circular(16),
                        onTap: () {
                          HapticFeedback.lightImpact();
                          widget.onStartDragMapDestination();
                        },
                        child: Container(
                          padding: const EdgeInsets.symmetric(vertical: 13, horizontal: 14),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(color: AppColors.borderSubtle),
                          ),
                          child: const Row(
                            children: [
                              Icon(
                                Icons.map_rounded,
                                color: AppColors.primary,
                                size: 22,
                              ),
                              SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      'Definir desembarque no mapa',
                                      style: TextStyle(
                                        fontSize: 14,
                                        fontWeight: FontWeight.w700,
                                        color: AppColors.textPrimary,
                                      ),
                                    ),
                                    Text(
                                      'Arraste o mapa para posicionar a chegada',
                                      style: TextStyle(
                                        fontSize: 11.5,
                                        color: AppColors.textSecondary,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              Icon(
                                Icons.chevron_right_rounded,
                                color: AppColors.iconMuted,
                                size: 20,
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),

                    if (autocompleteState.suggestions.isNotEmpty) ...[
                      const SizedBox(height: 16),
                      const Text(
                        'Sugestões de Endereço',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w800,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 8),
                      ...autocompleteState.suggestions.map((suggestion) {
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 8),
                          child: Material(
                            color: AppColors.cardBackground,
                            borderRadius: BorderRadius.circular(14),
                            child: InkWell(
                              borderRadius: BorderRadius.circular(14),
                              onTap: () => _handleDestinationSuggestionSelected(suggestion),
                              child: Container(
                                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(14),
                                  border: Border.all(color: AppColors.borderSubtle),
                                ),
                                child: Row(
                                  children: [
                                    const Icon(
                                      Icons.location_on_rounded,
                                      color: AppColors.primary,
                                      size: 20,
                                    ),
                                    const SizedBox(width: 12),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            suggestion.mainText,
                                            style: const TextStyle(
                                              fontSize: 14,
                                              fontWeight: FontWeight.w700,
                                              color: AppColors.textPrimary,
                                            ),
                                          ),
                                          if (suggestion.secondaryText.isNotEmpty) ...[
                                            const SizedBox(height: 2),
                                            Text(
                                              suggestion.secondaryText,
                                              style: const TextStyle(
                                                fontSize: 12,
                                                color: AppColors.textSecondary,
                                              ),
                                              maxLines: 1,
                                              overflow: TextOverflow.ellipsis,
                                            ),
                                          ],
                                        ],
                                      ),
                                    ),
                                    const Icon(
                                      Icons.chevron_right_rounded,
                                      color: AppColors.iconMuted,
                                      size: 20,
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        );
                      }),
                    ] else ...[
                      const SizedBox(height: 16),
                      const Text(
                        'Destinos Frequentes',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w800,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 8),
                      ...filteredDestinations.map((dest) {
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 8),
                          child: Material(
                            color: AppColors.cardBackground,
                            borderRadius: BorderRadius.circular(14),
                            child: InkWell(
                              borderRadius: BorderRadius.circular(14),
                              onTap: () {
                                HapticFeedback.lightImpact();
                                widget.onDestinationChosen(dest.title, dest.position);
                              },
                              child: Container(
                                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(14),
                                  border: Border.all(color: AppColors.borderSubtle),
                                ),
                                child: Row(
                                  children: [
                                    const _CheckeredCircleBadge(size: 18),
                                    const SizedBox(width: 12),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            dest.title,
                                            style: const TextStyle(
                                              fontSize: 14,
                                              fontWeight: FontWeight.w700,
                                              color: AppColors.textPrimary,
                                            ),
                                          ),
                                          const SizedBox(height: 2),
                                          Text(
                                            dest.subtitle,
                                            style: const TextStyle(
                                              fontSize: 12,
                                              color: AppColors.textSecondary,
                                            ),
                                            maxLines: 1,
                                            overflow: TextOverflow.ellipsis,
                                          ),
                                        ],
                                      ),
                                    ),
                                    const Icon(
                                      Icons.chevron_right_rounded,
                                      color: AppColors.iconMuted,
                                      size: 20,
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        );
                      }),
                    ],
                  ]),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

    // ==========================================================================
  // PASSO 4: ARRASTO DO MAPA PARA DESEMBARQUE ESTILO UBER (PINO FIXO)
  // ==========================================================================
  Widget _buildDragMapDestinationSheet() {
    return Align(
      alignment: Alignment.bottomCenter,
      child: Padding(
        padding: EdgeInsets.fromLTRB(16, 0, 16, 96 + widget.bottomSafeArea),
        child: Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: AppColors.borderSubtle, width: 1),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.14),
                blurRadius: 20,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  const _CheckeredCircleBadge(size: 22),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Ponto de Desembarque (Chegada)',
                          style: TextStyle(
                            fontSize: 11.5,
                            fontWeight: FontWeight.w700,
                            color: AppColors.textSecondary,
                          ),
                        ),
                        Text(
                          widget.destinationAddress.isNotEmpty
                              ? widget.destinationAddress
                              : 'Arraste o mapa até o destino...',
                          style: const TextStyle(
                            fontSize: 14.5,
                            fontWeight: FontWeight.w800,
                            color: AppColors.textPrimary,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    onPressed: widget.onCancelFlow,
                    icon: const Icon(
                      Icons.close_rounded,
                      color: AppColors.textSecondary,
                    ),
                    tooltip: 'Cancelar',
                  ),
                ],
              ),
              const SizedBox(height: 16),

              ElevatedButton(
                onPressed: () {
                  HapticFeedback.mediumImpact();
                  widget.onConfirmDestination();
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  foregroundColor: AppColors.textLight,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  elevation: 0,
                ),
                child: const Text(
                  'Confirmar Ponto de Desembarque',
                  style: TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // ==========================================================================
  // PASSO 5: ROUTE CONFIRMATION (Resumo da viagem pelas ruas de carro)
  // ==========================================================================
  Widget _buildRouteConfirmationSheet() {
    return Align(
      alignment: Alignment.bottomCenter,
      child: Padding(
        padding: EdgeInsets.fromLTRB(16, 0, 16, 96 + widget.bottomSafeArea),
        child: Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: AppColors.borderSubtle, width: 1),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.14),
                blurRadius: 24,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    'Confirmar Pedido de Carona',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w800,
                      color: AppColors.textPrimary,
                      letterSpacing: -0.2,
                    ),
                  ),
                  IconButton(
                    tooltip: 'Salvar este trajeto frequente',
                    onPressed: widget.onSaveRoute,
                    style: IconButton.styleFrom(
                      backgroundColor: AppColors.neutralLight,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    icon: const Icon(
                      Icons.bookmark_add_rounded,
                      size: 19,
                      color: AppColors.textPrimary,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),

              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: AppColors.neutralLight,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: AppColors.borderSubtle),
                ),
                child: Column(
                  children: [
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Padding(
                          padding: EdgeInsets.only(top: 3),
                          child: _OriginOpaqueDotBadge(size: 16),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                'Origem (Embarque)',
                                style: TextStyle(
                                  fontSize: 11,
                                  fontWeight: FontWeight.w700,
                                  color: AppColors.textSecondary,
                                ),
                              ),
                              Text(
                                widget.pickupAddress,
                                style: const TextStyle(
                                  fontSize: 13.5,
                                  fontWeight: FontWeight.w700,
                                  color: AppColors.textPrimary,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const Padding(
                      padding: EdgeInsets.symmetric(horizontal: 8, vertical: 8),
                      child: Divider(
                        height: 1,
                        thickness: 1,
                        color: AppColors.borderSubtle,
                      ),
                    ),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Padding(
                          padding: EdgeInsets.only(top: 3),
                          child: _CheckeredCircleBadge(size: 16),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                'Destino (Chegada)',
                                style: TextStyle(
                                  fontSize: 11,
                                  fontWeight: FontWeight.w700,
                                  color: AppColors.textSecondary,
                                ),
                              ),
                              Text(
                                widget.destinationAddress,
                                style: const TextStyle(
                                  fontSize: 13.5,
                                  fontWeight: FontWeight.w700,
                                  color: AppColors.textPrimary,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              ElevatedButton(
                onPressed: () {
                  HapticFeedback.mediumImpact();
                  widget.onConfirmRideOrder();
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  foregroundColor: AppColors.textLight,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  elevation: 0,
                ),
                child: const Text(
                  'Confirmar Pedido de Carona',
                  style: TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
                ),
              ),
              const SizedBox(height: 8),

              TextButton(
                onPressed: widget.onCancelFlow,
                style: TextButton.styleFrom(
                  foregroundColor: AppColors.textSecondary,
                  padding: const EdgeInsets.symmetric(vertical: 6),
                ),
                child: const Text(
                  'Cancelar e alterar rota',
                  style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // ==========================================================================
  // PASSO 6: LISTA DE MOTORISTAS DISPONÍVEIS
  // ==========================================================================
  Widget _buildDriverListSheet() {
    final matchState = ref.watch(passengerMatchProvider);

    return DraggableScrollableSheet(
      initialChildSize: 0.5,
      minChildSize: 0.16,
      maxChildSize: 0.85,
      snap: true,
      snapSizes: const [0.28, 0.5, 0.85],
      builder: (context, scrollController) {
        return Container(
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: const BorderRadius.vertical(top: Radius.circular(26)),
            border: Border.all(color: AppColors.borderSubtle, width: 1),
          ),
          child: CustomScrollView(
            controller: scrollController,
            slivers: [
              SliverToBoxAdapter(
                child: Center(
                  child: Container(
                    margin: const EdgeInsets.symmetric(vertical: 10),
                    width: 38,
                    height: 4,
                    decoration: BoxDecoration(
                      color: AppColors.borderMedium,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
              ),
              SliverPadding(
                padding: EdgeInsets.fromLTRB(16, 4, 16, 120 + widget.bottomSafeArea),
                sliver: SliverList(
                  delegate: SliverChildListDelegate([
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Motoristas Compatíveis',
                                style: TextStyle(
                                  fontSize: 17,
                                  fontWeight: FontWeight.w800,
                                  color: AppColors.textPrimary,
                                ),
                              ),
                              SizedBox(height: 2),
                              Text(
                                'Ordenados pela compatibilidade com o seu trajeto',
                                style: TextStyle(
                                  fontSize: 12,
                                  color: AppColors.textSecondary,
                                ),
                              ),
                            ],
                          ),
                        ),
                        IconButton(
                          icon: const Icon(Icons.refresh_rounded),
                          onPressed: matchState.phase == PassengerMatchPhase.loading ||
                                  matchState.isBusy
                              ? null
                              : widget.onRetryMatches,
                          tooltip: 'Buscar de novo',
                        ),
                        IconButton(
                          icon: const Icon(Icons.close_rounded),
                          onPressed: widget.onCancelFlow,
                          tooltip: 'Fechar',
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),
                    ..._buildDriverListContent(matchState),
                  ]),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  List<Widget> _buildDriverListContent(PassengerMatchState matchState) {
    final phase = matchState.phase;
    final drivers = matchState.matches;

    if (phase == PassengerMatchPhase.loading || phase == PassengerMatchPhase.idle) {
      return const [
        Padding(
          padding: EdgeInsets.all(28),
          child: Center(
            child: CircularProgressIndicator(color: AppColors.primary),
          ),
        ),
      ];
    }

    if (phase == PassengerMatchPhase.failed) {
      return [
        _buildDriverListMessage(
          matchState.errorMessage ?? 'Não foi possível buscar os motoristas.',
        ),
      ];
    }

    if (drivers.isEmpty) {
      return [
        _buildDriverListMessage(
          'Nenhum motorista compatível com o seu trajeto agora. '
          'Tente outro horário ou toque em atualizar mais tarde.',
        ),
      ];
    }

    final selected = matchState.selected;
    return [
      for (final driver in drivers)
        Padding(
          padding: const EdgeInsets.only(bottom: 10),
          child: MatchDriverCard(
            match: driver,
            isRequesting: phase == PassengerMatchPhase.requesting &&
                selected?.driverRouteId == driver.driverRouteId,
            isDisabled: matchState.isBusy,
            onRequest: () => widget.onSelectDriver(driver),
          ),
        ),
    ];
  }

  Widget _buildDriverListMessage(String text) {
    return Container(
      padding: const EdgeInsets.all(20),
      alignment: Alignment.center,
      child: Text(
        text,
        textAlign: TextAlign.center,
        style: const TextStyle(
          fontSize: 14,
          color: AppColors.textSecondary,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }

  // ==========================================================================
  // PASSO 7: AGUARDANDO O MOTORISTA ACEITAR OU RECUSAR O PEDIDO DE VAGA
  // ==========================================================================
  Widget _buildWaitingDriverSheet() {
    final matchState = ref.watch(passengerMatchProvider);

    return Align(
      alignment: Alignment.bottomCenter,
      child: Padding(
        padding: EdgeInsets.fromLTRB(16, 0, 16, 96 + widget.bottomSafeArea),
        child: MatchWaitingCard(
          match: matchState.selected,
          isCancelling: matchState.isCancelling,
          onCancel: widget.onCancelRequest,
        ),
      ),
    );
  }

  // ==========================================================================
  // PASSO 8: MOTORISTA ACEITOU! (MODAL ANIMADO COM RECOLHIMENTO PARA BUBBLE PIP)
  // ==========================================================================
}

class _DriverAcceptedPipSheet extends StatefulWidget {
  final MatchModel? driver;
  final VoidCallback onSendMessage;
  final VoidCallback onCancel;
  final double bottomSafeArea;

  const _DriverAcceptedPipSheet({
    required this.driver,
    required this.onSendMessage,
    required this.onCancel,
    required this.bottomSafeArea,
  });

  @override
  State<_DriverAcceptedPipSheet> createState() => _DriverAcceptedPipSheetState();
}

class _DriverAcceptedPipSheetState extends State<_DriverAcceptedPipSheet>
    with SingleTickerProviderStateMixin {
  late final AnimationController _animController;
  late final Animation<double> _slideAnimation;
  late final Animation<double> _fadeAnimation;
  late final Animation<double> _scaleAnimation;

  bool _isCollapsed = false;
  bool _manuallyOpened = false;
  Offset? _bubblePosition;

  @override
  void initState() {
    super.initState();
    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 500),
    );

    _slideAnimation = Tween<double>(begin: 60.0, end: 0.0).animate(
      CurvedAnimation(parent: _animController, curve: Curves.easeOutCubic),
    );
    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _animController, curve: Curves.easeOut),
    );
    _scaleAnimation = Tween<double>(begin: 0.92, end: 1.0).animate(
      CurvedAnimation(parent: _animController, curve: Curves.easeOutBack),
    );

    _animController.forward();

    // Aguarda 3 segundos após a confirmação e transforma suavemente o modal
    // em um ponto flutuante à direita da tela
    Future.delayed(const Duration(seconds: 3), () {
      if (mounted && !_manuallyOpened && !_isCollapsed) {
        setState(() {
          _isCollapsed = true;
        });
      }
    });
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_bubblePosition == null) {
      final size = MediaQuery.sizeOf(context);
      _bubblePosition = Offset(size.width - 76, size.height * 0.40);
    }
  }

  @override
  void dispose() {
    _animController.dispose();
    super.dispose();
  }

  Offset _clampPosition(Offset raw, Size size) {
    const bubbleSize = 58.0;
    const padding = 16.0;
    final topLimit = MediaQuery.paddingOf(context).top + 40;
    final bottomLimit = size.height - 120 - widget.bottomSafeArea;

    final dx = raw.dx.clamp(padding, size.width - bubbleSize - padding);
    final dy = raw.dy.clamp(topLimit, bottomLimit);
    return Offset(dx, dy);
  }

  @override
  Widget build(BuildContext context) {
    final screenSize = MediaQuery.sizeOf(context);
    final pos = _bubblePosition ?? Offset(screenSize.width - 76, screenSize.height * 0.40);

    return Stack(
      children: [
        // 1. PONTO FLUTUANTE À DIREITA (DRAGGABLE PiP BUBBLE)
        AnimatedPositioned(
          duration: const Duration(milliseconds: 320),
          curve: Curves.easeOutBack,
          left: pos.dx,
          top: pos.dy,
          child: AnimatedScale(
            scale: _isCollapsed ? 1.0 : 0.0,
            duration: const Duration(milliseconds: 280),
            curve: Curves.easeInOutBack,
            child: AnimatedOpacity(
              opacity: _isCollapsed ? 1.0 : 0.0,
              duration: const Duration(milliseconds: 220),
              child: _isCollapsed
                  ? GestureDetector(
                      onPanUpdate: (details) {
                        setState(() {
                          _bubblePosition = _clampPosition(
                            pos + details.delta,
                            screenSize,
                          );
                        });
                      },
                      onTap: () {
                        HapticFeedback.mediumImpact();
                        setState(() {
                          _isCollapsed = false;
                          _manuallyOpened = true;
                        });
                      },
                      child: Container(
                        width: 58,
                        height: 58,
                        decoration: BoxDecoration(
                          color: AppColors.dockBackground,
                          shape: BoxShape.circle,
                          border: Border.all(color: Colors.white, width: 2),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withValues(alpha: 0.35),
                              blurRadius: 18,
                              offset: const Offset(0, 6),
                            ),
                          ],
                        ),
                        child: Stack(
                          alignment: Alignment.center,
                          children: [
                            const Icon(
                              Icons.directions_car_rounded,
                              color: Colors.white,
                              size: 28,
                            ),
                            // Indicador verde pulsante de corrida em andamento
                            Positioned(
                              top: 4,
                              right: 4,
                              child: Container(
                                width: 12,
                                height: 12,
                                decoration: BoxDecoration(
                                  color: AppColors.success,
                                  shape: BoxShape.circle,
                                  border: Border.all(color: Colors.white, width: 2),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    )
                  : const SizedBox.shrink(),
            ),
          ),
        ),

        // 2. MODAL EXPANDIDO COMPLETO (COM ANIMAÇÃO DE ENTRADA E RETRAÇÃO)
        Align(
          alignment: Alignment.bottomCenter,
          child: AnimatedSlide(
            offset: _isCollapsed ? const Offset(0, 1.2) : Offset.zero,
            duration: const Duration(milliseconds: 380),
            curve: Curves.easeInOutCubic,
            child: AnimatedOpacity(
              opacity: _isCollapsed ? 0.0 : 1.0,
              duration: const Duration(milliseconds: 280),
              child: AnimatedBuilder(
                animation: _animController,
                builder: (context, child) {
                  return Transform.translate(
                    offset: Offset(0, _slideAnimation.value),
                    child: Transform.scale(
                      scale: _scaleAnimation.value,
                      child: Opacity(
                        opacity: _fadeAnimation.value,
                        child: child,
                      ),
                    ),
                  );
                },
                child: Padding(
                  padding: EdgeInsets.fromLTRB(16, 0, 16, 96 + widget.bottomSafeArea),
                  child: Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: AppColors.cardBackground,
                      borderRadius: BorderRadius.circular(26),
                      border: Border.all(color: AppColors.borderSubtle, width: 1),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withValues(alpha: 0.18),
                          blurRadius: 28,
                          offset: const Offset(0, 8),
                        ),
                      ],
                    ),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(10),
                              decoration: BoxDecoration(
                                color: AppColors.successBackground,
                                shape: BoxShape.circle,
                                border: Border.all(color: AppColors.successBorder),
                              ),
                              child: const Icon(
                                Icons.check_rounded,
                                color: AppColors.success,
                                size: 24,
                              ),
                            ),
                            const SizedBox(width: 14),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  const Text(
                                    'Carona Confirmada!',
                                    style: TextStyle(
                                      fontSize: 16,
                                      fontWeight: FontWeight.w800,
                                      color: AppColors.textPrimary,
                                    ),
                                  ),
                                  Text(
                                    widget.driver != null
                                        ? '${widget.driver!.otherUserName} aceitou o seu pedido'
                                        : 'O motorista aceitou o seu pedido',
                                    style: const TextStyle(
                                      fontSize: 12.5,
                                      color: AppColors.textSecondary,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            IconButton(
                              onPressed: () {
                                HapticFeedback.lightImpact();
                                setState(() {
                                  _isCollapsed = true;
                                });
                              },
                              icon: const Icon(
                                Icons.close_fullscreen_rounded,
                                size: 20,
                                color: AppColors.textSecondary,
                              ),
                              tooltip: 'Minimizar para botão flutuante',
                            ),
                          ],
                        ),
                        const SizedBox(height: 14),

                        if (widget.driver != null) ...[
                          Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: AppColors.neutralLight,
                              borderRadius: BorderRadius.circular(14),
                            ),
                            child: Row(
                              children: [
                                const Icon(
                                  Icons.directions_car_rounded,
                                  size: 20,
                                  color: AppColors.primary,
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Text(
                                    widget.driver!.vehicle?.description ??
                                        'Sai às ${widget.driver!.departureTime}',
                                    style: const TextStyle(
                                      fontSize: 13,
                                      fontWeight: FontWeight.w700,
                                      color: AppColors.textPrimary,
                                    ),
                                  ),
                                ),
                                if (widget.driver!.estimatedCostPerPassenger != null)
                                  Text(
                                    CurrencyFormatter.format(
                                      widget.driver!.estimatedCostPerPassenger!,
                                    ),
                                    style: const TextStyle(
                                      fontSize: 15,
                                      fontWeight: FontWeight.w800,
                                      color: AppColors.primary,
                                    ),
                                  ),
                              ],
                            ),
                          ),
                          const SizedBox(height: 16),
                        ],

                        ElevatedButton.icon(
                          onPressed: widget.onSendMessage,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AppColors.primary,
                            foregroundColor: AppColors.textLight,
                            padding: const EdgeInsets.symmetric(vertical: 14),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(16),
                            ),
                            elevation: 0,
                          ),
                          icon: const Icon(Icons.chat_bubble_rounded, size: 19),
                          label: const Text(
                            'Enviar mensagem ao motorista',
                            style: TextStyle(fontSize: 14.5, fontWeight: FontWeight.w700),
                          ),
                        ),
                        const SizedBox(height: 8),

                        TextButton(
                          onPressed: widget.onCancel,
                          style: TextButton.styleFrom(
                            foregroundColor: AppColors.textSecondary,
                            padding: const EdgeInsets.symmetric(vertical: 8),
                          ),
                          child: const Text(
                            'Fechar',
                            style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}


/// Mini bolinha preta opaca para o resumo de rota
class _OriginOpaqueDotBadge extends StatelessWidget {
  final double size;
  const _OriginOpaqueDotBadge({this.size = 16});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: AppColors.primary,
        shape: BoxShape.circle,
        border: Border.all(color: Colors.white, width: 1.5),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.18),
            blurRadius: 3,
            offset: const Offset(0, 1),
          ),
        ],
      ),
    );
  }
}

/// Mini bolinha quadriculada para o resumo de rota (remetendo à chegada)
class _CheckeredCircleBadge extends StatelessWidget {
  final double size;
  const _CheckeredCircleBadge({this.size = 16});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        border: Border.all(color: AppColors.primary, width: 1.2),
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
