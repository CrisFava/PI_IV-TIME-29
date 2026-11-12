// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/foundation.dart';

import 'payment_status.dart';

/// Situação do pagamento de um passageiro, conforme `PaymentResponseDTO`.
@immutable
class RidePaymentModel {
  final String rideId;
  final String passengerId;
  final String driverId;
  final String receiverName;

  /// Valor combinado no chat. `null` enquanto o motorista não solicitou o pagamento.
  final double? amount;
  final PaymentStatus status;
  final bool confirmedByDriver;

  /// Pix Copia e Cola da cobrança. Só vem enquanto ela está em aberto.
  final String? pixCopyPaste;
  final DateTime? requestedAt;
  final DateTime? paidAt;

  const RidePaymentModel({
    required this.rideId,
    required this.passengerId,
    required this.driverId,
    required this.receiverName,
    required this.amount,
    required this.status,
    required this.confirmedByDriver,
    required this.pixCopyPaste,
    required this.requestedAt,
    required this.paidAt,
  });

  bool get isRequested => amount != null;
  bool get isPaid => status == PaymentStatus.paid;

  /// Cobrança enviada e ainda não confirmada pelo motorista.
  bool get isOpen => isRequested && status == PaymentStatus.pending;

  factory RidePaymentModel.fromJson(Map<String, dynamic> json) =>
      RidePaymentModel(
        rideId: json['rideId'] as String? ?? '',
        passengerId: json['passengerId'] as String? ?? '',
        driverId: json['driverId'] as String? ?? '',
        receiverName: json['receiverName'] as String? ?? '',
        amount: (json['amount'] as num?)?.toDouble(),
        status: PaymentStatus.fromJson(json['status']),
        confirmedByDriver: json['confirmedByDriver'] as bool? ?? false,
        pixCopyPaste: json['pixCopyPaste'] as String?,
        requestedAt: DateTime.tryParse(json['requestedAt'] as String? ?? ''),
        paidAt: DateTime.tryParse(json['paidAt'] as String? ?? ''),
      );
}
