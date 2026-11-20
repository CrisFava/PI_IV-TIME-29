// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/network/api_exception.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../../security/presentation/widgets/report_sheet.dart';
import '../../data/repository/review_dismissal_store.dart';
import '../../state/pending_reviews.dart';
import '../../state/ride_reviews_controller.dart';

/// Tela de avaliação que abre quando a viagem é concluída (RF43).
///
/// Lista quem o usuário ainda não avaliou: o passageiro vê o motorista; o motorista
/// vê cada passageiro que viajou. Cada um tem estrelas e um comentário opcional, e
/// só quem recebeu estrelas é enviado. "Não quero avaliar" fecha e a avaliação desta
/// viagem não abre mais sozinha.
/// Os passageiros aparecem em linhas compactas (com muitos, a tela não fica enorme),
/// e todos têm o atalho para denunciar.
/// Devolve `true` quando todas as avaliações escolhidas foram enviadas.
class RideReviewPage extends ConsumerStatefulWidget {
  final String rideId;
  final List<ReviewTarget> targets;

  const RideReviewPage({
    super.key,
    required this.rideId,
    required this.targets,
  });

  @override
  ConsumerState<RideReviewPage> createState() => _RideReviewPageState();
}

/// O que o usuário preencheu para uma pessoa.
class _Draft {
  final TextEditingController comment = TextEditingController();
  int rating = 0;
  bool sent = false;
  String? error;

  // Linha compacta: o comentário só abre quando a pessoa quer escrever
  bool commentOpen = false;
}

class _RideReviewPageState extends ConsumerState<RideReviewPage> {
  static const List<String> _ratingLabels = [
    'Toque nas estrelas para avaliar',
    'Muito ruim',
    'Ruim',
    'Regular',
    'Boa',
    'Excelente',
  ];

  late final Map<String, _Draft> _drafts = {
    for (final target in widget.targets) target.userId: _Draft(),
  };
  bool _submitting = false;

  @override
  void dispose() {
    for (final draft in _drafts.values) {
      draft.comment.dispose();
    }
    super.dispose();
  }

  bool get _canSubmit =>
      !_submitting &&
      _drafts.values.any((draft) => draft.rating > 0 && !draft.sent);

  void _selectRating(String userId, int rating) {
    final draft = _drafts[userId]!;
    if (_submitting || draft.sent) return;
    HapticFeedback.lightImpact();
    setState(() {
      draft.rating = rating;
      draft.error = null;
    });
  }

  Future<void> _submit() async {
    if (!_canSubmit) return;

    // Limpa os erros da tentativa anterior: um aviso já resolvido (ex.: o comentário sem
    // estrelas foi apagado) não pode manter a tela aberta depois de um envio que deu certo.
    for (final draft in _drafts.values) {
      if (!draft.sent) draft.error = null;
    }

    // Comentário sem estrelas: a nota é obrigatória, então avisa em vez de perder o texto.
    var missingRating = false;
    for (final draft in _drafts.values) {
      if (!draft.sent &&
          draft.rating == 0 &&
          draft.comment.text.trim().isNotEmpty) {
        draft.error = 'Escolha de 1 a 5 estrelas para enviar o comentário.';
        missingRating = true;
      }
    }
    if (missingRating) {
      setState(() {});
      return;
    }

    HapticFeedback.mediumImpact();
    setState(() => _submitting = true);

    final reviews = ref.read(rideReviewsProvider(widget.rideId).notifier);
    for (final target in widget.targets) {
      final draft = _drafts[target.userId]!;
      if (draft.sent || draft.rating == 0) continue;

      try {
        await reviews.submit(
          reviewedId: target.userId,
          rating: draft.rating,
          comment: draft.comment.text,
        );
        draft.sent = true;
        draft.error = null;
      } catch (error) {
        // Já avaliado (409): o resultado é o mesmo que enviar, então não é falha.
        if (error is ApiException && error.status == 409) {
          draft.sent = true;
          draft.error = null;
        } else {
          draft.error = authErrorMessage(error);
        }
      }
      if (!mounted) return;
      setState(() {});
    }

    if (!mounted) return;
    setState(() => _submitting = false);

    // Enviar encerra a avaliação (quem ficou sem estrelas foi pulado). Só se algum envio
    // falhou a tela continua aberta, com o erro no cartão, para tentar de novo.
    final failed = _drafts.values.any(
      (draft) => draft.error != null && !draft.sent,
    );
    if (!failed) Navigator.of(context).pop(true);
  }

  @override
  Widget build(BuildContext context) {
    // Mantém as avaliações da viagem carregadas enquanto a tela está aberta.
    ref.watch(rideReviewsProvider(widget.rideId));

    final reviewingDriver = widget.targets.any((target) => target.isDriver);
    final anySent = _drafts.values.any((draft) => draft.sent);

    return PopScope(
      canPop: !_submitting,
      child: Scaffold(
        backgroundColor: AppColors.scaffoldBackground,
        body: SafeArea(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 24, 20, 8),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Avalie a carona',
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.w800,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      reviewingDriver
                          ? 'Como foi a carona com o motorista?'
                          : 'Como foi a carona com cada passageiro?',
                      style: const TextStyle(
                        fontSize: 13.5,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
              Expanded(
                child: ListView.separated(
                  keyboardDismissBehavior:
                      ScrollViewKeyboardDismissBehavior.onDrag,
                  padding: EdgeInsets.fromLTRB(
                    20,
                    8,
                    20,
                    16 + MediaQuery.viewInsetsOf(context).bottom,
                  ),
                  itemCount: widget.targets.length,
                  separatorBuilder: (_, _) =>
                      SizedBox(height: widget.targets.length > 1 ? 8 : 12),
                  itemBuilder: (_, index) {
                    final target = widget.targets[index];
                    void report() => showReportSheet(
                      context,
                      reportedId: target.userId,
                      reportedName: target.name,
                      rideId: widget.rideId,
                      // O motorista pode denunciar o passageiro que não pagou
                      canReportNonPayment: !target.isDriver,
                    );
                    // Vários passageiros: linhas compactas (um só fica no card grande)
                    if (!reviewingDriver && widget.targets.length > 1) {
                      final draft = _drafts[target.userId]!;
                      return _CompactReviewRow(
                        target: target,
                        draft: draft,
                        enabled: !_submitting,
                        onRating: (rating) =>
                            _selectRating(target.userId, rating),
                        onToggleComment: () => setState(
                          () => draft.commentOpen = !draft.commentOpen,
                        ),
                        onCommentChanged: () => setState(() {}),
                        onReport: report,
                      );
                    }
                    return _ReviewCard(
                      target: target,
                      draft: _drafts[target.userId]!,
                      labels: _ratingLabels,
                      enabled: !_submitting,
                      onRating: (rating) =>
                          _selectRating(target.userId, rating),
                      onCommentChanged: () => setState(() {}),
                      onReport: report,
                    );
                  },
                ),
              ),
              Container(
                padding: const EdgeInsets.fromLTRB(20, 12, 20, 12),
                decoration: const BoxDecoration(
                  color: AppColors.cardBackground,
                  border: Border(
                    top: BorderSide(color: AppColors.footerBorder),
                  ),
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    FilledButton(
                      onPressed: _canSubmit ? _submit : null,
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
                          : Text(
                              widget.targets.length > 1
                                  ? 'Enviar avaliações'
                                  : 'Enviar avaliação',
                              style: const TextStyle(
                                fontSize: 14.5,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                    ),
                    const SizedBox(height: 4),
                    TextButton(
                      onPressed: _submitting
                          ? null
                          : () async {
                              // Fechar depois de enviar, ou dispensar de vez
                              if (!anySent) {
                                await ref
                                    .read(reviewDismissalStoreProvider)
                                    .dismiss(widget.rideId);
                              }
                              if (context.mounted) {
                                Navigator.of(context).pop(anySent);
                              }
                            },
                      child: Text(
                        anySent ? 'Fechar' : 'Não quero avaliar',
                        style: const TextStyle(
                          fontWeight: FontWeight.w600,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Estrelas e comentário opcional para uma pessoa.
class _ReviewCard extends StatelessWidget {
  final ReviewTarget target;
  final _Draft draft;
  final List<String> labels;
  final bool enabled;
  final ValueChanged<int> onRating;
  final VoidCallback onCommentChanged;
  final VoidCallback onReport;

  const _ReviewCard({
    required this.target,
    required this.draft,
    required this.labels,
    required this.enabled,
    required this.onRating,
    required this.onCommentChanged,
    required this.onReport,
  });

  @override
  Widget build(BuildContext context) {
    final canEdit = enabled && !draft.sent;
    // Comentário escrito sem estrelas: avisa já, em vez de deixar o botão desligado sem explicar
    final needsStars =
        !draft.sent &&
        draft.error == null &&
        draft.rating == 0 &&
        draft.comment.text.trim().isNotEmpty;

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
          color: draft.sent ? AppColors.successBorder : AppColors.borderSubtle,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              const CircleAvatar(
                radius: 20,
                backgroundColor: AppColors.neutralLight,
                child: Icon(Icons.person_rounded, color: AppColors.textMedium),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      target.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    Text(
                      target.isDriver ? 'Motorista' : 'Passageiro',
                      style: const TextStyle(
                        fontSize: 12,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
              if (draft.sent)
                const Icon(
                  Icons.check_circle_rounded,
                  color: AppColors.success,
                ),
              _ReportButton(name: target.name, onReport: onReport),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              for (var star = 1; star <= 5; star++)
                IconButton(
                  tooltip: '$star de 5',
                  iconSize: 34,
                  onPressed: canEdit ? () => onRating(star) : null,
                  icon: Icon(
                    Icons.star_rounded,
                    color: star <= draft.rating
                        ? AppColors.ratingStar
                        : AppColors.ratingStarEmpty,
                  ),
                ),
            ],
          ),
          Center(
            child: Text(
              draft.sent
                  ? 'Você avaliou com ${draft.rating} de 5 estrelas'
                  : needsStars
                  ? 'Escolha de 1 a 5 estrelas para enviar o comentário'
                  : labels[draft.rating],
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 12.5,
                fontWeight: FontWeight.w600,
                color: draft.sent
                    ? AppColors.success
                    : needsStars
                    ? AppColors.errorText
                    : AppColors.textSecondary,
              ),
            ),
          ),
          if (draft.sent && draft.comment.text.trim().isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(
              '“${draft.comment.text.trim()}”',
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 13,
                fontStyle: FontStyle.italic,
                color: AppColors.textSecondary,
              ),
            ),
          ],
          if (!draft.sent) ...[
            const SizedBox(height: 12),
            TextField(
              controller: draft.comment,
              onChanged: (_) => onCommentChanged(),
              enabled: canEdit,
              minLines: 1,
              maxLines: 3,
              maxLength: 500,
              textCapitalization: TextCapitalization.sentences,
              style: const TextStyle(
                fontSize: 14,
                color: AppColors.textPrimary,
              ),
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
          ],
          if (draft.error != null) ...[
            const SizedBox(height: 4),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.errorBackground,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.errorBorder),
              ),
              child: Text(
                draft.error!,
                style: const TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: AppColors.errorText,
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

/// Denunciar quem está sendo avaliado.
class _ReportButton extends StatelessWidget {
  final String name;
  final VoidCallback onReport;

  const _ReportButton({required this.name, required this.onReport});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      tooltip: 'Denunciar $name',
      visualDensity: VisualDensity.compact,
      onPressed: () {
        HapticFeedback.lightImpact();
        onReport();
      },
      icon: const Icon(
        Icons.outlined_flag_rounded,
        size: 20,
        color: AppColors.error,
      ),
    );
  }
}

/// Um passageiro numa linha: nome, estrelas pequenas, comentário (abre no ícone)
/// e denúncia. Com 15 passageiros, a lista ainda cabe em poucas rolagens.
class _CompactReviewRow extends StatelessWidget {
  final ReviewTarget target;
  final _Draft draft;
  final bool enabled;
  final ValueChanged<int> onRating;
  final VoidCallback onToggleComment;
  final VoidCallback onCommentChanged;
  final VoidCallback onReport;

  const _CompactReviewRow({
    required this.target,
    required this.draft,
    required this.enabled,
    required this.onRating,
    required this.onToggleComment,
    required this.onCommentChanged,
    required this.onReport,
  });

  @override
  Widget build(BuildContext context) {
    final canEdit = enabled && !draft.sent;
    final hasComment = draft.comment.text.trim().isNotEmpty;
    final showComment =
        !draft.sent && (draft.commentOpen || hasComment || draft.error != null);
    final needsStars =
        !draft.sent && draft.error == null && draft.rating == 0 && hasComment;

    return Container(
      padding: const EdgeInsets.fromLTRB(12, 8, 4, 8),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: draft.sent ? AppColors.successBorder : AppColors.borderSubtle,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  target.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
              if (draft.sent)
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 8),
                  child: Text(
                    'Você avaliou com ${draft.rating} de 5 estrelas',
                    style: const TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                      color: AppColors.success,
                    ),
                  ),
                )
              else
                for (var star = 1; star <= 5; star++)
                  SizedBox(
                    width: 30,
                    height: 34,
                    child: IconButton(
                      tooltip: '$star de 5',
                      padding: EdgeInsets.zero,
                      iconSize: 24,
                      onPressed: canEdit ? () => onRating(star) : null,
                      icon: Icon(
                        Icons.star_rounded,
                        color: star <= draft.rating
                            ? AppColors.ratingStar
                            : AppColors.ratingStarEmpty,
                      ),
                    ),
                  ),
              if (!draft.sent)
                IconButton(
                  tooltip: 'Adicionar comentário',
                  visualDensity: VisualDensity.compact,
                  onPressed: canEdit ? onToggleComment : null,
                  icon: Icon(
                    hasComment
                        ? Icons.chat_bubble_rounded
                        : Icons.chat_bubble_outline_rounded,
                    size: 19,
                    color: AppColors.textPrimary,
                  ),
                ),
              _ReportButton(name: target.name, onReport: onReport),
            ],
          ),
          if (showComment) ...[
            const SizedBox(height: 4),
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: TextField(
                controller: draft.comment,
                onChanged: (_) => onCommentChanged(),
                enabled: canEdit,
                autofocus: draft.commentOpen && !hasComment,
                minLines: 1,
                maxLines: 3,
                maxLength: 500,
                textCapitalization: TextCapitalization.sentences,
                style: const TextStyle(
                  fontSize: 13.5,
                  color: AppColors.textPrimary,
                ),
                decoration: InputDecoration(
                  hintText: 'Comentário (opcional)',
                  hintStyle: const TextStyle(color: AppColors.textPlaceholder),
                  isDense: true,
                  counterText: '',
                  filled: true,
                  fillColor: AppColors.inputBackground,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: AppColors.borderSubtle),
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: AppColors.borderSubtle),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: AppColors.primary),
                  ),
                ),
              ),
            ),
          ],
          if (needsStars || draft.error != null)
            Padding(
              padding: const EdgeInsets.only(top: 4, right: 8),
              child: Text(
                draft.error ??
                    'Escolha de 1 a 5 estrelas para enviar o comentário',
                style: const TextStyle(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w600,
                  color: AppColors.errorText,
                ),
              ),
            ),
        ],
      ),
    );
  }
}
