// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/details_sheet.dart';

/// De quantos em quantos minutos a roda de horário anda.
const departureMinuteStep = 5;

/// O primeiro horário da roda de [time] em diante: sem os segundos e no próximo
/// múltiplo de [step] minutos (14:03 vira 14:05; 14:05 continua 14:05).
DateTime roundUpToMinuteStep(DateTime time, {int step = departureMinuteStep}) {
  final truncated = DateTime(
    time.year,
    time.month,
    time.day,
    time.hour,
    time.minute,
  );
  final remainder = truncated.minute % step;
  return remainder == 0
      ? truncated
      : truncated.add(Duration(minutes: step - remainder));
}

/// Escolha do horário de saída numa roda de hora e minuto, de 5 em 5 minutos.
/// Não existe "Agora": o usuário sempre escolhe o horário.
///
/// Devolve o horário escolhido no [day], ou null se o usuário fechar sem escolher.
Future<DateTime?> showDepartureTimeSheet(
  BuildContext context, {
  required DateTime day,
  DateTime? current,
}) {
  return showDetailsSheet<DateTime>(
    context,
    title: 'Horário de saída',
    builder: (sheetContext) => DepartureTimePicker(
      day: day,
      current: current,
      onChosen: (departure) => Navigator.of(sheetContext).pop(departure),
    ),
  );
}

/// O conteúdo do painel de horário.
class DepartureTimePicker extends StatefulWidget {
  final DateTime day;
  final DateTime? current;
  final ValueChanged<DateTime> onChosen;

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
  late DateTime _selected;

  @override
  void initState() {
    super.initState();
    // Abre no horário já escolhido ou no próximo da roda a partir de agora (a roda só
    // tem múltiplos de 5 minutos, então o horário de partida é arredondado para cima)
    final base = roundUpToMinuteStep(
      widget.current ?? DateTime.now().add(const Duration(minutes: 1)),
    );
    final sameDay =
        base.year == widget.day.year &&
        base.month == widget.day.month &&
        base.day == widget.day.day;
    _selected = sameDay || base.isBefore(widget.day)
        ? _onDay(base)
        // Perto da meia-noite, o próximo horário já é amanhã: fica o último de hoje
        : DateTime(
            widget.day.year,
            widget.day.month,
            widget.day.day,
            23,
            60 - departureMinuteStep,
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

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
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
              minuteInterval: departureMinuteStep,
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
          onPressed: () {
            HapticFeedback.selectionClick();
            widget.onChosen(_selected);
          },
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
}
