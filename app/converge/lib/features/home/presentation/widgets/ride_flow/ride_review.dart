// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../../core/config/styles.dart';
import '../../../../../core/widgets/details_section.dart';
import '../../../../../core/widgets/details_sheet.dart';
import '../../../../../core/widgets/details_summary_card.dart';
import '../../../../ride/presentation/widgets/ride_place_line.dart';
import '../../../../route/data/models/route_estimate_model.dart';
import 'ride_flow_format.dart';

/// Revisão final antes de publicar ou pedir a carona, com tudo o que foi
/// escolhido. Devolve true quando o usuário confirma.
Future<bool> showRideReview(
  BuildContext context, {
  required bool isDriver,
  required String originAddress,
  required String destinationAddress,
  required String departureLabel,
  required RouteEstimateModel? estimate,
  bool repeatWeekly = false,
  double searchRadiusKm = 3,
}) async {
  final confirmed = await showDetailsSheet<bool>(
    context,
    title: isDriver ? 'Confirme sua carona' : 'Confirme seu pedido',
    builder: (sheetContext) => RideReview(
      isDriver: isDriver,
      originAddress: originAddress,
      destinationAddress: destinationAddress,
      departureLabel: departureLabel,
      estimate: estimate,
      repeatWeekly: repeatWeekly,
      searchRadiusKm: searchRadiusKm,
      onConfirm: () => Navigator.of(sheetContext).pop(true),
      onBack: () => Navigator.of(sheetContext).pop(false),
    ),
  );
  return confirmed ?? false;
}

/// O conteúdo da revisão: partida, destino, quando sai, tempo e distância e,
/// para o motorista, se repete e até onde busca passageiros.
class RideReview extends StatelessWidget {
  final bool isDriver;
  final String originAddress;
  final String destinationAddress;
  final String departureLabel;
  final RouteEstimateModel? estimate;
  final bool repeatWeekly;
  final double searchRadiusKm;
  final VoidCallback onConfirm;
  final VoidCallback onBack;

  const RideReview({
    super.key,
    required this.isDriver,
    required this.originAddress,
    required this.destinationAddress,
    required this.departureLabel,
    required this.estimate,
    required this.onConfirm,
    required this.onBack,
    this.repeatWeekly = false,
    this.searchRadiusKm = 3,
  });

  @override
  Widget build(BuildContext context) {
    final trip = estimate;
    final minutes = trip?.durationMinutes;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        DetailsSection(
          title: 'Saindo de',
          child: RidePlaceLine(
            icon: Icons.trip_origin_rounded,
            text: originAddress,
            maxLines: null,
            emphasized: true,
          ),
        ),
        DetailsSection(
          title: 'Indo para',
          child: RidePlaceLine(
            icon: Icons.flag_rounded,
            text: destinationAddress,
            maxLines: null,
            emphasized: true,
          ),
        ),
        // Tudo num cartão só, uma linha por item: nada fica desalinhado
        Padding(
          padding: const EdgeInsets.only(top: 12),
          child: DetailsSummaryCard(
            rows: [
              (
                icon: Icons.event_rounded,
                label: 'Saída',
                value: departureLabel,
              ),
              if (minutes != null)
                (
                  icon: Icons.schedule_rounded,
                  label: 'Viagem',
                  value: '~${formatTripDuration(minutes)}',
                ),
              if (trip != null)
                (
                  icon: Icons.route_rounded,
                  label: 'Percurso',
                  value: '${formatRideKm(trip.distanceKm)} km',
                ),
              if (isDriver) ...[
                (
                  icon: repeatWeekly
                      ? Icons.event_repeat_rounded
                      : Icons.looks_one_rounded,
                  label: 'Frequência',
                  value: repeatWeekly ? 'Toda semana' : 'Só neste dia',
                ),
                (
                  icon: Icons.radar_rounded,
                  label: 'Raio de busca',
                  value: '${formatRideKm(searchRadiusKm)} km',
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: 24),
        FilledButton.icon(
          key: const ValueKey('ride_review_confirm'),
          onPressed: () {
            HapticFeedback.mediumImpact();
            onConfirm();
          },
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.primary,
            foregroundColor: AppColors.textLight,
            padding: const EdgeInsets.symmetric(vertical: 14),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
          ),
          icon: Icon(
            isDriver ? Icons.event_available_rounded : Icons.search_rounded,
            size: 20,
          ),
          label: Text(
            isDriver ? 'Publicar carona' : 'Buscar motoristas',
            style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
          ),
        ),
        const SizedBox(height: 8),
        TextButton(
          onPressed: () {
            HapticFeedback.lightImpact();
            onBack();
          },
          style: TextButton.styleFrom(foregroundColor: AppColors.textPrimary),
          child: const Text(
            'Voltar e ajustar',
            style: TextStyle(fontWeight: FontWeight.w700),
          ),
        ),
      ],
    );
  }
}
