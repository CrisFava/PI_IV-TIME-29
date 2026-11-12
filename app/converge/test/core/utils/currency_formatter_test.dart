// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/core/utils/currency_formatter.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('CurrencyFormatter.format', () {
    test('usa vírgula nos centavos e sempre duas casas', () {
      expect(CurrencyFormatter.format(12.5), 'R\$ 12,50');
      expect(CurrencyFormatter.format(0), 'R\$ 0,00');
      expect(CurrencyFormatter.format(7), 'R\$ 7,00');
    });

    test('separa os milhares com ponto', () {
      expect(CurrencyFormatter.format(1234.5), 'R\$ 1.234,50');
      expect(CurrencyFormatter.format(999999.99), 'R\$ 999.999,99');
      expect(CurrencyFormatter.format(100), 'R\$ 100,00');
    });

    test('mantém o sinal de valores negativos', () {
      expect(CurrencyFormatter.format(-3.2), '-R\$ 3,20');
    });
  });

  group('CurrencyFormatter.parse', () {
    test('aceita vírgula ou ponto como separador decimal', () {
      expect(CurrencyFormatter.parse('12,5'), 12.5);
      expect(CurrencyFormatter.parse('12.50'), 12.5);
      expect(CurrencyFormatter.parse('8'), 8);
    });

    test('ignora o símbolo da moeda e o separador de milhar', () {
      expect(CurrencyFormatter.parse('R\$ 1.234,56'), 1234.56);
      expect(CurrencyFormatter.parse(' R\$ 10,00 '), 10);
    });

    test('arredonda para duas casas', () {
      expect(CurrencyFormatter.parse('10,999'), 11);
      expect(CurrencyFormatter.parse('10,994'), 10.99);
    });

    test('recusa valores negativos em vez de inverter o sinal', () {
      expect(CurrencyFormatter.parse('-R\$ 3,20'), isNull);
      expect(CurrencyFormatter.parse('R\$ -3,20'), isNull);
      expect(CurrencyFormatter.parse('-12.5'), isNull);
    });

    test('devolve null quando o texto não é um valor', () {
      expect(CurrencyFormatter.parse(''), isNull);
      expect(CurrencyFormatter.parse('abc'), isNull);
      expect(CurrencyFormatter.parse('1,2,3'), isNull);
    });
  });
}
