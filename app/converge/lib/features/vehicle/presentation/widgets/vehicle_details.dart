// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/details_danger_button.dart';
import '../../../../core/widgets/details_section.dart';
import '../../data/models/vehicle_model.dart';
import 'license_plate_badge.dart';

/// Detalhes de um carro do perfil, com "Editar" e "Excluir". Excluir pede a
/// confirmação (e avisa quando é o único carro) antes de apagar.
class VehicleDetails extends StatefulWidget {
  final VehicleModel vehicle;
  final VoidCallback onEdit;

  /// Pede a confirmação e apaga; devolve se apagou.
  final Future<bool> Function() onDelete;

  const VehicleDetails({
    super.key,
    required this.vehicle,
    required this.onEdit,
    required this.onDelete,
  });

  @override
  State<VehicleDetails> createState() => _VehicleDetailsState();
}

class _VehicleDetailsState extends State<VehicleDetails> {
  bool _deleting = false;

  static String _number(double value) {
    final text = value.toStringAsFixed(1).replaceAll('.', ',');
    return text.endsWith(',0') ? text.substring(0, text.length - 2) : text;
  }

  Future<void> _delete() async {
    // A folha de detalhes antes da espera: depois dela, só fecha se ainda for a da frente
    final sheet = ModalRoute.of(context);
    setState(() => _deleting = true);
    final deleted = await widget.onDelete();
    if (!mounted) return;
    if (deleted) {
      if (sheet != null && sheet.isCurrent) Navigator.of(context).pop();
      return;
    }
    setState(() => _deleting = false);
  }

  @override
  Widget build(BuildContext context) {
    final vehicle = widget.vehicle;
    final seats = vehicle.passengerSeats;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    vehicle.displayName,
                    style: const TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w800,
                      color: AppColors.textPrimary,
                    ),
                  ),
                  if (vehicle.displayName != vehicle.model)
                    Text(
                      vehicle.model,
                      style: const TextStyle(
                        fontSize: 13.5,
                        color: AppColors.textDark,
                      ),
                    ),
                ],
              ),
            ),
            const SizedBox(width: 10),
            LicensePlateBadge(plate: vehicle.formattedPlate),
          ],
        ),
        DetailsSection(
          title: 'Carro',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _row(Icons.category_rounded, 'Tipo', vehicle.type.label),
              if (vehicle.color.isNotEmpty)
                _row(Icons.palette_rounded, 'Cor', vehicle.color),
              _row(
                Icons.event_seat_rounded,
                'Vagas para passageiros',
                '$seats',
              ),
            ],
          ),
        ),
        if (vehicle.fuelSources.isNotEmpty)
          DetailsSection(
            title: 'Combustível',
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                for (final fuel in vehicle.fuelSources)
                  _row(
                    Icons.local_gas_station_rounded,
                    fuel.energySource.label,
                    '${_number(fuel.averageConsumption)} '
                    '${fuel.energySource.consumptionUnitLabel}',
                  ),
              ],
            ),
          ),
        const SizedBox(height: 24),
        FilledButton.icon(
          key: const ValueKey('edit_vehicle_button'),
          onPressed: _deleting
              ? null
              : () {
                  HapticFeedback.lightImpact();
                  widget.onEdit();
                },
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.primary,
            foregroundColor: AppColors.textLight,
            padding: const EdgeInsets.symmetric(vertical: 13),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
            ),
          ),
          icon: const Icon(Icons.edit_rounded, size: 20),
          label: const Text(
            'Editar',
            style: TextStyle(fontWeight: FontWeight.w700),
          ),
        ),
        DetailsDangerButton(
          key: const ValueKey('delete_vehicle_button'),
          label: 'Excluir carro',
          isBusy: _deleting,
          onPressed: _delete,
        ),
      ],
    );
  }

  Widget _row(IconData icon, String label, String value) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 5),
    child: Row(
      children: [
        Icon(icon, size: 18, color: AppColors.textPrimary),
        const SizedBox(width: 10),
        Expanded(
          child: Text(
            label,
            style: const TextStyle(fontSize: 13.5, color: AppColors.textDark),
          ),
        ),
        Text(
          value,
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w800,
            color: AppColors.textPrimary,
          ),
        ),
      ],
    ),
  );
}
