// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

/// Horário e dias de um trajeto, no formato da API (`HH:mm` e `MONDAY`...).
/// O matching só junta saídas com até 30 min de diferença e com um dia em comum.
@immutable
class RouteSchedule {
  /// Tempo de viagem usado na chegada prevista enquanto a rota real não é calculada.
  static const Duration defaultTripDuration = Duration(minutes: 45);

  static const List<String> _apiWeekDays = [
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY',
    'SUNDAY',
  ];

  final String departureTime;
  final String estimatedArrivalTime;
  final Set<String> weekDays;

  const RouteSchedule({
    required this.departureTime,
    required this.estimatedArrivalTime,
    required this.weekDays,
  });

  /// Carona na hora (expediente e pedido de carona): sai agora, no dia de hoje.
  factory RouteSchedule.startingAt(
    DateTime now, {
    Duration tripDuration = defaultTripDuration,
  }) {
    final arrival = now.add(tripDuration);
    return RouteSchedule(
      departureTime: _hhmm(now),
      estimatedArrivalTime: _hhmm(arrival),
      weekDays: {_apiWeekDays[now.weekday - 1]},
    );
  }

  /// Trajeto frequente salvo pelo usuário: dias úteis, às 08:00.
  static const RouteSchedule weekdayCommute = RouteSchedule(
    departureTime: '08:00',
    estimatedArrivalTime: '08:45',
    weekDays: {'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'},
  );

  static String _hhmm(DateTime time) =>
      '${time.hour.toString().padLeft(2, '0')}:'
      '${time.minute.toString().padLeft(2, '0')}';
}
