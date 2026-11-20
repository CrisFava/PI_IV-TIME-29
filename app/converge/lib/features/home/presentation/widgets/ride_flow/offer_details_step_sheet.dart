// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../../core/config/styles.dart';
import '../../../../vehicle/data/models/vehicle_model.dart';
import 'ride_flow_format.dart';
import 'ride_flow_header.dart';
import 'ride_flow_sheet_frame.dart';

/// Etapa do motorista depois do horário: o carro da carona, quantas vagas oferecer
/// (até a capacidade do carro) e até onde buscar passageiros.
class OfferDetailsStepSheet extends StatelessWidget {
  final List<VehicleModel> vehicles;
  final String? selectedVehicleId;
  final ValueChanged<VehicleModel> onVehicleChanged;
  final int seats;
  final ValueChanged<int> onSeatsChanged;
  final double searchRadiusKm;
  final ValueChanged<double> onSearchRadiusChanged;
  final bool isSubmitting;
  final VoidCallback onSubmit;
  final VoidCallback onAddVehicle;
  final VoidCallback onBack;
  final VoidCallback onClose;
  final double bottomSafeArea;

  const OfferDetailsStepSheet({
    super.key,
    required this.vehicles,
    required this.selectedVehicleId,
    required this.onVehicleChanged,
    required this.seats,
    required this.onSeatsChanged,
    required this.searchRadiusKm,
    required this.onSearchRadiusChanged,
    required this.isSubmitting,
    required this.onSubmit,
    required this.onAddVehicle,
    required this.onBack,
    required this.onClose,
    this.bottomSafeArea = 0,
  });

  @override
  Widget build(BuildContext context) {
    final selected = vehicles
        .where((vehicle) => vehicle.id == selectedVehicleId)
        .firstOrNull;
    final maxSeats = selected?.passengerSeats ?? seats;

    return RideFlowSheetFrame(
      initialSize: 0.7,
      bottomSafeArea: bottomSafeArea,
      children: [
        RideFlowHeader(
          title: 'Sua carona',
          subtitle: 'O carro, as vagas e até onde buscar passageiros.',
          onBack: onBack,
          onClose: onClose,
        ),
        const SizedBox(height: 14),
        const _Label('Carro'),
        const SizedBox(height: 6),
        if (vehicles.isEmpty)
          OutlinedButton.icon(
            onPressed: onAddVehicle,
            icon: const Icon(Icons.add_rounded),
            label: const Text('Cadastrar um carro'),
          ),
        for (final vehicle in vehicles)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: _VehicleOption(
              vehicle: vehicle,
              selected: vehicle.id == selectedVehicleId,
              enabled: !isSubmitting,
              onTap: () => onVehicleChanged(vehicle),
            ),
          ),
        const SizedBox(height: 8),
        Row(
          children: [
            const Expanded(child: _Label('Vagas para passageiros')),
            IconButton(
              key: const ValueKey('offer_seats_minus'),
              tooltip: 'Menos uma vaga',
              onPressed: isSubmitting || seats <= 1
                  ? null
                  : () {
                      HapticFeedback.selectionClick();
                      onSeatsChanged(seats - 1);
                    },
              icon: const Icon(Icons.remove_circle_outline_rounded),
              color: AppColors.textPrimary,
            ),
            Text(
              '$seats',
              key: const ValueKey('offer_seats_value'),
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w800,
                color: AppColors.textPrimary,
              ),
            ),
            IconButton(
              key: const ValueKey('offer_seats_plus'),
              tooltip: 'Mais uma vaga',
              onPressed: isSubmitting || seats >= maxSeats
                  ? null
                  : () {
                      HapticFeedback.selectionClick();
                      onSeatsChanged(seats + 1);
                    },
              icon: const Icon(Icons.add_circle_outline_rounded),
              color: AppColors.textPrimary,
            ),
          ],
        ),
        const SizedBox(height: 6),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const _Label('Buscar passageiros a até'),
            Text(
              '${formatRideKm(searchRadiusKm)} km',
              style: const TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w800,
                color: AppColors.primary,
              ),
            ),
          ],
        ),
        Slider(
          value: searchRadiusKm,
          min: 1.0,
          max: 10.0,
          divisions: 18,
          activeColor: AppColors.primary,
          inactiveColor: AppColors.borderSubtle,
          onChanged: isSubmitting ? null : onSearchRadiusChanged,
        ),
        const Text(
          'Do seu caminho até onde o passageiro embarca, e do seu destino até o dele.',
          style: TextStyle(fontSize: 12, color: AppColors.textDark),
        ),
        const SizedBox(height: 18),
        SizedBox(
          height: 52,
          child: FilledButton.icon(
            key: const ValueKey('offer_details_submit'),
            onPressed: isSubmitting || selected == null
                ? null
                : () {
                    HapticFeedback.mediumImpact();
                    onSubmit();
                  },
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: AppColors.textLight,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
            icon: isSubmitting
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: AppColors.textLight,
                    ),
                  )
                : const Icon(Icons.fact_check_rounded, size: 20),
            label: const Text(
              'Revisar carona',
              style: TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
            ),
          ),
        ),
      ],
    );
  }
}

class _Label extends StatelessWidget {
  final String text;

  const _Label(this.text);

  @override
  Widget build(BuildContext context) => Text(
    text,
    style: const TextStyle(
      fontSize: 13,
      fontWeight: FontWeight.w700,
      color: AppColors.textPrimary,
    ),
  );
}

/// Um carro para escolher: modelo, placa e quantas vagas ele tem.
class _VehicleOption extends StatelessWidget {
  final VehicleModel vehicle;
  final bool selected;
  final bool enabled;
  final VoidCallback onTap;

  const _VehicleOption({
    required this.vehicle,
    required this.selected,
    required this.enabled,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: selected ? AppColors.neutralLight : AppColors.cardBackground,
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        key: ValueKey('offer_vehicle_${vehicle.id}'),
        borderRadius: BorderRadius.circular(14),
        onTap: enabled
            ? () {
                HapticFeedback.selectionClick();
                onTap();
              }
            : null,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(14),
            border: Border.all(
              color: selected ? AppColors.primary : AppColors.borderSubtle,
              width: selected ? 1.6 : 1,
            ),
          ),
          child: Row(
            children: [
              const Icon(
                Icons.directions_car_rounded,
                color: AppColors.textPrimary,
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      vehicle.nickname?.isNotEmpty ?? false
                          ? '${vehicle.nickname} · ${vehicle.model}'
                          : vehicle.model,
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    Text(
                      '${vehicle.licensePlate.toUpperCase()} · '
                      '${vehicle.passengerSeats} ${vehicle.passengerSeats == 1 ? 'vaga' : 'vagas'}',
                      style: const TextStyle(
                        fontSize: 12.5,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(
                selected
                    ? Icons.radio_button_checked_rounded
                    : Icons.radio_button_unchecked_rounded,
                color: selected ? AppColors.primary : AppColors.iconMuted,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
