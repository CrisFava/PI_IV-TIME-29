// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../../../../../core/config/styles.dart';

/// Painel retrátil das etapas de criar ou pedir carona. Abre baixo, para o mapa
/// continuar em foco, e pode ser puxado para cima ou encolhido. Só fecha
/// arrastando até embaixo.
class RideFlowSheetFrame extends StatelessWidget {
  final double initialSize;
  final double bottomSafeArea;
  final List<Widget> children;

  const RideFlowSheetFrame({
    super.key,
    required this.children,
    this.initialSize = 0.44,
    this.bottomSafeArea = 0,
  });

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      initialChildSize: initialSize,
      // O mínimo fica abaixo do menor encaixe: arrastar um pouco só encolhe o
      // painel (para 22%); fechar (SheetDragToDismiss) exige levar até embaixo
      minChildSize: 0.10,
      maxChildSize: 0.88,
      snap: true,
      snapSizes: [0.22, initialSize, 0.88],
      builder: (context, scrollController) {
        return Container(
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: const BorderRadius.vertical(top: Radius.circular(26)),
            border: Border.all(color: AppColors.borderSubtle),
            boxShadow: [
              BoxShadow(
                color: AppColors.primary.withValues(alpha: 0.12),
                blurRadius: 24,
                offset: const Offset(0, -6),
              ),
            ],
          ),
          child: ListView(
            controller: scrollController,
            padding: EdgeInsets.fromLTRB(16, 0, 16, 120 + bottomSafeArea),
            children: [
              Center(
                child: Container(
                  margin: const EdgeInsets.symmetric(vertical: 10),
                  width: 38,
                  height: 4,
                  decoration: BoxDecoration(
                    color: AppColors.borderMedium,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              ...children,
            ],
          ),
        );
      },
    );
  }
}
