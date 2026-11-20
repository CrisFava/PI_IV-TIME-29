// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../data/models/create_report_request.dart';
import '../../data/models/report_reason.dart';
import '../../state/report_controller.dart';

/// Abre o painel para denunciar um usuário. Devolve `true` quando a denúncia foi enviada.
///
/// [rideId] é a viagem em que aconteceu o problema, quando houver. Sem ela,
/// "Atraso ou ausência" não aparece, porque o backend exige a viagem.
Future<bool> showReportSheet(
  BuildContext context, {
  required String reportedId,
  required String reportedName,
  String? rideId,
  bool canReportNonPayment = false,
}) async {
  final sent = await showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    backgroundColor: AppColors.cardBackground,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(26)),
    ),
    builder: (_) => ReportSheet(
      reportedId: reportedId,
      reportedName: reportedName,
      rideId: rideId,
      canReportNonPayment: canReportNonPayment,
    ),
  );
  return sent ?? false;
}

class ReportSheet extends ConsumerStatefulWidget {
  final String reportedId;
  final String reportedName;
  final String? rideId;

  /// O motorista denunciando um passageiro da viagem: aparece "Não pagou a carona".
  final bool canReportNonPayment;

  const ReportSheet({
    super.key,
    required this.reportedId,
    required this.reportedName,
    this.rideId,
    this.canReportNonPayment = false,
  });

  @override
  ConsumerState<ReportSheet> createState() => _ReportSheetState();
}

class _ReportSheetState extends ConsumerState<ReportSheet> {
  final _descriptionController = TextEditingController();
  ReportReason? _reason;
  bool _submitting = false;
  String? _error;

  bool get _hasRide => widget.rideId != null && widget.rideId!.isNotEmpty;

  List<ReportReason> get _reasons => [
    for (final reason in ReportReason.values)
      if ((reason != ReportReason.lateNoShow || _hasRide) &&
          (reason != ReportReason.nonPayment ||
              (widget.canReportNonPayment && _hasRide)))
        reason,
  ];

  /// "Outro motivo" precisa de descrição, como no backend.
  bool get _canSubmit {
    final reason = _reason;
    if (reason == null || _submitting) return false;
    if (reason == ReportReason.other) {
      return _descriptionController.text.trim().isNotEmpty;
    }
    return true;
  }

  @override
  void dispose() {
    _descriptionController.dispose();
    super.dispose();
  }

  void _selectReason(ReportReason reason) {
    if (_submitting) return;
    HapticFeedback.lightImpact();
    setState(() {
      _reason = reason;
      _error = null;
    });
  }

  Future<void> _submit() async {
    final reason = _reason;
    if (reason == null || !_canSubmit) return;
    HapticFeedback.mediumImpact();
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      await ref
          .read(reportsProvider.notifier)
          .submit(
            reportedId: widget.reportedId,
            reason: reason,
            description: _descriptionController.text,
            rideId: _hasRide ? widget.rideId : null,
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
    final isOther = _reason == ReportReason.other;
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
            'Denunciar ${widget.reportedName}',
            style: const TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w800,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 4),
          const Text(
            'Escolha o motivo. A denúncia fica registrada para análise.',
            style: TextStyle(fontSize: 13, color: AppColors.textSecondary),
          ),
          const SizedBox(height: 14),
          for (final reason in _reasons)
            _ReasonTile(
              reason: reason,
              selected: _reason == reason,
              enabled: !_submitting,
              onTap: () => _selectReason(reason),
            ),
          const SizedBox(height: 8),
          TextField(
            controller: _descriptionController,
            enabled: !_submitting,
            minLines: 2,
            maxLines: 4,
            maxLength: CreateReportRequest.maxDescriptionLength,
            textCapitalization: TextCapitalization.sentences,
            onChanged: (_) => setState(() {}),
            style: const TextStyle(fontSize: 14, color: AppColors.textPrimary),
            decoration: InputDecoration(
              hintText: isOther
                  ? 'Conte o que aconteceu (obrigatório)'
                  : 'Descrição (opcional)',
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
            onPressed: _canSubmit ? _submit : null,
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: AppColors.textLight,
              disabledBackgroundColor: AppColors.buttonSecondary,
              disabledForegroundColor: AppColors.textPlaceholder,
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
                    'Enviar denúncia',
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

class _ReasonTile extends StatelessWidget {
  final ReportReason reason;
  final bool selected;
  final bool enabled;
  final VoidCallback onTap;

  const _ReasonTile({
    required this.reason,
    required this.selected,
    required this.enabled,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: enabled ? onTap : null,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          decoration: BoxDecoration(
            color: selected ? AppColors.neutralLight : AppColors.cardBackground,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(
              color: selected
                  ? AppColors.borderCardPressed
                  : AppColors.borderSubtle,
            ),
          ),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  reason.label,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
              if (selected)
                const Icon(
                  Icons.check_circle_rounded,
                  size: 20,
                  color: AppColors.primary,
                ),
            ],
          ),
        ),
      ),
    );
  }
}
