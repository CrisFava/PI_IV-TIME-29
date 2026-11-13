// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../data/models/create_vehicle_request.dart';
import '../../data/models/energy_source.dart';
import '../../data/models/fuel_source_model.dart';
import '../../data/models/vehicle_type.dart';
import '../../data/validation/invalid_vehicle_exception.dart';
import '../../data/validation/vehicle_validator.dart';
import 'plate_upper_case_formatter.dart';
import 'vehicle_capacity_stepper.dart';
import 'vehicle_choice_chips.dart';
import 'vehicle_error_banner.dart';
import 'vehicle_input_decoration.dart';

/// Formulário completo de um veículo. Só chama [onSubmit] com dados válidos:
/// cada campo é conferido e o pedido inteiro passa pelo [VehicleValidator].
class VehicleForm extends StatefulWidget {
  /// Dados para preencher o formulário (ao voltar para este passo, por exemplo).
  final CreateVehicleRequest? initial;
  final String submitLabel;
  final bool isSubmitting;

  /// Erro vindo de fora (ex.: o backend recusou o veículo).
  final String? errorMessage;
  final ValueChanged<CreateVehicleRequest> onSubmit;

  /// Mostra o botão de voltar ao lado do botão principal.
  final VoidCallback? onBack;

  /// Avisa a cada alteração, para quem precisa guardar o rascunho.
  final ValueChanged<CreateVehicleRequest>? onChanged;

  const VehicleForm({
    super.key,
    required this.submitLabel,
    required this.onSubmit,
    this.initial,
    this.isSubmitting = false,
    this.errorMessage,
    this.onBack,
    this.onChanged,
  });

  @override
  State<VehicleForm> createState() => _VehicleFormState();
}

class _VehicleFormState extends State<VehicleForm> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _model;
  late final TextEditingController _nickname;
  late final TextEditingController _plate;
  late final TextEditingController _color;
  late final TextEditingController _renavam;
  late final TextEditingController _consumption;
  late final TextEditingController _price;

  late VehicleType _type;
  late EnergySource _energy;
  late int _capacity;
  String? _capacityError;
  String? _localError;

  @override
  void initState() {
    super.initState();
    final initial = widget.initial;
    final fuel = initial?.fuelSource;
    _model = TextEditingController(text: initial?.model ?? '');
    _nickname = TextEditingController(text: initial?.nickname ?? '');
    _plate = TextEditingController(text: initial?.licensePlate ?? '');
    _color = TextEditingController(text: initial?.color ?? '');
    _renavam = TextEditingController(text: initial?.renavam ?? '');
    _consumption = TextEditingController(
      text: fuel == null ? '' : _decimal(fuel.averageConsumption),
    );
    _price = TextEditingController(
      text: fuel == null ? '' : _decimal(fuel.currentEnergyPrice),
    );
    _type = initial?.type ?? VehicleType.hatch;
    _energy = fuel?.energySource ?? EnergySource.gasoline;
    _capacity = initial?.totalCapacity ?? 5;
  }

  @override
  void dispose() {
    for (final controller in [
      _model,
      _nickname,
      _plate,
      _color,
      _renavam,
      _consumption,
      _price,
    ]) {
      controller.dispose();
    }
    super.dispose();
  }

  static String _decimal(double value) => value.toString().replaceAll('.', ',');

  CreateVehicleRequest _buildRequest() => CreateVehicleRequest(
    nickname: _nickname.text,
    model: _model.text,
    licensePlate: _plate.text,
    color: _color.text,
    type: _type,
    totalCapacity: _capacity,
    renavam: _renavam.text,
    fuelSource: FuelSourceModel(
      energySource: _energy,
      averageConsumption: VehicleValidator.parseDecimal(_consumption.text) ?? 0,
      currentEnergyPrice: VehicleValidator.parseDecimal(_price.text) ?? 0,
    ),
  );

  void _notifyChanged() => widget.onChanged?.call(_buildRequest());

  void _selectType(VehicleType type) {
    setState(() {
      _type = type;
      // Uma moto leva no máximo 2 lugares
      if (_capacity > type.maxCapacity) _capacity = type.maxCapacity;
      _capacityError = null;
    });
    _notifyChanged();
  }

  void _submit() {
    FocusScope.of(context).unfocus();
    final capacityError = VehicleValidator.validateCapacity(_capacity, _type);
    final fieldsValid = _formKey.currentState!.validate();
    setState(() {
      _capacityError = capacityError;
      _localError = null;
    });
    if (!fieldsValid || capacityError != null) {
      HapticFeedback.heavyImpact();
      return;
    }

    final request = _buildRequest();
    try {
      VehicleValidator.validate(request);
    } on InvalidVehicleException catch (e) {
      HapticFeedback.heavyImpact();
      setState(() => _localError = e.message);
      return;
    }

    HapticFeedback.mediumImpact();
    widget.onSubmit(request);
  }

  @override
  Widget build(BuildContext context) {
    final error = _localError ?? widget.errorMessage;
    final (minConsumption, maxConsumption) = VehicleValidator.consumptionRange(
      _type,
      _energy,
    );
    final (minPrice, maxPrice) = VehicleValidator.priceRange(_energy);
    final enabled = !widget.isSubmitting;

    return Form(
      key: _formKey,
      onChanged: _notifyChanged,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (error != null) ...[
            VehicleErrorBanner(message: error),
            const SizedBox(height: 14),
          ],
          TextFormField(
            controller: _model,
            enabled: enabled,
            textCapitalization: TextCapitalization.words,
            decoration: vehicleInputDecoration(
              label: 'Modelo',
              hint: 'Ex.: Onix 1.0',
              icon: Icons.directions_car_rounded,
            ),
            validator: VehicleValidator.validateModel,
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _nickname,
            enabled: enabled,
            textCapitalization: TextCapitalization.sentences,
            decoration: vehicleInputDecoration(
              label: 'Apelido (opcional)',
              hint: 'Ex.: Carro da faculdade',
              icon: Icons.label_rounded,
            ),
          ),
          const SizedBox(height: 12),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: TextFormField(
                  controller: _plate,
                  enabled: enabled,
                  textCapitalization: TextCapitalization.characters,
                  inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp('[A-Za-z0-9-]')),
                    LengthLimitingTextInputFormatter(8),
                    const PlateUpperCaseFormatter(),
                  ],
                  decoration: vehicleInputDecoration(
                    label: 'Placa',
                    hint: 'ABC1D23',
                    icon: Icons.pin_rounded,
                  ),
                  validator: VehicleValidator.validatePlate,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: TextFormField(
                  controller: _color,
                  enabled: enabled,
                  textCapitalization: TextCapitalization.words,
                  decoration: vehicleInputDecoration(
                    label: 'Cor',
                    hint: 'Ex.: Prata',
                    icon: Icons.palette_rounded,
                  ),
                  validator: VehicleValidator.validateColor,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _renavam,
            enabled: enabled,
            keyboardType: TextInputType.number,
            inputFormatters: [
              FilteringTextInputFormatter.digitsOnly,
              LengthLimitingTextInputFormatter(11),
            ],
            decoration: vehicleInputDecoration(
              label: 'RENAVAM',
              hint: '11 dígitos (está no documento do veículo)',
              icon: Icons.description_rounded,
            ),
            validator: VehicleValidator.validateRenavam,
          ),
          const SizedBox(height: 16),
          VehicleChoiceChips<VehicleType>(
            title: 'Tipo do veículo',
            options: VehicleType.values,
            selected: _type,
            labelOf: (type) => type.label,
            onSelected: _selectType,
            enabled: enabled,
          ),
          const SizedBox(height: 14),
          VehicleCapacityStepper(
            value: _capacity,
            max: _type.maxCapacity,
            errorText: _capacityError,
            enabled: enabled,
            onChanged: (value) {
              setState(() {
                _capacity = value;
                _capacityError = null;
              });
              _notifyChanged();
            },
          ),
          const SizedBox(height: 16),
          VehicleChoiceChips<EnergySource>(
            title: 'Combustível',
            options: EnergySource.values,
            selected: _energy,
            labelOf: (source) => source.label,
            onSelected: (source) {
              setState(() => _energy = source);
              _notifyChanged();
            },
            enabled: enabled,
          ),
          const SizedBox(height: 12),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: TextFormField(
                  controller: _consumption,
                  enabled: enabled,
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
                  inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'[0-9,.]')),
                  ],
                  decoration: vehicleInputDecoration(
                    label: 'Consumo médio',
                    hint:
                        '${minConsumption.toStringAsFixed(0)} a ${maxConsumption.toStringAsFixed(0)}',
                    suffix: _energy.consumptionUnitLabel,
                    icon: Icons.speed_rounded,
                  ),
                  validator: (value) => VehicleValidator.validateConsumption(
                    VehicleValidator.parseDecimal(value),
                    _type,
                    _energy,
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: TextFormField(
                  controller: _price,
                  enabled: enabled,
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
                  inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'[0-9,.]')),
                  ],
                  decoration: vehicleInputDecoration(
                    label: 'Preço',
                    hint:
                        '${minPrice.toStringAsFixed(2)} a ${maxPrice.toStringAsFixed(2)}',
                    suffix: _energy.priceUnitLabel,
                    icon: Icons.local_gas_station_rounded,
                  ),
                  validator: (value) => VehicleValidator.validatePrice(
                    VehicleValidator.parseDecimal(value),
                    _energy,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 22),
          Row(
            children: [
              if (widget.onBack != null) ...[
                Container(
                  width: 52,
                  height: 52,
                  decoration: BoxDecoration(
                    color: AppColors.neutralLight,
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: AppColors.borderSubtle),
                  ),
                  child: IconButton(
                    onPressed: enabled
                        ? () {
                            HapticFeedback.lightImpact();
                            widget.onBack!();
                          }
                        : null,
                    icon: const Icon(
                      Icons.arrow_back_rounded,
                      color: AppColors.textPrimary,
                      size: 22,
                    ),
                    tooltip: 'Voltar',
                  ),
                ),
                const SizedBox(width: 12),
              ],
              Expanded(
                child: SizedBox(
                  height: 52,
                  child: FilledButton(
                    key: const ValueKey('vehicle_form_submit'),
                    onPressed: enabled ? _submit : null,
                    style: FilledButton.styleFrom(
                      backgroundColor: AppColors.primary,
                      foregroundColor: AppColors.dockItemActive,
                      disabledBackgroundColor: AppColors.borderMedium,
                      elevation: 0,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(14),
                      ),
                    ),
                    child: widget.isSubmitting
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(
                              strokeWidth: 2.2,
                              color: AppColors.dockItemActive,
                            ),
                          )
                        : Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                widget.submitLabel,
                                style: const TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                              const Icon(Icons.arrow_forward_rounded, size: 20),
                            ],
                          ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
