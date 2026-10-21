import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../../core/config/styles.dart';
import '../../../ride/data/models/mock_driver_model.dart';

/// ============================================================================
/// COMPONENTE: HomePassengerSheet (Fluxo de Solicitação do Passageiro)
/// ============================================================================
/// Ordem do fluxo invertida conforme solicitado:
/// 1. Primeiro: Seleção do Ponto de Embarque (Origem)
/// 2. Segundo: Seleção do Ponto de Desembarque (Destino)
/// 3. Terceiro: Confirmação de Rota (com resumo Origem e Destino)
/// 4. Quarto: Lista de Motoristas Disponíveis
/// 5. Quinto: Aguardando Aceite (Simulação)
/// 6. Sexto: Corrida Aceita (Trajeto de aproximação e Chat)
/// ============================================================================
class HomePassengerSheet extends StatefulWidget {
  final PassengerRideStep step;
  final String pickupAddress;
  final String destinationAddress;
  final LatLng pickupLocation;
  final LatLng? destinationLocation;
  final List<MockNearbyDriver> availableDrivers;
  final MockNearbyDriver? selectedDriver;

  final VoidCallback onStartRideFlow;
  final VoidCallback onStartDragMapPickup;
  final VoidCallback onStartDragMapDestination;
  final void Function(String title, LatLng position) onPickupChosen;
  final void Function(String title, LatLng position) onDestinationChosen;
  final VoidCallback onConfirmPickup;
  final VoidCallback onConfirmDestination;
  final VoidCallback onConfirmRideOrder;
  final ValueChanged<MockNearbyDriver> onSelectDriver;
  final ValueChanged<bool> onSimulateDriverResponse;
  final VoidCallback onSendMessageToDriver;
  final VoidCallback onCancelFlow;
  final VoidCallback onSaveRoute;
  final double bottomSafeArea;

  const HomePassengerSheet({
    super.key,
    required this.step,
    required this.pickupAddress,
    required this.destinationAddress,
    required this.pickupLocation,
    this.destinationLocation,
    this.availableDrivers = const [],
    this.selectedDriver,
    required this.onStartRideFlow,
    required this.onStartDragMapPickup,
    required this.onStartDragMapDestination,
    required this.onPickupChosen,
    required this.onDestinationChosen,
    required this.onConfirmPickup,
    required this.onConfirmDestination,
    required this.onConfirmRideOrder,
    required this.onSelectDriver,
    required this.onSimulateDriverResponse,
    required this.onSendMessageToDriver,
    required this.onCancelFlow,
    required this.onSaveRoute,
    this.bottomSafeArea = 0,
  });

  @override
  State<HomePassengerSheet> createState() => _HomePassengerSheetState();
}

class _HomePassengerSheetState extends State<HomePassengerSheet> {
  final _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    switch (widget.step) {
      case PassengerRideStep.idle:
        return const SizedBox.shrink();
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
          driver: widget.selectedDriver,
          onSendMessage: widget.onSendMessageToDriver,
          onCancel: widget.onCancelFlow,
          bottomSafeArea: widget.bottomSafeArea,
        );
    }
  }

  // ==========================================================================
  // PASSO 1: SELEÇÃO DO PONTO DE EMBARQUE (ORIGEM)
  // ==========================================================================
  Widget _buildSelectPickupSheet() {
    return DraggableScrollableSheet(
      initialChildSize: 0.54,
      minChildSize: 0.34,
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
                    ...mockPickupSuggestionsList.map((spot) {
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
    final filteredDestinations = mockDestinationsList.where((dest) {
      if (query.isEmpty) return true;
      return dest.title.toLowerCase().contains(query) ||
          dest.subtitle.toLowerCase().contains(query);
    }).toList();

    return DraggableScrollableSheet(
      initialChildSize: 0.54,
      minChildSize: 0.34,
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
                    // Pill indicando o embarque já definido
                    GestureDetector(
                      onTap: () {
                        HapticFeedback.lightImpact();
                        widget.onStartRideFlow(); // Volta para trocar o embarque
                      },
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                        decoration: BoxDecoration(
                          color: AppColors.neutralLight,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: AppColors.borderSubtle),
                        ),
                        child: Row(
                          children: [
                            const _OriginOpaqueDotBadge(size: 14),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                'Embarque: ${widget.pickupAddress}',
                                style: const TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.w600,
                                  color: AppColors.textPrimary,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            const Text(
                              'Alterar',
                              style: TextStyle(
                                fontSize: 11.5,
                                fontWeight: FontWeight.w700,
                                color: AppColors.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),

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

                    // Campo de Busca
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
                            Icons.search_rounded,
                            color: AppColors.textSecondary,
                            size: 22,
                          ),
                          suffixIcon: _searchController.text.isNotEmpty
                              ? IconButton(
                                  icon: const Icon(Icons.clear_rounded, size: 18),
                                  onPressed: () {
                                    _searchController.clear();
                                    setState(() {});
                                  },
                                )
                              : null,
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
                        onChanged: (_) => setState(() {}),
                      ),
                    ),
                    const SizedBox(height: 12),

                    // Botão: "Definir desembarque no mapa"
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

                    // Lista de Sugestões de Destino
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
    final drivers = widget.availableDrivers;

    return DraggableScrollableSheet(
      initialChildSize: 0.44,
      minChildSize: 0.28,
      maxChildSize: 0.78,
      snap: true,
      snapSizes: const [0.28, 0.44, 0.78],
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
                        const Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Motoristas Disponíveis',
                              style: TextStyle(
                                fontSize: 17,
                                fontWeight: FontWeight.w800,
                                color: AppColors.textPrimary,
                              ),
                            ),
                            SizedBox(height: 2),
                            Text(
                              'Toque no motorista para solicitar a carona',
                              style: TextStyle(
                                fontSize: 12,
                                color: AppColors.textSecondary,
                              ),
                            ),
                          ],
                        ),
                        IconButton(
                          icon: const Icon(Icons.close_rounded),
                          onPressed: widget.onCancelFlow,
                          tooltip: 'Cancelar pedido',
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),

                    if (drivers.isEmpty)
                      Container(
                        padding: const EdgeInsets.all(24),
                        alignment: Alignment.center,
                        child: const Text(
                          'Nenhum outro motorista disponível no momento.',
                          style: TextStyle(
                            fontSize: 14,
                            color: AppColors.textSecondary,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      )
                    else
                      ...drivers.map((driver) {
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 10),
                          child: Material(
                            color: AppColors.cardBackground,
                            borderRadius: BorderRadius.circular(18),
                            child: InkWell(
                              borderRadius: BorderRadius.circular(18),
                              onTap: () {
                                HapticFeedback.mediumImpact();
                                widget.onSelectDriver(driver);
                              },
                              child: Container(
                                padding: const EdgeInsets.all(14),
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(18),
                                  border: Border.all(color: AppColors.borderSubtle),
                                ),
                                child: Row(
                                  children: [
                                    const CircleAvatar(
                                      radius: 24,
                                      backgroundColor: AppColors.neutralLight,
                                      child: Icon(
                                        Icons.person_rounded,
                                        color: AppColors.primary,
                                        size: 26,
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
                                              fontSize: 15,
                                              fontWeight: FontWeight.w700,
                                              color: AppColors.textPrimary,
                                            ),
                                          ),
                                          const SizedBox(height: 2),
                                          Text(
                                            '${driver.vehicleModel} • ${driver.licensePlate}',
                                            style: const TextStyle(
                                              fontSize: 12.5,
                                              color: AppColors.textSecondary,
                                            ),
                                          ),
                                          const SizedBox(height: 4),
                                          Row(
                                            children: [
                                              const Icon(
                                                Icons.star_rounded,
                                                size: 15,
                                                color: Colors.amber,
                                              ),
                                              const SizedBox(width: 4),
                                              Text(
                                                driver.rating.toStringAsFixed(1),
                                                style: const TextStyle(
                                                  fontSize: 12,
                                                  fontWeight: FontWeight.w700,
                                                  color: AppColors.textPrimary,
                                                ),
                                              ),
                                              const SizedBox(width: 10),
                                              Text(
                                                '${driver.availableSeats} vagas',
                                                style: const TextStyle(
                                                  fontSize: 12,
                                                  color: AppColors.textSecondary,
                                                ),
                                              ),
                                            ],
                                          ),
                                        ],
                                      ),
                                    ),
                                    Column(
                                      crossAxisAlignment: CrossAxisAlignment.end,
                                      children: [
                                        Text(
                                          'R\$ ${driver.contributionAmount.toStringAsFixed(2)}',
                                          style: const TextStyle(
                                            fontSize: 16,
                                            fontWeight: FontWeight.w800,
                                            color: AppColors.primary,
                                          ),
                                        ),
                                        const SizedBox(height: 4),
                                        Container(
                                          padding: const EdgeInsets.symmetric(
                                            horizontal: 10,
                                            vertical: 5,
                                          ),
                                          decoration: BoxDecoration(
                                            color: AppColors.primary,
                                            borderRadius: BorderRadius.circular(10),
                                          ),
                                          child: const Text(
                                            'Pedir',
                                            style: TextStyle(
                                              fontSize: 12,
                                              fontWeight: FontWeight.w700,
                                              color: AppColors.textLight,
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

  // ==========================================================================
  // PASSO 7: AGUARDANDO ACEITE DO MOTORISTA (COM BOTÕES DE SIMULAÇÃO)
  // ==========================================================================
  Widget _buildWaitingDriverSheet() {
    final driver = widget.selectedDriver;

    return Align(
      alignment: Alignment.bottomCenter,
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
                color: Colors.black.withValues(alpha: 0.16),
                blurRadius: 26,
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
                  const SizedBox(
                    width: 22,
                    height: 22,
                    child: CircularProgressIndicator(
                      strokeWidth: 2.5,
                      color: AppColors.primary,
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Aguardando o motorista...',
                          style: TextStyle(
                            fontSize: 15.5,
                            fontWeight: FontWeight.w800,
                            color: AppColors.textPrimary,
                          ),
                        ),
                        Text(
                          driver != null
                              ? 'Solicitação enviada para ${driver.name}'
                              : 'Conectando ao veículo...',
                          style: const TextStyle(
                            fontSize: 12.5,
                            color: AppColors.textSecondary,
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    onPressed: widget.onCancelFlow,
                    icon: const Icon(Icons.close_rounded),
                    tooltip: 'Cancelar solicitação',
                  ),
                ],
              ),
              const SizedBox(height: 18),

              // Botões explícitos de teste/simulação de aceite ou recusa
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.neutralLight,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: AppColors.borderSubtle),
                ),
                child: Column(
                  children: [
                    const Text(
                      'SIMULAÇÃO DE RESPOSTA DO MOTORISTA:',
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w800,
                        color: AppColors.textSecondary,
                        letterSpacing: 0.4,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Row(
                      children: [
                        Expanded(
                          child: ElevatedButton.icon(
                            style: ElevatedButton.styleFrom(
                              backgroundColor: AppColors.success,
                              foregroundColor: AppColors.textLight,
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                              padding: const EdgeInsets.symmetric(vertical: 10),
                              elevation: 0,
                            ),
                            onPressed: () => widget.onSimulateDriverResponse(true),
                            icon: const Icon(Icons.check_rounded, size: 18),
                            label: const Text(
                              'Aceitar',
                              style: TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: ElevatedButton.icon(
                            style: ElevatedButton.styleFrom(
                              backgroundColor: AppColors.error,
                              foregroundColor: AppColors.textLight,
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                              padding: const EdgeInsets.symmetric(vertical: 10),
                              elevation: 0,
                            ),
                            onPressed: () => widget.onSimulateDriverResponse(false),
                            icon: const Icon(Icons.close_rounded, size: 18),
                            label: const Text(
                              'Recusar',
                              style: TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // ==========================================================================
  // PASSO 8: MOTORISTA ACEITOU! (MODAL ANIMADO COM RECOLHIMENTO PARA BUBBLE PIP)
  // ==========================================================================
}

class _DriverAcceptedPipSheet extends StatefulWidget {
  final MockNearbyDriver? driver;
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
                                        ? '${widget.driver!.name} está a caminho'
                                        : 'Motorista a caminho',
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
                                    '${widget.driver!.vehicleModel} • Placa ${widget.driver!.licensePlate}',
                                    style: const TextStyle(
                                      fontSize: 13,
                                      fontWeight: FontWeight.w700,
                                      color: AppColors.textPrimary,
                                    ),
                                  ),
                                ),
                                Text(
                                  'R\$ ${widget.driver!.contributionAmount.toStringAsFixed(2)}',
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
                            'Cancelar carona',
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
