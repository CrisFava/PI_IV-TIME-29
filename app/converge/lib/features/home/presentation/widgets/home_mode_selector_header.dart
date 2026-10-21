import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';

/// Enum representando o modo de uso ativo na tela principal.
enum AppRideMode {
  passenger,
  driver;

  bool get isPassenger => this == AppRideMode.passenger;
  bool get isDriver => this == AppRideMode.driver;
}

/// ============================================================================
/// COMPONENTE: HomeModeSelectorHeader (Header com Seletor de Modo)
/// ============================================================================
/// Ilha flutuante translúcida com o Seletor de Modo (Passageiro vs Motorista):
/// - Bloqueio com cadeado (Icons.lock_rounded) caso não possua a role DRIVER.
/// - Redirecionamento para a aba de perfil ao tocar no cadeado.
/// ============================================================================
class HomeModeSelectorHeader extends StatelessWidget {
  final AppRideMode currentMode;
  final bool canDrive;
  final ValueChanged<AppRideMode> onModeChanged;
  final VoidCallback onLockedDriverTap;

  const HomeModeSelectorHeader({
    super.key,
    required this.currentMode,
    required this.canDrive,
    required this.onModeChanged,
    required this.onLockedDriverTap,
  });

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      bottom: false,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(22),
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 12, sigmaY: 12),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 6),
              decoration: BoxDecoration(
                color: AppColors.cardBackground.withValues(alpha: 0.92),
                borderRadius: BorderRadius.circular(22),
                border: Border.all(color: AppColors.borderSubtle, width: 1),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.08),
                    blurRadius: 18,
                    offset: const Offset(0, 6),
                  ),
                ],
              ),
              child: Container(
                padding: const EdgeInsets.all(4),
                decoration: BoxDecoration(
                  color: AppColors.neutralLight,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: AppColors.borderSubtle),
                ),
                child: Row(
                  children: [
                    // Botão Modo Passageiro
                    Expanded(
                      child: _buildModeTab(
                        title: 'Passageiro',
                        icon: Icons.person_rounded,
                        isSelected: currentMode.isPassenger,
                        isLocked: false,
                        onTap: () {
                          if (currentMode.isPassenger) return;
                          HapticFeedback.selectionClick();
                          onModeChanged(AppRideMode.passenger);
                        },
                      ),
                    ),

                    const SizedBox(width: 4),

                    // Botão Modo Motorista (Com status de permissão estrita)
                    Expanded(
                      child: _buildModeTab(
                        title: 'Motorista',
                        icon: canDrive ? Icons.directions_car_rounded : Icons.lock_rounded,
                        isSelected: currentMode.isDriver,
                        isLocked: !canDrive,
                        onTap: () {
                          if (!canDrive) {
                            onLockedDriverTap();
                            return;
                          }
                          if (currentMode.isDriver) return;
                          HapticFeedback.selectionClick();
                          onModeChanged(AppRideMode.driver);
                        },
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildModeTab({
    required String title,
    required IconData icon,
    required bool isSelected,
    required bool isLocked,
    required VoidCallback onTap,
  }) {
    return Material(
      color: isSelected ? AppColors.primary : Colors.transparent,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        splashColor: AppColors.primary.withValues(alpha: 0.12),
        highlightColor: Colors.transparent,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 240),
          curve: Curves.easeInOutCubic,
          padding: const EdgeInsets.symmetric(vertical: 9),
          alignment: Alignment.center,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(12),
            boxShadow: isSelected
                ? [
                    BoxShadow(
                      color: Colors.black.withValues(alpha: 0.18),
                      blurRadius: 8,
                      offset: const Offset(0, 2),
                    ),
                  ]
                : null,
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                icon,
                size: 17,
                color: isSelected
                    ? AppColors.textLight
                    : (isLocked ? AppColors.textMuted : AppColors.textSecondary),
              ),
              const SizedBox(width: 7),
              Text(
                title,
                style: TextStyle(
                  fontSize: 13.5,
                  fontWeight: isSelected ? FontWeight.w800 : FontWeight.w600,
                  color: isSelected
                      ? AppColors.textLight
                      : (isLocked ? AppColors.textMuted : AppColors.textSecondary),
                  letterSpacing: -0.2,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
