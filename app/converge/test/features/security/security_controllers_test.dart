// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

import 'package:converge/core/network/api_error_code.dart';
import 'package:converge/core/network/api_exception.dart';
import 'package:converge/features/security/data/models/block_model.dart';
import 'package:converge/features/security/data/models/report_reason.dart';
import 'package:converge/features/security/data/repository/security_repository_impl.dart';
import 'package:converge/features/security/state/blocked_users_controller.dart';
import 'package:converge/features/security/state/report_controller.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes/fake_security_repository.dart';

void main() {
  late FakeSecurityRepository repository;
  late ProviderContainer container;

  setUp(() {
    repository = FakeSecurityRepository();
    container = ProviderContainer.test(
      overrides: [securityRepositoryProvider.overrideWithValue(repository)],
    );
    container.listen(blockedUsersProvider, (_, _) {});
    container.listen(reportsProvider, (_, _) {});
    container.listen(blockedUserIdsProvider, (_, _) {});
  });

  group('BlockedUsersController', () {
    test('começa com a lista de bloqueados do backend', () async {
      await repository.blockUser('user-2');
      container.invalidate(blockedUsersProvider);

      final blocks = await container.read(blockedUsersProvider.future);

      expect(blocks.map((block) => block.blockedUserId), ['user-2']);
    });

    test('bloquear inclui na lista e usa o nome da tela de origem', () async {
      await container.read(blockedUsersProvider.future);

      await container
          .read(blockedUsersProvider.notifier)
          .block('user-2', name: 'Ana');

      final blocks = container.read(blockedUsersProvider).value!;
      expect(blocks.single.blockedUserId, 'user-2');
      expect(blocks.single.displayName, 'Ana');
      expect(container.read(blockedUserIdsProvider), {'user-2'});
    });

    test('bloquear a mesma pessoa duas vezes não duplica na lista', () async {
      await container.read(blockedUsersProvider.future);
      final controller = container.read(blockedUsersProvider.notifier);

      await controller.block('user-2', name: 'Ana');
      await controller.block('user-2', name: 'Ana');

      expect(container.read(blockedUsersProvider).value, hasLength(1));
    });

    test('desbloquear tira da lista e do conjunto de ids', () async {
      await container.read(blockedUsersProvider.future);
      final controller = container.read(blockedUsersProvider.notifier);
      await controller.block('user-2', name: 'Ana');
      await controller.block('user-3', name: 'Bia');

      await controller.unblock('user-2');

      expect(
        container
            .read(blockedUsersProvider)
            .value!
            .map((block) => block.blockedUserId),
        ['user-3'],
      );
      expect(container.read(blockedUserIdsProvider), {'user-3'});
      expect(repository.blocks.map((block) => block.blockedUserId), ['user-3']);
    });

    test('erro ao bloquear é propagado e a lista não muda', () async {
      await container.read(blockedUsersProvider.future);
      repository.failNext = const ApiException(
        code: ApiErrorCode.userNotFound,
        message: 'Usuário não encontrado.',
        status: 404,
      );

      await expectLater(
        container.read(blockedUsersProvider.notifier).block('user-9'),
        throwsA(isA<ApiException>().having((e) => e.status, 'status', 404)),
      );

      expect(container.read(blockedUsersProvider).value, isEmpty);
    });

    test('erro ao desbloquear é propagado e mantém o bloqueio', () async {
      await container.read(blockedUsersProvider.future);
      final controller = container.read(blockedUsersProvider.notifier);
      await controller.block('user-2', name: 'Ana');
      repository.failNext = ApiException.network();

      await expectLater(
        controller.unblock('user-2'),
        throwsA(isA<ApiException>()),
      );

      expect(container.read(blockedUserIdsProvider), {'user-2'});
    });

    test('ids bloqueados ficam vazios se a lista não carregar', () async {
      final failing = ProviderContainer.test(
        // Sem nova tentativa automática, para o erro aparecer no teste.
        retry: (_, _) => null,
        overrides: [
          securityRepositoryProvider.overrideWithValue(_FailingListRepository()),
        ],
      );
      failing.listen(blockedUsersProvider, (_, _) {});
      failing.listen(blockedUserIdsProvider, (_, _) {});
      await expectLater(
        failing.read(blockedUsersProvider.future),
        throwsA(isA<ApiException>()),
      );

      expect(failing.read(blockedUserIdsProvider), isEmpty);
    });
  });

  group('ReportController', () {
    test('enviar inclui a denúncia no começo da lista', () async {
      await container.read(reportsProvider.future);
      final controller = container.read(reportsProvider.notifier);

      await controller.submit(
        reportedId: 'user-2',
        reason: ReportReason.improperCharge,
      );
      final second = await controller.submit(
        reportedId: 'user-3',
        reason: ReportReason.other,
        description: 'Detalhes',
        rideId: 'ride-1',
      );

      final reports = container.read(reportsProvider).value!;
      expect(reports.first.id, second.id);
      expect(reports.map((report) => report.reportedId), ['user-3', 'user-2']);
      expect(reports.first.rideId, 'ride-1');
    });

    test('denúncia repetida propaga o erro do backend e não entra na lista', () async {
      await container.read(reportsProvider.future);
      final controller = container.read(reportsProvider.notifier);
      await controller.submit(
        reportedId: 'user-2',
        reason: ReportReason.inappropriateConduct,
      );

      await expectLater(
        controller.submit(
          reportedId: 'user-2',
          reason: ReportReason.inappropriateConduct,
        ),
        throwsA(
          isA<ApiException>()
              .having((e) => e.status, 'status', 400)
              .having((e) => e.fields, 'fields', {'reportedId'}),
        ),
      );

      expect(container.read(reportsProvider).value, hasLength(1));
    });
  });
}

/// Repositório cuja lista de bloqueados sempre falha.
class _FailingListRepository extends FakeSecurityRepository {
  @override
  Future<List<BlockModel>> listBlocks() => throw ApiException.network();
}
