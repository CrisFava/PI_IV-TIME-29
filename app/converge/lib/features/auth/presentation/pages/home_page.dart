import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../state/auth_action_controller.dart';
import '../../state/auth_controller.dart';
import '../../state/session/auth_session.dart';

class HomePage extends ConsumerWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(authControllerProvider).value;
    final user = session is Authenticated ? session.user : null;

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.white,
        title: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Image.asset(
              'assets/icon.png',
              width: 28,
              height: 28,
              fit: BoxFit.contain,
            ),
            const SizedBox(width: 8),
            const Text(
              'Converge',
              style: TextStyle(fontWeight: FontWeight.w700),
            ),
          ],
        ),
        actions: [
          TextButton.icon(
            onPressed: () => ref.read(authActionProvider.notifier).signOut(),
            icon: const Icon(Icons.logout, size: 18),
            label: const Text('Sair'),
          ),
        ],
      ),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: user == null
                ? const CircularProgressIndicator()
                : Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      border: Border.all(color: const Color(0xFFE5E7EB)),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Olá, ${user.name}!',
                          style: const TextStyle(
                            fontSize: 22,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        const SizedBox(height: 12),
                        Text('E-mail: ${user.email}'),
                        const SizedBox(height: 4),
                        Text('Telefone: ${user.phone}'),
                        const SizedBox(height: 4),
                        Text('Universidade: ${user.academicRecord.university}'),
                        const SizedBox(height: 4),
                        Text('Curso: ${user.academicRecord.course}'),
                        const SizedBox(height: 4),
                        Text(
                          'Matrícula: ${user.academicRecord.enrollmentNumber}',
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
