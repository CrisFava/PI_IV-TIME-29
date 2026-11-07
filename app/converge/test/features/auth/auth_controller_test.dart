// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:converge/core/network/api_exception.dart';
import 'package:converge/core/services/auth/auth_providers.dart';
import 'package:converge/features/auth/data/repository/auth_repository_impl.dart';
import 'package:converge/features/auth/state/auth_action_controller.dart';
import 'package:converge/features/auth/state/auth_controller.dart';
import 'package:converge/features/auth/state/become_driver_controller.dart';
import 'package:converge/features/auth/data/models/driver_license_input.dart';
import 'package:converge/features/auth/data/models/pix_key_type.dart';
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
  group('becomeDriver', () {
    test('promove o usu?rio autenticado a motorista com sucesso', () async {
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

      final initialSession =
          container.read(authControllerProvider).value as Authenticated;
      expect(initialSession.user.isDriver, isFalse);

      final updated =
          await container.read(authControllerProvider.notifier).becomeDriver();
      expect(updated.isDriver, isTrue);

      final currentSession =
          container.read(authControllerProvider).value as Authenticated;
      expect(currentSession.user.isDriver, isTrue);
    });

    test('repassa erro se a chamada ao backend falhar', () async {
      auth = FakeAuthService(
        initialUser: const AuthUser(
          uid: 'uid-1',
          email: 'ana@puc.edu.br',
          emailVerified: true,
        ),
      );
      repository = FakeAuthRepository(stored: fakeUser())
        ..becomeDriverError = ApiException.fromStatus(500);
      final container = createContainer();
      await container.read(authControllerProvider.future);

      expect(
        () => container.read(authControllerProvider.notifier).becomeDriver(),
        throwsA(isA<ApiException>()),
      );
    });

    test('BecomeDriverController ativa motorista e gerencia estado', () async {
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

      final ok =
          await container.read(becomeDriverProvider.notifier).becomeDriver();
      expect(ok, isTrue);

      final session =
          container.read(authControllerProvider).value as Authenticated;
      expect(session.user.isDriver, isTrue);
    });

    test('updatePixKey atualiza chave PIX e tipo na sess?o', () async {
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

      final updated = await container
          .read(authControllerProvider.notifier)
          .updatePixKey(
            pixKey: '123.456.789-00',
            pixKeyType: PixKeyType.cpf,
          );

      expect(updated.pixKey, '123.456.789-00');
      expect(updated.pixKeyType, PixKeyType.cpf);

      final session =
          container.read(authControllerProvider).value as Authenticated;
      expect(session.user.pixKey, '123.456.789-00');
      expect(session.user.pixKeyType, PixKeyType.cpf);
    });

    test('activateDriverAtomic cadastra PIX e ativa motorista atomicamente', () async {
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

      final ok = await container
          .read(becomeDriverProvider.notifier)
          .activateDriverAtomic(
            pixKey: 'ana@puc.edu.br',
            pixKeyType: PixKeyType.email,
            license: DriverLicenseInput(
              number: '12345678901',
              category: DriverLicenseCategory.b,
              expiresAt: DateTime(2028, 12, 31),
            ),
            verificationDelay: Duration.zero,
          );

      expect(ok, isTrue);
      expect(repository.lastSubmittedLicense, isNotNull);
      expect(repository.lastSubmittedLicense!.number, '12345678901');

      final session =
          container.read(authControllerProvider).value as Authenticated;
      expect(session.user.isDriver, isTrue);
      expect(session.user.pixKey, 'ana@puc.edu.br');
      expect(session.user.pixKeyType, PixKeyType.email);
    });
    test('submitDriverLicense envia carteira de motorista', () async {
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

      final license = DriverLicenseInput(
        number: '12345678901',
        category: DriverLicenseCategory.ab,
        expiresAt: DateTime(2030, 1, 1),
      );
      await container
          .read(authControllerProvider.notifier)
          .submitDriverLicense(license);

      expect(repository.lastSubmittedLicense, license);
    });

    test('validateCpf retorna resultado do validador', () async {
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

      final result = await container
          .read(authControllerProvider.notifier)
          .validateCpf('123.456.789-00');

      expect(result.valid, isTrue);
      expect(result.message, 'CPF válido.');
    });
  });
}
