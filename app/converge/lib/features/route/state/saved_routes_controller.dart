import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/create_route_request.dart';
import '../data/models/route_response_model.dart';
import '../data/repository/route_repository.dart';
import '../data/repository/route_repository_impl.dart';

@immutable
class SavedRoutesState {
  final bool isLoading;
  final List<RouteResponseModel> routes;
  final String? errorMessage;

  const SavedRoutesState({
    this.isLoading = false,
    this.routes = const [],
    this.errorMessage,
  });

  SavedRoutesState copyWith({
    bool? isLoading,
    List<RouteResponseModel>? routes,
    String? errorMessage,
  }) {
    return SavedRoutesState(
      isLoading: isLoading ?? this.isLoading,
      routes: routes ?? this.routes,
      errorMessage: errorMessage,
    );
  }
}

final savedRoutesProvider =
    NotifierProvider<SavedRoutesController, SavedRoutesState>(
  SavedRoutesController.new,
);

class SavedRoutesController extends Notifier<SavedRoutesState> {
  RouteRepository get _repository => ref.read(routeRepositoryProvider);

  @override
  SavedRoutesState build() {
    return const SavedRoutesState();
  }

  Future<void> loadMyRoutes() async {
    state = state.copyWith(isLoading: true, errorMessage: null);
    try {
      final list = await _repository.listMyRoutes();
      state = state.copyWith(isLoading: false, routes: list);
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: 'Não foi possível carregar os trajetos salvos.',
      );
    }
  }

  Future<RouteResponseModel> saveRoute(CreateRouteRequest request) async {
    state = state.copyWith(isLoading: true, errorMessage: null);
    try {
      final saved = await _repository.createRoute(request);
      state = state.copyWith(
        isLoading: false,
        routes: [saved, ...state.routes],
      );
      return saved;
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: 'Erro ao salvar o trajeto.',
      );
      rethrow;
    }
  }

  /// Salva o trajeto localmente no estado da aplicação quando não autenticado
  /// ou como fallback resiliente contra falhas de conexão/sessão.
  RouteResponseModel saveLocalRoute(CreateRouteRequest request) {
    final localRoute = RouteResponseModel(
      id: 'local_${DateTime.now().millisecondsSinceEpoch}',
      type: request.type,
      status: 'ACTIVE',
      vehicleId: request.vehicleId,
      origin: request.origin,
      destination: request.destination,
      departureTime: request.departureTime,
      estimatedArrivalTime: request.estimatedArrivalTime,
      weekDays: request.weekDays,
      maxDetourKm: request.maxDetourKm,
      totalSeats: request.totalSeats,
      availableSeats: request.totalSeats,
      routeDistanceKm: null,
      encodedPolyline: null,
      createdAt: DateTime.now().toIso8601String(),
      updatedAt: DateTime.now().toIso8601String(),
    );
    state = state.copyWith(
      isLoading: false,
      routes: [localRoute, ...state.routes],
    );
    return localRoute;
  }

  Future<void> deleteRoute(String routeId) async {
    try {
      await _repository.deleteRoute(routeId);
      state = state.copyWith(
        routes: state.routes.where((r) => r.id != routeId).toList(),
      );
    } catch (e) {
      state = state.copyWith(errorMessage: 'Erro ao excluir o trajeto.');
      rethrow;
    }
  }
}
