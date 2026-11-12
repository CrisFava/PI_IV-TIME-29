// Autor: Cristian Eduardo Fava - RA 25000636

/// Formatação e leitura de valores em reais.
abstract final class CurrencyFormatter {
  /// `1234.5` vira `R$ 1.234,50`.
  static String format(double value) {
    final fixed = value.abs().toStringAsFixed(2);
    final separator = fixed.indexOf('.');
    final integer = fixed.substring(0, separator);
    final cents = fixed.substring(separator + 1);

    final grouped = StringBuffer();
    for (var i = 0; i < integer.length; i++) {
      if (i > 0 && (integer.length - i) % 3 == 0) grouped.write('.');
      grouped.write(integer[i]);
    }

    return '${value < 0 ? '-' : ''}R\$ $grouped,$cents';
  }

  /// Lê o que o usuário digitou (`12,5`, `12.50`, `R$ 1.234,56`).
  /// Devolve `null` quando o texto não é um valor, inclusive valores negativos.
  static double? parse(String input) {
    // Recusa antes de limpar o texto, senão "-3,20" viraria 3,20.
    if (input.contains('-')) return null;

    var text = input.replaceAll(RegExp(r'[^0-9,.]'), '');
    if (text.isEmpty) return null;

    // Com vírgula, ela é o separador decimal e o ponto é o de milhar.
    if (text.contains(',')) {
      text = text.replaceAll('.', '').replaceAll(',', '.');
    }

    final value = double.tryParse(text);
    if (value == null) return null;

    return (value * 100).round() / 100;
  }
}
