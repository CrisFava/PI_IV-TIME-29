// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/features/ride/data/models/participant_status.dart';
import 'package:converge/features/ride/data/models/payment_status.dart';
import 'package:converge/features/ride/data/models/ride_model.dart';
import 'package:converge/features/ride/data/models/ride_payment_model.dart';
import 'package:converge/features/ride/data/models/ride_status.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes/fake_ride.dart';

void main() {
  group('RideModel.fromJson', () {
    test('lê os campos principais da viagem', () {
      final ride = fakeRide();

      expect(ride.id, fakeRideId);
      expect(ride.sequentialNumber, 7);
      expect(ride.driverId, fakeDriverId);
      expect(ride.status, RideStatus.confirmed);
      expect(ride.chatRoomId, 'ride_ride-1');
      expect(ride.origin.address, 'Rua das Flores, 50');
      expect(ride.destination.address, 'PUC-Campinas');
      expect(ride.departureDateTime, DateTime(2026, 11, 16, 7, 30));
      expect(ride.completedAt, isNull);
      expect(ride.vehicleModel, 'Onix');
      expect(ride.vehicleLicensePlate, 'ABC1D23');
      expect(ride.participants, hasLength(2));
      expect(ride.driverName, 'Lucas Silveira');
    });

    test('status desconhecido não quebra a leitura', () {
      final ride = fakeRide(status: 'NOVO_STATUS', passengerStatus: 'EXPIRED');

      expect(ride.status, RideStatus.unknown);
      expect(ride.passengerOf(fakePassengerId)!.status, ParticipantStatus.unknown);
    });

    test('JSON vazio vira uma viagem com valores neutros', () {
      final ride = RideModel.fromJson(const {});

      expect(ride.id, '');
      expect(ride.participants, isEmpty);
      expect(ride.chatRoomId, isNull);
      expect(ride.driverName, 'Motorista');
    });
  });

  group('RideModel.passengersWithSeat', () {
    test('traz só passageiros aceitos e identificáveis', () {
      final ride = fakeRide(
        extraParticipants: [
          {
            'userId': 'passenger-2',
            'name': 'Bruno Lima',
            'role': 'PASSENGER',
            'status': 'REQUESTED',
          },
          {
            'userId': 'passenger-3',
            'name': 'Carla Dias',
            'role': 'PASSENGER',
            'status': 'NO_SHOW',
          },
          // Visão básica: o backend esconde o id de quem o usuário não pode identificar.
          {
            'userId': null,
            'name': 'Diego Reis',
            'role': 'PASSENGER',
            'status': 'CONFIRMED',
          },
        ],
      );

      expect(ride.passengersWithSeat.map((p) => p.userId), [fakePassengerId]);
    });

    test('passageiro que concluiu a viagem continua na lista', () {
      final ride = fakeRide(status: 'COMPLETED', passengerStatus: 'COMPLETED');

      expect(ride.passengersWithSeat.single.status, ParticipantStatus.completed);
    });
  });

  group('RideModel.passengerOf', () {
    test('prefere a participação com vaga quando a pessoa pediu mais de uma vez', () {
      final ride = fakeRide(
        passengerStatus: 'CANCELLED',
        extraParticipants: [
          {
            'userId': fakePassengerId,
            'name': 'Ana Souza',
            'role': 'PASSENGER',
            'status': 'CONFIRMED',
          },
        ],
      );

      expect(ride.passengerOf(fakePassengerId)!.status, ParticipantStatus.confirmed);
    });

    test('devolve null para quem não é passageiro da viagem', () {
      final ride = fakeRide();

      expect(ride.passengerOf('desconhecido'), isNull);
      expect(ride.passengerOf(fakeDriverId), isNull);
    });
  });

  group('RidePaymentModel', () {
    test('sem valor, o pagamento ainda não foi solicitado', () {
      final payment = RidePaymentModel.fromJson(const {
        'rideId': fakeRideId,
        'passengerId': fakePassengerId,
        'driverId': fakeDriverId,
        'receiverName': 'Lucas Silveira',
        'amount': null,
        'status': 'PENDING',
        'confirmedByDriver': false,
        'pixCopyPaste': null,
      });

      expect(payment.isRequested, isFalse);
      expect(payment.isOpen, isFalse);
      expect(payment.isPaid, isFalse);
    });

    test('cobrança em aberto traz o valor e o código PIX', () {
      final payment = RidePaymentModel.fromJson(const {
        'rideId': fakeRideId,
        'passengerId': fakePassengerId,
        'driverId': fakeDriverId,
        'receiverName': 'Lucas Silveira',
        'amount': 12.5,
        'status': 'PENDING',
        'confirmedByDriver': false,
        'pixCopyPaste': '000201...',
        'requestedAt': '2026-11-16T07:00:00',
      });

      expect(payment.amount, 12.5);
      expect(payment.isOpen, isTrue);
      expect(payment.pixCopyPaste, '000201...');
      expect(payment.requestedAt, DateTime(2026, 11, 16, 7));
    });

    test('pago deixa de estar em aberto', () {
      final payment = RidePaymentModel.fromJson(const {
        'amount': 12.5,
        'status': 'PAID',
        'confirmedByDriver': true,
        'paidAt': '2026-11-16T09:00:00',
      });

      expect(payment.status, PaymentStatus.paid);
      expect(payment.isPaid, isTrue);
      expect(payment.isOpen, isFalse);
    });
  });
}
