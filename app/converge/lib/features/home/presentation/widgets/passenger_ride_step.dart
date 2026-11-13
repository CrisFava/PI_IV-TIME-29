// Autor: Allan Giovanni Matias Paes - RA 25008211

/// Passos do fluxo de solicitação de carona pelo passageiro (Uber-like).
enum PassengerRideStep {
  /// Estado de repouso: mapa limpo, sem motoristas, botão "Pedir carona".
  idle,

  /// Passo 0: Escolha inicial antes do embarque (Usar uma rota salva ou Selecionar no mapa).
  chooseRouteOption,

  /// Passo 1: Seleção do Ponto de Embarque (Origem).
  selectPickup,

  /// Passo 2: Arrasto do mapa estilo Uber com pino fixo para definir o Embarque.
  dragMapPickup,

  /// Passo 3: Busca/Seleção do Ponto de Desembarque (Destino).
  searchDestination,

  /// Passo 4: Arrasto do mapa estilo Uber com pino fixo para definir o Desembarque.
  dragMapDestination,

  /// Passo 5: Confirmação prévia com o trajeto desenhado no mapa antes de confirmar.
  routeConfirmation,

  /// Passo 6: Lista dos motoristas compatíveis com o trajeto (matching).
  driverList,

  /// Passo 7: Pedido de vaga enviado; aguardando o motorista aceitar ou recusar.
  waitingDriver,

  /// Passo 8: Motorista aceitou! Exibe o ponto de partida dele e o atalho para o chat.
  driverAccepted,
}
