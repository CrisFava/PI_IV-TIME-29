// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:flutter/material.dart';

/// Constrói o rótulo estilizado de um botão de ação com texto e ícone direcional.
Widget buildActionButtonLabel(String text) {
  return Row(
    mainAxisAlignment: MainAxisAlignment.spaceBetween,
    children: [Text(text), const Icon(Icons.navigate_next)],
  );
}
