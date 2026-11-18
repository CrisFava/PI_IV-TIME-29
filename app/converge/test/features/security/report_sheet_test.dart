// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

import 'package:converge/core/network/api_error_code.dart';
import 'package:converge/core/network/api_exception.dart';
import 'package:converge/features/security/data/repository/security_repository_impl.dart';
import 'package:converge/features/security/presentation/widgets/block_confirm_dialog.dart';
import 'package:converge/features/security/presentation/widgets/report_sheet.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes/fake_security_repository.dart';

void main() {
  late FakeSecurityRepository repository;

  setUp(() => repository = FakeSecurityRepository());

  Future<void> pump(WidgetTester tester, Widget child) {
    return tester.pumpWidget(
      ProviderScope(
        overrides: [securityRepositoryProvider.overrideWithValue(repository)],
        child: MaterialApp(home: Scaffold(body: child)),
      ),
    );
  }

  Future<void> pumpSheet(WidgetTester tester, {String? rideId}) => pump(
    tester,
    SingleChildScrollView(
      child: ReportSheet(
        reportedId: 'user-2',
        reportedName: 'Ana',
        rideId: rideId,
      ),
    ),
  );

  FilledButton submitButton(WidgetTester tester) =>
      tester.widget<FilledButton>(find.byType(FilledButton));

  group('ReportSheet', () {
    testWidgets('botão fica desabilitado até escolher um motivo', (tester) async {
      await pumpSheet(tester);

      expect(find.text('Denunciar Ana'), findsOneWidget);
      expect(submitButton(tester).onPressed, isNull);

      await tester.tap(find.text('Conduta inadequada'));
      await tester.pump();

      expect(submitButton(tester).onPressed, isNotNull);
    });

    testWidgets('"Outro motivo" só habilita com descrição', (tester) async {
      await pumpSheet(tester);

      await tester.tap(find.text('Outro motivo'));
      await tester.pump();
      expect(submitButton(tester).onPressed, isNull);

      await tester.enterText(find.byType(TextField), 'Detalhes do problema');
      await tester.pump();
      expect(submitButton(tester).onPressed, isNotNull);
    });

    testWidgets('atraso ou ausência só aparece quando há viagem', (tester) async {
      await pumpSheet(tester);
      expect(find.text('Atraso ou ausência'), findsNothing);

      await pumpSheet(tester, rideId: 'ride-1');
      expect(find.text('Atraso ou ausência'), findsOneWidget);
    });

    testWidgets('enviar grava a denúncia com a viagem', (tester) async {
      await pumpSheet(tester, rideId: 'ride-1');

      await tester.tap(find.text('Atraso ou ausência'));
      await tester.pump();
      await tester.tap(find.byType(FilledButton));
      await tester.pumpAndSettle();

      expect(repository.reports.single.reportedId, 'user-2');
      expect(repository.reports.single.rideId, 'ride-1');
    });

    testWidgets('erro do backend aparece na tela e o botão volta a ficar ativo', (
      tester,
    ) async {
      repository.failNext = const ApiException(
        code: ApiErrorCode.badRequest,
        message: 'Você já fez essa denúncia e ela ainda está em análise.',
        status: 400,
      );
      await pumpSheet(tester);

      await tester.tap(find.text('Cobrança indevida'));
      await tester.pump();
      await tester.tap(find.byType(FilledButton));
      await tester.pumpAndSettle();

      expect(
        find.text('Você já fez essa denúncia e ela ainda está em análise.'),
        findsOneWidget,
      );
      expect(submitButton(tester).onPressed, isNotNull);
      expect(repository.reports, isEmpty);
    });
  });

  group('BlockConfirmDialog', () {
    testWidgets('confirmar bloqueia o usuário', (tester) async {
      await pump(
        tester,
        const BlockConfirmDialog(userId: 'user-2', userName: 'Ana'),
      );

      expect(find.text('Bloquear Ana?'), findsOneWidget);
      await tester.tap(find.widgetWithText(FilledButton, 'Bloquear'));
      await tester.pumpAndSettle();

      expect(repository.blocks.single.blockedUserId, 'user-2');
    });

    testWidgets('erro mostra a mensagem e não bloqueia', (tester) async {
      repository.failNext = ApiException.network();
      await pump(
        tester,
        const BlockConfirmDialog(userId: 'user-2', userName: 'Ana'),
      );

      await tester.tap(find.widgetWithText(FilledButton, 'Bloquear'));
      await tester.pumpAndSettle();

      expect(
        find.text('Não foi possível conectar ao servidor. Verifique sua internet.'),
        findsOneWidget,
      );
      expect(repository.blocks, isEmpty);
    });
  });
}
