import 'dart:ui';
import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';

/// ============================================================================
/// COMPONENTE: HomeFloatingHeader (Camada Superior - Ilha Flutuante de Boas-Vindas)
/// ============================================================================
/// Card flutuante translúcido posicionado no topo, com efeito de desfoque
/// (BackdropFilter), identificação da universidade ativa e saudação do usuário.
/// ============================================================================
class HomeFloatingHeader extends StatelessWidget {
  final String userName;
  final String universityName;
  final VoidCallback onLogout;

  const HomeFloatingHeader({
    super.key,
    required this.userName,
    required this.universityName,
    required this.onLogout,
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
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
              decoration: BoxDecoration(
                color: AppColors.cardBackground.withValues(alpha: 0.90),
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
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Linha Superior: Logo + Badge de Universidade + Ação Sair
                  Row(
                    children: [
                      Container(
                        width: 28,
                        height: 28,
                        decoration: BoxDecoration(
                          color: AppColors.primary,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        alignment: Alignment.center,
                        child: Image.asset(
                          'assets/icon.png',
                          width: 18,
                          height: 18,
                          fit: BoxFit.contain,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 4,
                        ),
                        decoration: BoxDecoration(
                          color: AppColors.neutralLight,
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(color: AppColors.borderSubtle),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Icon(
                              Icons.school_rounded,
                              size: 13,
                              color: AppColors.textPrimary,
                            ),
                            const SizedBox(width: 5),
                            Text(
                              universityName.isNotEmpty
                                  ? universityName
                                  : 'Universidade',
                              style: const TextStyle(
                                fontSize: 11.5,
                                fontWeight: FontWeight.w700,
                                color: AppColors.textPrimary,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const Spacer(),
                      IconButton(
                        tooltip: 'Sair da conta',
                        visualDensity: VisualDensity.compact,
                        onPressed: onLogout,
                        icon: const Icon(
                          Icons.logout_rounded,
                          size: 19,
                          color: AppColors.textPrimary,
                        ),
                      ),
                    ],
                  ),

                  const SizedBox(height: 10),

                  // Linha Inferior: Saudação e Subtítulo
                  Text(
                    'Olá, $userName!',
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w800,
                      color: AppColors.textPrimary,
                      letterSpacing: -0.3,
                    ),
                  ),
                  const SizedBox(height: 2),
                  const Text(
                    'Conecte-se com motoristas universitários para o seu trajeto.',
                    style: TextStyle(
                      fontSize: 12.5,
                      color: AppColors.textSecondary,
                      height: 1.3,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
