// Autor: Cristian Eduardo Fava - RA 25000636
// Coautor: Allan Giovanni Matias Paes - RA 25008211

import 'dart:async';
import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/services/auth/auth_providers.dart';
import '../../../core/services/auth/auth_service.dart';
import '../../../core/services/storage/profile_photo_storage_service.dart';
// ---
import '../data/models/cpf_validation_result.dart';
import '../data/models/driver_license_input.dart';
import '../data/models/participation_role.dart';
import '../data/models/pix_key_type.dart';
// ---
import '../data/models/user_model.dart';
import '../data/repository/auth_repository.dart';
import '../data/repository/auth_repository_impl.dart';
import 'inputs/profile_input.dart';
import 'inputs/register_input.dart';
import 'session/auth_session.dart';
import '../../../core/services/auth/auth_exception.dart';
import '../../../core/services/auth/auth_user.dart';

final authControllerProvider =
    AsyncNotifierProvider<AuthController, AuthSession>(AuthController.new);

/// Dono da sessão: combina a identidade do Firebase com o perfil do backend.
class AuthController extends AsyncNotifier<AuthSession> {
  AuthService get _auth => ref.read(authServiceProvider);
  AuthRepository get _repository => ref.read(authRepositoryProvider);
  ProfilePhotoStorageService get _photoStorage =>
      ref.read(profilePhotoStorageProvider);

  /// Incrementado a cada troca de sessão (login/logout) para invalidar
  /// mutações em voo iniciadas por uma sessão anterior.
  int _sessionEpoch = 0;

  /// Fila que serializa mutações de perfil (`PUT /users/me`) para evitar que
  /// uma atualização paralela sobrescreva campos alterados por outra.
  Future<void> _profileMutationQueue = Future<void>.value();

  @override
  Future<AuthSession> build() async {
    final auth = ref.watch(authServiceProvider);

    // Reage a logout feito fora do controller (token revogado, conta
    // removida). Logins são tratados pelas próprias ações.
    final subscription = auth.authStateChanges().listen((user) {
      if (user == null && state.value is! Unauthenticated) {
        _sessionEpoch++;
        state = const AsyncData(Unauthenticated());
      }
    });
    ref.onDispose(subscription.cancel);

    // Espera o Firebase restaurar a sessão persistida antes de decidir.
    final user = await auth.authStateChanges().first;
    if (user == null) return const Unauthenticated();
    return _loadSession(user);
  }

  /// Recarrega a sessão do zero (após falha na abertura do app).
  void retry() => ref.invalidateSelf();

  Future<void> signIn({required String email, required String password}) async {
    _sessionEpoch++;
    final user = await _auth.signIn(email: email, password: password);
    try {
      state = AsyncData(await _loadSession(user));
    } catch (_) {
      // Sem perfil carregado, não mantém o usuário logado pela metade.
      await _auth.signOut();
      rethrow;
    }
  }

  /// Cria a conta no Firebase e o cadastro no backend.
  ///
  /// Se uma tentativa anterior já criou a conta no Firebase para o mesmo e-mail
  /// mas falhou ao gravar o perfil no backend, reaproveita a conta atual em vez
  /// de tentar criar outra (o que falharia com `email-already-in-use`).
  Future<void> register(RegisterInput input) async {
    final current = _auth.currentUser;
    final sameEmail =
        current?.email != null &&
        current!.email!.trim().toLowerCase() ==
            input.email.trim().toLowerCase();
    final user = sameEmail
        ? current
        : await _auth.signUp(email: input.email, password: input.password);
    await _createProfile(user, input.profile);
  }

  /// Conclui o cadastro de quem está em [ProfileIncomplete].
  Future<void> completeProfile(ProfileInput profile) async {
    final user = _auth.currentUser;
    if (user == null) {
      state = const AsyncData(Unauthenticated());
      throw const AuthException(
        'no-current-user',
        'Sua sessão expirou. Entre novamente.',
      );
    }
    await _createProfile(user, profile);
  }

  /// Recarrega o perfil do backend, mantendo a sessão atual em caso de erro.
  Future<void> refreshProfile() async {
    final user = _auth.currentUser;
    if (user == null) return;
    state = AsyncData(await _loadSession(user));
  }

  /// Atualiza a chave PIX do usuário autenticado (`PUT /users/me`).
  Future<UserModel> updatePixKey({
    required String pixKey,
    required PixKeyType pixKeyType,
  }) async {
    final currentSession = state.value;
    if (currentSession is! Authenticated) {
      throw const AuthException(
        'not-authenticated',
        'Usuário não autenticado.',
      );
    }
    final user = currentSession.user;
    return _runProfileMutation(
      expectedUserId: user.id,
      expectedFirebaseUid: user.firebaseUid,
      expectedEpoch: _sessionEpoch,
      mutate: (latestUser) => _repository.updateProfile(
        name: latestUser.name,
        phone: _normalizePhone(latestUser.phone),
        photoUrl: latestUser.photoUrl,
        pixKey: pixKey,
        pixKeyType: pixKeyType,
      ),
    );
  }

  /// Atualiza nome, telefone e chave PIX do usuário autenticado (`PUT /users/me`).
  /// A chave PIX atual é mantida quando não vier uma nova.
  Future<UserModel> updateProfile({
    required String name,
    required String phone,
    String? pixKey,
    PixKeyType? pixKeyType,
  }) async {
    final currentSession = state.value;
    if (currentSession is! Authenticated) {
      throw const AuthException(
        'not-authenticated',
        'Usuário não autenticado.',
      );
    }
    final user = currentSession.user;
    final hasNewPix = pixKey != null && pixKey.trim().isNotEmpty;
    return _runProfileMutation(
      expectedUserId: user.id,
      expectedFirebaseUid: user.firebaseUid,
      expectedEpoch: _sessionEpoch,
      mutate: (latestUser) => _repository.updateProfile(
        name: name.trim(),
        phone: _normalizePhone(phone),
        photoUrl: latestUser.photoUrl,
        pixKey: hasNewPix ? pixKey.trim() : latestUser.pixKey,
        pixKeyType: hasNewPix ? pixKeyType : latestUser.pixKeyType,
      ),
    );
  }

  /// Faz upload da nova foto para o Firebase Storage
  /// (`profile/{firebaseUid}/pictures/{fileName}`) e atualiza o perfil (`PUT /users/me`).
  ///
  /// O envio ao backend é serializado com outras alterações de perfil e lê os
  /// campos mais recentes da mesma sessão antes de gravar, evitando sobrescrever
  /// edições feitas durante o upload ou gravar dados em outra conta após troca
  /// de sessão.
  Future<UserModel> updateProfilePhoto({
    required Uint8List bytes,
    required String fileName,
  }) async {
    final currentSession = state.value;
    if (currentSession is! Authenticated) {
      throw const AuthException(
        'not-authenticated',
        'Usuário não autenticado.',
      );
    }
    final startedUser = currentSession.user;
    final startedEpoch = _sessionEpoch;

    final downloadUrl = await _photoStorage.uploadProfilePhoto(
      firebaseUid: startedUser.firebaseUid,
      bytes: bytes,
      fileName: fileName,
    );

    return _runProfileMutation(
      expectedUserId: startedUser.id,
      expectedFirebaseUid: startedUser.firebaseUid,
      expectedEpoch: startedEpoch,
      mutate: (latestUser) => _repository.updateProfile(
        name: latestUser.name,
        phone: _normalizePhone(latestUser.phone),
        photoUrl: downloadUrl,
        pixKey: latestUser.pixKey,
        pixKeyType: latestUser.pixKeyType,
      ),
    );
  }

  /// Executa uma mutação de perfil de forma serializada, garantindo que a
  /// sessão ativa antes e depois da chamada pertença à mesma conta que iniciou
  /// a operação.
  Future<UserModel> _runProfileMutation({
    required String expectedUserId,
    required String expectedFirebaseUid,
    required int expectedEpoch,
    required Future<UserModel> Function(UserModel latestUser) mutate,
  }) {
    final completer = Completer<UserModel>();
    final previous = _profileMutationQueue;
    _profileMutationQueue = previous.catchError((_) {}).then((_) async {
      try {
        final latestUser = _assertSameAuthenticatedUser(
          expectedUserId: expectedUserId,
          expectedFirebaseUid: expectedFirebaseUid,
          expectedEpoch: expectedEpoch,
        );
        final updated = await mutate(latestUser);
        _assertSameAuthenticatedUser(
          expectedUserId: expectedUserId,
          expectedFirebaseUid: expectedFirebaseUid,
          expectedEpoch: expectedEpoch,
        );
        state = AsyncData(Authenticated(updated));
        completer.complete(updated);
      } catch (error, stackTrace) {
        completer.completeError(error, stackTrace);
      }
    });
    return completer.future;
  }

  UserModel _assertSameAuthenticatedUser({
    required String expectedUserId,
    required String expectedFirebaseUid,
    required int expectedEpoch,
  }) {
    final latest = state.value;
    final authUid = _auth.currentUser?.uid;
    if (_sessionEpoch != expectedEpoch ||
        latest is! Authenticated ||
        latest.user.id != expectedUserId ||
        latest.user.firebaseUid != expectedFirebaseUid ||
        authUid != expectedFirebaseUid) {
      throw const AuthException(
        'session-changed',
        'Sua sessão mudou enquanto o perfil era salvo. Confira os dados e tente de novo.',
      );
    }
    return latest.user;
  }

  static String _normalizePhone(String raw) {
    final digits = raw.replaceAll(RegExp(r'\D'), '');
    if (digits.length == 11) {
      return '(${digits.substring(0, 2)}) ${digits.substring(2, 7)}-${digits.substring(7)}';
    } else if (digits.length == 10) {
      return '(${digits.substring(0, 2)}) ${digits.substring(2, 6)}-${digits.substring(6)}';
    }
    return raw;
  }

  /// Envia a Carteira de Motorista (`POST /users/me/driver-license`).
  Future<UserModel> submitDriverLicense(DriverLicenseInput license) async {
    final updated = await _repository.submitDriverLicense(license);
    final withLicense = updated.copyWith(hasDriverLicense: true);
    state = AsyncData(Authenticated(withLicense));
    return withLicense;
  }

  /// Promove o usuário autenticado a motorista chamando `POST /users/me/driver`.
  Future<UserModel> becomeDriver() async {
    final updated = await _repository.becomeDriver();
    final ensuredDriver = updated.copyWith(
      participationRoles: {
        ...updated.participationRoles,
        ParticipationRole.driver,
      },
      hasDriverLicense: true,
    );
    state = AsyncData(Authenticated(ensuredDriver));
    return ensuredDriver;
  }

  /// Valida o CPF através do Socket Server TCP via backend.
  Future<CpfValidationResult> validateCpf(String cpf) =>
      _repository.validateCpf(cpf);

  /// Verifica se o e-mail ainda está disponível para cadastro.
  Future<bool> isEmailAvailable(String email) =>
      _repository.isEmailAvailable(email);

  Future<void> sendPasswordResetEmail(String email) =>
      _auth.sendPasswordResetEmail(email);

  Future<void> signOut() async {
    _sessionEpoch++;
    await _auth.signOut();
    state = const AsyncData(Unauthenticated());
  }

  Future<AuthSession> _loadSession(AuthUser user) async {
    try {
      return Authenticated(await _repository.fetchCurrentUser());
    } on ApiException catch (e) {
      if (e.isNotFound) return ProfileIncomplete(user);
      rethrow;
    }
  }

  Future<void> _createProfile(AuthUser user, ProfileInput profile) async {
    final email = user.email;
    if (email == null) {
      throw const AuthException(
        'missing-email',
        'Sua conta não possui e-mail. Entre com e-mail e senha.',
      );
    }

    try {
      final created = await _repository.createUser(
        profile.toRequest(firebaseUid: user.uid, email: email),
      );
      state = AsyncData(Authenticated(created));
    } on ApiException catch (e) {
      // 409 pode significar que uma tentativa anterior (ex.: timeout) já
      // criou o cadastro. Se o perfil existir, a sessão está completa.
      if (e.isConflict) {
        final existing = await _tryFetchCurrentUser();
        if (existing != null) {
          state = AsyncData(Authenticated(existing));
          return;
        }
      }
      state = AsyncData(ProfileIncomplete(user));
      rethrow;
    } catch (_) {
      state = AsyncData(ProfileIncomplete(user));
      rethrow;
    }
  }

  Future<UserModel?> _tryFetchCurrentUser() async {
    try {
      return await _repository.fetchCurrentUser();
    } on ApiException {
      return null;
    }
  }
}
