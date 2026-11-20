// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/details_sheet.dart';
import '../../data/models/driver_offer.dart';
import '../../data/models/driver_ride_request.dart';
import '../../state/driver_ride_requests_provider.dart';
import '../../state/ride_request_actions_controller.dart';
import 'matching_message.dart';
import 'matching_section_title.dart';
import 'ride_request_card.dart';
import 'ride_request_details.dart';

/// Os pedidos de vaga de uma carona, dentro dos detalhes dela: do mais antigo ao
/// mais novo, 5 por página, com busca pelo nome (para dar carona a um amigo).
/// Com o carro cheio, a lista some e só volta se alguém sair.
class OfferRideRequests extends ConsumerStatefulWidget {
  final DriverOffer offer;
  final ValueChanged<DriverRideRequest> onAccept;
  final ValueChanged<DriverRideRequest> onReject;
  final ValueChanged<DriverRideRequest> onOpenChat;

  const OfferRideRequests({
    super.key,
    required this.offer,
    required this.onAccept,
    required this.onReject,
    required this.onOpenChat,
  });

  @override
  ConsumerState<OfferRideRequests> createState() => _OfferRideRequestsState();
}

class _OfferRideRequestsState extends ConsumerState<OfferRideRequests> {
  static const _perPage = 5;
  final _search = TextEditingController();
  int _page = 0;

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  /// Detalhes do pedido: de onde sai, para onde vai, a hora e o mapa do desvio.
  void _openDetails(DriverRideRequest request) {
    showDetailsSheet<void>(
      context,
      title: 'Pedido de vaga',
      builder: (sheetContext) => RideRequestDetails(
        request: request,
        onAccept: () {
          Navigator.of(sheetContext).pop();
          widget.onAccept(request);
        },
        onReject: () {
          Navigator.of(sheetContext).pop();
          widget.onReject(request);
        },
        onOpenChat: () {
          Navigator.of(sheetContext).pop();
          widget.onOpenChat(request);
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final busy = ref.watch(rideRequestActionsProvider);
    final rideIds = {for (final ride in widget.offer.rides) ride.id};
    final requests = [
      for (final request
          in ref.watch(driverRideRequestsProvider).value ??
              const <DriverRideRequest>[])
        if (rideIds.contains(request.rideId)) request,
    ];

    final all = pendingRideRequests(requests);
    if (all.isEmpty) return const SizedBox.shrink();

    final found = pendingRideRequests(requests, query: _search.text);
    final pages = (found.length / _perPage).ceil();
    final page = pages == 0 ? 0 : _page.clamp(0, pages - 1);
    final shown = found.skip(page * _perPage).take(_perPage).toList();

    return Padding(
      padding: const EdgeInsets.only(top: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          MatchingSectionTitle(title: 'Pedidos de vaga (${all.length})'),
          _RequestSearchField(
            controller: _search,
            onChanged: () => setState(() => _page = 0),
          ),
          const SizedBox(height: 10),
          if (shown.isEmpty)
            MatchingMessage(text: 'Nenhum pedido de "${_search.text.trim()}".'),
          for (final request in shown)
            Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: RideRequestCard(
                request: request,
                isBusy: busy.contains(request.key),
                onAccept: () => widget.onAccept(request),
                onReject: () => widget.onReject(request),
                onOpenChat: () => widget.onOpenChat(request),
                onOpenDetails: () => _openDetails(request),
              ),
            ),
          if (pages > 1)
            _RequestPager(
              page: page,
              pages: pages,
              onChanged: (value) {
                HapticFeedback.lightImpact();
                setState(() => _page = value);
              },
            ),
        ],
      ),
    );
  }
}

/// Busca pelo nome de quem pediu (ex.: dar carona para um amigo).
class _RequestSearchField extends StatelessWidget {
  final TextEditingController controller;
  final VoidCallback onChanged;

  const _RequestSearchField({
    required this.controller,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return TextField(
      key: const ValueKey('ride_request_search'),
      controller: controller,
      onChanged: (_) => onChanged(),
      textInputAction: TextInputAction.search,
      style: const TextStyle(fontSize: 14, color: AppColors.textPrimary),
      decoration: InputDecoration(
        hintText: 'Buscar passageiro pelo nome',
        hintStyle: const TextStyle(color: AppColors.textPlaceholder),
        prefixIcon: const Icon(
          Icons.search_rounded,
          size: 20,
          color: AppColors.iconMuted,
        ),
        suffixIcon: controller.text.isEmpty
            ? null
            : IconButton(
                tooltip: 'Limpar busca',
                onPressed: () {
                  controller.clear();
                  onChanged();
                },
                icon: const Icon(
                  Icons.close_rounded,
                  size: 18,
                  color: AppColors.iconMuted,
                ),
              ),
        filled: true,
        fillColor: AppColors.inputBackground,
        isDense: true,
        contentPadding: const EdgeInsets.symmetric(vertical: 12),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: AppColors.borderSubtle),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: AppColors.borderSubtle),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: AppColors.primary),
        ),
      ),
    );
  }
}

/// "‹ Página 1 de 3 ›" abaixo dos pedidos.
class _RequestPager extends StatelessWidget {
  final int page;
  final int pages;
  final ValueChanged<int> onChanged;

  const _RequestPager({
    required this.page,
    required this.pages,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        IconButton(
          key: const ValueKey('ride_requests_previous_page'),
          tooltip: 'Página anterior',
          onPressed: page > 0 ? () => onChanged(page - 1) : null,
          color: AppColors.textPrimary,
          disabledColor: AppColors.iconMuted,
          icon: const Icon(Icons.chevron_left_rounded),
        ),
        Text(
          'Página ${page + 1} de $pages',
          style: const TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w700,
            color: AppColors.textDark,
          ),
        ),
        IconButton(
          key: const ValueKey('ride_requests_next_page'),
          tooltip: 'Próxima página',
          onPressed: page < pages - 1 ? () => onChanged(page + 1) : null,
          color: AppColors.textPrimary,
          disabledColor: AppColors.iconMuted,
          icon: const Icon(Icons.chevron_right_rounded),
        ),
      ],
    );
  }
}
