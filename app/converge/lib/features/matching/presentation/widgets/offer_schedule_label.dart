// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

const _order = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];
const _short = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];
const _full = [
  'segunda',
  'terça',
  'quarta',
  'quinta',
  'sexta',
  'sábado',
  'domingo',
];
const _weekdays = {'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'};

/// Quando a carona oferecida sai, toda semana: `Toda segunda às 07:30`,
/// `Dias úteis às 08:00`, `Todos os dias às 07:00` ou `Seg, Qua e Sex às 07:30`.
String offerScheduleLabel(Set<String> weekDays, String departureTime) {
  final days = [
    for (var i = 0; i < _order.length; i++)
      if (weekDays.contains(_order[i])) i,
  ];

  final String when;
  if (days.length == 7) {
    when = 'Todos os dias';
  } else if (weekDays.length == 5 && weekDays.containsAll(_weekdays)) {
    when = 'Dias úteis';
  } else if (days.length == 1) {
    final day = _full[days.single];
    when = days.single >= 5 ? 'Todo $day' : 'Toda $day';
  } else if (days.isEmpty) {
    when = 'Sem dia definido';
  } else {
    final names = [for (final d in days) _short[d]];
    when = '${names.sublist(0, names.length - 1).join(', ')} e ${names.last}';
  }
  return '$when às $departureTime';
}
