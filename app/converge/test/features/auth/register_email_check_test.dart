// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:converge/core/services/auth/auth_providers.dart';
import 'package:converge/features/auth/data/repository/auth_repository_impl.dart';
import 'package:converge/features/auth/presentation/pages/register_page.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes/fake_auth_repository.dart';
import 'fakes/fake_auth_service.dart';

void main() {
  testWidgets(
    'bloqueia avanço no passo 1 e exibe erro quando e-mail já está cadastrado',
    (tester) async {
      final repository = FakeAuthRepository()..emailAvailable = false;

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            authServiceProvider.overrideWithValue(FakeAuthService()),
            authRepositoryProvider.overrideWithValue(repository),
          ],
          child: const MaterialApp(home: RegisterPage()),
        ),
      );
      await tester.pumpAndSettle();

      await tester.enterText(
        find.byType(TextFormField),
        'existente@puccampinas.edu.br',
      );
      await tester.pump();

      await tester.tap(find.textContaining('Continuar'));
      await tester.pumpAndSettle();

      expect(
        find.text(
          'Já existe uma conta cadastrada com este e-mail. Tente fazer login.',
        ),
        findsOneWidget,
      );
      // Continua no passo 1, sem abrir o formulário de dados pessoais
      expect(find.text('Qual é o seu e-mail acadêmico?'), findsOneWidget);
    },
  );

  testWidgets('avança para o passo 2 quando e-mail está disponível', (
    tester,
  ) async {
    final repository = FakeAuthRepository()..emailAvailable = true;

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          authServiceProvider.overrideWithValue(FakeAuthService()),
          authRepositoryProvider.overrideWithValue(repository),
        ],
        child: const MaterialApp(home: RegisterPage()),
      ),
    );
    await tester.pumpAndSettle();

    await tester.enterText(
      find.byType(TextFormField),
      'novo@puccampinas.edu.br',
    );
    await tester.pump();

    await tester.tap(find.textContaining('Continuar'));
    await tester.pumpAndSettle();

    expect(find.byKey(const ValueKey('register_error_banner')), findsNothing);
    expect(find.text('Qual é o seu e-mail acadêmico?'), findsNothing);
  });
}
