// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../config/styles.dart';

/// Uma linha do [DetailsSummaryCard]: ícone, o que é e o valor.
typedef DetailsSummaryRow = ({IconData icon, String label, String value});

/// Um resumo em cartão único, uma linha por item: o ícone e o nome à esquerda e
/// o valor à direita. Substitui a grade de cartões lado a lado, que ficava com
/// alturas diferentes quando um texto quebrava de linha.
class DetailsSummaryCard extends StatelessWidget {
  final List<DetailsSummaryRow> rows;

  const DetailsSummaryCard({super.key, required this.rows});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
      decoration: BoxDecoration(
        color: AppColors.neutralLight,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Column(
        children: [
          for (final row in rows)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 7),
              child: Row(
                children: [
                  Icon(row.icon, size: 18, color: AppColors.textPrimary),
                  const SizedBox(width: 10),
                  Flexible(
                    child: Text(
                      row.label,
                      style: const TextStyle(
                        fontSize: 13.5,
                        color: AppColors.textDark,
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      row.value,
                      textAlign: TextAlign.right,
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w800,
                        color: AppColors.textPrimary,
                      ),
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}
