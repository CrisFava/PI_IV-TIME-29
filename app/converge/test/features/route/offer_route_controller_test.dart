// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/route/data/models/create_route_request.dart';
import 'package:converge/features/route/data/models/route_place_model.dart';
import 'package:converge/features/route/data/repository/route_repository_impl.dart';
import 'package:converge/features/route/state/offer_route_controller.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../matching/fakes/fake_matching_route_repository.dart';

void main() {
  late FakeMatchingRouteRepository routes;
  late ProviderContainer container;

  OfferRouteController controller() =>
      container.read(offerRouteProvider.notifier);

  CreateRouteRequest offerAt(String departureTime, Set<String> weekDays) =>
      CreateRouteRequest(
        type: AppRouteType.offer,
        vehicleId: 'vehicle-1',
        origin: const RoutePlaceModel(
          address: 'Av. Francisco Glicério, 1000',
          city: 'Campinas',
          latitude: -22.9064,
          longitude: -47.0616,
        ),
        destination: const RoutePlaceModel(
          address: 'PUC-Campinas',
          city: 'Campinas',
          latitude: -22.8338,
          longitude: -47.0519,
        ),
        departureTime: departureTime,
        estimatedArrivalTime: '08:45',
        weekDays: weekDays,
        maxDetourKm: 5,
        totalSeats: 3,
      );

  setUp(() {
    routes = FakeMatchingRouteRepository();
    container = ProviderContainer.test(
      overrides: [routeRepositoryProvider.overrideWithValue(routes)],
    );
    container.listen(offerRouteProvider, (_, _) {});
  });

  test('a mesma carona já publicada (409): usa a que existe', () async {
    routes.conflictOnCreate = true;

    final saved = await controller().publish(offerAt('07:30', {'MONDAY'}));

    expect(saved.id, 'saved-elsewhere');
  });
}
