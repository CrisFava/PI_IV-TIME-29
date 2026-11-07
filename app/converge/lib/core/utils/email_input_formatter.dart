// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:flutter/services.dart';

/// Formata e normaliza o e-mail removendo espaços em branco e convertendo para minúsculas.
class EmailInputFormatter extends TextInputFormatter {
  const EmailInputFormatter();

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final sanitized = newValue.text.replaceAll(' ', '').toLowerCase();
    return TextEditingValue(
      text: sanitized,
      selection: TextSelection.collapsed(offset: sanitized.length),
    );
  }
}
