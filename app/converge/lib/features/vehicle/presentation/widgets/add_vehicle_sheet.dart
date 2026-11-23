// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../auth/state/auth_action_controller.dart';
import '../../data/models/create_vehicle_request.dart';
import '../../data/models/vehicle_model.dart';
import '../../state/my_vehicles_controller.dart';
import 'vehicle_form.dart';

/// Abre o cadastro de um carro. Devolve true quando o carro foi salvo.
Future<bool?> showAddVehicleSheet(BuildContext context) =>
    _showVehicleSheet(context, const AddVehicleSheet());

/// Abre a edição de um carro, já preenchida. Devolve true quando foi salvo.
Future<bool?> showEditVehicleSheet(
  BuildContext context,
  VehicleModel vehicle,
) => _showVehicleSheet(context, AddVehicleSheet(vehicle: vehicle));

Future<bool?> _showVehicleSheet(BuildContext context, AddVehicleSheet sheet) {
  HapticFeedback.mediumImpact();
  return showModalBottomSheet<bool>(
    context: context,
    backgroundColor: AppColors.cardBackground,
    barrierColor: AppColors.barrier,
    isScrollControlled: true,
    useSafeArea: true,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
    ),
    builder: (_) => sheet,
  );
}

/// Folha com o formulário de carro, usada no perfil e no desbloqueio do motorista.
/// Com [vehicle], edita esse carro; sem ele, cadastra um novo.
class AddVehicleSheet extends ConsumerStatefulWidget {
  final VehicleModel? vehicle;

  const AddVehicleSheet({super.key, this.vehicle});

  @override
  ConsumerState<AddVehicleSheet> createState() => _AddVehicleSheetState();
}

class _AddVehicleSheetState extends ConsumerState<AddVehicleSheet> {
  bool _isSaving = false;
  String? _error;

  Future<void> _save(CreateVehicleRequest request) async {
    setState(() {
      _isSaving = true;
      _error = null;
    });
    try {
      final vehicle = widget.vehicle;
      final controller = ref.read(myVehiclesProvider.notifier);
      if (vehicle == null) {
        await controller.add(request);
      } else {
        await controller.edit(vehicle.id, request);
      }
      if (!mounted) return;
      HapticFeedback.mediumImpact();
      Navigator.of(context).pop(true);
    } catch (e) {
      if (!mounted) return;
      HapticFeedback.heavyImpact();
      setState(() {
        _isSaving = false;
        _error = authErrorMessage(e);
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final isEditing = widget.vehicle != null;
    final bottomInset = MediaQuery.viewInsetsOf(context).bottom;
    final bottomPadding = MediaQuery.paddingOf(context).bottom;

    return Padding(
      padding: EdgeInsets.only(bottom: bottomInset),
      child: SingleChildScrollView(
        padding: EdgeInsets.fromLTRB(22, 12, 22, 24 + bottomPadding),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(
              child: Container(
                width: 36,
                height: 4,
                decoration: BoxDecoration(
                  color: AppColors.borderMedium,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: AppColors.primary,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Icon(
                    Icons.directions_car_rounded,
                    color: AppColors.dockItemActive,
                    size: 24,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        isEditing ? 'Editar carro' : 'Adicionar carro',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w800,
                          color: AppColors.textPrimary,
                          letterSpacing: -0.3,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        isEditing ? 'Altere os dados e salve.' : 'Com um carro cadastrado, o modo motorista fica liberado.',
                        style: const TextStyle(
                          fontSize: 12.5,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  tooltip: 'Fechar',
                  onPressed: _isSaving
                      ? null
                      : () => Navigator.of(context).pop(false),
                  icon: const Icon(
                    Icons.close_rounded,
                    color: AppColors.textSecondary,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 18),
            VehicleForm(
              initial: isEditing
                  ? CreateVehicleRequest.fromVehicle(widget.vehicle!)
                  : null,
              submitLabel: isEditing ? 'Salvar alterações' : 'Salvar carro',
              isSubmitting: _isSaving,
              errorMessage: _error,
              onSubmit: _save,
            ),
          ],
        ),
      ),
    );
  }
}
