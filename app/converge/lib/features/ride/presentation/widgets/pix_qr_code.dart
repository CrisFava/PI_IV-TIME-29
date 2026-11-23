// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../../../core/config/styles.dart';

/// QR Code do Pix Copia e Cola (RF39), desenhado no próprio app a partir do código.
/// Serve para pagar com outro celular; no mesmo aparelho, o caminho é copiar o código.
class PixQrCode extends StatelessWidget {
  /// Pix Copia e Cola que vira QR Code.
  final String pixCode;

  /// Lado do QR Code em pixels lógicos.
  final double size;

  const PixQrCode({super.key, required this.pixCode, this.size = 184});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: AppColors.cardBackground,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppColors.borderSubtle),
        ),
        child: QrImageView(
          data: pixCode,
          size: size,
          padding: EdgeInsets.zero,
          backgroundColor: AppColors.cardBackground,
          eyeStyle: const QrEyeStyle(
            eyeShape: QrEyeShape.square,
            color: AppColors.textPrimary,
          ),
          dataModuleStyle: const QrDataModuleStyle(
            dataModuleShape: QrDataModuleShape.square,
            color: AppColors.textPrimary,
          ),
          semanticsLabel: 'QR Code do PIX',
        ),
      ),
    );
  }
}
