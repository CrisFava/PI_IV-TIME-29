// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

/// Período de agregação dos indicadores, conforme `IndicatorPeriod` do backend.
enum IndicatorPeriod {
  weekly('WEEKLY', 'Semana'),
  monthly('MONTHLY', 'Mês'),
  semesterly('SEMESTERLY', 'Semestre'),
  allTime('ALL_TIME', 'Total');

  final String apiValue;
  final String label;

  const IndicatorPeriod(this.apiValue, this.label);

  /// Referência do período que contém [date], no formato do backend
  /// (`IndicatorReference`): `2026-W47`, `2026-11`, `2026-S2`. O total não tem.
  /// Sem ela, o backend devolve o último período com dados, e não o atual.
  String? referenceFor(DateTime date) => switch (this) {
    weekly => _isoWeek(date),
    monthly => '${date.year}-${_twoDigits(date.month)}',
    semesterly => '${date.year}-S${date.month <= 6 ? 1 : 2}',
    allTime => null,
  };

  static String _twoDigits(int value) => value.toString().padLeft(2, '0');

  // Semana ISO 8601: começa na segunda; a semana 1 é a que tem a primeira quinta do ano
  static String _isoWeek(DateTime date) {
    final day = DateTime.utc(date.year, date.month, date.day);
    final thursday = day.add(Duration(days: DateTime.thursday - day.weekday));
    final firstDay = DateTime.utc(thursday.year, 1, 1);
    final week = thursday.difference(firstDay).inDays ~/ 7 + 1;
    return '${thursday.year}-W${_twoDigits(week)}';
  }
}
