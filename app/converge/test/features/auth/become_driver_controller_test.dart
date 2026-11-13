// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:converge/features/auth/data/models/driver_license_input.dart';
import 'package:converge/features/auth/data/models/pix_key_type.dart';
import 'package:converge/features/auth/state/auth_controller.dart';
import 'package:converge/features/auth/state/become_driver_controller.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../vehicle/fakes/fake_signed_in_auth_controller.dart';
import 'fakes/fake_user.dart';

void main() {
  late FakeSignedInAuthController auth;
  late ProviderContainer container;

  final license = DriverLicenseInput(
    number: '12345678901',
    category: DriverLicenseCategory.b,
    expiresAt: DateTime(2030),
  );

  setUp(() async {
    container = ProviderContainer.test(
      overrides: [
        authControllerProvider.overrideWith(
          () => auth = FakeSignedInAuthController(fakeUser()),
        ),
      ],
    );
    await container.read(authControllerProvider.future);
  });

  Future<void> activate({
    String pixKey = '12345678909',
    DriverLicenseInput? cnh,
  }) => container
      .read(becomeDriverProvider.notifier)
      .activateDriverAtomic(
        pixKey: pixKey,
        pixKeyType: PixKeyType.cpf,
        license: cnh ?? license,
        verificationDelay: Duration.zero,
      );

  test(
    'nova tentativa sem mudanças não reenvia nada nem reativa o papel',
    () async {
      await activate();
      await activate();

      expect(auth.updatePixKeyCalls, 1);
      expect(auth.submitDriverLicenseCalls, 1);
      expect(auth.becomeDriverCalls, 1);
    },
  );

  test('nova tentativa com PIX e CNH alterados grava os novos dados', () async {
    await activate();
    await activate(
      pixKey: '98765432100',
      cnh: DriverLicenseInput(
        number: '10987654321',
        category: DriverLicenseCategory.ab,
        expiresAt: DateTime(2031),
      ),
    );

    expect(auth.updatePixKeyCalls, 2);
    expect(auth.submitDriverLicenseCalls, 2);
    expect(auth.becomeDriverCalls, 1);
  });
}
