// Autor: Allan Giovanni Matias Paes - RA 25008211

/// Etapas de criar ou pedir carona. Partida, destino e horário (até
/// [routeConfirmation]) valem para o motorista e o passageiro; as seguintes,
/// só para o pedido do passageiro.
enum PassengerRideStep {
  /// Estado de repouso: mapa limpo, sem motoristas, botão "Pedir carona".
  idle,

  /// Etapa 1: de onde sai (busca ou "Selecionar no mapa").
  selectPickup,

  /// Etapa 1 no mapa: pino fixo no centro, arrastando o mapa até a partida.
  dragMapPickup,

  /// Etapa 2: para onde vai (o mesmo layout da partida).
  searchDestination,

  /// Etapa 2 no mapa: pino fixo no centro, arrastando o mapa até o destino.
  dragMapDestination,

  /// Etapa do trajeto: o mapa em foco com o caminho inteiro e "Confirmar trajeto".
  routePreview,

  /// Etapa 3: dia e horário; o motorista também escolhe se repete toda semana.
  routeConfirmation,

  /// Motorista: o carro, as vagas e o raio de busca, antes da revisão.
  offerDetails,

  /// Passageiro: Lista dos motoristas compatíveis com o trajeto (matching).
  driverList,

  /// Passageiro: Pedido de vaga enviado; aguardando o motorista aceitar ou recusar.
  waitingDriver,

  /// Passageiro: Motorista aceitou! Exibe o ponto de partida dele e o atalho para o chat.
  driverAccepted,
}
