import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../config/styles.dart';

class RideDockNavigation extends StatefulWidget {
  /// Índice da aba atualmente selecionada (0 a 3).
  final int currentIndex;

  /// Callback disparado ao alternar entre as abas.
  final ValueChanged<int> onTabSelected;

  /// Callback disparado quando o usuário seleciona "Pedir ride".
  final VoidCallback onRequestRide;

  /// Callback disparado quando o usuário seleciona "Iniciar expediente de motorista".
  final VoidCallback onStartDriverShift;

  /// Estado de abertura opcional controlado externamente.
  final bool? isOpen;

  /// Callback para alternar o estado de abertura.
  final VoidCallback? onToggleOpen;

  /// Se o usuário está habilitado para ser motorista (role DRIVER).
  final bool canDrive;

  /// Callback acionado quando o usuário clica na ação de motorista bloqueada.
  final VoidCallback? onLockedDriverTap;

  const RideDockNavigation({
    super.key,
    required this.currentIndex,
    required this.onTabSelected,
    required this.onRequestRide,
    required this.onStartDriverShift,
    this.isOpen,
    this.onToggleOpen,
    this.canDrive = true,
    this.onLockedDriverTap,
  }) : assert(currentIndex >= 0 && currentIndex <= 3,
            'currentIndex deve estar entre 0 e 3.');

  @override
  State<RideDockNavigation> createState() => _RideDockNavigationState();
}

class _RideDockNavigationState extends State<RideDockNavigation>
    with SingleTickerProviderStateMixin {
  late final AnimationController _animController;
  late final Animation<double> _curvedAnimation;

  bool _internalIsOpen = false;
  bool get _effectiveIsOpen => widget.isOpen ?? _internalIsOpen;

  @override
  void initState() {
    super.initState();
    // Duração otimizada para 400ms (avançar) e 340ms (retrair)
    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 400),
      reverseDuration: const Duration(milliseconds: 340),
    );

    _curvedAnimation = CurvedAnimation(
      parent: _animController,
      curve: Curves.easeInOutCubic,
      reverseCurve: Curves.easeInOutCubic,
    );

    if (_effectiveIsOpen) {
      _animController.value = 1.0;
    }
  }

  @override
  void didUpdateWidget(covariant RideDockNavigation oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.isOpen != null && widget.isOpen != oldWidget.isOpen) {
      if (widget.isOpen!) {
        _animController.forward();
      } else {
        _animController.reverse();
      }
    }
  }

  @override
  void dispose() {
    _animController.dispose();
    super.dispose();
  }

  void _handleToggle() {
    HapticFeedback.mediumImpact();
    if (widget.onToggleOpen != null) {
      widget.onToggleOpen!();
    } else {
      setState(() {
        _internalIsOpen = !_internalIsOpen;
        if (_internalIsOpen) {
          _animController.forward();
        } else {
          _animController.reverse();
        }
      });
    }
  }

  void _handleTabTap(int targetIndex) {
    if (_effectiveIsOpen) {
      _handleToggle();
    }
    if (widget.currentIndex == targetIndex) return;
    HapticFeedback.lightImpact();
    widget.onTabSelected(targetIndex);
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _curvedAnimation,
      builder: (context, child) {
        final progress = _curvedAnimation.value;

        return Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            ClipRect(
              child: Align(
                alignment: Alignment.bottomCenter,
                heightFactor: progress,
                child: Opacity(
                  opacity: progress.clamp(0.0, 1.0),
                  child: Transform.translate(
                    offset: Offset(0, 14 * (1.0 - progress)),
                    child: Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: _RidesPopoverCard(
                        onRequestRide: () {
                          _handleToggle();
                          widget.onRequestRide();
                        },
                        canDrive: widget.canDrive,
                        onLockedDriverTap: widget.onLockedDriverTap,
                        onStartDriverShift: () {
                          _handleToggle();
                          if (widget.canDrive) {
                            widget.onStartDriverShift();
                          } else {
                            widget.onLockedDriverTap?.call();
                          }
                        },
                        onClose: _handleToggle,
                      ),
                    ),
                  ),
                ),
              ),
            ),

            SizedBox(
              height: 66,
              child: Stack(
                alignment: Alignment.bottomCenter,
                clipBehavior: Clip.none,
                children: [
                  Container(
                    height: 60,
                    decoration: BoxDecoration(
                      color: AppColors.dockBackground,
                      borderRadius: BorderRadius.circular(30),
                      border: Border.all(
                        color: Colors.white.withValues(alpha: 0.12),
                        width: 1,
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withValues(alpha: 0.32),
                          blurRadius: 26,
                          offset: const Offset(0, 10),
                          spreadRadius: 2,
                        ),
                      ],
                    ),
                    child: Row(
                      children: [
                        // Asa Esquerda (flex 1): [0] Início, [1] Buscar
                        Expanded(
                          child: Row(
                            children: [
                              Expanded(
                                child: _SymmetricTabItem(
                                  icon: Icons.home_rounded,
                                  label: 'Início',
                                  isSelected: widget.currentIndex == 0,
                                  onTap: () => _handleTabTap(0),
                                ),
                              ),
                              Expanded(
                                child: _SymmetricTabItem(
                                  icon: Icons.search_rounded,
                                  label: 'Buscar e Rotas',
                                  isSelected: widget.currentIndex == 1,
                                  onTap: () => _handleTabTap(1),
                                ),
                              ),
                            ],
                          ),
                        ),

                        // Espaço central reservado para o botão proeminente
                        const SizedBox(width: 64),

                        // Asa Direita (flex 1): [2] Chat, [3] Perfil
                        Expanded(
                          child: Row(
                            children: [
                              Expanded(
                                child: _SymmetricTabItem(
                                  icon: Icons.chat_bubble_outline_rounded,
                                  label: 'Chat',
                                  isSelected: widget.currentIndex == 2,
                                  onTap: () => _handleTabTap(2),
                                ),
                              ),
                              Expanded(
                                child: _SymmetricTabItem(
                                  icon: Icons.person_rounded,
                                  label: 'Perfil',
                                  isSelected: widget.currentIndex == 3,
                                  onTap: () => _handleTabTap(3),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),

                  Positioned(
                    top: -10 + (16 * progress),
                    child: _CenterRideButton(
                      progress: progress,
                      onPressed: _handleToggle,
                    ),
                  ),
                ],
              ),
            ),
          ],
        );
      },
    );
  }
}

class _SymmetricTabItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool isSelected;
  final VoidCallback onTap;

  const _SymmetricTabItem({
    required this.icon,
    required this.label,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: label,
      selected: isSelected,
      button: true,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(28),
        splashColor: Colors.white.withValues(alpha: 0.10),
        highlightColor: Colors.transparent,
        child: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              AnimatedScale(
                scale: isSelected ? 1.16 : 1.0,
                duration: const Duration(milliseconds: 280),
                curve: Curves.easeOutBack,
                child: Icon(
                  icon,
                  size: 24,
                  color: isSelected
                      ? AppColors.dockItemActive
                      : AppColors.dockItemInactive,
                ),
              ),
              const SizedBox(height: 4),
              AnimatedContainer(
                duration: const Duration(milliseconds: 260),
                curve: Curves.easeOutCubic,
                width: isSelected ? 5 : 0,
                height: 5,
                decoration: BoxDecoration(
                  color: isSelected
                      ? AppColors.dockIndicatorDot
                      : Colors.transparent,
                  shape: BoxShape.circle,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CenterRideButton extends StatefulWidget {
  final double progress;
  final VoidCallback onPressed;

  const _CenterRideButton({
    required this.progress,
    required this.onPressed,
  });

  @override
  State<_CenterRideButton> createState() => _CenterRideButtonState();
}

class _CenterRideButtonState extends State<_CenterRideButton> {
  bool _isPressed = false;

  @override
  Widget build(BuildContext context) {
    final progress = widget.progress;
    final currentScale = (_isPressed ? 0.78 : (1.0 - (0.14 * progress)));
    final isExpanded = progress > 0.5;

    return Semantics(
      label: isExpanded ? 'Fechar opções de Caronas' : 'Abrir opções de Caronas',
      button: true,
      child: GestureDetector(
        onTapDown: (_) => setState(() => _isPressed = true),
        onTapUp: (_) {
          setState(() => _isPressed = false);
          widget.onPressed();
        },
        onTapCancel: () => setState(() => _isPressed = false),
        behavior: HitTestBehavior.opaque,
        child: Transform.scale(
          scale: currentScale,
          child: Container(
            width: 60,
            height: 60,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: Color.lerp(
                AppColors.carButtonResting,
                AppColors.carButtonDepressed,
                progress,
              ),
              border: Border.all(
                color: Color.lerp(
                  Colors.white.withValues(alpha: 0.24),
                  Colors.white,
                  progress,
                )!,
                width: 2.0 + (0.4 * progress),
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.42 - (0.17 * progress)),
                  blurRadius: 18 - (12 * progress),
                  offset: Offset(0, 8 - (6 * progress)),
                  spreadRadius: 1.0 - (1.0 * progress),
                ),
              ],
            ),
            child: Center(
              child: Transform.rotate(
                angle: progress * 3.141592653589793,
                child: AnimatedSwitcher(
                  duration: const Duration(milliseconds: 200),
                  transitionBuilder: (child, animation) => ScaleTransition(
                    scale: animation,
                    child: child,
                  ),
                  child: Icon(
                    isExpanded
                        ? Icons.close
                        : Icons.directions_car_rounded,
                    key: ValueKey(isExpanded),
                    size: 27,
                    color: Colors.white,
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}


class _RidesPopoverCard extends StatelessWidget {
  final VoidCallback onRequestRide;
  final VoidCallback onStartDriverShift;
  final VoidCallback onClose;
  final bool canDrive;
  final VoidCallback? onLockedDriverTap;

  const _RidesPopoverCard({
    required this.onRequestRide,
    required this.onStartDriverShift,
    required this.onClose,
    this.canDrive = true,
    this.onLockedDriverTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: AppColors.borderSubtle),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.16),
            blurRadius: 26,
            offset: const Offset(0, -6),
          ),
        ],
      ),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Row(
                children: [
                  Icon(
                    Icons.directions_car_rounded,
                    size: 18,
                    color: AppColors.textPrimary,
                  ),
                  SizedBox(width: 8),
                  Text(
                    'Caronas',
                    style: TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w800,
                      color: AppColors.textPrimary,
                      letterSpacing: -0.3,
                    ),
                  ),
                ],
              ),
              IconButton(
                visualDensity: VisualDensity.compact,
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(minWidth: 28, minHeight: 28),
                icon: const Icon(
                  Icons.close_rounded,
                  size: 18,
                  color: AppColors.textMuted,
                ),
                onPressed: onClose,
              ),
            ],
          ),
          const SizedBox(height: 10),

          _CompactActionRow(
            title: 'Pedir carona',
            subtitle: 'Encontre motoristas na sua rota com rateio.',
            icon: Icons.hail_rounded,
            onTap: onRequestRide,
          ),
          const SizedBox(height: 8),

          _CompactActionRow(
            title: 'Iniciar expediente de motorista',
            subtitle: canDrive
                ? 'Abra vagas no trajeto e compartilhe custos.'
                : 'Requer perfil de motorista cadastrado.',
            icon: canDrive ? Icons.drive_eta_rounded : Icons.lock_rounded,
            badgeLabel: canDrive ? 'Disponível' : 'Bloqueado',
            onTap: canDrive
                ? onStartDriverShift
                : (onLockedDriverTap ?? onStartDriverShift),
          ),
        ],
      ),
    );
  }
}

class _CompactActionRow extends StatefulWidget {
  final String title;
  final String subtitle;
  final IconData icon;
  final String? badgeLabel;
  final VoidCallback onTap;

  const _CompactActionRow({
    required this.title,
    required this.subtitle,
    required this.icon,
    this.badgeLabel,
    required this.onTap,
  });

  @override
  State<_CompactActionRow> createState() => _CompactActionRowState();
}

class _CompactActionRowState extends State<_CompactActionRow> {
  bool _isPressed = false;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: (_) => setState(() => _isPressed = true),
      onTapUp: (_) {
        setState(() => _isPressed = false);
        HapticFeedback.lightImpact();
        widget.onTap();
      },
      onTapCancel: () => setState(() => _isPressed = false),
      behavior: HitTestBehavior.opaque,
      child: AnimatedScale(
        scale: _isPressed ? 0.98 : 1.0,
        duration: const Duration(milliseconds: 140),
        curve: Curves.easeOutCubic,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          decoration: BoxDecoration(
            color: _isPressed
                ? AppColors.neutralLight
                : AppColors.cardSecondary,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(
              color: _isPressed
                  ? AppColors.borderCardPressed
                  : AppColors.borderSubtle,
              width: 1.1,
            ),
          ),
          child: Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: AppColors.cardBackground,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: AppColors.borderSubtle),
                ),
                child: Icon(
                  widget.icon,
                  size: 19,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Flexible(
                          child: Text(
                            widget.title,
                            style: const TextStyle(
                              fontSize: 13.5,
                              fontWeight: FontWeight.w700,
                              color: AppColors.textPrimary,
                              letterSpacing: -0.2,
                            ),
                          ),
                        ),
                        if (widget.badgeLabel != null) ...[
                          const SizedBox(width: 6),
                          Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 6, vertical: 2),
                            decoration: BoxDecoration(
                              color: AppColors.primary,
                              borderRadius: BorderRadius.circular(6),
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                const Icon(
                                  Icons.fiber_manual_record_rounded,
                                  size: 6,
                                  color: Colors.white,
                                ),
                                const SizedBox(width: 3),
                                Text(
                                  widget.badgeLabel!,
                                  style: const TextStyle(
                                    fontSize: 9.5,
                                    fontWeight: FontWeight.w700,
                                    color: Colors.white,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ],
                    ),
                    const SizedBox(height: 2),
                    Text(
                      widget.subtitle,
                      style: const TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w400,
                        color: AppColors.textSecondary,
                        height: 1.25,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 6),
              const Icon(
                Icons.arrow_forward_ios_rounded,
                size: 11,
                color: AppColors.textMuted,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
