import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/ride_dock_navigation.dart';
import '../../state/auth_action_controller.dart';
import '../../state/auth_controller.dart';
import '../../state/session/auth_session.dart';

class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
  int _currentTabIndex = 0;
  bool _isCarMenuOpen = false;
  late final PageController _pageController;

  @override
  void initState() {
    super.initState();
    _pageController = PageController(initialPage: _currentTabIndex);
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  void _onTabSelected(int index) {
    if (_isCarMenuOpen) {
      setState(() => _isCarMenuOpen = false);
    }
    if (_currentTabIndex == index) return;
    setState(() => _currentTabIndex = index);
    _pageController.animateToPage(
      index,
      duration: const Duration(milliseconds: 320),
      curve: Curves.easeOutCubic,
    );
  }

  void _showFeedback(String message) {
    ScaffoldMessenger.of(context).hideCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Row(
          children: [
            const Icon(
              Icons.check_circle_rounded,
              color: Colors.white,
              size: 18,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                message,
                style: const TextStyle(
                  fontWeight: FontWeight.w600,
                  fontSize: 13.5,
                  color: Colors.white,
                ),
              ),
            ),
          ],
        ),
        backgroundColor: AppColors.primary,
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        margin: const EdgeInsets.only(bottom: 90, left: 16, right: 16),
        duration: const Duration(seconds: 3),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(authControllerProvider).value;
    final user = session is Authenticated ? session.user : null;
    final bottomPadding = MediaQuery.paddingOf(context).bottom;

    return Scaffold(
      backgroundColor: AppColors.scaffoldBackground,
      body: user == null
          ? const Center(
              child: CircularProgressIndicator(
                color: AppColors.primary,
                strokeWidth: 2.8,
              ),
            )
          : Stack(
              children: [
                // 1. Estrutura Principal: AppBar + PageView (Dentro do Stack)
                Positioned.fill(
                  child: Column(
                    children: [
                      _buildCustomAppBar(),
                      Expanded(
                        child: PageView(
                          controller: _pageController,
                          onPageChanged: (index) {
                            setState(() {
                              _currentTabIndex = index;
                              if (_isCarMenuOpen) _isCarMenuOpen = false;
                            });
                          },
                          children: [
                            _buildHomeTab(user),
                            _buildSearchAndHistoryTab(),
                            _buildChatTab(),
                            _buildProfileTab(user),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),

                // 2. Barreira translúcida que SOBRESCREVE A TELA INTEIRA (INCLUINDO A APPBAR!)
                Positioned.fill(
                  child: IgnorePointer(
                    ignoring: !_isCarMenuOpen,
                    child: AnimatedOpacity(
                      opacity: _isCarMenuOpen ? 1.0 : 0.0,
                      duration: const Duration(milliseconds: 400),
                      curve: Curves.easeInOutCubic,
                      child: GestureDetector(
                        onTap: () => setState(() => _isCarMenuOpen = false),
                        behavior: HitTestBehavior.opaque,
                        child: Container(
                          color: AppColors.barrier,
                        ),
                      ),
                    ),
                  ),
                ),

                // 3. Dock Flutuante + Popover suspenso diretamente no rodapé
                Positioned(
                  left: 16,
                  right: 16,
                  bottom: bottomPadding > 0 ? bottomPadding : 16,
                  child: RideDockNavigation(
                    currentIndex: _currentTabIndex,
                    onTabSelected: _onTabSelected,
                    isOpen: _isCarMenuOpen,
                    onToggleOpen: () {
                      setState(() => _isCarMenuOpen = !_isCarMenuOpen);
                    },
                    onRequestRide: () {
                      setState(() => _isCarMenuOpen = false);
                      _showFeedback('Ação "Pedir carona" acionada!');
                    },
                    onStartDriverShift: () {
                      setState(() => _isCarMenuOpen = false);
                      _showFeedback(
                          'Ação "Iniciar expediente de motorista" acionada!');
                    },
                  ),
                ),
              ],
            ),
    );
  }

  Widget _buildCustomAppBar() {
    return Container(
      color: AppColors.cardBackground,
      child: SafeArea(
        bottom: false,
        child: Container(
          height: kToolbarHeight,
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Image.asset(
                    'assets/icon.png',
                    width: 28,
                    height: 28,
                    fit: BoxFit.contain,
                  ),
                  const SizedBox(width: 10),
                  const Text(
                    'Converge',
                    style: TextStyle(
                      fontWeight: FontWeight.w800,
                      fontSize: 20,
                      color: AppColors.textPrimary,
                      letterSpacing: -0.4,
                    ),
                  ),
                ],
              ),
              IconButton(
                tooltip: 'Sair da conta',
                onPressed: () => ref.read(authActionProvider.notifier).signOut(),
                icon: const Icon(
                  Icons.logout_rounded,
                  size: 20,
                  color: AppColors.textPrimary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // --- ABA 0: INÍCIO ---
  Widget _buildHomeTab(dynamic user) {
    return ListView(
      key: const ValueKey('home_tab'),
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 130),
      children: [
        Container(
          padding: const EdgeInsets.all(22),
          decoration: BoxDecoration(
            color: AppColors.primary,
            borderRadius: BorderRadius.circular(24),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.14),
                blurRadius: 18,
                offset: const Offset(0, 8),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    'Universidade Ativa',
                    style: TextStyle(
                      color: AppColors.textMuted,
                      fontSize: 13,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.16),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      user.academicRecord.university.toString().isNotEmpty
                          ? user.academicRecord.university.toString()
                          : 'Universidade',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 12,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),
              Text(
                'Olá, ${user.name}!',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 22,
                  fontWeight: FontWeight.w800,
                  letterSpacing: -0.4,
                ),
              ),
              const SizedBox(height: 6),
              const Text(
                'Arraste para o lado para trocar de aba ou toque no carrinho para ver as opções.',
                style: TextStyle(
                  color: AppColors.textMuted,
                  fontSize: 13.5,
                  height: 1.4,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),

        Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: AppColors.borderSubtle),
          ),
          child: Row(
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: const BoxDecoration(
                  color: AppColors.cardSecondary,
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.swipe_rounded,
                  color: AppColors.textPrimary,
                  size: 22,
                ),
              ),
              const SizedBox(width: 14),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Troca de Aba por Arraste',
                      style: TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 14,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    SizedBox(height: 3),
                    Text(
                      'Deslize horizontalmente na tela para navegar suavemente entre as abas.',
                      style: TextStyle(
                        fontSize: 12.5,
                        color: AppColors.textSecondary,
                        height: 1.35,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  // --- ABA 1: BUSCAR E ROTAS (COM HISTÓRICO INTEGRADO) ---
  Widget _buildSearchAndHistoryTab() {
    return ListView(
      key: const ValueKey('search_and_history_tab'),
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 130),
      children: [
        // 1. Barra de Busca
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: AppColors.borderSubtle),
          ),
          child: const Row(
            children: [
              Icon(Icons.search_rounded, color: AppColors.textPrimary, size: 22),
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
            ],
          ),
        ),
        const SizedBox(height: 22),

        // 2. Seção: Rotas Frequentes
        const Text(
          'Rotas Recomendadas no Campus',
          style: TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w800,
            color: AppColors.textPrimary,
            letterSpacing: -0.2,
          ),
        ),
        const SizedBox(height: 12),
        _buildRouteCard(
          origem: 'Terminal Central / Barão Geraldo',
          destino: 'Campus I - Bloco H',
          horario: 'Saída às 07:45',
          vagas: '3 vagas disponíveis',
        ),
        const SizedBox(height: 10),
        _buildRouteCard(
          origem: 'Cambuí / Centro',
          destino: 'Campus II - Medicina',
          horario: 'Saída às 08:15',
          vagas: '2 vagas disponíveis',
        ),

        const SizedBox(height: 26),

        // 3. Seção: Histórico de Viagens Integrado
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              'Histórico de Viagens Anteriores',
              style: TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w800,
                color: AppColors.textPrimary,
                letterSpacing: -0.2,
              ),
            ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
              decoration: BoxDecoration(
                color: AppColors.neutralLight,
                borderRadius: BorderRadius.circular(8),
              ),
              child: const Text(
                'Recentes',
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.w600,
                  color: AppColors.textSecondary,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        _buildHistoryItem(
          data: 'Ontem às 18:20',
          rota: 'Campus I → Barão Geraldo',
          status: 'Concluída',
          motorista: 'Lucas Silveira',
          valor: 'R\$ 6,50',
        ),
        const SizedBox(height: 10),
        _buildHistoryItem(
          data: '18 de Outubro às 07:30',
          rota: 'Centro → Campus I',
          status: 'Concluída',
          motorista: 'Mariana Costa',
          valor: 'R\$ 5,00',
        ),
      ],
    );
  }

  Widget _buildRouteCard({
    required String origem,
    required String destino,
    required String horario,
    required String vagas,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.circle, size: 8, color: AppColors.textPrimary),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  origem,
                  style: const TextStyle(
                    fontWeight: FontWeight.w600,
                    fontSize: 13.5,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
            ],
          ),
          Padding(
            padding: const EdgeInsets.only(left: 3),
            child: Container(
              width: 2,
              height: 12,
              color: AppColors.routeLine,
            ),
          ),
          Row(
            children: [
              const Icon(Icons.location_on_rounded,
                  size: 10, color: AppColors.textPrimary),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  destino,
                  style: const TextStyle(
                    fontWeight: FontWeight.w700,
                    fontSize: 14,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                horario,
                style: const TextStyle(fontSize: 12, color: AppColors.textSecondary),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: AppColors.neutralLight,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  vagas,
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
    );
  }

  Widget _buildHistoryItem({
    required String data,
    required String rota,
    required String status,
    required String motorista,
    required String valor,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: AppColors.cardSecondary,
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: AppColors.borderSubtle),
            ),
            child: const Icon(
              Icons.check_circle_outline_rounded,
              color: AppColors.textPrimary,
              size: 20,
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  rota,
                  style: const TextStyle(
                    fontWeight: FontWeight.w700,
                    fontSize: 14,
                    color: AppColors.textPrimary,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  '$data • Motorista: $motorista',
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppColors.textSecondary,
                  ),
                ),
              ],
            ),
          ),
          Text(
            valor,
            style: const TextStyle(
              fontWeight: FontWeight.w700,
              fontSize: 14,
              color: AppColors.textPrimary,
            ),
          ),
        ],
      ),
    );
  }

  // --- ABA 2: CHAT ---
  Widget _buildChatTab() {
    return ListView(
      key: const ValueKey('chat_tab'),
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 130),
      children: [
        const Text(
          'Mensagens das Caronas',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w700,
            color: AppColors.textPrimary,
          ),
        ),
        const SizedBox(height: 12),
        _buildChatThread(
          nome: 'Carona Campus I (07:45)',
          ultimaMensagem: 'Lucas: Chegando no portão 2 em 3 minutos!',
          horario: '07:42',
          unread: true,
        ),
        const SizedBox(height: 10),
        _buildChatThread(
          nome: 'Mariana Costa (Motorista)',
          ultimaMensagem: 'Você: Obrigado pela carona de hoje!',
          horario: 'Ontem',
          unread: false,
        ),
      ],
    );
  }

  Widget _buildChatThread({
    required String nome,
    required String ultimaMensagem,
    required String horario,
    required bool unread,
  }) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Row(
        children: [
          const CircleAvatar(
            radius: 20,
            backgroundColor: AppColors.neutralLight,
            child: Icon(
              Icons.forum_rounded,
              color: AppColors.textPrimary,
              size: 20,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      nome,
                      style: TextStyle(
                        fontWeight: unread ? FontWeight.w800 : FontWeight.w600,
                        fontSize: 14,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    Text(
                      horario,
                      style: const TextStyle(
                        fontSize: 11,
                        color: AppColors.textMuted,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  ultimaMensagem,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: 12.5,
                    color: unread
                        ? AppColors.textPrimary
                        : AppColors.textSecondary,
                    fontWeight: unread ? FontWeight.w600 : FontWeight.w400,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // --- ABA 3: PERFIL ---
  Widget _buildProfileTab(dynamic user) {
    return ListView(
      key: const ValueKey('profile_tab'),
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 130),
      children: [
        Container(
          padding: const EdgeInsets.all(22),
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: AppColors.borderSubtle),
          ),
          child: Column(
            children: [
              const CircleAvatar(
                radius: 38,
                backgroundColor: AppColors.neutralLight,
                child: Icon(
                  Icons.person_rounded,
                  size: 42,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 12),
              Text(
                user.name.toString(),
                style: const TextStyle(
                  fontSize: 19,
                  fontWeight: FontWeight.w700,
                  color: AppColors.textPrimary,
                  letterSpacing: -0.3,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                user.email.toString(),
                style: const TextStyle(
                  fontSize: 13,
                  color: AppColors.textSecondary,
                ),
              ),
              const SizedBox(height: 18),
              const Divider(color: AppColors.neutralLight),
              const SizedBox(height: 10),
              _buildProfileRow('Telefone', user.phone.toString()),
              _buildProfileRow(
                  'Universidade', user.academicRecord.university.toString()),
              _buildProfileRow('Curso', user.academicRecord.course.toString()),
              _buildProfileRow('Matrícula',
                  user.academicRecord.enrollmentNumber.toString()),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildProfileRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 7),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: const TextStyle(
              fontSize: 13,
              color: AppColors.textSecondary,
              fontWeight: FontWeight.w500,
            ),
          ),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.end,
              style: const TextStyle(
                fontSize: 13.5,
                color: AppColors.textPrimary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
