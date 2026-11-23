// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Fecha um painel arrastável (`DraggableScrollableSheet`) quando o usuário o arrasta
/// para baixo até perto do tamanho mínimo.
///
/// Envolva o painel (ou um ancestral dele): as notificações de arraste sobem pela árvore.
/// Para o painel continuar podendo ficar "minimizado", deixe o `minChildSize` dele um
/// pouco abaixo do menor tamanho de encaixe (`snapSizes.first`): só descer além do
/// encaixe, até `minExtent + dismissMargin`, fecha o painel.
class SheetDragToDismiss extends StatefulWidget {
  final VoidCallback onDismiss;
  final Widget child;

  /// Quanto acima do `minChildSize` o painel já conta como fechado.
  final double dismissMargin;

  const SheetDragToDismiss({
    super.key,
    required this.onDismiss,
    required this.child,
    this.dismissMargin = 0.07,
  });

  @override
  State<SheetDragToDismiss> createState() => _SheetDragToDismissState();
}

class _SheetDragToDismissState extends State<SheetDragToDismiss> {
  // Evita chamar onDismiss várias vezes durante o mesmo arraste
  bool _dismissed = false;

  bool _onNotification(DraggableScrollableNotification notification) {
    final threshold = notification.minExtent + widget.dismissMargin;

    if (notification.extent > threshold) {
      // O painel voltou a subir (ou um painel novo apareceu no mesmo lugar)
      _dismissed = false;
    } else if (!_dismissed) {
      _dismissed = true;
      HapticFeedback.lightImpact();
      widget.onDismiss();
    }
    return false;
  }

  @override
  Widget build(BuildContext context) {
    return NotificationListener<DraggableScrollableNotification>(
      onNotification: _onNotification,
      child: widget.child,
    );
  }
}
