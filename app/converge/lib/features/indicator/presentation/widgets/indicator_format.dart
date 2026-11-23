// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

/// Números dos indicadores no formato brasileiro: `1.240`, `1.240 km`, `186 kg`.
String formatIndicatorInt(num value) {
  final digits = value.round().abs().toString();
  final grouped = StringBuffer();
  for (var i = 0; i < digits.length; i++) {
    if (i > 0 && (digits.length - i) % 3 == 0) grouped.write('.');
    grouped.write(digits[i]);
  }
  return '${value < 0 ? '-' : ''}$grouped';
}

/// Valores pequenos ganham uma casa decimal (`3,5`); os grandes, só a parte inteira.
String formatIndicatorDecimal(double value) {
  if (value.abs() >= 100) return formatIndicatorInt(value);
  final text = value.toStringAsFixed(1).replaceAll('.', ',');
  return text.endsWith(',0') ? text.substring(0, text.length - 2) : text;
}
