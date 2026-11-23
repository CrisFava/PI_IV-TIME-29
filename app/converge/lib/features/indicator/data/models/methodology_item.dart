// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/foundation.dart';

/// Um item da metodologia dos cálculos (RF61): título e explicação.
@immutable
class MethodologyItem {
  // Títulos para as chaves que o backend devolve em `GET /indicators/methodology`
  static const Map<String, String> _titles = {
    'aviso': 'Estimativas',
    'co2': 'CO₂ evitado',
    'carroEvitado': 'Carros evitados',
    'arvoreEquivalente': 'Árvores equivalentes',
    'fonteCombustiveis': 'Fatores dos combustíveis',
    'fonteEletricidade': 'Fator da energia elétrica',
  };

  final String title;
  final String text;

  const MethodologyItem({required this.title, required this.text});

  /// Os itens na ordem acima; chaves novas do backend aparecem no fim com o próprio nome.
  static List<MethodologyItem> listFromJson(Map<String, dynamic> json) => [
    for (final key in _titles.keys)
      if (json[key] is String)
        MethodologyItem(title: _titles[key]!, text: json[key] as String),
    for (final entry in json.entries)
      if (!_titles.containsKey(entry.key) && entry.value is String)
        MethodologyItem(title: entry.key, text: entry.value as String),
  ];
}
