// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../state/indicator_providers.dart';

/// Abre "Como calculamos?" (RF61): a metodologia e os fatores de emissão.
Future<void> showMethodologySheet(BuildContext context) {
  HapticFeedback.lightImpact();
  return showModalBottomSheet<void>(
    context: context,
    backgroundColor: AppColors.cardBackground,
    barrierColor: AppColors.barrier,
    isScrollControlled: true,
    useSafeArea: true,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(26)),
    ),
    builder: (_) => const MethodologySheet(),
  );
}

class MethodologySheet extends ConsumerWidget {
  const MethodologySheet({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final methodology = ref.watch(methodologyProvider);

    // Rola quando a metodologia passa da altura da folha
    return SingleChildScrollView(
      padding: EdgeInsets.fromLTRB(
        22,
        14,
        22,
        24 + MediaQuery.paddingOf(context).bottom,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Row(
            children: [
              Icon(
                Icons.science_rounded,
                size: 20,
                color: AppColors.textPrimary,
              ),
              SizedBox(width: 8),
              Text(
                'Como calculamos?',
                style: TextStyle(
                  fontSize: 17,
                  fontWeight: FontWeight.w800,
                  color: AppColors.textPrimary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          ...switch (methodology) {
            AsyncData(:final value) => [
              for (final item in value)
                Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.title,
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        item.text,
                        style: const TextStyle(
                          fontSize: 12.5,
                          color: AppColors.textSecondary,
                          height: 1.35,
                        ),
                      ),
                    ],
                  ),
                ),
            ],
            AsyncError(:final error) => [
              Text(
                authErrorMessage(error),
                style: const TextStyle(
                  fontSize: 13,
                  color: AppColors.errorText,
                ),
              ),
            ],
            _ => const [
              Padding(
                padding: EdgeInsets.all(20),
                child: Center(
                  child: CircularProgressIndicator(color: AppColors.primary),
                ),
              ),
            ],
          },
        ],
      ),
    );
  }
}
