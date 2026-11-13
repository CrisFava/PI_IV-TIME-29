// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

/// Dados do veículo inválidos, detectados no app antes de chamar a API.
/// [field] é o campo do `CreateVehicleRequest` com o problema.
class InvalidVehicleException implements Exception {
  final String message;
  final String field;

  const InvalidVehicleException(this.message, {required this.field});

  @override
  String toString() => message;
}
