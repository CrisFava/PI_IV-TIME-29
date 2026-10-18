import 'package:converge/core/network/api_error_code.dart';
import 'package:converge/core/network/api_exception.dart';
import 'package:converge/core/network/api_response.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('ApiException.fromResponse', () {
    test('lê o error do envelope ApplicationResponse', () {
      final error = ApiException.fromResponse({
        'success': false,
        'data': null,
        'error': {
          'code': 'EMAIL_ALREADY_EXISTS',
          'message': 'E-mail já cadastrado.',
          'fields': ['email'],
        },
        'timestamp': '2026-10-15T12:00:00Z',
      }, status: 409);

      expect(error.status, 409);
      expect(error.code, ApiErrorCode.emailAlreadyExists);
      expect(error.message, 'E-mail já cadastrado.');
      expect(error.fields, {'email'});
      expect(error.isConflict, isTrue);
    });

    test('código desconhecido vira ApiErrorCode.unknown', () {
      final error = ApiException.fromResponse({
        'success': false,
        'error': {'code': 'SOMETHING_NEW', 'message': 'x'},
      }, status: 400);

      expect(error.code, ApiErrorCode.unknown);
      expect(error.fields, isEmpty);
    });

    test('corpo fora do envelope usa mensagem padrão por status', () {
      final error = ApiException.fromResponse({
        'timestamp': '2026-10-15T12:00:00Z',
        'status': 404,
        'error': 'Not Found',
        'path': '/api/v1/users/me',
      }, status: 404);

      expect(error.isNotFound, isTrue);
      expect(error.code, ApiErrorCode.unknown);
      expect(error.message, 'Recurso não encontrado.');
    });
  });

  group('unwrapApiResponse', () {
    Map<String, dynamic> identity(Map<String, dynamic> json) => json;

    test('devolve o data em caso de sucesso', () {
      final data = unwrapApiResponse({
        'success': true,
        'data': {'id': 1},
        'error': null,
        'timestamp': '2026-10-15T12:00:00Z',
      }, identity);

      expect(data, {'id': 1});
    });

    test('lança ApiException quando success é false', () {
      expect(
        () => unwrapApiResponse(
          {
            'success': false,
            'data': null,
            'error': {'code': 'NOT_ALLOWED', 'message': 'Proibido'},
          },
          identity,
          status: 403,
        ),
        throwsA(
          isA<ApiException>().having(
            (e) => e.code,
            'code',
            ApiErrorCode.notAllowed,
          ),
        ),
      );
    });

    test('lança ApiException para corpo fora do envelope', () {
      expect(
        () => unwrapApiResponse({'id': 1}, identity),
        throwsA(isA<ApiException>()),
      );
    });
  });
}
