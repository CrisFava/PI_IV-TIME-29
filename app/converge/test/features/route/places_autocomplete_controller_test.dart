import 'package:converge/features/route/data/models/create_route_request.dart';
import 'package:converge/features/route/data/models/place_details_model.dart';
import 'package:converge/features/route/data/models/place_suggestion_model.dart';
import 'package:converge/features/route/data/models/route_response_model.dart';
import 'package:converge/features/route/data/repository/route_repository.dart';
import 'package:converge/features/route/data/repository/route_repository_impl.dart';
import 'package:converge/features/route/state/places_autocomplete_controller.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

class FakeRouteRepository implements RouteRepository {
  int autocompleteCallCount = 0;
  String? lastInput;

  @override
  Future<List<PlaceSuggestionModel>> autocomplete({
    required String input,
    String? sessionToken,
  }) async {
    autocompleteCallCount++;
    lastInput = input;
    return [
      PlaceSuggestionModel(
        placeId: 'fake_id',
        mainText: 'Local para $input',
        secondaryText: 'Campinas - SP',
      ),
    ];
  }

  @override
  Future<PlaceDetailsModel> getPlaceDetails({
    required String placeId,
    String? sessionToken,
  }) async {
    return const PlaceDetailsModel(
      address: 'Endereço Teste',
      city: 'Campinas',
      latitude: -22.9,
      longitude: -47.0,
    );
  }

  @override
  Future<RouteResponseModel> createRoute(CreateRouteRequest request) {
    throw UnimplementedError();
  }

  @override
  Future<List<RouteResponseModel>> listMyRoutes() {
    throw UnimplementedError();
  }

  @override
  Future<RouteResponseModel> getRoute(String routeId) {
    throw UnimplementedError();
  }

  @override
  Future<void> deleteRoute(String routeId) {
    throw UnimplementedError();
  }
}

void main() {
  group('PlacesAutocompleteController - Debounce de 1 segundo', () {
    late FakeRouteRepository fakeRepository;
    late ProviderContainer container;
    late ProviderSubscription<PlacesAutocompleteState> subscription;

    setUp(() {
      fakeRepository = FakeRouteRepository();
      container = ProviderContainer(
        overrides: [
          routeRepositoryProvider.overrideWithValue(fakeRepository),
        ],
      );
      // Mantém o provider ativo no teste
      subscription = container.listen(placesAutocompleteProvider, (_, _) {});
    });

    tearDown(() {
      subscription.close();
      container.dispose();
    });

    test('Texto com menos de 3 caracteres não dispara autocomplete', () async {
      final controller =
          container.read(placesAutocompleteProvider.notifier);

      controller.onQueryChanged('ab');
      await Future<void>.delayed(const Duration(milliseconds: 1100));

      expect(fakeRepository.autocompleteCallCount, 0);
      expect(container.read(placesAutocompleteProvider).suggestions, isEmpty);
    });

    test('Digitação rápida aguarda 1 segundo sem digitar antes de chamar backend', () async {
      final controller =
          container.read(placesAutocompleteProvider.notifier);

      // Usuário começa a digitar: "puc" -> "puc c" -> "puc campinas"
      controller.onQueryChanged('puc');
      await Future<void>.delayed(const Duration(milliseconds: 400));
      expect(fakeRepository.autocompleteCallCount, 0);

      controller.onQueryChanged('puc c');
      await Future<void>.delayed(const Duration(milliseconds: 400));
      expect(fakeRepository.autocompleteCallCount, 0);

      controller.onQueryChanged('puc campinas');
      // Aguarda 600ms (ainda não completou 1s desde a última digitação)
      await Future<void>.delayed(const Duration(milliseconds: 600));
      expect(fakeRepository.autocompleteCallCount, 0);

      // Agora completa o 1 segundo de inatividade (mais 500ms -> total 1100ms)
      await Future<void>.delayed(const Duration(milliseconds: 500));

      expect(fakeRepository.autocompleteCallCount, 1);
      expect(fakeRepository.lastInput, 'puc campinas');

      final state = container.read(placesAutocompleteProvider);
      expect(state.isLoading, false);
      expect(state.suggestions, isNotEmpty);
      expect(state.suggestions.first.mainText, 'Local para puc campinas');
    });

    test('getPlaceDetails retorna detalhes com coordenadas', () async {
      final controller =
          container.read(placesAutocompleteProvider.notifier);

      final details = await controller.getPlaceDetails('fake_id');
      expect(details, isNotNull);
      expect(details!.city, 'Campinas');
      expect(details.latitude, -22.9);
      expect(details.longitude, -47.0);
    });
  });
}
