// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/details_sheet.dart';

/// Escolha do horário de saída: atalhos (Agora, Em 30 min, Em 1 h) quando o dia
/// é hoje e uma roda de hora e minuto, de 5 em 5 min.
///
/// Devolve null se o usuário fechar sem escolher. Escolhendo, devolve
/// `(departure: ...)`, em que `departure` nulo é "Agora".
Future<({DateTime? departure})?> showDepartureTimeSheet(
  BuildContext context, {
  required DateTime day,
  DateTime? current,
}) {
  return showDetailsSheet<({DateTime? departure})>(
    context,
    title: 'Horário de saída',
    builder: (sheetContext) => DepartureTimePicker(
      day: day,
      current: current,
      onChosen: (departure) =>
          Navigator.of(sheetContext).pop((departure: departure)),
    ),
  );
}

/// O conteúdo do painel de horário.
class DepartureTimePicker extends StatefulWidget {
  final DateTime day;
  final DateTime? current;

  /// Horário escolhido; null é "Agora".
  final ValueChanged<DateTime?> onChosen;

  const DepartureTimePicker({
    super.key,
    required this.day,
    required this.onChosen,
    this.current,
  });

  @override
  State<DepartureTimePicker> createState() => _DepartureTimePickerState();
}

class _DepartureTimePickerState extends State<DepartureTimePicker> {
  static const int _step = 5;

  late DateTime _selected;

  bool get _isToday {
    final now = DateTime.now();
    return widget.day.year == now.year &&
        widget.day.month == now.month &&
        widget.day.day == now.day;
  }

  @override
  void initState() {
    super.initState();
    final rounded = _roundUp(widget.current ?? DateTime.now());
    final sameDay =
        rounded.year == widget.day.year &&
        rounded.month == widget.day.month &&
        rounded.day == widget.day.day;
    // Arredondar perto da meia-noite pode virar o dia seguinte: no dia escolhido
    // fica o último horário dele (23:55), e não 00:00, que já teria passado
    _selected = sameDay
        ? rounded
        : (rounded.isAfter(widget.day)
              ? DateTime(
                  widget.day.year,
                  widget.day.month,
                  widget.day.day,
                  23,
                  55,
                )
              : _onDay(rounded));
  }

  // A roda anda de 5 em 5 min: arredonda para o próximo múltiplo. Com segundos,
  // 10:30:20 vira 10:35 (10:30 já passou); 10:30:00 exato fica 10:30.
  static DateTime _roundUp(DateTime time) {
    final hasSeconds =
        time.second > 0 || time.millisecond > 0 || time.microsecond > 0;
    final base = hasSeconds ? time.add(const Duration(minutes: 1)) : time;
    final extra = (_step - base.minute % _step) % _step;
    final rounded = base.add(Duration(minutes: extra));
    return DateTime(
      rounded.year,
      rounded.month,
      rounded.day,
      rounded.hour,
      rounded.minute,
    );
  }

  DateTime _onDay(DateTime time) => DateTime(
    widget.day.year,
    widget.day.month,
    widget.day.day,
    time.hour,
    time.minute,
  );

  static String _hhmm(DateTime time) =>
      '${time.hour.toString().padLeft(2, '0')}:'
      '${time.minute.toString().padLeft(2, '0')}';

  void _choose(DateTime? departure) {
    HapticFeedback.selectionClick();
    widget.onChosen(departure);
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (_isToday) ...[
          Row(
            children: [
              _shortcut('Agora', () => _choose(null)),
              const SizedBox(width: 8),
              _shortcut(
                'Em 30 min',
                () => _choose(
                  _roundUp(DateTime.now().add(const Duration(minutes: 30))),
                ),
              ),
              const SizedBox(width: 8),
              _shortcut(
                'Em 1 h',
                () => _choose(
                  _roundUp(DateTime.now().add(const Duration(hours: 1))),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
        ],
        SizedBox(
          height: 190,
          child: CupertinoTheme(
            data: const CupertinoThemeData(
              textTheme: CupertinoTextThemeData(
                dateTimePickerTextStyle: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w600,
                  color: AppColors.textPrimary,
                ),
              ),
            ),
            child: CupertinoDatePicker(
              key: const ValueKey('departure_time_wheel'),
              mode: CupertinoDatePickerMode.time,
              use24hFormat: true,
              minuteInterval: _step,
              initialDateTime: _selected,
              onDateTimeChanged: (time) {
                HapticFeedback.selectionClick();
                setState(() => _selected = _onDay(time));
              },
            ),
          ),
        ),
        const SizedBox(height: 16),
        FilledButton(
          key: const ValueKey('confirm_departure_time'),
          onPressed: () => _choose(_selected),
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.primary,
            foregroundColor: AppColors.textLight,
            padding: const EdgeInsets.symmetric(vertical: 14),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
          ),
          child: Text(
            'Confirmar ${_hhmm(_selected)}',
            style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
          ),
        ),
      ],
    );
  }

  Widget _shortcut(String label, VoidCallback onTap) {
    return Expanded(
      child: OutlinedButton(
        onPressed: onTap,
        style: OutlinedButton.styleFrom(
          foregroundColor: AppColors.textPrimary,
          side: const BorderSide(color: AppColors.borderMedium),
          padding: const EdgeInsets.symmetric(vertical: 12),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
        ),
        child: Text(label, style: const TextStyle(fontWeight: FontWeight.w700)),
      ),
    );
  }
}
