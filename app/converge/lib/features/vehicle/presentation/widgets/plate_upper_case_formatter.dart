// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/services.dart';

/// Converte a placa digitada para maiúsculas.
class PlateUpperCaseFormatter extends TextInputFormatter {
  const PlateUpperCaseFormatter();

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) => newValue.copyWith(text: newValue.text.toUpperCase());
}
