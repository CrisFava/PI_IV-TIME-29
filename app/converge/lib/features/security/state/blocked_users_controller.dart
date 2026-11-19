// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/block_model.dart';
import '../data/repository/security_repository.dart';
import '../data/repository/security_repository_impl.dart';

/// Usuários que o usuário logado bloqueou.
final blockedUsersProvider =
    AsyncNotifierProvider.autoDispose<BlockedUsersController, List<BlockModel>>(
      BlockedUsersController.new,
    );

/// Ids dos bloqueados. Vazio enquanto a lista carrega ou se ela falhar.
final blockedUserIdsProvider = Provider.autoDispose<Set<String>>(
  (ref) => {
    for (final block in ref.watch(blockedUsersProvider).value ?? const <BlockModel>[])
      block.blockedUserId,
  },
);

class BlockedUsersController extends AsyncNotifier<List<BlockModel>> {
  SecurityRepository get _repository => ref.read(securityRepositoryProvider);

  @override
  Future<List<BlockModel>> build() => _repository.listBlocks();

  /// Bloqueia e inclui na lista. Erros são propagados para a tela exibir a mensagem.
  Future<BlockModel> block(String userId, {String? name}) async {
    final created = await _repository.blockUser(userId);
    final block = BlockModel(
      id: created.id,
      blockedUserId: created.blockedUserId,
      // A resposta do bloqueio não traz o nome; usa o da tela de origem.
      blockedUserName: created.blockedUserName ?? name,
      createdAt: created.createdAt,
    );
    if (ref.mounted) {
      final current = state.value ?? const <BlockModel>[];
      state = AsyncData([
        block,
        for (final item in current)
          if (item.blockedUserId != block.blockedUserId) item,
      ]);
    }
    return block;
  }

  /// Desbloqueia e tira da lista. Erros são propagados.
  Future<void> unblock(String userId) async {
    await _repository.unblockUser(userId);
    if (ref.mounted) {
      state = AsyncData([
        for (final item in state.value ?? const <BlockModel>[])
          if (item.blockedUserId != userId) item,
      ]);
    }
  }
}
