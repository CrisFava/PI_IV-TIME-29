# Requisitos Funcionais — Converge

Os requisitos funcionais descrevem o que o sistema Converge deve fazer.

| ID | Requisito |
|---|---|
| **RF01** | O sistema deve permitir o cadastro de usuários na plataforma. |
| **RF02** | O sistema deve permitir que o usuário informe seus dados acadêmicos (instituição, curso, matrícula) durante o cadastro. |
| **RF03** | O sistema deve permitir a verificação do vínculo acadêmico do usuário com a universidade. |
| **RF04** | O sistema deve permitir que o usuário realize login na plataforma. |
| **RF05** | O sistema deve permitir que o usuário encerre sua sessão (logout). |
| **RF06** | O sistema deve permitir o cadastro e atualização das informações de perfil do usuário, incluindo foto e dados de contato. |
| **RF07** | O sistema deve permitir o cadastro das informações do veículo utilizado pelo motorista (modelo, placa, cor, capacidade total de passageiros, fonte de energia utilizada (combustível ou elétrico), preço médio da energia e consumo médio (km/l para combustão ou km/kWh para elétrico)). |
| **RF08** | O sistema deve permitir que o motorista informe a capacidade de vagas disponíveis para compartilhamento em um trajeto. |
| **RF09** | O sistema deve permitir o cadastro de um trajeto, contendo origem e destino. |
| **RF10** | O sistema deve permitir informar os horários de partida associados ao trajeto. |
| **RF11** | O sistema deve permitir informar os dias da semana em que o trajeto é realizado. |
| **RF12** | O sistema deve permitir que o usuário indique se deseja oferecer ou procurar carona. |
| **RF13** | O sistema deve permitir que o motorista defina um desvio máximo aceitável de rota para aceitar passageiros. |
| **RF14** | O sistema deve utilizar as informações de origem, destino, horário, dias da semana, capacidade disponível e desvio aceitável para identificar possíveis combinações de carona entre usuários. |
| **RF15** | O sistema deve identificar trajetos e horários compatíveis entre usuários que oferecem e que procuram carona. |
| **RF16** | O módulo de matching, integrado ao backend, deve calcular um índice de compatibilidade para cada combinação encontrada e ordenar os resultados. |
| **RF17** | O sistema deve apresentar ao usuário as caronas compatíveis identificadas pelo mecanismo de matching, ordenadas pelo índice de compatibilidade. |
| **RF18** | O sistema deve permitir a visualização das informações de perfil relevantes dos usuários envolvidos em uma combinação de carona. |
| **RF19** | O sistema deve permitir que um passageiro solicite uma vaga e que o motorista aceite ou recuse a solicitação. |
| **RF20** | O sistema deve permitir que os usuários confirmem uma carona a partir de uma combinação compatível identificada. |
| **RF21** | O sistema deve decrementar as vagas disponíveis ao confirmar uma carona e incrementá-las ao cancelar, marcando a carona como lotada quando o número de vagas disponíveis chegar a zero. |
| **RF22** | O sistema deve permitir a visualização dos detalhes da viagem, incluindo trajeto, horário, veículo e participantes. |
| **RF23** | O sistema deve permitir que o motorista registre a ausência de um passageiro durante a viagem, alterando o status da participação para "ausente". |
| **RF24** | O sistema deve liberar a vaga do passageiro ausente e desconsiderá-lo nos indicadores e no rateio, sem cobrá-lo, atualizando o valor estimado dos demais participantes efetivos. |
| **RF25** | O sistema deve permitir que o motorista denuncie o passageiro ausente, com o motivo "ausência" pré-preenchido. |
| **RF26** | O sistema deve registrar o histórico de ausências por usuário e exibi-lo no perfil. |
| **RF27** | O sistema deve permitir a comunicação em tempo real entre os participantes de uma carona por meio de chat integrado na plataforma. |
| **RF28** | O sistema deve permitir o cancelamento de uma carona por qualquer participante, quando aplicável, notificando os demais envolvidos. |
| **RF29** | O sistema deve calcular uma estimativa do custo total da viagem. |
| **RF30** | O sistema deve calcular o custo de energia (combustível ou eletricidade) a partir da distância da rota planejada, incluindo os desvios para buscar e deixar passageiros, do consumo médio do veículo e do preço da energia, somando pedágios e estacionamento informados pelo motorista. |
| **RF31** | O sistema deve calcular o desvio de cada combinação de carona pela diferença entre a distância da rota original do motorista e a distância da rota com as paradas do passageiro, registrando esse valor ao confirmar a carona. |
| **RF32** | O sistema deve realizar o rateio dos custos estimados da viagem entre os participantes. |
| **RF33** | O sistema deve ratear o custo da viagem entre o motorista e os passageiros efetivos, de modo que o motorista sempre participe da divisão. |
| **RF34** | O sistema deve informar a cada participante o valor estimado que lhe corresponde no rateio. |
| **RF35** | O sistema deve exibir ao passageiro, antes da confirmação da carona, o valor estimado atual e o valor máximo que poderá ser cobrado, correspondente à metade do custo da rota com o desvio do passageiro, informando que o valor estimado pode aumentar até o máximo em caso de desistências e que o valor final só é definido ao término da viagem. |
| **RF36** | O sistema deve atualizar o valor estimado de cada participante sempre que houver entrada, cancelamento ou ausência de participantes. |
| **RF37** | O sistema deve registrar o valor máximo no momento da confirmação da carona e garantir que o valor final cobrado do passageiro não o ultrapasse, mesmo que outros participantes cancelem ou faltem, ou que o custo da viagem seja alterado depois. |
| **RF38** | O sistema deve calcular o valor final de cada participante apenas ao concluir a viagem, com base nos participantes efetivos, nos desvios e nas despesas informadas. |
| **RF39** | O sistema deve disponibilizar as informações necessárias para pagamento via PIX entre os participantes (chave PIX do motorista e valor por participante). |
| **RF40** | O sistema deve realizar a cobrança sempre por viagem individual, via PIX, inclusive para passageiros com vínculo recorrente. |
| **RF41** | O sistema deve permitir que o passageiro marque uma viagem como paga e que o motorista confirme o recebimento, registrando o status do pagamento como pendente, aguardando confirmação, pago ou não aplicável. Enquanto estiver aguardando confirmação do motorista, a cobrança não é considerada vencida. |
| **RF42** | O sistema deve registrar o histórico de viagens realizadas pelo usuário, incluindo data, trajeto e participantes. |
| **RF43** | O sistema deve permitir que os participantes realizem avaliações mútuas após a conclusão de uma viagem. |
| **RF44** | O sistema deve permitir a visualização do histórico de avaliações recebidas por um usuário. |
| **RF45** | O sistema deve permitir que um usuário denuncie outro usuário, com descrição do motivo. |
| **RF46** | O sistema deve permitir que um usuário bloqueie outro usuário. |
| **RF47** | O sistema deve impedir interações incompatíveis com um bloqueio ativo — incluindo exibição nos resultados de matching, aceite de carona e comunicação via chat — conforme as regras da plataforma. |
| **RF48** | O sistema deve manter as informações necessárias para moderação e análise de denúncias recebidas. |
| **RF49** | O sistema deve considerar nos indicadores apenas viagens com status concluída e com pelo menos um passageiro que efetivamente participou, excluindo ausentes e cancelados. |
| **RF50** | O sistema deve registrar os dados necessários para geração de indicadores de mobilidade e sustentabilidade. |
| **RF51** | O sistema deve apresentar a quantidade de caronas realizadas pelo usuário e no total da plataforma. |
| **RF52** | O sistema deve apresentar a quantidade de quilômetros compartilhados. |
| **RF53** | O sistema deve estimar a quantidade de veículos potencialmente evitados pelo compartilhamento de viagens. |
| **RF54** | O sistema deve estimar a economia de energia (em litros ou kWh e em R$) das viagens compartilhadas, com base no consumo médio do veículo e na distância, dividindo a economia pela quantidade de participantes efetivos do veículo. |
| **RF55** | O sistema deve estimar a quantidade de CO₂ potencialmente evitada pelas viagens compartilhadas a partir da energia economizada e do fator de emissão do tipo de energia do veículo. |
| **RF56** | O sistema deve apresentar a economia financeira do usuário: para o passageiro, o valor pago em comparação ao custo estimado de ir sozinho; para o motorista, o valor recuperado com o rateio. |
| **RF57** | O sistema deve apresentar o impacto ambiental em equivalências compreensíveis, como a quantidade de árvores necessárias para absorver o CO₂ evitado. |
| **RF58** | O sistema deve apresentar os indicadores por período (semana, mês e semestre), com a evolução histórica em gráfico. O acumulado histórico total é calculado a partir da soma dos indicadores mensais, não armazenado como período próprio. |
| **RF59** | O sistema deve disponibilizar os indicadores de mobilidade e sustentabilidade de forma acessível ao usuário. |
| **RF60** | O sistema deve permitir a visualização de informações relacionadas ao impacto ambiental e econômico das viagens compartilhadas. |
| **RF61** | O sistema deve exibir a metodologia dos cálculos dos indicadores (fatores de emissão e premissas adotadas), informando que os valores apresentados são estimativas. |
| **RF62** | O sistema deve permitir a atualização das informações cadastrais do usuário e dos trajetos registrados.|
| **RF63** | O sistema deve exigir que o motorista cadastre e tenha a CNH verificada (número, categoria, validade e foto do documento) para poder oferecer carona, retirando-o do matching automaticamente quando a CNH estiver vencida. |
| **RF64** | O sistema deve permitir o estabelecimento de um vínculo recorrente de vaga reservada entre passageiro e motorista, podendo ser proposto por qualquer um dos dois e dependendo do aceite do outro. |
| **RF65** | O sistema deve reservar automaticamente a vaga do passageiro vinculado nos dias combinados (modelo opt-out), permitindo que ele avise previamente quando não for em um dia específico, liberando a vaga para outros passageiros. |
| **RF66** | O sistema deve liberar a vaga reservada sem penalidade quando o passageiro avisar com antecedência mínima (até as 20h da véspera); avisos posteriores são tratados como cancelamento tardio. |
| **RF67** | O sistema deve controlar a inadimplência por viagem com tolerância decrescente conforme o número de viagens vencidas, bloqueando o passageiro do matching ao atingir o limite e só o regularizando quando todas as dívidas vencidas forem pagas. |
| **RF68** | O sistema deve exibir ao passageiro uma área de pendências de pagamento com as viagens pendentes e seus status. |
| **RF69** | O sistema deve conceder e revogar automaticamente os selos de confiança e de sustentabilidade com base no comportamento e no impacto ambiental acumulado do usuário, recalculados periodicamente. |
| **RF70** | O sistema deve registrar o status de cada solicitação de vaga (solicitada, recusada, confirmada), mantendo o registro das recusas para histórico e moderação. |
| **RF71** | O sistema deve manter uma trilha de auditoria das ações sensíveis da plataforma (confirmação de pagamento, registro de ausência, cancelamentos, bloqueios, resolução de denúncias e verificações), para fins de moderação e suporte. |