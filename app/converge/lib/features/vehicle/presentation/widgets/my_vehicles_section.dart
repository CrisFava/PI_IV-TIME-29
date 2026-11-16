// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/details_sheet.dart';
import '../../../../core/widgets/show_app_notice.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../data/models/vehicle_model.dart';
import '../../state/my_vehicles_controller.dart';
import 'add_vehicle_sheet.dart';
import 'confirm_delete_vehicle_dialog.dart';
import 'vehicle_details.dart';
import 'vehicle_list_tile.dart';
import 'vehicles_empty_state.dart';

/// Seção "Meus carros" do perfil do motorista: lista e adicionar. Tocar num
/// carro abre os detalhes, com editar e excluir.
class MyVehiclesSection extends ConsumerStatefulWidget {
  const MyVehiclesSection({super.key});

  @override
  ConsumerState<MyVehiclesSection> createState() => _MyVehiclesSectionState();
}

class _MyVehiclesSectionState extends ConsumerState<MyVehiclesSection> {
  // Carros sendo apagados: cada um sai do conjunto quando a sua requisição termina
  final Set<String> _deletingIds = {};

  void _showMessage(String message) =>
      showAppNotice(context, message, type: AppNoticeType.error);

  Future<void> _add() async {
    await showAddVehicleSheet(context);
  }

  void _openDetails(VehicleModel vehicle, int total) {
    showDetailsSheet<void>(
      context,
      title: 'Detalhes do carro',
      builder: (sheetContext) => VehicleDetails(
        vehicle: vehicle,
        // Editar troca os detalhes pela folha de edição
        onEdit: () {
          Navigator.of(sheetContext).pop();
          showEditVehicleSheet(context, vehicle);
        },
        onDelete: () => _delete(vehicle, total),
      ),
    );
  }

  /// Pede a confirmação e apaga o carro; devolve se apagou.
  Future<bool> _delete(VehicleModel vehicle, int total) async {
    if (_deletingIds.contains(vehicle.id)) return false;
    final isLast = total == 1;
    final confirmed = await confirmDeleteVehicle(
      context,
      vehicle: vehicle,
      isLastVehicle: isLast,
    );
    if (!confirmed || !mounted || _deletingIds.contains(vehicle.id)) {
      return false;
    }

    setState(() => _deletingIds.add(vehicle.id));
    try {
      await ref.read(myVehiclesProvider.notifier).remove(vehicle.id);
      return true;
    } catch (e) {
      if (mounted) _showMessage(authErrorMessage(e));
      return false;
    } finally {
      if (mounted) setState(() => _deletingIds.remove(vehicle.id));
    }
  }

  @override
  Widget build(BuildContext context) {
    final vehicles = ref.watch(myVehiclesProvider);

    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              const Icon(
                Icons.garage_rounded,
                size: 20,
                color: AppColors.textPrimary,
              ),
              const SizedBox(width: 8),
              const Expanded(
                child: Text(
                  'Meus carros',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w800,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
              TextButton.icon(
                key: const ValueKey('add_vehicle_button'),
                onPressed: _add,
                style: TextButton.styleFrom(
                  foregroundColor: AppColors.textPrimary,
                ),
                icon: const Icon(Icons.add_rounded, size: 18),
                label: const Text(
                  'Adicionar',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          ...switch (vehicles) {
            AsyncData(:final value) when value.isEmpty => [
              VehiclesEmptyState(onAdd: _add),
            ],
            AsyncData(:final value) => [
              for (final vehicle in value)
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: VehicleListTile(
                    vehicle: vehicle,
                    isDeleting: _deletingIds.contains(vehicle.id),
                    onOpenDetails: () => _openDetails(vehicle, value.length),
                  ),
                ),
            ],
            AsyncError(:final error) => [
              Text(
                authErrorMessage(error),
                style: const TextStyle(
                  fontSize: 13,
                  color: AppColors.textSecondary,
                ),
              ),
              TextButton(
                onPressed: () => ref.invalidate(myVehiclesProvider),
                child: const Text('Tentar de novo'),
              ),
            ],
            _ => const [
              Padding(
                padding: EdgeInsets.all(16),
                child: Center(
                  child: CircularProgressIndicator(color: AppColors.primary),
                ),
              ),
            ],
          },
        ],
      ),
    );
  }
}
