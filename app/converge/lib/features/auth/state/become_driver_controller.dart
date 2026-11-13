// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/cpf_validation_result.dart';
import '../data/models/driver_license_input.dart';
import '../data/models/participation_role.dart';
import '../data/models/pix_key_type.dart';
import 'auth_controller.dart';
import 'session/auth_session.dart';

final becomeDriverProvider =
    NotifierProvider<BecomeDriverController, AsyncValue<void>>(
      BecomeDriverController.new,
    );

/// Controller de estado para a transição atômica de ativação de motorista:
/// 1. Cadastro da chave PIX (`PUT /users/me`)
/// 2. Validação simulada da Carteira de Motorista (CNH)
/// 3. Promoção final a motorista (`POST /users/me/driver`)
class BecomeDriverController extends Notifier<AsyncValue<void>> {
  @override
  AsyncValue<void> build() => const AsyncData(null);

  AuthController get _session => ref.read(authControllerProvider.notifier);

  // O que já foi gravado no backend nesta ativação, por usuário. Numa nova
  // tentativa (ex.: o carro foi recusado), só o que mudou é enviado de novo.
  ({String userId, String pixKey, PixKeyType pixKeyType})? _savedPix;
  ({
    String userId,
    String number,
    DriverLicenseCategory category,
    DateTime expiresAt,
  })?
  _savedLicense;

  Authenticated? get _currentSession {
    final session = ref.read(authControllerProvider).value;
    return session is Authenticated ? session : null;
  }

  /// Valida CPF utilizando o validador no backend/socket server.
  Future<CpfValidationResult> validateCpf(String cpf) =>
      _session.validateCpf(cpf);

  /// Salva a chave PIX no perfil do usuário no backend.
  Future<bool> savePixKey({
    required String pixKey,
    required PixKeyType pixKeyType,
  }) async {
    if (state.isLoading) return false;
    state = const AsyncLoading();
    try {
      await _session.updatePixKey(pixKey: pixKey, pixKeyType: pixKeyType);
      state = const AsyncData(null);
      return true;
    } catch (e, st) {
      state = AsyncError(e, st);
      rethrow;
    }
  }

  /// Executa a transição atômica completa para ativação do modo motorista:
  /// 1. Cadastra/persiste a chave PIX no backend (`PUT /users/me`).
  /// 2. Cadastra os dados da CNH no backend (`POST /users/me/driver-license`).
  /// 3. Aguarda a simulação de aprovação visual da CNH.
  /// 4. Ativa o papel de motorista no backend (`POST /users/me/driver`).
  ///
  /// Pode ser chamada de novo depois de uma falha: PIX e CNH só são reenviados
  /// se mudaram, e o papel de motorista só é ativado se o usuário ainda não o tiver.
  Future<bool> activateDriverAtomic({
    required String pixKey,
    required PixKeyType pixKeyType,
    required DriverLicenseInput license,
    Duration verificationDelay = const Duration(milliseconds: 1500),
  }) async {
    if (state.isLoading) return false;
    state = const AsyncLoading();

    try {
      final userId = _currentSession?.user.id ?? '';

      // Passo 1: Cadastro da chave PIX no backend
      final pix = (userId: userId, pixKey: pixKey, pixKeyType: pixKeyType);
      if (_savedPix != pix) {
        await _session.updatePixKey(pixKey: pixKey, pixKeyType: pixKeyType);
        _savedPix = pix;
      }

      // Passo 2: Cadastro oficial da CNH no backend (satisfaz os requisitos do motorista)
      final cnh = (
        userId: userId,
        number: license.number,
        category: license.category,
        expiresAt: license.expiresAt,
      );
      if (_savedLicense != cnh) {
        await _session.submitDriverLicense(license);

        // Passo 3: Simulação de aprovação visual da carteira de motorista
        if (verificationDelay > Duration.zero) {
          await Future<void>.delayed(verificationDelay);
        }
        _savedLicense = cnh;
      }

      // Passo 4: Ativação oficial no backend (POST /users/me/driver)
      final roles = _currentSession?.user.participationRoles ?? const {};
      if (!roles.contains(ParticipationRole.driver)) {
        await _session.becomeDriver();
      }

      state = const AsyncData(null);
      return true;
    } catch (e, st) {
      state = AsyncError(e, st);
      rethrow;
    }
  }

  /// Chamada direta de ativação caso os dados já estejam salvos.
  Future<bool> becomeDriver() async {
    if (state.isLoading) return false;
    state = const AsyncLoading();
    try {
      await _session.becomeDriver();
      state = const AsyncData(null);
      return true;
    } catch (e, st) {
      state = AsyncError(e, st);
      rethrow;
    }
  }
}
