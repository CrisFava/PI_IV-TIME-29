// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'dart:async';
import 'dart:typed_data';

import 'package:converge/core/services/auth/auth_exception.dart';
import 'package:converge/core/services/auth/auth_providers.dart';
import 'package:converge/core/services/auth/auth_user.dart';
import 'package:converge/core/services/storage/profile_photo_storage_service.dart';
import 'package:converge/features/auth/data/repository/auth_repository_impl.dart';
import 'package:converge/features/auth/presentation/widgets/profile_photo_avatar.dart';
import 'package:converge/features/auth/state/auth_controller.dart';
import 'package:converge/features/auth/state/session/auth_session.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes/fake_auth_repository.dart';
import 'fakes/fake_auth_service.dart';
import 'fakes/fake_user.dart';

class _FakeProfilePhotoStorageService implements ProfilePhotoStorageService {
  String? lastFirebaseUid;
  String? lastFileName;
  Uint8List? lastBytes;
  Completer<String>? completer;
  String nextUrl = 'https://storage.googleapis.com/converge/avatar.png';

  @override
  Future<String> uploadProfilePhoto({
    required String firebaseUid,
    required Uint8List bytes,
    required String fileName,
  }) {
    lastFirebaseUid = firebaseUid;
    lastBytes = bytes;
    lastFileName = fileName;
    if (completer != null) return completer!.future;
    return Future.value(nextUrl);
  }
}

void main() {
  group('FirebaseProfilePhotoStorageService.buildStoragePath', () {
    test('monta o caminho profile/{firebaseUid}/pictures/{arquivo.ext}', () {
      expect(
        FirebaseProfilePhotoStorageService.buildStoragePath(
          firebaseUid: 'fb-uid-42',
          fileName: 'foto_perfil.png',
        ),
        'profile/fb-uid-42/pictures/foto_perfil.png',
      );
      expect(
        FirebaseProfilePhotoStorageService.buildStoragePath(
          firebaseUid: 'fb-uid-42',
          fileName: 'selfie.webp',
        ),
        'profile/fb-uid-42/pictures/selfie.webp',
      );
    });

    test('sanitiza barras e caracteres especiais preservando a extensão', () {
      expect(
        FirebaseProfilePhotoStorageService.buildStoragePath(
          firebaseUid: 'fb-uid-42',
          fileName: '/tmp/minha foto (1).jpeg',
        ),
        'profile/fb-uid-42/pictures/minha_foto__1_.jpeg',
      );
    });
  });

  group('AuthController.updateProfilePhoto', () {
    test('faz upload no storage e persiste a nova photoUrl no perfil', () async {
      final auth = FakeAuthService(
        initialUser: const AuthUser(
          uid: 'uid-1',
          email: 'ana@puc.edu.br',
          emailVerified: true,
        ),
      );
      final repository = FakeAuthRepository(stored: fakeUser());
      final storage = _FakeProfilePhotoStorageService()
        ..nextUrl = 'https://cdn.example.com/profile/uid-1/pictures/foto.png';

      final container = ProviderContainer(
        overrides: [
          authServiceProvider.overrideWithValue(auth),
          authRepositoryProvider.overrideWithValue(repository),
          profilePhotoStorageProvider.overrideWithValue(storage),
        ],
      );
      addTearDown(container.dispose);

      await container.read(authControllerProvider.future);

      final bytes = Uint8List.fromList([1, 2, 3, 4]);
      final updated = await container
          .read(authControllerProvider.notifier)
          .updateProfilePhoto(bytes: bytes, fileName: 'foto.png');

      expect(storage.lastFirebaseUid, 'uid-1');
      expect(storage.lastFileName, 'foto.png');
      expect(storage.lastBytes, bytes);
      expect(
        updated.photoUrl,
        'https://cdn.example.com/profile/uid-1/pictures/foto.png',
      );
      final session = container.read(authControllerProvider).value;
      expect(session, isA<Authenticated>());
      expect(
        (session! as Authenticated).user.photoUrl,
        'https://cdn.example.com/profile/uid-1/pictures/foto.png',
      );
    });

    test('falha se usuário não estiver autenticado', () async {
      final container = ProviderContainer(
        overrides: [
          authServiceProvider.overrideWithValue(FakeAuthService()),
          authRepositoryProvider.overrideWithValue(FakeAuthRepository()),
          profilePhotoStorageProvider.overrideWithValue(
            _FakeProfilePhotoStorageService(),
          ),
        ],
      );
      addTearDown(container.dispose);

      await container.read(authControllerProvider.future);

      await expectLater(
        container
            .read(authControllerProvider.notifier)
            .updateProfilePhoto(
              bytes: Uint8List.fromList([1]),
              fileName: 'a.png',
            ),
        throwsA(isA<AuthException>()),
      );
    });

    test(
      'preserva nome e chave PIX alterados enquanto a foto estava sendo enviada',
      () async {
        final auth = FakeAuthService(
          initialUser: const AuthUser(
            uid: 'uid-1',
            email: 'ana@puc.edu.br',
            emailVerified: true,
          ),
        );
        final repository = FakeAuthRepository(stored: fakeUser());
        final uploadCompleter = Completer<String>();
        final storage = _FakeProfilePhotoStorageService()
          ..completer = uploadCompleter;

        final container = ProviderContainer(
          overrides: [
            authServiceProvider.overrideWithValue(auth),
            authRepositoryProvider.overrideWithValue(repository),
            profilePhotoStorageProvider.overrideWithValue(storage),
          ],
        );
        addTearDown(container.dispose);

        await container.read(authControllerProvider.future);
        final notifier = container.read(authControllerProvider.notifier);

        // Inicia o upload da foto
        final photoFuture = notifier.updateProfilePhoto(
          bytes: Uint8List.fromList([1, 2, 3]),
          fileName: 'nova.png',
        );

        // Enquanto o upload está em andamento, o usuário edita nome e telefone
        await notifier.updateProfile(
          name: 'Ana Atualizada',
          phone: '19999998888',
        );

        // Upload conclui agora
        uploadCompleter.complete('https://cdn.example.com/nova.png');
        final updated = await photoFuture;

        expect(updated.name, 'Ana Atualizada');
        expect(updated.phone, '(19) 99999-8888');
        expect(updated.photoUrl, 'https://cdn.example.com/nova.png');
      },
    );

    test(
      'aborta gravação do perfil se houver logout durante o upload da foto',
      () async {
        final auth = FakeAuthService(
          initialUser: const AuthUser(
            uid: 'uid-1',
            email: 'ana@puc.edu.br',
            emailVerified: true,
          ),
        );
        final repository = FakeAuthRepository(stored: fakeUser());
        final uploadCompleter = Completer<String>();
        final storage = _FakeProfilePhotoStorageService()
          ..completer = uploadCompleter;

        final container = ProviderContainer(
          overrides: [
            authServiceProvider.overrideWithValue(auth),
            authRepositoryProvider.overrideWithValue(repository),
            profilePhotoStorageProvider.overrideWithValue(storage),
          ],
        );
        addTearDown(container.dispose);

        await container.read(authControllerProvider.future);
        final notifier = container.read(authControllerProvider.notifier);

        final photoFuture = notifier.updateProfilePhoto(
          bytes: Uint8List.fromList([1, 2, 3]),
          fileName: 'nova.png',
        );

        await notifier.signOut();
        uploadCompleter.complete('https://cdn.example.com/nova.png');

        await expectLater(
          photoFuture,
          throwsA(
            isA<AuthException>().having((e) => e.code, 'code', 'session-changed'),
          ),
        );
        expect(repository.stored?.photoUrl, isNull);
      },
    );
  });

  group('ProfilePhotoAvatar widget', () {
    testWidgets(
      'exibe opções "Ver foto" e "Alterar foto" e anima shimmer durante upload',
      (tester) async {
        final auth = FakeAuthService(
          initialUser: const AuthUser(
            uid: 'uid-1',
            email: 'ana@puc.edu.br',
            emailVerified: true,
          ),
        );
        final repository = FakeAuthRepository(stored: fakeUser());
        final uploadCompleter = Completer<String>();
        final storage = _FakeProfilePhotoStorageService()
          ..completer = uploadCompleter;

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              authServiceProvider.overrideWithValue(auth),
              authRepositoryProvider.overrideWithValue(repository),
              profilePhotoStorageProvider.overrideWithValue(storage),
            ],
            child: MaterialApp(
              home: Scaffold(
                body: Center(
                  child: Consumer(
                    builder: (_, ref, _) {
                      final session = ref.watch(authControllerProvider).value;
                      final user = session is Authenticated
                          ? session.user
                          : fakeUser();
                      return ProfilePhotoAvatar(
                        user: user,
                        photoPicker: (_) async => PickedProfilePhoto(
                          bytes: Uint8List.fromList([9, 8, 7]),
                          fileName: 'nova_foto.png',
                        ),
                      );
                    },
                  ),
                ),
              ),
            ),
          ),
        );
        await tester.pumpAndSettle();

        // Toca no avatar -> abre bottom sheet com as duas opções
        await tester.tap(find.byKey(const ValueKey('profile_photo_avatar')));
        await tester.pumpAndSettle();

        expect(find.text('Ver foto'), findsOneWidget);
        expect(find.text('Alterar foto'), findsOneWidget);

        // Escolhe "Alterar foto" -> "Galeria"
        await tester.tap(
          find.byKey(const ValueKey('profile_photo_change_option')),
        );
        await tester.pumpAndSettle();

        await tester.tap(
          find.byKey(const ValueKey('profile_photo_gallery_option')),
        );
        await tester.pump();
        await tester.pump(const Duration(milliseconds: 100));

        // Durante o upload, exibe a animação de shimmer
        expect(
          find.byKey(const ValueKey('profile_photo_shimmer')),
          findsOneWidget,
        );

        // Finaliza upload
        uploadCompleter.complete('');
        await tester.pumpAndSettle();

        expect(storage.lastFirebaseUid, 'uid-1');
        expect(storage.lastFileName, 'nova_foto.png');
        expect(
          find.byKey(const ValueKey('profile_photo_shimmer')),
          findsNothing,
        );
      },
    );
  });
}
