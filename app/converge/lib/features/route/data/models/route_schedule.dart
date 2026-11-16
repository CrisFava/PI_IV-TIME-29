// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

import 'route_recurrence.dart';

/// Horário e dias de um trajeto, no formato da API (`HH:mm` e `MONDAY`...).
/// A carona de um dia só leva a data (`yyyy-MM-dd`); a semanal, só os dias.
/// O matching só junta saídas com até 30 min de diferença e com um dia em comum.
@immutable
class RouteSchedule {
  /// Tempo de viagem usado na chegada prevista enquanto a rota real não é calculada.
  static const Duration defaultTripDuration = Duration(minutes: 45);

  /// Carona oferecida "Agora": sai daqui a pouco, para dar tempo de alguém pedir
  /// vaga. Com a saída no minuto atual, ela já teria "saído" para o servidor e não
  /// apareceria para nenhum passageiro.
  static const Duration immediateOfferLead = Duration(minutes: 15);

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
  final RouteRecurrence recurrence;
  final String? date;

  const RouteSchedule({
    required this.departureTime,
    required this.estimatedArrivalTime,
    required this.weekDays,
    this.recurrence = RouteRecurrence.weekly,
    this.date,
  });

  /// Carona no dia e horário escolhidos: por padrão só nessa data; com
  /// [repeatWeekly], toda semana no mesmo dia e horário.
  factory RouteSchedule.startingAt(
    DateTime departure, {
    bool repeatWeekly = false,
    Duration tripDuration = defaultTripDuration,
  }) {
    final arrival = departure.add(tripDuration);
    return RouteSchedule(
      departureTime: _hhmm(departure),
      estimatedArrivalTime: _hhmm(arrival),
      weekDays: {_apiWeekDays[departure.weekday - 1]},
      recurrence: repeatWeekly ? RouteRecurrence.weekly : RouteRecurrence.once,
      date: repeatWeekly ? null : _isoDate(departure),
    );
  }

  /// Trajeto frequente salvo pelo usuário: dias úteis, às 08:00.
  static const RouteSchedule weekdayCommute = RouteSchedule(
    departureTime: '08:00',
    estimatedArrivalTime: '08:45',
    weekDays: {'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'},
  );

  static String _isoDate(DateTime day) =>
      '${day.year}-${day.month.toString().padLeft(2, '0')}-'
      '${day.day.toString().padLeft(2, '0')}';

  static String _hhmm(DateTime time) =>
      '${time.hour.toString().padLeft(2, '0')}:'
      '${time.minute.toString().padLeft(2, '0')}';
}
