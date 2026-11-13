// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/matching/data/models/driver_offer.dart';
import 'package:converge/features/matching/state/driver_offers_provider.dart';
import 'package:converge/features/matching/state/matching_viewer_provider.dart';
import 'package:converge/features/ride/data/repository/ride_repository_impl.dart';
import 'package:converge/features/route/data/models/create_route_request.dart';
import 'package:converge/features/route/data/models/route_place_model.dart';
import 'package:converge/features/route/data/repository/route_repository_impl.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ride/fakes/fake_ride.dart';
import 'fakes/fake_matching_route_repository.dart';
import 'fakes/fake_seat_ride_repository.dart';

void main() {
  late FakeMatchingRouteRepository routes;
  late FakeSeatRideRepository rides;
  late ProviderContainer container;

  CreateRouteRequest offer(String departureTime) => CreateRouteRequest(
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
    weekDays: const {'MONDAY'},
    maxDetourKm: 5,
    totalSeats: 3,
  );

  setUp(() {
    routes = FakeMatchingRouteRepository();
    rides = FakeSeatRideRepository();
    container = ProviderContainer.test(
      overrides: [
        routeRepositoryProvider.overrideWithValue(routes),
        rideRepositoryProvider.overrideWithValue(rides),
        matchingViewerIdProvider.overrideWithValue(fakeDriverId),
      ],
    );
  });

  // Começa a observar só depois de o teste montar os trajetos e as viagens
  Future<List<DriverOffer>> loadOffers() {
    container.listen(myDriverOffersProvider, (_, _) {});
    return container.read(myDriverOffersProvider.future);
  }

  test(
    'a viagem fica dentro da carona de onde saiu, e a com viagem vem antes',
    () async {
      routes.routes
        ..add(
          FakeMatchingRouteRepository.routeFrom(
            'offer-sem-pedido',
            offer('06:00'),
          ),
        )
        // fakeRide sai do trajeto route-1
        ..add(FakeMatchingRouteRepository.routeFrom('route-1', offer('07:30')));
      rides.myRides = [fakeRide(status: 'CONFIRMED')];

      final offers = await loadOffers();

      expect(offers.map((o) => o.route?.id), ['route-1', 'offer-sem-pedido']);
      expect(offers.first.rides.single.id, fakeRideId);
    },
  );

  test('viagem de uma carona já cancelada continua aparecendo', () async {
    rides.myRides = [fakeRide(status: 'CONFIRMED')];

    final offers = await loadOffers();

    expect(offers.single.route, isNull);
    expect(offers.single.rides.single.id, fakeRideId);
  });

}
