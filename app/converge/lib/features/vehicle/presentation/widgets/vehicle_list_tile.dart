// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../data/models/vehicle_model.dart';

/// Um carro na lista do perfil: tocar no cartão abre a edição; a lixeira apaga.
class VehicleListTile extends StatelessWidget {
  final VehicleModel vehicle;
  final VoidCallback onEdit;
  final VoidCallback onDelete;
  final bool isDeleting;

  const VehicleListTile({
    super.key,
    required this.vehicle,
    required this.onEdit,
    required this.onDelete,
    this.isDeleting = false,
  });

  @override
  Widget build(BuildContext context) {
    final fuel = vehicle.fuelSources.isEmpty ? null : vehicle.fuelSources.first;
    final details = [
      vehicle.formattedPlate,
      if (vehicle.color.isNotEmpty) vehicle.color,
      '${vehicle.passengerSeats} ${vehicle.passengerSeats == 1 ? 'vaga' : 'vagas'}',
      if (fuel != null) fuel.energySource.label,
    ].join(' • ');

    return Material(
      color: AppColors.cardSecondary,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        key: ValueKey('vehicle_tile_${vehicle.id}'),
        borderRadius: BorderRadius.circular(16),
        onTap: isDeleting
            ? null
            : () {
                HapticFeedback.lightImpact();
                onEdit();
              },
        child: Container(
          padding: const EdgeInsets.fromLTRB(14, 12, 6, 12),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: AppColors.borderSubtle),
          ),
          child: Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: AppColors.cardBackground,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppColors.borderSubtle),
                ),
                child: Icon(
                  vehicle.type.isMotorcycle
                      ? Icons.two_wheeler_rounded
                      : Icons.directions_car_rounded,
                  size: 21,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      vehicle.displayName,
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    if (vehicle.displayName != vehicle.model)
                      Text(
                        vehicle.model,
                        style: const TextStyle(
                          fontSize: 12,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    const SizedBox(height: 2),
                    Text(
                      details,
                      style: const TextStyle(
                        fontSize: 11.5,
                        color: AppColors.textMuted,
                      ),
                    ),
                  ],
                ),
              ),
              isDeleting
                  ? const Padding(
                      padding: EdgeInsets.all(12),
                      child: SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: AppColors.error,
                        ),
                      ),
                    )
                  : IconButton(
                      tooltip: 'Apagar carro',
                      onPressed: onDelete,
                      icon: const Icon(
                        Icons.delete_outline_rounded,
                        color: AppColors.error,
                      ),
                    ),
            ],
          ),
        ),
      ),
    );
  }
}
