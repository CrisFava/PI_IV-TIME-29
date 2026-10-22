import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/place_details_model.dart';
import '../data/models/place_suggestion_model.dart';
import '../data/repository/route_repository.dart';
import '../data/repository/route_repository_impl.dart';

@immutable
class PlacesAutocompleteState {
  final String query;
  final bool isLoading;
  final List<PlaceSuggestionModel> suggestions;
  final String? errorMessage;

  const PlacesAutocompleteState({
    this.query = '',
    this.isLoading = false,
    this.suggestions = const [],
    this.errorMessage,
  });

  PlacesAutocompleteState copyWith({
    String? query,
    bool? isLoading,
    List<PlaceSuggestionModel>? suggestions,
    String? errorMessage,
  }) {
    return PlacesAutocompleteState(
      query: query ?? this.query,
      isLoading: isLoading ?? this.isLoading,
      suggestions: suggestions ?? this.suggestions,
      errorMessage: errorMessage,
    );
  }
}

final placesAutocompleteProvider = NotifierProvider.autoDispose<
    PlacesAutocompleteController, PlacesAutocompleteState>(
  PlacesAutocompleteController.new,
);

class PlacesAutocompleteController extends Notifier<PlacesAutocompleteState> {
  RouteRepository get _repository => ref.read(routeRepositoryProvider);
  Timer? _debounceTimer;

  @override
  PlacesAutocompleteState build() {
    ref.onDispose(() {
      _debounceTimer?.cancel();
    });
    return const PlacesAutocompleteState();
  }

  /// Atualiza o texto de busca com debounce obrigatório de 1 segundo (1000ms).
  ///
  /// Se a pessoa parar de digitar por 1s, dispara a chamada à rota
  /// `/api/v1/places/autocomplete` do backend.
  void onQueryChanged(String input) {
    _debounceTimer?.cancel();

    final trimmed = input.trim();
    if (trimmed.length < 3) {
      if (!ref.mounted) return;
      state = state.copyWith(
        query: input,
        isLoading: false,
        suggestions: const [],
        errorMessage: null,
      );
      return;
    }

    if (!ref.mounted) return;
    state = state.copyWith(
      query: input,
      isLoading: true,
      errorMessage: null,
    );

    _debounceTimer = Timer(const Duration(seconds: 1), () async {
      await _fetchSuggestions(trimmed);
    });
  }

  Future<void> _fetchSuggestions(String input) async {
    try {
      final results = await _repository.autocomplete(input: input);
      if (!ref.mounted) return;
      state = state.copyWith(
        isLoading: false,
        suggestions: results,
        errorMessage: null,
      );
    } catch (e) {
      if (!ref.mounted) return;
      state = state.copyWith(
        isLoading: false,
        errorMessage: 'Erro ao buscar sugestões de lugares.',
      );
    }
  }

  /// Busca os detalhes completos e coordenadas de um lugar sugerido.
  Future<PlaceDetailsModel?> getPlaceDetails(String placeId) async {
    try {
      return await _repository.getPlaceDetails(placeId: placeId);
    } catch (_) {
      return null;
    }
  }

  /// Limpa a busca e resultados.
  void clear() {
    _debounceTimer?.cancel();
    if (!ref.mounted) return;
    state = const PlacesAutocompleteState();
  }
}
