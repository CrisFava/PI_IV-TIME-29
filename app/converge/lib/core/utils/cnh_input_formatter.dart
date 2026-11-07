// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:flutter/services.dart';

/// Limita o número de registro da CNH em até 11 dígitos numéricos.
class CnhInputFormatter extends TextInputFormatter {
  const CnhInputFormatter();

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final digits = newValue.text.replaceAll(RegExp(r'\D'), '');
    final clipped = digits.length > 11 ? digits.substring(0, 11) : digits;

    return TextEditingValue(
      text: clipped,
      selection: TextSelection.collapsed(offset: clipped.length),
    );
  }
}
