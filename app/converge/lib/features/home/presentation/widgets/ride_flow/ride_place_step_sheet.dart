// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../../../core/config/styles.dart';
import '../../../../../core/widgets/show_app_notice.dart';
import '../../../../route/data/models/place_suggestion_model.dart';
import '../../../../route/state/places_autocomplete_controller.dart';
import '../campus_spots.dart';
import '../destination_dot_badge.dart';
import '../floating_search_lens_icon.dart';
import '../origin_dot_badge.dart';
import 'ride_flow_header.dart';
import 'ride_flow_sheet_frame.dart';

/// Etapa 1 (partida) ou 2 (destino) de criar ou pedir carona, com o mesmo
/// layout: "Selecionar no mapa" em destaque e, embaixo, o campo para digitar o
/// endereço (com a busca do Google e a lupa animada). Escolher avança sozinho.
class RidePlaceStepSheet extends ConsumerStatefulWidget {
  /// Partida (true) ou destino final (false).
  final bool isOrigin;
  final bool isDriver;

  /// Endereço já escolhido nesta etapa (vazio se ainda não há).
  final String selectedAddress;

  final VoidCallback onPickOnMap;
  final void Function(String address, LatLng position) onPlaceChosen;

  /// Segue com o endereço já escolhido (ao voltar para esta etapa).
  final VoidCallback onContinue;

  /// Etapa anterior; nulo na primeira, que só fecha.
  final VoidCallback? onBack;
  final VoidCallback onClose;
  final double bottomSafeArea;

  const RidePlaceStepSheet({
    super.key,
    required this.isOrigin,
    required this.isDriver,
    required this.selectedAddress,
    required this.onPickOnMap,
    required this.onPlaceChosen,
    required this.onContinue,
    required this.onClose,
    this.onBack,
    this.bottomSafeArea = 0,
  });

  @override
  ConsumerState<RidePlaceStepSheet> createState() => _RidePlaceStepSheetState();
}

class _RidePlaceStepSheetState extends ConsumerState<RidePlaceStepSheet> {
  final _searchController = TextEditingController();

  // Buscando os detalhes da sugestão tocada: as outras ficam desabilitadas
  bool _resolving = false;

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  String get _title =>
      widget.isOrigin ? 'De onde você sai?' : 'Para onde você vai?';

  String get _subtitle {
    if (!widget.isOrigin) return 'O destino final da viagem.';
    return widget.isDriver
        ? 'O ponto de partida da sua carona.'
        : 'Onde o motorista vai te buscar.';
  }

  Future<void> _chooseSuggestion(PlaceSuggestionModel suggestion) async {
    if (_resolving) return;
    HapticFeedback.lightImpact();
    FocusScope.of(context).unfocus();
    setState(() => _resolving = true);
    final details = await ref
        .read(placesAutocompleteProvider.notifier)
        .getPlaceDetails(suggestion.placeId);
    if (!mounted) return;
    setState(() => _resolving = false);
    if (details == null) {
      showAppNotice(
        context,
        'Não foi possível obter detalhes deste endereço.',
        type: AppNoticeType.error,
      );
      return;
    }
    _searchController.clear();
    ref.read(placesAutocompleteProvider.notifier).clear();
    widget.onPlaceChosen(
      details.address,
      LatLng(details.latitude, details.longitude),
    );
  }

  void _clearSearch() {
    HapticFeedback.lightImpact();
    _searchController.clear();
    ref.read(placesAutocompleteProvider.notifier).clear();
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final autocomplete = ref.watch(placesAutocompleteProvider);
    final typing = _searchController.text.trim().isNotEmpty;
    final campusSpots = widget.isOrigin
        ? [
            for (final spot in campusPickupSuggestions)
              (spot.title, spot.subtitle, spot.position),
          ]
        : [
            for (final spot in campusDestinationSuggestions)
              (spot.title, spot.subtitle, spot.position),
          ];
    final badge = widget.isOrigin
        ? const OriginDotBadge(size: 16)
        : const DestinationDotBadge(size: 16);

    // Metade da tela: o conteúdo fica acima da barra de navegação
    return RideFlowSheetFrame(
      initialSize: 0.5,
      bottomSafeArea: widget.bottomSafeArea,
      children: [
        RideFlowHeader(
          title: _title,
          subtitle: _subtitle,
          onBack: widget.onBack,
          onClose: widget.onClose,
        ),
        const SizedBox(height: 14),

        // Selecionar no mapa: arrasta o mapa até o ponto exato
        FilledButton.icon(
          key: ValueKey(
            widget.isOrigin ? 'pick_origin_on_map' : 'pick_destination_on_map',
          ),
          onPressed: () {
            HapticFeedback.lightImpact();
            widget.onPickOnMap();
          },
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.primary,
            foregroundColor: AppColors.textLight,
            padding: const EdgeInsets.symmetric(vertical: 14),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
          ),
          icon: const Icon(Icons.map_rounded, size: 20),
          label: const Text(
            'Selecionar no mapa',
            style: TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
          ),
        ),
        const SizedBox(height: 14),
        const Text(
          'Ou digite o endereço',
          style: TextStyle(
            fontSize: 12.5,
            fontWeight: FontWeight.w700,
            color: AppColors.textDark,
          ),
        ),
        const SizedBox(height: 6),
        TextField(
          key: ValueKey(
            widget.isOrigin
                ? 'origin_search_input'
                : 'destination_search_input',
          ),
          controller: _searchController,
          style: const TextStyle(
            fontSize: 14.5,
            fontWeight: FontWeight.w600,
            color: AppColors.textPrimary,
          ),
          decoration: InputDecoration(
            hintText: 'Rua, bairro ou ponto de referência',
            hintStyle: const TextStyle(
              fontSize: 13.5,
              color: AppColors.textPlaceholder,
            ),
            prefixIcon: Icon(
              widget.isOrigin ? Icons.trip_origin_rounded : Icons.flag_rounded,
              size: 20,
              color: AppColors.textPrimary,
            ),
            suffixIcon: autocomplete.isLoading
                ? const FloatingSearchLensIcon(
                    key: ValueKey('floating_search_lens'),
                  )
                : typing
                ? IconButton(
                    tooltip: 'Limpar',
                    onPressed: _clearSearch,
                    icon: const Icon(
                      Icons.clear_rounded,
                      size: 18,
                      color: AppColors.textSecondary,
                    ),
                  )
                : const Icon(
                    Icons.search_rounded,
                    size: 20,
                    color: AppColors.textMuted,
                  ),
            filled: true,
            fillColor: AppColors.neutralLight,
            contentPadding: const EdgeInsets.symmetric(
              horizontal: 14,
              vertical: 14,
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: const BorderSide(color: AppColors.borderSubtle),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: const BorderSide(
                color: AppColors.primary,
                width: 1.5,
              ),
            ),
          ),
          onChanged: (text) {
            setState(() {});
            ref.read(placesAutocompleteProvider.notifier).onQueryChanged(text);
          },
        ),
        const SizedBox(height: 10),

        // Sugestões do Google enquanto digita; sem busca, os pontos do campus
        if (typing && _resolving)
          const Padding(
            padding: EdgeInsets.all(16),
            child: Center(
              child: CircularProgressIndicator(color: AppColors.primary),
            ),
          )
        else if (typing)
          for (final suggestion in autocomplete.suggestions)
            _placeOption(
              title: suggestion.mainText,
              subtitle: suggestion.secondaryText,
              icon: Icons.location_on_rounded,
              onTap: () => _chooseSuggestion(suggestion),
            )
        else ...[
          if (widget.selectedAddress.isNotEmpty) ...[
            _selectedPlace(badge),
            const SizedBox(height: 12),
          ],
          const Text(
            'Pontos no campus',
            style: TextStyle(
              fontSize: 12.5,
              fontWeight: FontWeight.w700,
              color: AppColors.textDark,
            ),
          ),
          const SizedBox(height: 6),
          for (final (title, subtitle, position) in campusSpots)
            _placeOption(
              title: title,
              subtitle: subtitle,
              icon: Icons.school_rounded,
              selected: widget.selectedAddress == title,
              onTap: () {
                HapticFeedback.lightImpact();
                widget.onPlaceChosen(title, position);
              },
            ),
        ],
      ],
    );
  }

  // O endereço já escolhido, com o botão para seguir
  Widget _selectedPlace(Widget badge) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.neutralLight,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              badge,
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  widget.selectedAddress,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          OutlinedButton(
            key: const ValueKey('ride_flow_continue'),
            onPressed: () {
              HapticFeedback.mediumImpact();
              widget.onContinue();
            },
            style: OutlinedButton.styleFrom(
              foregroundColor: AppColors.textPrimary,
              side: const BorderSide(color: AppColors.primary),
              padding: const EdgeInsets.symmetric(vertical: 12),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(14),
              ),
            ),
            child: const Text(
              'Continuar com este endereço',
              style: TextStyle(fontWeight: FontWeight.w700),
            ),
          ),
        ],
      ),
    );
  }

  Widget _placeOption({
    required String title,
    required String subtitle,
    required IconData icon,
    required VoidCallback onTap,
    bool selected = false,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Material(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(14),
        child: InkWell(
          borderRadius: BorderRadius.circular(14),
          onTap: onTap,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(14),
              border: Border.all(
                color: selected ? AppColors.primary : AppColors.borderSubtle,
                width: selected ? 1.5 : 1,
              ),
            ),
            child: Row(
              children: [
                Icon(icon, size: 20, color: AppColors.textPrimary),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: const TextStyle(
                          fontSize: 13.5,
                          fontWeight: FontWeight.w700,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      if (subtitle.isNotEmpty)
                        Text(
                          subtitle,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 12,
                            color: AppColors.textSecondary,
                          ),
                        ),
                    ],
                  ),
                ),
                const Icon(
                  Icons.chevron_right_rounded,
                  size: 20,
                  color: AppColors.iconMuted,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
