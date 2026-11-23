// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/foundation.dart';

import 'payment_status.dart';

/// Situação do pagamento de um passageiro, conforme `PaymentResponseDTO`.
@immutable
class RidePaymentModel {
  /// Viagem da cobrança.
  final String rideId;

  /// Passageiro cobrado.
  final String passengerId;

  /// Motorista, quem recebe.
  final String driverId;

  /// Nome do motorista, exibido no pagamento.
  final String receiverName;

  /// Valor cobrado: o do rateio, definido no fim da viagem do passageiro.
  /// `null` enquanto a cobrança não foi emitida.
  final double? amount;

  /// Situação do pagamento.
  final PaymentStatus status;

  /// Pix Copia e Cola da cobrança. Só vem enquanto ela está em aberto.
  final String? pixCopyPaste;

  /// Quando a cobrança foi emitida.
  final DateTime? requestedAt;

  /// Quando o passageiro informou o pagamento.
  final DateTime? paidAt;

  const RidePaymentModel({
    required this.rideId,
    required this.passengerId,
    required this.driverId,
    required this.receiverName,
    required this.amount,
    required this.status,
    required this.pixCopyPaste,
    required this.requestedAt,
    required this.paidAt,
  });

  /// Cobrança já emitida (tem valor).
  bool get isRequested => amount != null;

  /// Passageiro já informou o pagamento.
  bool get isPaid => status == PaymentStatus.paid;

  /// Cobrança emitida e ainda não paga: o passageiro não pode pedir nem oferecer carona.
  bool get isOpen => isRequested && status == PaymentStatus.pending;

  /// Lê o pagamento do JSON da API.
  factory RidePaymentModel.fromJson(Map<String, dynamic> json) =>
      RidePaymentModel(
        rideId: json['rideId'] as String? ?? '',
        passengerId: json['passengerId'] as String? ?? '',
        driverId: json['driverId'] as String? ?? '',
        receiverName: json['receiverName'] as String? ?? '',
        amount: (json['amount'] as num?)?.toDouble(),
        status: PaymentStatus.fromJson(json['status']),
        pixCopyPaste: json['pixCopyPaste'] as String?,
        requestedAt: DateTime.tryParse(json['requestedAt'] as String? ?? ''),
        paidAt: DateTime.tryParse(json['paidAt'] as String? ?? ''),
      );
}
