// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../state/ride_reviews_controller.dart';

/// Abre o painel de avaliação de um participante de uma viagem concluída (RF43).
/// Devolve `true` quando a avaliação foi enviada.
///
/// [reviewedIsDriver] só muda o texto ("Como foi a carona com este motorista?"); o papel gravado é
/// calculado pela API. Fechar o painel sem enviar devolve `false`.
Future<bool> showReviewSheet(
  BuildContext context, {
  required String rideId,
  required String reviewedId,
  required String reviewedName,
  required bool reviewedIsDriver,
}) async {
  final sent = await showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    backgroundColor: AppColors.cardBackground,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(26)),
    ),
    builder: (_) => _ReviewSheet(
      rideId: rideId,
      reviewedId: reviewedId,
      reviewedName: reviewedName,
      reviewedIsDriver: reviewedIsDriver,
    ),
  );

  return sent ?? false;
}

/// Conteúdo do painel: estrelas, comentário opcional e o botão de enviar.
class _ReviewSheet extends ConsumerStatefulWidget {
  /// Viagem avaliada.
  final String rideId;

  /// Quem é avaliado.
  final String reviewedId;

  /// Nome mostrado no título.
  final String reviewedName;

  /// Se quem é avaliado foi o motorista da viagem.
  final bool reviewedIsDriver;

  const _ReviewSheet({
    required this.rideId,
    required this.reviewedId,
    required this.reviewedName,
    required this.reviewedIsDriver,
  });

  @override
  ConsumerState<_ReviewSheet> createState() => _ReviewSheetState();
}

/// Estado do painel: a nota escolhida, o comentário, o envio em andamento e o erro.
class _ReviewSheetState extends ConsumerState<_ReviewSheet> {
  /// Texto abaixo das estrelas para cada nota; a posição 0 é o convite antes de escolher.
  static const List<String> _ratingLabels = [
    'Toque nas estrelas para avaliar',
    'Muito ruim',
    'Ruim',
    'Regular',
    'Boa',
    'Excelente',
  ];

  /// Comentário digitado (até 500 caracteres).
  final _commentController = TextEditingController();

  /// Nota escolhida; 0 enquanto nenhuma estrela foi tocada.
  int _rating = 0;

  /// Envio em andamento: estrelas, campo e botão ficam travados.
  bool _submitting = false;

  /// Mensagem de erro mostrada acima do botão.
  String? _error;

  /// Libera o controller do comentário.
  @override
  void dispose() {
    _commentController.dispose();
    super.dispose();
  }

  /// Escolhe a nota com vibração leve e limpa o erro. Ignorado durante o envio.
  void _selectRating(int rating) {
    if (_submitting) return;
    HapticFeedback.lightImpact();
    setState(() {
      _rating = rating;
      _error = null;
    });
  }

  /// Envia a avaliação. Sem nota, mostra o erro sem chamar a API. Em sucesso fecha o painel com
  /// `true`; em erro, mantém o painel aberto com a mensagem e libera para tentar de novo.
  Future<void> _submit() async {
    if (_submitting) return;
    if (_rating == 0) {
      setState(() => _error = 'Escolha uma nota de 1 a 5 estrelas.');
      return;
    }

    HapticFeedback.mediumImpact();
    setState(() {
      _submitting = true;
      _error = null;
    });

    try {
      await ref
          .read(rideReviewsProvider(widget.rideId).notifier)
          .submit(
            reviewedId: widget.reviewedId,
            rating: _rating,
            comment: _commentController.text,
          );
      if (mounted) Navigator.of(context).pop(true);
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _submitting = false;
        _error = authErrorMessage(error);
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final role = widget.reviewedIsDriver ? 'motorista' : 'passageiro';

    // Rolável: com o teclado aberto em tela pequena, o botão continua alcançável.
    return SingleChildScrollView(
      padding: EdgeInsets.fromLTRB(
        20,
        12,
        20,
        20 + MediaQuery.viewInsetsOf(context).bottom,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Center(
            child: Container(
              width: 38,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.borderMedium,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 18),
          Text(
            'Avaliar ${widget.reviewedName}',
            style: const TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w800,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Como foi a carona com este $role?',
            style: const TextStyle(
              fontSize: 13,
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              for (var star = 1; star <= 5; star++)
                IconButton(
                  tooltip: '$star de 5',
                  iconSize: 38,
                  onPressed: () => _selectRating(star),
                  icon: Icon(
                    Icons.star_rounded,
                    color: star <= _rating
                        ? AppColors.ratingStar
                        : AppColors.ratingStarEmpty,
                  ),
                ),
            ],
          ),
          Center(
            child: Text(
              _ratingLabels[_rating],
              style: const TextStyle(
                fontSize: 12.5,
                fontWeight: FontWeight.w600,
                color: AppColors.textSecondary,
              ),
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _commentController,
            enabled: !_submitting,
            minLines: 2,
            maxLines: 4,
            maxLength: 500,
            textCapitalization: TextCapitalization.sentences,
            style: const TextStyle(fontSize: 14, color: AppColors.textPrimary),
            decoration: InputDecoration(
              hintText: 'Comentário (opcional)',
              hintStyle: const TextStyle(color: AppColors.textPlaceholder),
              filled: true,
              fillColor: AppColors.inputBackground,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: const BorderSide(color: AppColors.borderSubtle),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: const BorderSide(color: AppColors.borderSubtle),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: const BorderSide(color: AppColors.primary),
              ),
            ),
          ),
          if (_error != null) ...[
            const SizedBox(height: 4),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.errorBackground,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.errorBorder),
              ),
              child: Text(
                _error!,
                style: const TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: AppColors.errorText,
                ),
              ),
            ),
          ],
          const SizedBox(height: 14),
          FilledButton(
            onPressed: _submitting ? null : _submit,
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: AppColors.textLight,
              disabledBackgroundColor: AppColors.buttonSecondary,
              padding: const EdgeInsets.symmetric(vertical: 15),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
            child: _submitting
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                      strokeWidth: 2.2,
                      color: AppColors.primary,
                    ),
                  )
                : const Text(
                    'Enviar avaliação',
                    style: TextStyle(
                      fontSize: 14.5,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
          ),
        ],
      ),
    );
  }
}
