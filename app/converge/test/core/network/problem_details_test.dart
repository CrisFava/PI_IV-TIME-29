import 'package:converge/core/network/problem_details.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('ProblemDetailsException.fromJson', () {
    test('lê o envelope ApplicationResponse do backend', () {
      final problem = ProblemDetailsException.fromJson({
        'success': false,
        'data': null,
        'error': {
          'code': 'BAD_REQUEST',
          'message': 'Driver requirements not met.',
          'fields': ['pixKey', 'driverLicense'],
        },
        'timestamp': '2026-10-15T12:00:00Z',
      }, status: 400);

      expect(problem.status, 400);
      expect(problem.code, 'BAD_REQUEST');
      expect(problem.detail, 'Driver requirements not met.');
      expect(problem.errors?.keys, containsAll(['pixKey', 'driverLicense']));
    });

    test('lê RFC 7807', () {
      final problem = ProblemDetailsException.fromJson({
        'title': 'Conflito',
        'detail': 'E-mail já cadastrado.',
        'status': 409,
        'code': 'EMAIL_ALREADY_EXISTS',
      }, status: 409);

      expect(problem.title, 'Conflito');
      expect(problem.detail, 'E-mail já cadastrado.');
      expect(problem.code, 'EMAIL_ALREADY_EXISTS');
    });

    test('usa mensagem padrão por status para corpos desconhecidos', () {
      final problem = ProblemDetailsException.fromJson({
        'timestamp': '2026-10-15T12:00:00Z',
        'status': 404,
        'error': 'Not Found',
        'path': '/api/v1/users/me',
      }, status: 404);

      expect(problem.isNotFound, isTrue);
      expect(problem.detail, 'Recurso não encontrado.');
    });
  });
}
