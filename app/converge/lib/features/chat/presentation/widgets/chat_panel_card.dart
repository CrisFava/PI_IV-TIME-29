// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// Faixa fixa no topo da conversa, com um ícone, um texto e as ações do momento.
class ChatPanelCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? subtitle;
  final bool success;
  final List<Widget> actions;

  const ChatPanelCard({
    super.key,
    required this.icon,
    required this.title,
    this.subtitle,
    this.success = false,
    this.actions = const [],
  });

  @override
  Widget build(BuildContext context) {
    final accent = success ? AppColors.success : AppColors.textPrimary;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(16, 10, 16, 10),
      decoration: BoxDecoration(
        color: success ? AppColors.successBackground : AppColors.cardBackground,
        border: Border(
          bottom: BorderSide(
            color: success ? AppColors.successBorder : AppColors.borderSubtle,
          ),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.only(top: 1),
                child: Icon(icon, size: 18, color: accent),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: TextStyle(
                        fontSize: 13.5,
                        fontWeight: FontWeight.w700,
                        color: accent,
                      ),
                    ),
                    if (subtitle != null) ...[
                      const SizedBox(height: 2),
                      Text(
                        subtitle!,
                        style: const TextStyle(
                          fontSize: 12,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
          if (actions.isNotEmpty) ...[
            const SizedBox(height: 8),
            Wrap(spacing: 8, runSpacing: 6, children: actions),
          ],
        ],
      ),
    );
  }
}
