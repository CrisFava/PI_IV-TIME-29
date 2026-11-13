// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../../core/config/styles.dart';
import '../../../ride/data/models/driver_shift_payload.dart';
import '../../../route/data/models/place_suggestion_model.dart';
import '../../../route/state/places_autocomplete_controller.dart';

/// ============================================================================
/// COMPONENTE: HomeDriverFormSheet (Modo Motorista - Painel e Formulário)
/// ============================================================================
/// Painel retrátil com formulário para iniciar expediente de motorista:
/// 1. Destino do motorista (com busca Google Maps após 1s e lupa animada)
/// 2. Raio de destino (área de tolerância do passageiro)
/// 3. Raio em km a partir do local atual (refletido no mapa para apitar buscas)
/// 4. Horário de fim de busca com aviso de timeout automático
/// 5. Ação de salvar trajeto, botão de fechar e início de expediente
/// ============================================================================
class HomeDriverFormSheet extends ConsumerStatefulWidget {
  final double initialSearchRadiusKm;
  final ValueChanged<double> onSearchRadiusChanged;
  final ValueChanged<DriverShiftPayload> onStartShift;
  final VoidCallback onSaveRoute;
  final VoidCallback? onClose;
  final void Function(String address, LatLng position)? onDestinationSelected;
  final double bottomSafeArea;

  const HomeDriverFormSheet({
    super.key,
    this.initialSearchRadiusKm = 5.0,
    required this.onSearchRadiusChanged,
    required this.onStartShift,
    required this.onSaveRoute,
    this.onClose,
    this.onDestinationSelected,
    this.bottomSafeArea = 0,
  });

  @override
  ConsumerState<HomeDriverFormSheet> createState() =>
      _HomeDriverFormSheetState();
}

class _HomeDriverFormSheetState extends ConsumerState<HomeDriverFormSheet> {
  final _formKey = GlobalKey<FormState>();
  final _destinationController = TextEditingController();

  late double _searchRadiusKm;
  double _destinationRadiusKm = 3.0;
  TimeOfDay _endSearchTime = TimeOfDay.fromDateTime(
    DateTime.now().add(const Duration(hours: 2)),
  );

  @override
  void initState() {
    super.initState();
    _searchRadiusKm = widget.initialSearchRadiusKm;
  }

  @override
  void dispose() {
    _destinationController.dispose();
    super.dispose();
  }

  Future<void> _handleSuggestionSelected(
    PlaceSuggestionModel suggestion,
  ) async {
    HapticFeedback.lightImpact();
    FocusScope.of(context).unfocus();

    final fullLabel = suggestion.secondaryText.isNotEmpty
        ? '${suggestion.mainText}, ${suggestion.secondaryText}'
        : suggestion.mainText;

    setState(() {
      _destinationController.text = fullLabel;
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
        _destinationController.text = resolvedAddress;
      });
      widget.onDestinationSelected?.call(
        resolvedAddress,
        LatLng(details.latitude, details.longitude),
      );
    }
  }

  Future<void> _selectTime(BuildContext context) async {
    final picked = await showTimePicker(
      context: context,
      initialTime: _endSearchTime,
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: const ColorScheme.light(
              primary: AppColors.primary,
              onPrimary: AppColors.textLight,
              onSurface: AppColors.textPrimary,
            ),
          ),
          child: child!,
        );
      },
    );
    if (picked != null) {
      setState(() => _endSearchTime = picked);
    }
  }

  void _submit() {
    if (!_formKey.currentState!.validate()) return;

    HapticFeedback.mediumImpact();
    final payload = DriverShiftPayload(
      driverId: 'current_user',
      destination: _destinationController.text.trim(),
      destinationRadiusKm: _destinationRadiusKm,
      searchRadiusKm: _searchRadiusKm,
      endSearchTime: _endSearchTime,
      startedAt: DateTime.now(),
    );

    widget.onStartShift(payload);
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
                                  'Iniciar Expediente de Motorista',
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
                            // Botão para fechar o popup de expediente de motorista
                            IconButton(
                              key: const ValueKey('close_driver_shift_sheet'),
                              tooltip: 'Fechar expediente de motorista',
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
                          // 1. Campo de Destino com Autocomplete Google Maps (1s debounce)
                          const Text(
                            'Para onde você está indo?',
                            style: TextStyle(
                              fontSize: 13.5,
                              fontWeight: FontWeight.w600,
                              color: AppColors.textPrimary,
                            ),
                          ),
                          const SizedBox(height: 6),
                          TextFormField(
                            key: const ValueKey('driver_destination_input'),
                            controller: _destinationController,
                            style: const TextStyle(
                              fontSize: 14,
                              color: AppColors.textPrimary,
                            ),
                            decoration: InputDecoration(
                              hintText: 'Ex.: Campus Universitário / Reitoria',
                              hintStyle: const TextStyle(
                                color: AppColors.textMuted,
                                fontSize: 13.5,
                              ),
                              prefixIcon: const Icon(
                                Icons.pin_drop_rounded,
                                size: 18,
                                color: AppColors.textPrimary,
                              ),
                              suffixIcon: autocompleteState.isLoading
                                  ? const FloatingSearchLensIcon(
                                      key: ValueKey('floating_search_lens'),
                                    )
                                  : _destinationController.text.isNotEmpty
                                      ? IconButton(
                                          tooltip: 'Limpar destino',
                                          icon: const Icon(
                                            Icons.clear_rounded,
                                            size: 18,
                                            color: AppColors.textSecondary,
                                          ),
                                          onPressed: () {
                                            HapticFeedback.lightImpact();
                                            _destinationController.clear();
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
                                borderSide: const BorderSide(
                                  color: AppColors.borderSubtle,
                                ),
                              ),
                              focusedBorder: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(14),
                                borderSide: const BorderSide(
                                  color: AppColors.primary,
                                  width: 1.5,
                                ),
                              ),
                              errorBorder: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(14),
                                borderSide: const BorderSide(
                                  color: AppColors.error,
                                ),
                              ),
                              focusedErrorBorder: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(14),
                                borderSide: const BorderSide(
                                  color: AppColors.error,
                                  width: 1.5,
                                ),
                              ),
                            ),
                            onChanged: (text) {
                              setState(() {});
                              ref
                                  .read(placesAutocompleteProvider.notifier)
                                  .onQueryChanged(text);
                            },
                            validator: (v) => (v == null || v.trim().isEmpty)
                                ? 'Informe seu destino'
                                : null,
                          ),

                          // Lista de sugestões retornadas pelo Google Maps (após 1s de digitação)
                          if (autocompleteState.suggestions.isNotEmpty) ...[
                            const SizedBox(height: 8),
                            Container(
                              decoration: BoxDecoration(
                                color: AppColors.cardBackground,
                                borderRadius: BorderRadius.circular(14),
                                border: Border.all(
                                  color: AppColors.borderSubtle,
                                ),
                              ),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: [
                                  for (int i = 0;
                                      i < autocompleteState.suggestions.length;
                                      i++) ...[
                                    if (i > 0)
                                      const Divider(
                                        height: 1,
                                        color: AppColors.borderSubtle,
                                      ),
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
                                                crossAxisAlignment:
                                                    CrossAxisAlignment.start,
                                                children: [
                                                  Text(
                                                    autocompleteState
                                                        .suggestions[i]
                                                        .mainText,
                                                    style: const TextStyle(
                                                      fontSize: 13.5,
                                                      fontWeight:
                                                          FontWeight.w700,
                                                      color:
                                                          AppColors.textPrimary,
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
                                                      overflow:
                                                          TextOverflow.ellipsis,
                                                      style: const TextStyle(
                                                        fontSize: 11.5,
                                                        color: AppColors
                                                            .textSecondary,
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

                          const SizedBox(height: 18),

                          // 2. Raio de Destino do Passageiro
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              const Text(
                                'Tolerância no destino do passageiro',
                                style: TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                  color: AppColors.textPrimary,
                                ),
                              ),
                              Text(
                                '${_destinationRadiusKm.toStringAsFixed(1)} km',
                                style: const TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w700,
                                  color: AppColors.primary,
                                ),
                              ),
                            ],
                          ),
                          Slider(
                            value: _destinationRadiusKm,
                            min: 1.0,
                            max: 10.0,
                            divisions: 18,
                            activeColor: AppColors.primary,
                            inactiveColor: AppColors.borderSubtle,
                            onChanged: (val) {
                              setState(() => _destinationRadiusKm = val);
                            },
                          ),
                          const Text(
                            'O passageiro deve desembarcar em até esta distância do seu destino final.',
                            style: TextStyle(
                              fontSize: 11.5,
                              color: AppColors.textSecondary,
                            ),
                          ),

                          const SizedBox(height: 18),

                          // 3. Raio em KM Visual no Mapa (Para apitar buscas)
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              const Text(
                                'Raio de busca ao seu redor (no mapa)',
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
                            max: 15.0,
                            divisions: 28,
                            activeColor: AppColors.primary,
                            inactiveColor: AppColors.borderSubtle,
                            onChanged: (val) {
                              setState(() => _searchRadiusKm = val);
                              widget.onSearchRadiusChanged(val);
                            },
                          ),
                          const Text(
                            'O aplicativo apitará solicitações de passageiros dentro do círculo desenhado no mapa.',
                            style: TextStyle(
                              fontSize: 11.5,
                              color: AppColors.textSecondary,
                            ),
                          ),

                          const SizedBox(height: 18),

                          // 4. Horário de Fim de Busca
                          const Text(
                            'Horário de fim de busca',
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: AppColors.textPrimary,
                            ),
                          ),
                          const SizedBox(height: 6),
                          InkWell(
                            borderRadius: BorderRadius.circular(14),
                            onTap: () => _selectTime(context),
                            child: Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 14,
                                vertical: 12,
                              ),
                              decoration: BoxDecoration(
                                color: AppColors.neutralLight,
                                borderRadius: BorderRadius.circular(14),
                                border: Border.all(
                                  color: AppColors.borderSubtle,
                                ),
                              ),
                              child: Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Row(
                                    children: [
                                      const Icon(
                                        Icons.alarm_rounded,
                                        size: 18,
                                        color: AppColors.textPrimary,
                                      ),
                                      const SizedBox(width: 8),
                                      Text(
                                        _endSearchTime.format(context),
                                        style: const TextStyle(
                                          fontSize: 14,
                                          fontWeight: FontWeight.w700,
                                          color: AppColors.textPrimary,
                                        ),
                                      ),
                                    ],
                                  ),
                                  const Text(
                                    'Alterar',
                                    style: TextStyle(
                                      fontSize: 12.5,
                                      fontWeight: FontWeight.w600,
                                      color: AppColors.textSecondary,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(height: 4),
                          Container(
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: AppColors.neutralLight,
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: const Row(
                              children: [
                                Icon(
                                  Icons.info_outline_rounded,
                                  size: 15,
                                  color: AppColors.textSecondary,
                                ),
                                SizedBox(width: 8),
                                Expanded(
                                  child: Text(
                                    'Atingido este horário sem corridas agendadas, seu expediente se encerra automaticamente.',
                                    style: TextStyle(
                                      fontSize: 11,
                                      color: AppColors.textSecondary,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),

                          const SizedBox(height: 22),

                          // 5. Botão de Iniciar Expediente
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
                              onPressed: _submit,
                              icon: const Icon(
                                Icons.flash_on_rounded,
                                size: 18,
                              ),
                              label: const Text(
                                'Iniciar Expediente',
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
  State<FloatingSearchLensIcon> createState() =>
      _FloatingSearchLensIconState();
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
            child: Transform.scale(
              scale: scale,
              child: child,
            ),
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
