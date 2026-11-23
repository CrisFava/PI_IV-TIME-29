// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// Faixa que ocupa o lugar do campo de mensagem quando o outro usuário está bloqueado.
/// O `ChatPage` mostra esta faixa quando a pessoa está na minha lista de bloqueados (RF47).
/// Só aparece para quem bloqueou: o bloqueio no chat é feito pelo app.
class BlockedChatBar extends StatelessWidget {
  const BlockedChatBar({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.fromLTRB(
        16,
        14,
        16,
        14 + MediaQuery.paddingOf(context).bottom,
      ),
      decoration: const BoxDecoration(
        color: AppColors.cardBackground,
        border: Border(top: BorderSide(color: AppColors.borderSubtle)),
      ),
      child: const Row(
        children: [
          Icon(Icons.block_rounded, size: 20, color: AppColors.textSecondary),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'Você bloqueou este usuário. Desbloqueie em Bloqueados, no seu '
              'perfil, para voltar a enviar mensagens.',
              style: TextStyle(fontSize: 13, color: AppColors.textSecondary),
            ),
          ),
        ],
      ),
    );
  }
}
