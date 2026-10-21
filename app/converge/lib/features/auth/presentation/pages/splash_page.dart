import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../state/auth_action_controller.dart';
import '../../state/auth_controller.dart';

class SplashPage extends ConsumerWidget {
  const SplashPage({super.key});

  Widget _buildSplashContent() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Image.asset(
          'assets/converge_logo_1.png',
          width: 180,
          fit: BoxFit.contain,
        ),
        const SizedBox(height: 32),
        const CircularProgressIndicator(
          color: AppColors.primary,
          strokeWidth: 2.8,
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(authControllerProvider);

    return Scaffold(
      backgroundColor: Colors.white,
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: session.when(
            data: (_) => _buildSplashContent(),
            loading: () => _buildSplashContent(),
            error: (err, _) => Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Image.asset(
                  'assets/icon.png',
                  width: 120,
                  fit: BoxFit.contain,
                ),
                const SizedBox(height: 24),
                const Icon(Icons.error_outline, size: 48, color: Colors.red),
                const SizedBox(height: 16),
                Text(
                  authErrorMessage(err),
                  textAlign: TextAlign.center,
                  style: const TextStyle(fontSize: 16),
                ),
                const SizedBox(height: 24),
                FilledButton(
                  onPressed: () =>
                      ref.read(authControllerProvider.notifier).retry(),
                  child: const Text('Tentar novamente'),
                ),
                const SizedBox(height: 8),
                TextButton(
                  onPressed: () =>
                      ref.read(authControllerProvider.notifier).signOut(),
                  child: const Text('Sair da conta'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
