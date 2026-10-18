import 'package:converge/core/network/api_exception.dart';
import 'package:converge/core/services/auth/auth_providers.dart';
import 'package:converge/features/auth/data/repository/auth_repository_impl.dart';
import 'package:converge/features/auth/state/auth_action_controller.dart';
import 'package:converge/features/auth/state/auth_controller.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:converge/core/services/auth/auth_exception.dart';
import 'package:converge/core/services/auth/auth_user.dart';
import 'package:converge/features/auth/state/inputs/profile_input.dart';
import 'package:converge/features/auth/state/inputs/register_input.dart';
import 'package:converge/features/auth/state/session/auth_session.dart';

import 'fakes/fake_auth_repository.dart';
import 'fakes/fake_auth_service.dart';
import 'fakes/fake_user.dart';

const _profile = ProfileInput(
  name: 'Ana Souza',
  phone: '(19) 91234-5678',
  university: 'PUC-Campinas',
  course: 'Eng. Software',
  enrollmentNumber: '2024-00123',
);

void main() {
  late FakeAuthService auth;
  late FakeAuthRepository repository;

  ProviderContainer createContainer() {
    final container = ProviderContainer(
      overrides: [
        authServiceProvider.overrideWithValue(auth),
        authRepositoryProvider.overrideWithValue(repository),
      ],
    );
    addTearDown(container.dispose);
    return container;
  }

  setUp(() {
    auth = FakeAuthService();
    repository = FakeAuthRepository();
  });

  group('abertura do app', () {
    test('sem usuário no Firebase: Unauthenticated', () async {
      final container = createContainer();
      expect(
        await container.read(authControllerProvider.future),
        isA<Unauthenticated>(),
      );
    });

    test('usuário com perfil no backend: Authenticated', () async {
      auth = FakeAuthService(
        initialUser: const AuthUser(
          uid: 'uid-1',
          email: 'ana@puc.edu.br',
          emailVerified: true,
        ),
      );
      repository = FakeAuthRepository(stored: fakeUser());
      final container = createContainer();

      final session = await container.read(authControllerProvider.future);
      expect(session, isA<Authenticated>());
    });

    test('usuário sem perfil no backend: ProfileIncomplete', () async {
      auth = FakeAuthService(
        initialUser: const AuthUser(
          uid: 'uid-1',
          email: 'ana@puc.edu.br',
          emailVerified: true,
        ),
      );
      final container = createContainer();

      expect(
        await container.read(authControllerProvider.future),
        isA<ProfileIncomplete>(),
      );
    });
  });

  group('signIn', () {
    test('carrega o perfil e autentica', () async {
      repository = FakeAuthRepository(stored: fakeUser());
      final container = createContainer();
      await container.read(authControllerProvider.future);

      final ok = await container
          .read(authActionProvider.notifier)
          .signIn(email: 'ana@puc.edu.br', password: '123456');

      expect(ok, isTrue);
      expect(
        container.read(authControllerProvider).value,
        isA<Authenticated>(),
      );
    });

    test(
      'credencial inválida: expõe a mensagem e continua deslogado',
      () async {
        auth.nextError = const AuthException(
          'invalid-credential',
          'E-mail ou senha incorretos.',
        );
        final container = createContainer();
        container.listen(authActionProvider, (_, _) {});
        await container.read(authControllerProvider.future);

        final ok = await container
            .read(authActionProvider.notifier)
            .signIn(email: 'ana@puc.edu.br', password: 'errada');

        expect(ok, isFalse);
        final action = container.read(authActionProvider);
        expect(authErrorMessage(action.error!), 'E-mail ou senha incorretos.');
        expect(
          container.read(authControllerProvider).value,
          isA<Unauthenticated>(),
        );
      },
    );

    test('backend fora do ar: desloga do Firebase', () async {
      repository.fetchError = ApiException.network();
      final container = createContainer();
      await container.read(authControllerProvider.future);

      await expectLater(
        container
            .read(authControllerProvider.notifier)
            .signIn(email: 'ana@puc.edu.br', password: '123456'),
        throwsA(isA<ApiException>()),
      );
      expect(auth.signOutCalls, 1);
      expect(auth.currentUser, isNull);
    });
  });

  group('register', () {
    test('cria a conta e o perfil no backend', () async {
      final container = createContainer();
      await container.read(authControllerProvider.future);

      await container
          .read(authControllerProvider.notifier)
          .register(
            const RegisterInput(
              email: 'ana@puc.edu.br',
              password: '123456',
              profile: _profile,
            ),
          );

      expect(
        container.read(authControllerProvider).value,
        isA<Authenticated>(),
      );
      final request = repository.created.single.toJson();
      expect(request['firebaseUid'], 'uid-1');
      expect(request['participationRoles'], ['PASSENGER']);
      final academic = request['academicRecord'] as Map<String, dynamic>;
      expect(academic['institutionalEmail'], 'ana@puc.edu.br');
      expect(academic['status'], 'PENDING');
      expect(academic['verificationMethod'], 'INSTITUTIONAL_EMAIL');
    });

    test(
      'falha no backend: mantém a conta e vai para ProfileIncomplete',
      () async {
        repository.createError = ApiException.fromResponse({
          'success': false,
          'data': null,
          'error': {
            'code': 'BAD_REQUEST',
            'message': 'Telefone inválido',
            'fields': ['phone'],
          },
        }, status: 400);
        final container = createContainer();
        await container.read(authControllerProvider.future);

        await expectLater(
          container
              .read(authControllerProvider.notifier)
              .register(
                const RegisterInput(
                  email: 'ana@puc.edu.br',
                  password: '123456',
                  profile: _profile,
                ),
              ),
          throwsA(isA<ApiException>()),
        );
        expect(
          container.read(authControllerProvider).value,
          isA<ProfileIncomplete>(),
        );
        expect(auth.currentUser, isNotNull);

        repository.createError = null;
        await container
            .read(authControllerProvider.notifier)
            .completeProfile(_profile);
        expect(
          container.read(authControllerProvider).value,
          isA<Authenticated>(),
        );
      },
    );

    test(
      '409 com perfil já existente (retry após timeout): autentica',
      () async {
        repository.createError = ApiException.fromStatus(409);
        repository.stored = fakeUser();
        final container = createContainer();
        await container.read(authControllerProvider.future);

        await container
            .read(authControllerProvider.notifier)
            .register(
              const RegisterInput(
                email: 'ana@puc.edu.br',
                password: '123456',
                profile: _profile,
              ),
            );

        expect(
          container.read(authControllerProvider).value,
          isA<Authenticated>(),
        );
      },
    );
  });

  test('signOut externo (token revogado) derruba a sessão', () async {
    auth = FakeAuthService(
      initialUser: const AuthUser(
        uid: 'uid-1',
        email: 'ana@puc.edu.br',
        emailVerified: true,
      ),
    );
    repository = FakeAuthRepository(stored: fakeUser());
    final container = createContainer();
    await container.read(authControllerProvider.future);

    await auth.signOut();
    await Future<void>.delayed(Duration.zero);

    expect(
      container.read(authControllerProvider).value,
      isA<Unauthenticated>(),
    );
  });
}
