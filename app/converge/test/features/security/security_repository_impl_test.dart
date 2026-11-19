// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

import 'package:converge/core/network/api_error_code.dart';
import 'package:converge/core/network/api_exception.dart';
import 'package:converge/core/network/interceptors/error_interceptor.dart';
import 'package:converge/features/security/data/models/block_model.dart';
import 'package:converge/features/security/data/models/create_report_request.dart';
import 'package:converge/features/security/data/models/report_reason.dart';
import 'package:converge/features/security/data/models/report_status.dart';
import 'package:converge/features/security/data/repository/security_repository_impl.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  late Dio dio;
  late SecurityRepositoryImpl repository;
  late List<RequestOptions> requests;

  /// Responde toda requisição com [body] e guarda o que foi pedido.
  void respondWith(Object? body, {int status = 200}) {
    dio.interceptors.insert(
      0,
      InterceptorsWrapper(
        onRequest: (options, handler) {
          requests.add(options);
          final response = Response<dynamic>(
            requestOptions: options,
            statusCode: status,
            data: body,
          );
          if (status >= 400) {
            handler.reject(
              DioException(requestOptions: options, response: response),
              true,
            );
          } else {
            handler.resolve(response);
          }
        },
      ),
    );
  }

  Map<String, dynamic> success(Object? data) => {
    'success': true,
    'data': data,
    'error': null,
    'timestamp': '2026-11-16T12:00:00Z',
  };

  Map<String, dynamic> blockJson() => {
    'id': 'block-1',
    'blockedUserId': 'user-2',
    'blockedUserName': 'Ana',
    'createdAt': '2026-11-16T12:00:00Z',
  };

  Map<String, dynamic> reportJson() => {
    'id': 'report-1',
    'reportedId': 'user-2',
    'rideId': 'ride-1',
    'reason': 'LATE_NO_SHOW',
    'description': 'Não apareceu',
    'status': 'PENDING',
    'createdAt': '2026-11-16T12:00:00Z',
  };

  setUp(() {
    dio = Dio()..interceptors.add(ErrorInterceptor());
    repository = SecurityRepositoryImpl(dio);
    requests = [];
  });

  test('blockUser envia o id no corpo e lê o bloqueio criado', () async {
    respondWith(success(blockJson()), status: 201);

    final block = await repository.blockUser('user-2');

    expect(requests.single.method, 'POST');
    expect(requests.single.path, '/blocks');
    expect(requests.single.data, {'blockedUserId': 'user-2'});
    expect(block.blockedUserId, 'user-2');
    expect(block.displayName, 'Ana');
    expect(block.createdAt, DateTime.utc(2026, 11, 16, 12));
  });

  test('unblockUser chama DELETE com o id e aceita resposta sem corpo', () async {
    respondWith(null, status: 204);

    await repository.unblockUser('user-2');

    expect(requests.single.method, 'DELETE');
    expect(requests.single.path, '/blocks/user-2');
  });

  test('listBlocks lê a lista do envelope', () async {
    respondWith(success([blockJson(), blockJson()..['blockedUserId'] = 'u3']));

    final blocks = await repository.listBlocks();

    expect(requests.single.path, '/blocks');
    expect(blocks.map((block) => block.blockedUserId), ['user-2', 'u3']);
  });

  test('createReport envia só os campos preenchidos e lê a denúncia', () async {
    respondWith(success(reportJson()), status: 201);

    final report = await repository.createReport(
      const CreateReportRequest(
        reportedId: 'user-2',
        reason: ReportReason.lateNoShow,
        description: ' Não apareceu ',
        rideId: 'ride-1',
      ),
    );

    expect(requests.single.path, '/reports');
    expect(requests.single.data, {
      'reportedId': 'user-2',
      'reason': 'LATE_NO_SHOW',
      'description': 'Não apareceu',
      'rideId': 'ride-1',
    });
    expect(report.reason, ReportReason.lateNoShow);
    expect(report.status, ReportStatus.pending);
  });

  test('listMyReports lê a lista de /reports/mine', () async {
    respondWith(success([reportJson()]));

    final reports = await repository.listMyReports();

    expect(requests.single.path, '/reports/mine');
    expect(reports.single.rideId, 'ride-1');
  });

  test('erro do backend vira ApiException com a mensagem do envelope', () async {
    respondWith({
      'success': false,
      'data': null,
      'error': {
        'code': 'BAD_REQUEST',
        'message': 'Você já fez essa denúncia e ela ainda está em análise.',
        'fields': ['reportedId'],
      },
      'timestamp': '2026-11-16T12:00:00Z',
    }, status: 400);

    await expectLater(
      repository.createReport(
        const CreateReportRequest(
          reportedId: 'user-2',
          reason: ReportReason.inappropriateConduct,
        ),
      ),
      throwsA(
        isA<ApiException>()
            .having((e) => e.code, 'code', ApiErrorCode.badRequest)
            .having((e) => e.status, 'status', 400)
            .having((e) => e.fields, 'fields', {'reportedId'}),
      ),
    );
  });

  test('sem resposta do servidor, lança erro de rede', () async {
    dio.interceptors.insert(
      0,
      InterceptorsWrapper(
        onRequest: (options, handler) =>
            handler.reject(DioException(requestOptions: options), true),
      ),
    );

    await expectLater(
      repository.listBlocks(),
      throwsA(isA<ApiException>().having((e) => e.isNetworkError, 'rede', true)),
    );
  });

  group('modelos', () {
    test('ReportReason converte de e para o valor do backend', () {
      for (final reason in ReportReason.values) {
        expect(ReportReason.fromJson(reason.toJson()), reason);
      }
      expect(ReportReason.fromJson('DESCONHECIDO'), isNull);
    });

    test('CreateReportRequest omite descrição em branco e viagem vazia', () {
      const request = CreateReportRequest(
        reportedId: 'user-2',
        reason: ReportReason.other,
        description: '   ',
        rideId: '',
      );

      expect(request.toJson(), {'reportedId': 'user-2', 'reason': 'OTHER'});
    });

    test('BlockModel sem nome mostra um texto genérico', () {
      final block = BlockModel.fromJson(const {'id': 'b', 'blockedUserId': 'u'});

      expect(block.displayName, 'Usuário');
    });
  });
}
