// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:converge/app_router.dart';
import 'package:converge/app_widget.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

void main() {
  testWidgets('o app fala português: o seletor de data vem em pt-BR', (
    tester,
  ) async {
    final router = GoRouter(
      routes: [
        GoRoute(
          path: '/',
          builder: (context, _) => Scaffold(
            body: Center(
              child: TextButton(
                onPressed: () => showDatePicker(
                  context: context,
                  initialDate: DateTime(2026, 11, 20),
                  firstDate: DateTime(2026),
                  lastDate: DateTime(2027),
                ),
                child: const Text('abrir'),
              ),
            ),
          ),
        ),
      ],
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [appRouterProvider.overrideWithValue(router)],
        child: const AppWidget(),
      ),
    );
    await tester.tap(find.text('abrir'));
    await tester.pumpAndSettle();

    expect(find.text('Cancelar'), findsOneWidget);
    expect(find.text('Cancel'), findsNothing);
  });
}
