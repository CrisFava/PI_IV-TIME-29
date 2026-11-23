// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

const _weekDays = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];

String _twoDigits(int value) => value.toString().padLeft(2, '0');

/// Texto da saída da carona: `Hoje às 18:30`, `Amanhã às 07:00` ou
/// `Seg, 16/11 às 07:00`. Sem horário escolhido: `Escolher dia e horário`.
String rideDepartureLabel(DateTime? departure, DateTime now) {
  if (departure == null) return 'Escolher dia e horário';

  final time = '${_twoDigits(departure.hour)}:${_twoDigits(departure.minute)}';
  final today = DateTime(now.year, now.month, now.day);
  final day = DateTime(departure.year, departure.month, departure.day);
  final daysAhead = day.difference(today).inDays;

  return switch (daysAhead) {
    0 => 'Hoje às $time',
    1 => 'Amanhã às $time',
    _ =>
      '${_weekDays[departure.weekday - 1]}, '
          '${_twoDigits(departure.day)}/${_twoDigits(departure.month)} às $time',
  };
}
