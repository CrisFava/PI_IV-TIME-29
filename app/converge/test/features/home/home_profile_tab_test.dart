// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:converge/features/auth/state/auth_controller.dart';
import 'package:converge/features/home/presentation/widgets/home_profile_tab.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../chat/fakes/fake_chat_user.dart';
import '../vehicle/fakes/fake_signed_in_auth_controller.dart';

/// Testes unitários e de widgets para home profile tab.
/// Cobre cenários como: "Sair da Conta" fica no fim do perfil, depois de "Meu impacto", "Sair da Conta" continua pedindo confirmação.
/// Os casos de teste estão descritos de forma autoexplicativa nas chamadas de [test] e [group].
void main() {
  testWidgets('"Sair da Conta" fica no fim do perfil, depois de "Meu impacto"', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(800, 2400);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);

    final user = fakeChatUser(id: 'user-1', name: 'Ana Souza');
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          authControllerProvider.overrideWith(
            () => FakeSignedInAuthController(user),
          ),
        ],
        child: MaterialApp(
          home: Scaffold(
            body: HomeProfileTab(user: user, bottomPadding: 0),
          ),
        ),
      ),
    );
    await tester.pump();

    double top(Finder finder) => tester.getTopLeft(finder).dy;
    final editProfile = top(find.byKey(const ValueKey('edit_profile_button')));
    final blockedUsers = top(find.byKey(const ValueKey('blocked_users_button')));
    final myImpact = top(find.byKey(const ValueKey('my_impact_card')));
    final signOut = top(find.text('Sair da Conta'));

    expect(editProfile, lessThan(blockedUsers));
    expect(blockedUsers, lessThan(myImpact));
    expect(myImpact, lessThan(signOut));
  });

  testWidgets('"Sair da Conta" continua pedindo confirmação', (tester) async {
    tester.view.physicalSize = const Size(800, 2400);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);

    final user = fakeChatUser(id: 'user-1', name: 'Ana Souza');
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          authControllerProvider.overrideWith(
            () => FakeSignedInAuthController(user),
          ),
        ],
        child: MaterialApp(
          home: Scaffold(
            body: HomeProfileTab(user: user, bottomPadding: 0),
          ),
        ),
      ),
    );
    await tester.pump();

    await tester.tap(find.text('Sair da Conta'));
    await tester.pumpAndSettle();

    expect(
      find.text('Deseja realmente sair da sua conta Converge?'),
      findsOneWidget,
    );
  });
}
