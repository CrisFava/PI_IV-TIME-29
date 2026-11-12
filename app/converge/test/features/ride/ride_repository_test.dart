// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/core/network/api_exception.dart';
import 'package:converge/core/network/interceptors/error_interceptor.dart';
import 'package:converge/features/ride/data/models/payment_status.dart';
import 'package:converge/features/ride/data/models/ride_status.dart';
import 'package:converge/features/ride/data/repository/ride_endpoints.dart';
import 'package:converge/features/ride/data/repository/ride_repository_impl.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes/fake_ride.dart';

void main() {
  late Dio dio;
  late RideRepositoryImpl repository;
  late List<RequestOptions> requests;

  /// Responde toda requisição com [body] e guarda o que foi pedido.
  void respondWith(Object? body, {int status = 200}) {
    dio.interceptors.insert(
      0,
      InterceptorsWrapper(
        onRequest: (options, handler) {
          requests.add(options);
          final response = Response<dynamic>(
            requestOptions: options,
            statusCode: status,
            data: body,
          );
          if (status >= 400) {
            handler.reject(
              DioException(requestOptions: options, response: response),
              true,
            );
          } else {
            handler.resolve(response);
          }
        },
      ),
    );
  }

  Map<String, dynamic> success(Object? data) => {
    'success': true,
    'data': data,
    'error': null,
    'timestamp': '2026-11-12T12:00:00Z',
  };

  setUp(() {
    dio = Dio()..interceptors.add(ErrorInterceptor());
    repository = RideRepositoryImpl(dio);
    requests = [];
  });

  test('listMyRides lê a lista de viagens do envelope', () async {
    respondWith(success([fakeRideJson(), fakeRideJson(status: 'COMPLETED')]));

    final rides = await repository.listMyRides();

    expect(requests.single.method, 'GET');
    expect(requests.single.path, RideEndpoints.rides);
    expect(rides.map((ride) => ride.status), [
      RideStatus.confirmed,
      RideStatus.completed,
    ]);
  });

  test(
    'requestSeat envia os trajetos e a data só com ano, mês e dia',
    () async {
      respondWith(
        success(fakeRideJson(status: 'AWAITING_CONFIRMATION')),
        status: 201,
      );

      await repository.requestSeat(
        driverRouteId: 'route-driver',
        passengerRouteId: 'route-passenger',
        date: DateTime(2026, 3, 9, 18, 45),
      );

      expect(requests.single.method, 'POST');
      expect(requests.single.path, RideEndpoints.request);
      expect(requests.single.data, {
        'driverRouteId': 'route-driver',
        'passengerRouteId': 'route-passenger',
        'date': '2026-03-09',
      });
    },
  );

  test(
    'aceitar e recusar passageiro usam o endpoint de cada passageiro',
    () async {
      respondWith(success(fakeRideJson()));

      await repository.acceptPassenger('ride-1', 'passenger-1');
      await repository.rejectPassenger(
        'ride-1',
        'passenger-2',
        reason: 'Lotado',
      );

      expect(requests[0].path, '/rides/ride-1/passengers/passenger-1/accept');
      expect(requests[1].path, '/rides/ride-1/passengers/passenger-2/reject');
      expect(requests[1].data, {'reason': 'Lotado'});
    },
  );

  test('iniciar e concluir a viagem devolvem a viagem atualizada', () async {
    respondWith(success(fakeRideJson(status: 'IN_PROGRESS')));

    final started = await repository.startRide('ride-1');
    respondWith(success(fakeRideJson(status: 'COMPLETED')));
    final completed = await repository.completeRide('ride-1');

    expect(started.status, RideStatus.inProgress);
    expect(completed.status, RideStatus.completed);
    expect(requests[0].path, '/rides/ride-1/start');
    expect(requests[1].path, '/rides/ride-1/complete');
  });

  test('requestPayment envia o valor com duas casas e lê a cobrança', () async {
    respondWith(
      success({
        'rideId': 'ride-1',
        'passengerId': 'passenger-1',
        'driverId': 'driver-1',
        'receiverName': 'Lucas Silveira',
        'amount': 12.5,
        'status': 'PENDING',
        'confirmedByDriver': false,
        'pixCopyPaste': '00020126...6304ABCD',
        'requestedAt': '2026-11-16T07:00:00',
        'paidAt': null,
      }),
    );

    final payment = await repository.requestPayment(
      'ride-1',
      'passenger-1',
      amount: 12.5,
    );

    expect(requests.single.method, 'POST');
    expect(
      requests.single.path,
      '/rides/ride-1/passengers/passenger-1/payment/request',
    );
    expect(requests.single.data, {'amount': '12.50'});
    expect(payment.amount, 12.5);
    expect(payment.isOpen, isTrue);
    expect(payment.pixCopyPaste, '00020126...6304ABCD');
  });

  test('confirmPayment e getPayment usam os endpoints de pagamento', () async {
    respondWith(
      success({
        'rideId': 'ride-1',
        'passengerId': 'passenger-1',
        'driverId': 'driver-1',
        'receiverName': 'Lucas Silveira',
        'amount': 12.5,
        'status': 'PAID',
        'confirmedByDriver': true,
        'pixCopyPaste': null,
      }),
    );

    final confirmed = await repository.confirmPayment('ride-1', 'passenger-1');
    await repository.getPayment('ride-1', 'passenger-1');

    expect(confirmed.status, PaymentStatus.paid);
    expect(requests[0].method, 'POST');
    expect(
      requests[0].path,
      '/rides/ride-1/passengers/passenger-1/payment/confirm',
    );
    expect(requests[1].method, 'GET');
    expect(requests[1].path, '/rides/ride-1/passengers/passenger-1/payment');
  });

  test(
    'erro do backend vira ApiException com a mensagem do envelope',
    () async {
      respondWith({
        'success': false,
        'data': null,
        'error': {
          'code': 'BAD_REQUEST',
          'message': 'Solicite o pagamento antes de confirmar o recebimento.',
          'fields': ['paymentStatus'],
        },
        'timestamp': '2026-11-12T12:00:00Z',
      }, status: 400);

      await expectLater(
        repository.confirmPayment('ride-1', 'passenger-1'),
        throwsA(
          isA<ApiException>()
              .having((e) => e.status, 'status', 400)
              .having(
                (e) => e.message,
                'message',
                'Solicite o pagamento antes de confirmar o recebimento.',
              )
              .having((e) => e.fields, 'fields', {'paymentStatus'}),
        ),
      );
    },
  );
}
