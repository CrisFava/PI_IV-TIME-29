// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

const _months = [
  'jan',
  'fev',
  'mar',
  'abr',
  'mai',
  'jun',
  'jul',
  'ago',
  'set',
  'out',
  'nov',
  'dez',
];

/// Rótulo curto do período no eixo do gráfico: o dia em que a semana começa
/// (`16/11`, segunda-feira), o mês (`nov`) ou o semestre (`2º/26`).
String indicatorSeriesLabel(String reference) {
  final week = RegExp(r'^(\d{4})-W(\d{2})$').firstMatch(reference);
  if (week != null) {
    final monday = _mondayOfIsoWeek(
      int.parse(week.group(1)!),
      int.parse(week.group(2)!),
    );
    return '${_twoDigits(monday.day)}/${_twoDigits(monday.month)}';
  }

  // Semestre: "2º/26"
  final semester = RegExp(r'^\d{2}(\d{2})-S([12])$').firstMatch(reference);
  if (semester != null) return '${semester.group(2)}º/${semester.group(1)}';

  final month = RegExp(r'^\d{4}-(\d{2})$').firstMatch(reference);
  if (month != null) return _months[int.parse(month.group(1)!) - 1];

  return reference;
}

// A semana 1 do ISO é a que tem o dia 4 de janeiro; as semanas começam na segunda
DateTime _mondayOfIsoWeek(int year, int week) {
  final january4 = DateTime.utc(year, 1, 4);
  final firstMonday = january4.subtract(Duration(days: january4.weekday - 1));
  return firstMonday.add(Duration(days: 7 * (week - 1)));
}

String _twoDigits(int value) => value.toString().padLeft(2, '0');
