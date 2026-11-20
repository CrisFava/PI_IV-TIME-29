// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

final reviewDismissalStoreProvider = Provider<ReviewDismissalStore>(
  (ref) => const ReviewDismissalStore(),
);

/// Viagens em que o usuário tocou em "Não quero avaliar", guardadas no aparelho:
/// a avaliação delas não abre mais sozinha.
class ReviewDismissalStore {
  static const String _key = 'reviews_dismissed';

  const ReviewDismissalStore();

  /// Se o armazenamento do aparelho falhar, nada conta como dispensado.
  Future<bool> isDismissed(String rideId) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      return prefs.getStringList(_key)?.contains(rideId) ?? false;
    } catch (_) {
      return false;
    }
  }

  Future<void> dismiss(String rideId) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final dismissed = prefs.getStringList(_key) ?? const [];
      if (dismissed.contains(rideId)) return;
      await prefs.setStringList(_key, [...dismissed, rideId]);
    } catch (_) {
      // Sem guardar: a avaliação pode aparecer de novo na próxima abertura do app
    }
  }
}
