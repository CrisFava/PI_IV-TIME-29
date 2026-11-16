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

/// Rótulo curto do período no eixo do gráfico: `S47` (semana) ou `nov` (mês).
String indicatorSeriesLabel(String reference) {
  final week = RegExp(r'^\d{4}-W(\d{2})$').firstMatch(reference);
  if (week != null) return 'S${int.parse(week.group(1)!)}';

  final month = RegExp(r'^\d{4}-(\d{2})$').firstMatch(reference);
  if (month != null) return _months[int.parse(month.group(1)!) - 1];

  return reference;
}
