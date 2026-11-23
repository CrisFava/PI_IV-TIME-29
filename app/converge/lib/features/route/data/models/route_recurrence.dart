// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

/// Com que frequência o trajeto acontece, conforme `RecurrenceType` do backend:
/// num dia só (`ONCE`, com data) ou toda semana nos dias escolhidos (`WEEKLY`).
enum RouteRecurrence {
  once('ONCE'),
  weekly('WEEKLY');

  final String apiValue;

  const RouteRecurrence(this.apiValue);

  /// Trajetos antigos vêm sem o campo: valem como semanais.
  static RouteRecurrence fromApi(String? value) =>
      value == once.apiValue ? once : weekly;
}
