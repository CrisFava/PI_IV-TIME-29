// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// Visual dos campos do cadastro de veículo (o mesmo do cadastro de motorista).
InputDecoration vehicleInputDecoration({
  required String label,
  required IconData icon,
  String? hint,
  String? suffix,
}) {
  OutlineInputBorder border(Color color, [double width = 1]) =>
      OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide(color: color, width: width),
      );

  return InputDecoration(
    labelText: label,
    hintText: hint,
    suffixText: suffix,
    prefixIcon: Icon(icon, size: 20, color: AppColors.textSecondary),
    filled: true,
    fillColor: AppColors.inputBackground,
    errorMaxLines: 3,
    border: border(AppColors.borderSubtle),
    enabledBorder: border(AppColors.borderSubtle),
    focusedBorder: border(AppColors.primary, 1.5),
    errorBorder: border(AppColors.errorBorder),
    focusedErrorBorder: border(AppColors.error, 1.5),
  );
}
