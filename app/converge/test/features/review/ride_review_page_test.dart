// Autor: Sara Fernandes Monteiro - RA 25024107

import 'package:converge/core/network/api_error_code.dart';
import 'package:converge/core/network/api_exception.dart';
import 'package:converge/features/review/data/models/create_review_request.dart';
import 'package:converge/features/review/data/models/review_model.dart';
import 'package:converge/features/review/data/repository/review_dismissal_store.dart';
import 'package:converge/features/review/data/repository/review_repository_impl.dart';
import 'package:converge/features/review/presentation/pages/ride_review_page.dart';
import 'package:converge/features/review/state/pending_reviews.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../ride/fakes/fake_ride.dart';
import 'fakes/fake_review_repository.dart';

const _ana = ReviewTarget(
  userId: 'passenger-1',
  name: 'Ana Souza',
  isDriver: false,
);
const _bia = ReviewTarget(
  userId: 'passenger-2',
  name: 'Bia Lima',
  isDriver: false,
);
const _lucas = ReviewTarget(
  userId: fakeDriverId,
  name: 'Lucas Silveira',
  isDriver: true,
);

void main() {
  late FakeReviewRepository reviews;

  setUp(() => reviews = FakeReviewRepository(reviewerId: 'quem-avalia'));

  /// Abre a tela como o app faz (empurrada sobre outra), para o botão poder fechá-la.
  Future<void> openPage(WidgetTester tester, List<ReviewTarget> targets) async {
    tester.view.physicalSize = const Size(800, 1800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [reviewRepositoryProvider.overrideWithValue(reviews)],
        child: MaterialApp(
          home: Builder(
            builder: (context) => Scaffold(
              body: Center(
                child: ElevatedButton(
                  onPressed: () => Navigator.of(context).push<bool>(
                    MaterialPageRoute(
                      fullscreenDialog: true,
                      builder: (_) =>
                          RideReviewPage(rideId: fakeRideId, targets: targets),
                    ),
                  ),
                  child: const Text('abrir'),
                ),
              ),
            ),
          ),
        ),
      ),
    );
    await tester.tap(find.text('abrir'));
    await tester.pumpAndSettle();
  }

  testWidgets(
    'motorista vê todos os passageiros, e o botão só liga com estrelas',
    (tester) async {
      await openPage(tester, const [_ana, _bia]);

      expect(find.text('Ana Souza'), findsOneWidget);
      expect(find.text('Bia Lima'), findsOneWidget);
      expect(
        find.text('Como foi a carona com cada passageiro?'),
        findsOneWidget,
      );
      expect(
        tester.widget<FilledButton>(find.byType(FilledButton)).onPressed,
        isNull,
      );

      await tester.tap(find.byTooltip('4 de 5').first);
      await tester.pump();

      expect(
        tester.widget<FilledButton>(find.byType(FilledButton)).onPressed,
        isNotNull,
      );
    },
  );

  testWidgets(
    'envia só quem recebeu estrelas, com o comentário, e fecha a tela',
    (tester) async {
      await openPage(tester, const [_ana, _bia]);

      await tester.tap(find.byTooltip('5 de 5').first);
      // Linha compacta: o comentário abre no ícone
      await tester.tap(find.byTooltip('Adicionar comentário').first);
      await tester.pump();
      await tester.enterText(
        find.byType(TextField).first,
        'Pontual e simpática',
      );
      await tester.pump();
      await tester.tap(find.text('Enviar avaliações'));
      await tester.pumpAndSettle();

      expect(reviews.reviews, hasLength(1));
      expect(reviews.reviews.single.reviewedId, 'passenger-1');
      expect(reviews.reviews.single.rating, 5);
      expect(reviews.reviews.single.comment, 'Pontual e simpática');

      // Enviar encerra a avaliação, mesmo com a Bia sem estrelas
      expect(find.byType(RideReviewPage), findsNothing);
    },
  );

  testWidgets('comentário sem estrelas não é enviado e avisa', (tester) async {
    await openPage(tester, const [_ana, _bia]);

    await tester.tap(find.byTooltip('5 de 5').first);
    await tester.tap(find.byTooltip('Adicionar comentário').last);
    await tester.pump();
    await tester.enterText(find.byType(TextField).last, 'Chegou atrasada');
    await tester.pump();
    await tester.tap(find.text('Enviar avaliações'));
    await tester.pumpAndSettle();

    expect(reviews.reviews, isEmpty);
    expect(
      find.text('Escolha de 1 a 5 estrelas para enviar o comentário.'),
      findsOneWidget,
    );
    expect(find.byType(RideReviewPage), findsOneWidget);
  });

  testWidgets(
    'apagar o comentário sem estrelas e enviar de novo envia e fecha a tela',
    (tester) async {
      await openPage(tester, const [_ana, _bia]);

      await tester.tap(find.byTooltip('5 de 5').first);
      await tester.tap(find.byTooltip('Adicionar comentário').last);
      await tester.pump();
      await tester.enterText(find.byType(TextField).last, 'Chegou atrasada');
      await tester.pump();
      await tester.tap(find.text('Enviar avaliações'));
      await tester.pumpAndSettle();
      expect(
        find.text('Escolha de 1 a 5 estrelas para enviar o comentário.'),
        findsOneWidget,
      );

      await tester.enterText(find.byType(TextField).last, '');
      await tester.pump();
      await tester.tap(find.text('Enviar avaliações'));
      await tester.pumpAndSettle();

      expect(reviews.reviews.map((review) => review.reviewedId), [
        'passenger-1',
      ]);
      expect(find.byType(RideReviewPage), findsNothing);
    },
  );

  testWidgets(
    '"Não quero avaliar" fecha sem enviar e não pergunta de novo desta viagem',
    (tester) async {
      SharedPreferences.setMockInitialValues({});
      await openPage(tester, const [_ana, _bia]);

      await tester.tap(find.byTooltip('3 de 5').first);
      await tester.pump();
      await tester.tap(find.text('Não quero avaliar'));
      await tester.pumpAndSettle();

      expect(reviews.reviews, isEmpty);
      expect(find.byType(RideReviewPage), findsNothing);
      expect(
        await const ReviewDismissalStore().isDismissed(fakeRideId),
        isTrue,
      );
    },
  );

  testWidgets('passageiro avalia só o motorista', (tester) async {
    await openPage(tester, const [_lucas]);

    expect(find.text('Lucas Silveira'), findsOneWidget);
    expect(find.text('Motorista'), findsOneWidget);
    expect(find.text('Como foi a carona com o motorista?'), findsOneWidget);

    await tester.tap(find.byTooltip('5 de 5'));
    await tester.pump();
    await tester.tap(find.text('Enviar avaliação'));
    await tester.pumpAndSettle();

    expect(reviews.reviews.single.reviewedId, fakeDriverId);
    expect(find.byType(RideReviewPage), findsNothing);
  });

  testWidgets(
    'comentário sem estrelas mostra o aviso na hora e some ao escolher estrelas',
    (tester) async {
      await openPage(tester, const [_ana]);

      await tester.enterText(find.byType(TextField), 'Muito simpática');
      await tester.pump();

      expect(
        find.text('Escolha de 1 a 5 estrelas para enviar o comentário'),
        findsOneWidget,
      );
      expect(
        tester.widget<FilledButton>(find.byType(FilledButton)).onPressed,
        isNull,
      );

      await tester.tap(find.byTooltip('5 de 5'));
      await tester.pump();

      expect(
        find.text('Escolha de 1 a 5 estrelas para enviar o comentário'),
        findsNothing,
      );
      expect(find.text('Excelente'), findsOneWidget);
      expect(
        tester.widget<FilledButton>(find.byType(FilledButton)).onPressed,
        isNotNull,
      );
    },
  );

  testWidgets(
    'se um envio falha, a tela fica aberta com o erro e os outros foram enviados',
    (tester) async {
      reviews = _FlakyReviews(failFor: 'passenger-2');
      await openPage(tester, const [_ana, _bia]);

      await tester.tap(find.byTooltip('5 de 5').first); // Ana
      await tester.tap(find.byTooltip('4 de 5').last); // Bia
      await tester.pump();
      await tester.tap(find.text('Enviar avaliações'));
      await tester.pumpAndSettle();

      expect(reviews.reviews.map((review) => review.reviewedId), [
        'passenger-1',
      ]);
      expect(find.byType(RideReviewPage), findsOneWidget);
      expect(find.text('Você avaliou com 5 de 5 estrelas'), findsOneWidget);
      expect(find.text('Sem conexão.'), findsOneWidget);
      expect(find.text('Fechar'), findsOneWidget);
    },
  );
}

/// Avaliações em que o envio para uma pessoa falha, como numa queda de rede.
class _FlakyReviews extends FakeReviewRepository {
  final String failFor;

  _FlakyReviews({required this.failFor}) : super(reviewerId: 'quem-avalia');

  @override
  Future<ReviewModel> createReview(CreateReviewRequest request) async {
    if (request.reviewedId == failFor) {
      throw const ApiException(
        code: ApiErrorCode.network,
        message: 'Sem conexão.',
        status: 0,
      );
    }
    return super.createReview(request);
  }
}
