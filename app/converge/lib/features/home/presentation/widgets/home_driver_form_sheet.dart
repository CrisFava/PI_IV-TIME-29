// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../../core/config/styles.dart';
import '../../../route/data/models/place_suggestion_model.dart';
import '../../../route/state/places_autocomplete_controller.dart';
import 'ride_departure_tile.dart';

/// ============================================================================
/// COMPONENTE: HomeDriverFormSheet (Modo Motorista - Painel e Formulário)
/// ============================================================================
/// Painel retrátil com o formulário para o motorista publicar uma carona agendada:
/// 1. De onde sai e para onde vai (com busca Google Maps após 1s e lupa animada)
/// 2. Até quantos km do caminho e do destino os passageiros podem estar
/// 3. Quando a carona sai e o botão de publicar
/// ============================================================================
class HomeDriverFormSheet extends ConsumerStatefulWidget {
  final double initialSearchRadiusKm;
  final ValueChanged<double> onSearchRadiusChanged;

  /// Publica a carona com o desvio máximo escolhido, em km.
  final ValueChanged<double> onPublish;
  final VoidCallback onSaveRoute;
  final VoidCallback? onClose;
  final void Function(String address, LatLng position)? onDestinationSelected;

  /// De onde a carona sai (a localização atual não é usada).
  final void Function(String address, LatLng position)? onOriginSelected;
  final double bottomSafeArea;

  /// Publicando a carona: o botão fica desabilitado, com progresso.
  final bool isPublishing;

  /// Quando a carona sai ("Agora" por padrão) e a troca de dia e horário.
  final String departureLabel;
  final VoidCallback? onChangeDeparture;

  const HomeDriverFormSheet({
    super.key,
    this.initialSearchRadiusKm = 3.0,
    required this.onSearchRadiusChanged,
    required this.onPublish,
    required this.onSaveRoute,
    this.onClose,
    this.onDestinationSelected,
    this.onOriginSelected,
    this.bottomSafeArea = 0,
    this.isPublishing = false,
    this.departureLabel = 'Agora',
    this.onChangeDeparture,
  });

  @override
  ConsumerState<HomeDriverFormSheet> createState() =>
      _HomeDriverFormSheetState();
}

class _HomeDriverFormSheetState extends ConsumerState<HomeDriverFormSheet> {
  final _formKey = GlobalKey<FormState>();
  final _destinationController = TextEditingController();
  final _originController = TextEditingController();

  // Qual campo de endereço está sendo digitado (as sugestões aparecem embaixo dele)
  bool _editingOrigin = false;

  late double _searchRadiusKm;

  @override
  void initState() {
    super.initState();
    _searchRadiusKm = widget.initialSearchRadiusKm;
  }

  @override
  void dispose() {
    _destinationController.dispose();
    _originController.dispose();
    super.dispose();
  }

  Future<void> _handleSuggestionSelected(
    PlaceSuggestionModel suggestion,
  ) async {
    HapticFeedback.lightImpact();
    FocusScope.of(context).unfocus();

    // A sugestão vai para o campo que estava sendo digitado
    final isOrigin = _editingOrigin;
    final controller = isOrigin ? _originController : _destinationController;
    final fullLabel = suggestion.secondaryText.isNotEmpty
        ? '${suggestion.mainText}, ${suggestion.secondaryText}'
        : suggestion.mainText;

    setState(() {
      controller.text = fullLabel;
    });
    ref.read(placesAutocompleteProvider.notifier).clear();

    final details = await ref
        .read(placesAutocompleteProvider.notifier)
        .getPlaceDetails(suggestion.placeId);

    if (details != null && mounted) {
      final resolvedAddress = details.address.isNotEmpty
          ? details.address
          : fullLabel;
      setState(() {
        controller.text = resolvedAddress;
      });
      final position = LatLng(details.latitude, details.longitude);
      if (isOrigin) {
        widget.onOriginSelected?.call(resolvedAddress, position);
      } else {
        widget.onDestinationSelected?.call(resolvedAddress, position);
      }
    }
  }

  void _submit() {
    if (!_formKey.currentState!.validate()) return;

    HapticFeedback.mediumImpact();
    widget.onPublish(_searchRadiusKm);
  }

  /// Campo de endereço com a busca do Google Maps. As sugestões aparecem embaixo
  /// do campo que está sendo digitado.
  List<Widget> _buildPlaceInput({
    required bool isOrigin,
    required TextEditingController controller,
    required String label,
    required String hint,
    required IconData icon,
    required String emptyMessage,
    required PlacesAutocompleteState autocompleteState,
  }) {
    final active = _editingOrigin == isOrigin;
    return [
      Text(
        label,
        style: TextStyle(
          fontSize: 13.5,
          fontWeight: FontWeight.w600,
          color: AppColors.textPrimary,
        ),
      ),
      const SizedBox(height: 6),
      TextFormField(
        key: ValueKey(
          isOrigin ? 'driver_origin_input' : 'driver_destination_input',
        ),
        controller: controller,
        onTap: () => setState(() => _editingOrigin = isOrigin),
        style: const TextStyle(fontSize: 14, color: AppColors.textPrimary),
        decoration: InputDecoration(
          hintText: hint,
          hintStyle: const TextStyle(
            color: AppColors.textMuted,
            fontSize: 13.5,
          ),
          prefixIcon: Icon(icon, size: 18, color: AppColors.textPrimary),
          suffixIcon: autocompleteState.isLoading && active
              ? const FloatingSearchLensIcon(
                  key: ValueKey('floating_search_lens'),
                )
              : controller.text.isNotEmpty
              ? IconButton(
                  tooltip: 'Limpar',
                  icon: const Icon(
                    Icons.clear_rounded,
                    size: 18,
                    color: AppColors.textSecondary,
                  ),
                  onPressed: () {
                    HapticFeedback.lightImpact();
                    controller.clear();
                    ref.read(placesAutocompleteProvider.notifier).clear();
                    setState(() {});
                  },
                )
              : const Icon(
                  Icons.search_rounded,
                  size: 18,
                  color: AppColors.textMuted,
                ),
          filled: true,
          fillColor: AppColors.neutralLight,
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 14,
            vertical: 12,
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: const BorderSide(color: AppColors.borderSubtle),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: const BorderSide(color: AppColors.primary, width: 1.5),
          ),
          errorBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: const BorderSide(color: AppColors.error),
          ),
          focusedErrorBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: const BorderSide(color: AppColors.error, width: 1.5),
          ),
        ),
        onChanged: (text) {
          setState(() => _editingOrigin = isOrigin);
          ref.read(placesAutocompleteProvider.notifier).onQueryChanged(text);
        },
        validator: (v) => (v == null || v.trim().isEmpty) ? emptyMessage : null,
      ),

      // Lista de sugestões retornadas pelo Google Maps (após 1s de digitação)
      if (active && autocompleteState.suggestions.isNotEmpty) ...[
        const SizedBox(height: 8),
        Container(
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: AppColors.borderSubtle),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              for (
                int i = 0;
                i < autocompleteState.suggestions.length;
                i++
              ) ...[
                if (i > 0)
                  const Divider(height: 1, color: AppColors.borderSubtle),
                InkWell(
                  borderRadius: BorderRadius.circular(14),
                  onTap: () => _handleSuggestionSelected(
                    autocompleteState.suggestions[i],
                  ),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 10,
                    ),
                    child: Row(
                      children: [
                        const Icon(
                          Icons.location_on_rounded,
                          size: 18,
                          color: AppColors.primary,
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                autocompleteState.suggestions[i].mainText,
                                style: const TextStyle(
                                  fontSize: 13.5,
                                  fontWeight: FontWeight.w700,
                                  color: AppColors.textPrimary,
                                ),
                              ),
                              if (autocompleteState
                                  .suggestions[i]
                                  .secondaryText
                                  .isNotEmpty)
                                Text(
                                  autocompleteState
                                      .suggestions[i]
                                      .secondaryText,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(
                                    fontSize: 11.5,
                                    color: AppColors.textSecondary,
                                  ),
                                ),
                            ],
                          ),
                        ),
                        const Icon(
                          Icons.north_west_rounded,
                          size: 15,
                          color: AppColors.iconMuted,
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ],
    ];
  }

  @override
  Widget build(BuildContext context) {
    final autocompleteState = ref.watch(placesAutocompleteProvider);

    return DraggableScrollableSheet(
      initialChildSize: 0.46,
      minChildSize: 0.10,
      maxChildSize: 0.88,
      snap: true,
      snapSizes: const [0.22, 0.46, 0.88],
      builder: (context, scrollController) {
        return Container(
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: const BorderRadius.vertical(top: Radius.circular(26)),
            border: Border.all(color: AppColors.borderSubtle, width: 1),
            boxShadow: [
              BoxShadow(
                color: AppColors.primary.withValues(alpha: 0.12),
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
                padding: EdgeInsets.fromLTRB(
                  16,
                  4,
                  16,
                  120 + widget.bottomSafeArea,
                ),
                sliver: SliverList(
                  delegate: SliverChildListDelegate([
                    // Cabeçalho do Modo Motorista com Botão de Salvar e Fechar
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Expanded(
                          child: Row(
                            children: [
                              Icon(
                                Icons.drive_eta_rounded,
                                size: 20,
                                color: AppColors.textPrimary,
                              ),
                              SizedBox(width: 8),
                              Flexible(
                                child: Text(
                                  'Oferecer carona',
                                  overflow: TextOverflow.ellipsis,
                                  style: TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.w800,
                                    color: AppColors.textPrimary,
                                    letterSpacing: -0.3,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                        Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            // Botão rápido para salvar trajeto
                            IconButton(
                              tooltip: 'Salvar trajeto frequente',
                              visualDensity: VisualDensity.compact,
                              onPressed: widget.onSaveRoute,
                              icon: const Icon(
                                Icons.bookmark_add_rounded,
                                size: 20,
                                color: AppColors.textPrimary,
                              ),
                            ),
                            // Botão para fechar a oferta de carona
                            IconButton(
                              key: const ValueKey('close_driver_shift_sheet'),
                              tooltip: 'Fechar oferta de carona',
                              visualDensity: VisualDensity.compact,
                              onPressed: () {
                                HapticFeedback.lightImpact();
                                ref
                                    .read(placesAutocompleteProvider.notifier)
                                    .clear();
                                widget.onClose?.call();
                              },
                              icon: const Icon(
                                Icons.close_rounded,
                                size: 20,
                                color: AppColors.textPrimary,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),

                    const SizedBox(height: 14),

                    Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          // 1. De onde a carona sai e para onde vai (busca do Google Maps)
                          ..._buildPlaceInput(
                            isOrigin: true,
                            controller: _originController,
                            label: 'Saindo de',
                            hint: 'Ex.: Rua, bairro ou ponto de referência',
                            icon: Icons.trip_origin_rounded,
                            emptyMessage: 'Informe de onde você sai',
                            autocompleteState: autocompleteState,
                          ),
                          const SizedBox(height: 12),
                          ..._buildPlaceInput(
                            isOrigin: false,
                            controller: _destinationController,
                            label: 'Indo para',
                            hint: 'Ex.: Campus Universitário / Reitoria',
                            icon: Icons.pin_drop_rounded,
                            emptyMessage: 'Informe seu destino',
                            autocompleteState: autocompleteState,
                          ),

                          const SizedBox(height: 18),

                          // 2. Distância máxima: do caminho até o embarque e do destino até o do passageiro
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              const Text(
                                'Buscar passageiros a até',
                                style: TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                  color: AppColors.textPrimary,
                                ),
                              ),
                              Text(
                                '${_searchRadiusKm.toStringAsFixed(1)} km',
                                style: const TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w700,
                                  color: AppColors.primary,
                                ),
                              ),
                            ],
                          ),
                          Slider(
                            value: _searchRadiusKm,
                            min: 1.0,
                            max: 10.0,
                            divisions: 18,
                            activeColor: AppColors.primary,
                            inactiveColor: AppColors.borderSubtle,
                            onChanged: (val) {
                              setState(() => _searchRadiusKm = val);
                              widget.onSearchRadiusChanged(val);
                            },
                          ),
                          const Text(
                            'Do seu caminho até onde o passageiro embarca, e do seu destino até o dele.',
                            style: TextStyle(
                              fontSize: 11.5,
                              color: AppColors.textSecondary,
                            ),
                          ),

                          const SizedBox(height: 18),

                          // 3. Quando a carona sai e o botão de publicar
                          if (widget.onChangeDeparture != null) ...[
                            RideDepartureTile(
                              label: widget.departureLabel,
                              onTap: widget.onChangeDeparture!,
                            ),
                            const SizedBox(height: 12),
                          ],
                          SizedBox(
                            height: 50,
                            child: FilledButton.icon(
                              style: FilledButton.styleFrom(
                                backgroundColor: AppColors.primary,
                                foregroundColor: AppColors.textLight,
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(14),
                                ),
                              ),
                              onPressed: widget.isPublishing ? null : _submit,
                              icon: widget.isPublishing
                                  ? const SizedBox(
                                      width: 18,
                                      height: 18,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2,
                                        color: AppColors.textLight,
                                      ),
                                    )
                                  : const Icon(
                                      Icons.event_available_rounded,
                                      size: 18,
                                    ),
                              label: const Text(
                                'Publicar carona',
                                style: TextStyle(
                                  fontSize: 15,
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                            ),
                          ),
                        ],
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
}

/// Ícone de lupa com animação fluida (movimento lateral + órbita suave de loading)
/// para sinalizar de forma agradável que a pesquisa no Google Maps está em andamento.
class FloatingSearchLensIcon extends StatefulWidget {
  const FloatingSearchLensIcon({super.key});

  @override
  State<FloatingSearchLensIcon> createState() => _FloatingSearchLensIconState();
}

class _FloatingSearchLensIconState extends State<FloatingSearchLensIcon>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1150),
    )..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, child) {
        final angle = _controller.value * 2 * math.pi;
        // Movimento lateral principal + leve flutuação vertical (órbita de "inspeção/loading")
        final dx = math.sin(angle) * 4.2;
        final dy = math.cos(angle * 2) * 1.6;
        // Leve inclinação acompanhando o deslocamento horizontal
        final tilt = math.sin(angle) * 0.14;
        final scale = 0.96 + (math.cos(angle) * 0.06);

        return Transform.translate(
          offset: Offset(dx, dy),
          child: Transform.rotate(
            angle: tilt,
            child: Transform.scale(scale: scale, child: child),
          ),
        );
      },
      child: const Icon(
        Icons.search_rounded,
        size: 20,
        color: AppColors.primary,
      ),
    );
  }
}
