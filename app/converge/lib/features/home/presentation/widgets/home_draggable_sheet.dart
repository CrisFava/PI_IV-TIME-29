import 'package:flutter/material.dart';

import '../../../../core/config/styles.dart';


class HomeDraggableSheet extends StatelessWidget {
  final VoidCallback onSearchTap;
  final ValueChanged<String>? onDestinationSelected;
  final ValueChanged<String>? onRideSelected;
  final double bottomSafeArea;

  const HomeDraggableSheet({
    super.key,
    required this.onSearchTap,
    this.onDestinationSelected,
    this.onRideSelected,
    this.bottomSafeArea = 0,
  });

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      initialChildSize: 0.32,
      minChildSize: 0.20,
      maxChildSize: 0.82,
      snap: true,
      snapSizes: const [0.20, 0.32, 0.82],
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
              // Alça de Arraste (Drag Handle)
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

              // Conteúdo do Painel com Bottom Padding Protetor contra o Dock
              SliverPadding(
                padding: EdgeInsets.fromLTRB(16, 4, 16, 120 + bottomSafeArea),
                sliver: SliverList(
                  delegate: SliverChildListDelegate([
                    // 1. Campo de Busca Tátil
                    GestureDetector(
                      onTap: onSearchTap,
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 16,
                          vertical: 13,
                        ),
                        decoration: BoxDecoration(
                          color: AppColors.neutralLight,
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: AppColors.borderSubtle),
                        ),
                        child: const Row(
                          children: [
                            Icon(
                              Icons.search_rounded,
                              color: AppColors.textPrimary,
                              size: 21,
                            ),
                            SizedBox(width: 12),
                            Expanded(
                              child: Text(
                                'Para onde você vai hoje no campus?',
                                style: TextStyle(
                                  color: AppColors.textMuted,
                                  fontSize: 14,
                                  fontWeight: FontWeight.w400,
                                ),
                              ),
                            ),
                            Icon(
                              Icons.tune_rounded,
                              color: AppColors.textSecondary,
                              size: 19,
                            ),
                          ],
                        ),
                      ),
                    ),

                    const SizedBox(height: 18),

                    // 2. Atalhos Rápidos de Destino no Campus
                    SizedBox(
                      height: 38,
                      child: ListView(
                        scrollDirection: Axis.horizontal,
                        physics: const BouncingScrollPhysics(),
                        children: [
                          _buildShortcutChip(
                            icon: Icons.school_rounded,
                            label: 'Campus I - Bloco H',
                            onTap: () => onDestinationSelected?.call('Campus I - Bloco H'),
                          ),
                          const SizedBox(width: 8),
                          _buildShortcutChip(
                            icon: Icons.local_hospital_rounded,
                            label: 'Campus II - Medicina',
                            onTap: () => onDestinationSelected?.call('Campus II - Medicina'),
                          ),
                          const SizedBox(width: 8),
                          _buildShortcutChip(
                            icon: Icons.local_library_rounded,
                            label: 'Biblioteca Central',
                            onTap: () => onDestinationSelected?.call('Biblioteca Central'),
                          ),
                          const SizedBox(width: 8),
                          _buildShortcutChip(
                            icon: Icons.directions_bus_rounded,
                            label: 'Terminal Barão',
                            onTap: () => onDestinationSelected?.call('Terminal Barão'),
                          ),
                        ],
                      ),
                    ),

                    const SizedBox(height: 24),

                    // 3. Título de Caronas Próximas
                    const Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          'Caronas Disponíveis no Trajeto',
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w800,
                            color: AppColors.textPrimary,
                            letterSpacing: -0.2,
                          ),
                        ),
                        Text(
                          'Ver todas',
                          style: TextStyle(
                            fontSize: 12.5,
                            fontWeight: FontWeight.w600,
                            color: AppColors.textSecondary,
                          ),
                        ),
                      ],
                    ),

                    const SizedBox(height: 12),

                    // 4. Cards Táteis de Carona no Estilo Uber
                    _buildRideCard(
                      driverName: 'Lucas Silveira',
                      course: 'Eng. de Software',
                      origin: 'Barão Geraldo / Terminal',
                      destination: 'Campus I - Bloco H',
                      departureTime: 'Saída às 07:45',
                      availableSeats: 3,
                      price: 'R\$ 5,50',
                      onTap: () => onRideSelected?.call('Lucas Silveira'),
                    ),

                    const SizedBox(height: 10),

                    _buildRideCard(
                      driverName: 'Mariana Costa',
                      course: 'Medicina',
                      origin: 'Cambuí / Centro',
                      destination: 'Campus II - Saúde',
                      departureTime: 'Saída às 08:15',
                      availableSeats: 2,
                      price: 'R\$ 6,00',
                      onTap: () => onRideSelected?.call('Mariana Costa'),
                    ),

                    const SizedBox(height: 10),

                    _buildRideCard(
                      driverName: 'Rodrigo Nunes',
                      course: 'Direito',
                      origin: 'Taquaral / Shopping',
                      destination: 'Campus I - Portão 2',
                      departureTime: 'Saída às 08:30',
                      availableSeats: 4,
                      price: 'R\$ 4,50',
                      onTap: () => onRideSelected?.call('Rodrigo Nunes'),
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

  Widget _buildShortcutChip({
    required IconData icon,
    required String label,
    required VoidCallback onTap,
  }) {
    return Material(
      color: AppColors.chipBackground,
      borderRadius: BorderRadius.circular(19),
      child: InkWell(
        borderRadius: BorderRadius.circular(19),
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(19),
            border: Border.all(color: AppColors.chipBorder),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 14, color: AppColors.textPrimary),
              const SizedBox(width: 6),
              Text(
                label,
                style: const TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: AppColors.textPrimary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildRideCard({
    required String driverName,
    required String course,
    required String origin,
    required String destination,
    required String departureTime,
    required int availableSeats,
    required String price,
    required VoidCallback onTap,
  }) {
    return Material(
      color: AppColors.cardBackground,
      borderRadius: BorderRadius.circular(18),
      child: InkWell(
        borderRadius: BorderRadius.circular(18),
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.all(15),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: AppColors.borderSubtle),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Cabeçalho: Motorista e Preço
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 36,
                        height: 36,
                        decoration: const BoxDecoration(
                          color: AppColors.neutralLight,
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.person_rounded,
                          size: 20,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(width: 10),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            driverName,
                            style: const TextStyle(
                              fontSize: 13.5,
                              fontWeight: FontWeight.w700,
                              color: AppColors.textPrimary,
                            ),
                          ),
                          Text(
                            course,
                            style: const TextStyle(
                              fontSize: 11.5,
                              color: AppColors.textSecondary,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 10,
                      vertical: 5,
                    ),
                    decoration: BoxDecoration(
                      color: AppColors.primary,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      price,
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                        color: Colors.white,
                      ),
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 12),
              const Divider(color: AppColors.neutralLight, height: 1),
              const SizedBox(height: 12),

              // Rota: Origem -> Destino
              Row(
                children: [
                  const Icon(Icons.circle, size: 8, color: AppColors.textPrimary),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      origin,
                      style: const TextStyle(
                        fontSize: 12.5,
                        fontWeight: FontWeight.w500,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ),
                ],
              ),
              Padding(
                padding: const EdgeInsets.only(left: 3),
                child: Container(
                  width: 2,
                  height: 10,
                  color: AppColors.routeLine,
                ),
              ),
              Row(
                children: [
                  const Icon(
                    Icons.location_on_rounded,
                    size: 10,
                    color: AppColors.textPrimary,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      destination,
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                        color: AppColors.textPrimary,
                      ),
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 10),

              // Rodapé do Card: Horário e Vagas
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: [
                      const Icon(
                        Icons.access_time_rounded,
                        size: 14,
                        color: AppColors.textSecondary,
                      ),
                      const SizedBox(width: 5),
                      Text(
                        departureTime,
                        style: const TextStyle(
                          fontSize: 11.5,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 3,
                    ),
                    decoration: BoxDecoration(
                      color: AppColors.neutralLight,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      '$availableSeats vagas restantes',
                      style: const TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w600,
                        color: AppColors.textPrimary,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
