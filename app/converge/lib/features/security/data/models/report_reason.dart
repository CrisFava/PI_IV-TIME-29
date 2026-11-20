// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

/// Motivo da denúncia, igual ao enum `ReportReason` do backend.
enum ReportReason {
  lateNoShow('LATE_NO_SHOW', 'Atraso ou ausência'),
  inappropriateConduct('INAPPROPRIATE_CONDUCT', 'Conduta inadequada'),
  improperCharge('IMPROPER_CHARGE', 'Cobrança indevida'),

  /// Só o motorista, sobre um passageiro da viagem que não pagou (inadimplência).
  nonPayment('NON_PAYMENT', 'Não pagou a carona'),
  other('OTHER', 'Outro motivo');

  final String value;
  final String label;

  const ReportReason(this.value, this.label);

  String toJson() => value;

  static ReportReason? fromJson(String? value) {
    for (final reason in values) {
      if (reason.value == value) return reason;
    }
    return null;
  }
}
