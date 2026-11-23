// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/more_info_hint.dart';
import '../../data/models/vehicle_model.dart';
import 'license_plate_badge.dart';

/// Um carro na lista do perfil. Tocar no cartão abre os detalhes, onde ficam
/// "Editar" e "Excluir" (o mesmo padrão dos cards de "Caronas ativas").
class VehicleListTile extends StatelessWidget {
  final VehicleModel vehicle;
  final VoidCallback onOpenDetails;
  final bool isDeleting;

  const VehicleListTile({
    super.key,
    required this.vehicle,
    required this.onOpenDetails,
    this.isDeleting = false,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.cardBackground,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        key: ValueKey('vehicle_tile_${vehicle.id}'),
        borderRadius: BorderRadius.circular(16),
        onTap: isDeleting
            ? null
            : () {
                HapticFeedback.lightImpact();
                onOpenDetails();
              },
        child: Container(
          padding: const EdgeInsets.fromLTRB(14, 12, 10, 10),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: AppColors.borderMedium),
          ),
          // Nome (e modelo) com "Mais informações" embaixo; a placa à direita,
          // no alto, na linha do nome
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      vehicle.displayName,
                      style: const TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w800,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    if (vehicle.displayName != vehicle.model)
                      Text(
                        vehicle.model,
                        style: const TextStyle(
                          fontSize: 12.5,
                          color: AppColors.textDark,
                        ),
                      ),
                    const SizedBox(height: 6),
                    isDeleting
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: AppColors.error,
                            ),
                          )
                        : const MoreInfoHint(),
                  ],
                ),
              ),
              const SizedBox(width: 10),
              LicensePlateBadge(plate: vehicle.formattedPlate),
            ],
          ),
        ),
      ),
    );
  }
}
