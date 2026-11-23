// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

/// O que o usuário pode fazer como motorista.
enum DriverAccess {
  /// Ainda não ativou o modo motorista (precisa da CNH, do carro e do PIX).
  notDriver,

  /// É motorista, mas não tem nenhum carro ativo: o acesso fica bloqueado.
  needsVehicle,

  /// É motorista, mas a lista de carros ainda carrega ou falhou: sem saber se ele
  /// tem carro, o acesso fica suspenso (não é mostrado como "sem carro").
  unavailable,

  /// Motorista com pelo menos um carro: acesso liberado.
  ready;

  bool get canDrive => this == DriverAccess.ready;
  bool get isUnavailable => this == DriverAccess.unavailable;
  bool get isBlockedByVehicle => this == DriverAccess.needsVehicle;
}
