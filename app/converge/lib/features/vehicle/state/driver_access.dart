// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

/// O que o usuário pode fazer como motorista.
enum DriverAccess {
  /// Ainda não ativou o modo motorista (precisa da CNH, do carro e do PIX).
  notDriver,

  /// É motorista, mas não tem nenhum carro ativo: o acesso fica bloqueado.
  needsVehicle,

  /// Motorista com pelo menos um carro: acesso liberado.
  ready;

  bool get canDrive => this == DriverAccess.ready;
  bool get isBlockedByVehicle => this == DriverAccess.needsVehicle;
}
