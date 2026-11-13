// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

/// Etapas do passageiro entre escolher o trajeto e o motorista responder.
enum PassengerMatchPhase {
  /// Nada em andamento.
  idle,

  /// Buscando motoristas compatíveis.
  loading,

  /// Lista de motoristas pronta (pode estar vazia).
  ready,

  /// Enviando o pedido de vaga.
  requesting,

  /// Pedido enviado; consultando a viagem até o motorista responder.
  waiting,

  /// O motorista aceitou: o chat está liberado.
  accepted,

  /// A busca falhou (ver `errorMessage`).
  failed,
}
