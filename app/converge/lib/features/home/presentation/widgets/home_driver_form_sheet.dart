import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';
import '../../../ride/data/models/driver_shift_payload.dart';

/// ============================================================================
/// COMPONENTE: HomeDriverFormSheet (Modo Motorista - Painel e Formulário)
/// ============================================================================
/// Painel retrátil com formulário para iniciar expediente de motorista:
/// 1. Destino do motorista
/// 2. Raio de destino (área de tolerância do passageiro)
/// 3. Raio em km a partir do local atual (refletido no mapa para apitar buscas)
/// 4. Horário de fim de busca com aviso de timeout automático
/// 5. Ação de salvar trajeto e botão principal de início de expediente
/// ============================================================================
class HomeDriverFormSheet extends StatefulWidget {
  final double initialSearchRadiusKm;
  final ValueChanged<double> onSearchRadiusChanged;
  final ValueChanged<DriverShiftPayload> onStartShift;
  final VoidCallback onSaveRoute;
  final double bottomSafeArea;

  const HomeDriverFormSheet({
    super.key,
    this.initialSearchRadiusKm = 5.0,
    required this.onSearchRadiusChanged,
    required this.onStartShift,
    required this.onSaveRoute,
    this.bottomSafeArea = 0,
  });

  @override
  State<HomeDriverFormSheet> createState() => _HomeDriverFormSheetState();
}

class _HomeDriverFormSheetState extends State<HomeDriverFormSheet> {
  final _formKey = GlobalKey<FormState>();
  final _destinationController = TextEditingController();

  late double _searchRadiusKm;
  double _destinationRadiusKm = 3.0;
  TimeOfDay _endSearchTime = TimeOfDay.fromDateTime(
    DateTime.now().add(const Duration(hours: 2)),
  );

  @override
  void initState() {
    super.initState();
    _searchRadiusKm = widget.initialSearchRadiusKm;
  }

  @override
  void dispose() {
    _destinationController.dispose();
    super.dispose();
  }

  Future<void> _selectTime(BuildContext context) async {
    final picked = await showTimePicker(
      context: context,
      initialTime: _endSearchTime,
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: const ColorScheme.light(
              primary: AppColors.primary,
              onPrimary: Colors.white,
              onSurface: AppColors.textPrimary,
            ),
          ),
          child: child!,
        );
      },
    );
    if (picked != null) {
      setState(() => _endSearchTime = picked);
    }
  }

  void _submit() {
    if (!_formKey.currentState!.validate()) return;

    HapticFeedback.mediumImpact();
    final payload = DriverShiftPayload(
      driverId: 'current_user',
      destination: _destinationController.text.trim(),
      destinationRadiusKm: _destinationRadiusKm,
      searchRadiusKm: _searchRadiusKm,
      endSearchTime: _endSearchTime,
      startedAt: DateTime.now(),
    );

    widget.onStartShift(payload);
  }

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      initialChildSize: 0.42,
      minChildSize: 0.22,
      maxChildSize: 0.88,
      snap: true,
      snapSizes: const [0.22, 0.42, 0.88],
      builder: (context, scrollController) {
        return Container(
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: const BorderRadius.vertical(top: Radius.circular(26)),
            border: Border.all(color: AppColors.borderSubtle, width: 1),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.12),
                blurRadius: 24,
                offset: const Offset(0, -6),
              ),
            ],
          ),
          child: CustomScrollView(
            controller: scrollController,
            slivers: [
              // Alça de arraste
              SliverToBoxAdapter(
                child: Center(
                  child: Container(
                    margin: const EdgeInsets.symmetric(vertical: 10),
                    width: 38,
                    height: 4,
                    decoration: BoxDecoration(
                      color: AppColors.borderMedium,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
              ),

              SliverPadding(
                padding: EdgeInsets.fromLTRB(16, 4, 16, 120 + widget.bottomSafeArea),
                sliver: SliverList(
                  delegate: SliverChildListDelegate([
                    // Cabeçalho do Modo Motorista com Badge
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Row(
                          children: [
                            Icon(
                              Icons.drive_eta_rounded,
                              size: 20,
                              color: AppColors.textPrimary,
                            ),
                            SizedBox(width: 8),
                            Text(
                              'Iniciar Expediente de Motorista',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.w800,
                                color: AppColors.textPrimary,
                                letterSpacing: -0.3,
                              ),
                            ),
                          ],
                        ),
                        // Botão rápido para salvar trajeto
                        IconButton(
                          tooltip: 'Salvar trajeto frequente',
                          visualDensity: VisualDensity.compact,
                          onPressed: widget.onSaveRoute,
                          icon: const Icon(
                            Icons.bookmark_add_rounded,
                            size: 20,
                            color: AppColors.textPrimary,
                          ),
                        ),
                      ],
                    ),

                    const SizedBox(height: 14),

                    Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          // 1. Campo de Destino
                          const Text(
                            'Para onde você está indo?',
                            style: TextStyle(
                              fontSize: 13.5,
                              fontWeight: FontWeight.w600,
                              color: AppColors.textPrimary,
                            ),
                          ),
                          const SizedBox(height: 6),
                          TextFormField(
                            controller: _destinationController,
                            style: const TextStyle(
                              fontSize: 14,
                              color: AppColors.textPrimary,
                            ),
                            decoration: InputDecoration(
                              hintText: 'Ex.: Campus Universitário / Reitoria',
                              hintStyle: const TextStyle(
                                color: AppColors.textMuted,
                                fontSize: 13.5,
                              ),
                              prefixIcon: const Icon(
                                Icons.pin_drop_rounded,
                                size: 18,
                                color: AppColors.textPrimary,
                              ),
                              filled: true,
                              fillColor: AppColors.neutralLight,
                              contentPadding: const EdgeInsets.symmetric(
                                horizontal: 14,
                                vertical: 12,
                              ),
                              enabledBorder: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(14),
                                borderSide: const BorderSide(
                                  color: AppColors.borderSubtle,
                                ),
                              ),
                              focusedBorder: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(14),
                                borderSide: const BorderSide(
                                  color: AppColors.primary,
                                  width: 1.5,
                                ),
                              ),
                            ),
                            validator: (v) =>
                                (v == null || v.trim().isEmpty)
                                    ? 'Informe seu destino'
                                    : null,
                          ),

                          const SizedBox(height: 18),

                          // 2. Raio de Destino do Passageiro
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              const Text(
                                'Tolerância no destino do passageiro',
                                style: TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                  color: AppColors.textPrimary,
                                ),
                              ),
                              Text(
                                '${_destinationRadiusKm.toStringAsFixed(1)} km',
                                style: const TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w700,
                                  color: AppColors.primary,
                                ),
                              ),
                            ],
                          ),
                          Slider(
                            value: _destinationRadiusKm,
                            min: 1.0,
                            max: 10.0,
                            divisions: 18,
                            activeColor: AppColors.primary,
                            inactiveColor: AppColors.borderSubtle,
                            onChanged: (val) {
                              setState(() => _destinationRadiusKm = val);
                            },
                          ),
                          const Text(
                            'O passageiro deve desembarcar em até esta distância do seu destino final.',
                            style: TextStyle(
                              fontSize: 11.5,
                              color: AppColors.textSecondary,
                            ),
                          ),

                          const SizedBox(height: 18),

                          // 3. Raio em KM Visual no Mapa (Para apitar buscas)
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              const Text(
                                'Raio de busca ao seu redor (no mapa)',
                                style: TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                  color: AppColors.textPrimary,
                                ),
                              ),
                              Text(
                                '${_searchRadiusKm.toStringAsFixed(1)} km',
                                style: const TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w700,
                                  color: AppColors.primary,
                                ),
                              ),
                            ],
                          ),
                          Slider(
                            value: _searchRadiusKm,
                            min: 1.0,
                            max: 15.0,
                            divisions: 28,
                            activeColor: AppColors.primary,
                            inactiveColor: AppColors.borderSubtle,
                            onChanged: (val) {
                              setState(() => _searchRadiusKm = val);
                              widget.onSearchRadiusChanged(val);
                            },
                          ),
                          const Text(
                            'O aplicativo apitará solicitações de passageiros dentro do círculo desenhado no mapa.',
                            style: TextStyle(
                              fontSize: 11.5,
                              color: AppColors.textSecondary,
                            ),
                          ),

                          const SizedBox(height: 18),

                          // 4. Horário de Fim de Busca
                          const Text(
                            'Horário de fim de busca',
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: AppColors.textPrimary,
                            ),
                          ),
                          const SizedBox(height: 6),
                          InkWell(
                            borderRadius: BorderRadius.circular(14),
                            onTap: () => _selectTime(context),
                            child: Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 14,
                                vertical: 12,
                              ),
                              decoration: BoxDecoration(
                                color: AppColors.neutralLight,
                                borderRadius: BorderRadius.circular(14),
                                border: Border.all(color: AppColors.borderSubtle),
                              ),
                              child: Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Row(
                                    children: [
                                      const Icon(
                                        Icons.alarm_rounded,
                                        size: 18,
                                        color: AppColors.textPrimary,
                                      ),
                                      const SizedBox(width: 8),
                                      Text(
                                        _endSearchTime.format(context),
                                        style: const TextStyle(
                                          fontSize: 14,
                                          fontWeight: FontWeight.w700,
                                          color: AppColors.textPrimary,
                                        ),
                                      ),
                                    ],
                                  ),
                                  const Text(
                                    'Alterar',
                                    style: TextStyle(
                                      fontSize: 12.5,
                                      fontWeight: FontWeight.w600,
                                      color: AppColors.textSecondary,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(height: 4),
                          Container(
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: AppColors.neutralLight,
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: const Row(
                              children: [
                                Icon(
                                  Icons.info_outline_rounded,
                                  size: 15,
                                  color: AppColors.textSecondary,
                                ),
                                SizedBox(width: 8),
                                Expanded(
                                  child: Text(
                                    'Atingido este horário sem corridas agendadas, seu expediente se encerra automaticamente.',
                                    style: TextStyle(
                                      fontSize: 11,
                                      color: AppColors.textSecondary,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),

                          const SizedBox(height: 22),

                          // 5. Botão de Iniciar Expediente
                          SizedBox(
                            height: 50,
                            child: FilledButton.icon(
                              style: FilledButton.styleFrom(
                                backgroundColor: AppColors.primary,
                                foregroundColor: Colors.white,
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(14),
                                ),
                              ),
                              onPressed: _submit,
                              icon: const Icon(Icons.flash_on_rounded, size: 18),
                              label: const Text(
                                'Iniciar Expediente',
                                style: TextStyle(
                                  fontSize: 15,
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ]),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
