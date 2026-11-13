// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/vehicle/data/repository/vehicle_repository_impl.dart';
import 'package:converge/features/vehicle/data/validation/invalid_vehicle_exception.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes/valid_vehicle_request.dart';

void main() {
  test('dados inválidos lançam exceção sem chamar a API', () async {
    var requests = 0;
    final dio = Dio()
      ..interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            requests++;
            handler.reject(DioException(requestOptions: options));
          },
        ),
      );
    final repository = VehicleRepositoryImpl(dio);

    await expectLater(
      repository.createVehicle(validVehicleRequest(licensePlate: 'XX')),
      throwsA(isA<InvalidVehicleException>()),
    );
    expect(requests, 0);
  });
}
