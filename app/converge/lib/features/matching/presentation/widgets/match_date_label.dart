// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

const _weekDays = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];

String _twoDigits(int value) => value.toString().padLeft(2, '0');

/// Ex.: `Seg, 16/11`.
String matchDateLabel(DateTime date) =>
    '${_weekDays[date.weekday - 1]}, ${_twoDigits(date.day)}/${_twoDigits(date.month)}';

/// Ex.: `Seg, 16/11 às 07:30`.
String matchDateTimeLabel(DateTime dateTime) =>
    '${matchDateLabel(dateTime)} às ${_twoDigits(dateTime.hour)}:${_twoDigits(dateTime.minute)}';
