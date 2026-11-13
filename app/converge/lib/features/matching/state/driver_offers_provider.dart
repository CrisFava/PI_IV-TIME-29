// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../ride/data/models/ride_model.dart';
import '../../ride/data/models/ride_status.dart';
import '../../route/data/models/create_route_request.dart';
import '../../route/data/models/route_response_model.dart';
import '../../route/data/repository/route_repository_impl.dart';
import '../data/models/driver_offer.dart';
import 'driver_rides_provider.dart';

/// Caronas publicadas pelo motorista (trajetos de oferta ativos).
/// Recarregue com `ref.invalidate(myOfferRoutesProvider)`.
final myOfferRoutesProvider =
    FutureProvider.autoDispose<List<RouteResponseModel>>((ref) async {
      final routes = await ref.watch(routeRepositoryProvider).listMyRoutes();
      return [
        for (final route in routes)
          if (route.type == AppRouteType.offer && route.status == 'ACTIVE')
            route,
      ];
    });

/// "Minhas caronas": cada carona oferecida com as viagens dela. Viagens de uma
/// oferta já cancelada (com passageiros) também aparecem. A viagem em andamento
/// vem primeiro; depois, as que têm viagem marcada, da mais próxima à mais distante.
final myDriverOffersProvider = FutureProvider.autoDispose<List<DriverOffer>>((
  ref,
) async {
  final routes = await ref.watch(myOfferRoutesProvider.future);
  final rides = await ref.watch(myDrivenRidesProvider.future);

  final routeIds = {for (final route in routes) route.id};
  final offers = [
    for (final route in routes)
      DriverOffer(
        route: route,
        rides: [
          for (final ride in rides)
            if (ride.driverRouteId == route.id) ride,
        ],
      ),
    for (final ride in rides)
      if (!routeIds.contains(ride.driverRouteId))
        DriverOffer(route: null, rides: [ride]),
  ];

  offers.sort(_compareOffers);
  return offers;
});

// Ordem: em andamento, depois pela próxima viagem, depois pelo horário da oferta
int _compareOffers(DriverOffer a, DriverOffer b) {
  final byGroup = _group(a).compareTo(_group(b));
  if (byGroup != 0) return byGroup;

  final byNext = (_nextDeparture(a.rides) ?? DateTime(9999)).compareTo(
    _nextDeparture(b.rides) ?? DateTime(9999),
  );
  if (byNext != 0) return byNext;

  return (a.route?.departureTime ?? '').compareTo(b.route?.departureTime ?? '');
}

int _group(DriverOffer offer) {
  if (offer.rides.any((r) => r.status == RideStatus.inProgress)) return 0;
  return _nextDeparture(offer.rides) == null ? 2 : 1;
}

DateTime? _nextDeparture(List<RideModel> rides) {
  DateTime? next;
  for (final ride in rides) {
    final departure = ride.departureDateTime;
    if (departure != null && (next == null || departure.isBefore(next))) {
      next = departure;
    }
  }
  return next;
}
