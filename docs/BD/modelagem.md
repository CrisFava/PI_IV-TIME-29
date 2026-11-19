# Modelagem de Banco de Dados — Converge

> **Banco de dados:** MongoDB (Atlas em produção)
> **Estratégia:** Monólito Modular — cada módulo de negócio é dono exclusivo de suas coleções.
> Referências entre coleções de módulos distintos são feitas **exclusivamente por `String id`**, sem `@DBRef`.
> Valores monetários são armazenados como **`Decimal128`** no MongoDB e **`BigDecimal`** no Java. A diferença de centavos no rateio é sempre atribuída ao motorista.
> Coordenadas geográficas são armazenadas no formato **GeoJSON `Point`** exigido pelo índice `2dsphere` do MongoDB: `{ "type": "Point", "coordinates": [longitude, latitude] }` — **longitude sempre antes da latitude**.
> **Datas e horas:** todos os campos `ISODate` são armazenados em **UTC**. Campos de hora recorrente (`departureTime`, `estimatedArrivalTime`) são `String "HH:mm"` interpretados no fuso oficial da aplicação (**America/Sao_Paulo**). Ao combinar uma data concreta com um horário recorrente para gerar `departureDateTime`, a conversão para UTC deve usar explicitamente America/Sao_Paulo — nunca o fuso do servidor.
> **Chat:** as mensagens entre participantes **não ficam no MongoDB** — vivem no **Firebase Realtime Database**, conforme a arquitetura do projeto (comunicação em tempo real). O MongoDB guarda apenas o identificador da sala de conversa quando necessário (ex.: um `chatRoomId` na viagem para localizar o histórico no Firebase). Por isso não há coleção `messages`/`chat` nesta modelagem.
>
> Estrutura no Realtime Database: `chats/{chatRoomId}/{passengerId}/messages/{pushId}`, com os campos `senderId`, `senderName`, `senderUid` (uid do Firebase, conferido pelas regras; é ele que diz de quem é a mensagem), `type` (`TEXT`, `PAYMENT_REQUEST`, `PAYMENT_CONFIRMED`, `SYSTEM`), `text`, `amount` e `sentAt` (horário do servidor). O `chatRoomId` é criado quando o motorista aceita o primeiro passageiro, e cada passageiro aceito tem uma conversa particular com o motorista. As mensagens de pagamento e de viagem são só avisos: a fonte de verdade continua sendo a API. As regras de acesso ficam em `app/converge/database.rules.json`.
> **Pagamento PIX:** no MVP o pagamento usa **QR Code PIX estático** ("Pix Copia e Cola"), gerado pelo próprio backend, sem gateway e sem API externa. O valor da cobrança vai travado dentro do payload e o dinheiro vai direto da conta do passageiro para a do motorista — o Converge nunca toca no dinheiro. Como o banco não devolve nenhuma confirmação ao servidor, a confirmação de recebimento é **manual** (o motorista marca que recebeu). A verificação automática via gateway (QR Code dinâmico + webhook) fica como evolução futura.

---

## Visão Geral das Coleções

| Coleção | Módulo dono | Descrição |
|---|---|---|
| `users` | user | Dados de cadastro, perfil, vínculo acadêmico, reputação e selos |
| `vehicles` | user | Veículos cadastrados pelo motorista |
| `routes` | route | Trajetos cadastrados para oferecer ou procurar carona |
| `rides` | ride | Viagens confirmadas, participantes, custos e status |
| `reviews` | review | Avaliações mútuas após conclusão de viagem |
| `reports` | security | Denúncias entre usuários com motivo e status |
| `blocks` | security | Registros de bloqueio entre usuários |
| `reserved_seats` | ride | Vínculos recorrentes de vaga reservada entre passageiro e motorista (opt-out) |
| `matchings` | matching | Resultados calculados do algoritmo de compatibilidade |
| `indicators` | indicator | Indicadores de mobilidade e sustentabilidade por usuário e período |
| `platform_indicators` | indicator | Indicadores globais agregados da plataforma (RF51) |
| `indicator_ride_registrations` | indicator | Viagens já contabilizadas nos indicadores (evita reprocessar) |
| `audit_logs` | shared | Trilha de auditoria de ações sensíveis (moderação, pagamento, segurança) |
| `universities` | shared | Lista de referência de universidades (padroniza o filtro de matching) |
| `notifications` | shared | Avisos para o usuário (pagamento, cancelamento, solicitação de vaga) |
| `idempotency_keys` | shared | Chaves de idempotência já processadas (evita operação duplicada) |

---

## 1. Coleção `users`

**Módulo:** `modules/user`
**Descrição:** Armazena todos os dados de um usuário da plataforma — cadastro, perfil, vínculo acadêmico, reputação por papel e selos de reconhecimento.

```json
{
  "_id": "ObjectId (String)",
  "firebaseUid": "String",
  "name": "String",
  "email": "String",
  "phone": "String",
  "photoUrl": "String | null",
  "participationRoles": ["PASSENGER | DRIVER"],
  "academicRecord": {
    "university": "String",
    "course": "String",
    "enrollmentNumber": "String",
    "institutionalEmail": "String",
    "status": "PENDING | UNDER_REVIEW | VERIFIED | REJECTED",
    "verificationMethod": "INSTITUTIONAL_EMAIL | DOCUMENT",
    "documentUrl": "String | null",
    "verifiedAt": "ISODate | null",
    "reviewedAt": "ISODate | null",
    "rejectionReason": "String | null"
  },
  "driverLicense": {
    "number": "String",
    "category": "A | B | AB | C | D | E",
    "expiresAt": "ISODate",
    "documentUrl": "String | null",
    "status": "PENDING | UNDER_REVIEW | VERIFIED | REJECTED | EXPIRED",
    "verifiedAt": "ISODate | null",
    "rejectionReason": "String | null"
  },
  "pixKey": "String | null",
  "pixKeyType": "CPF | PHONE | EMAIL | RANDOM | null",
  "paymentStanding": "UP_TO_DATE | HAS_OVERDUE | BLOCKED",
  "overdueRidesCount": "Integer",
  "isDefaulter": "Boolean",
  "defaulterSince": "ISODate | null",
  "reputation": {
    "driver": {
      "level": "HIGH | MEDIUM | LOW | null",
      "averageRating": "Double",
      "totalReviews": "Integer",
      "window": {
        "completedRides": "Integer",
        "driverInitiatedCancellations": "Integer",
        "delays": "Integer",
        "substantiatedReports": "Integer"
      },
      "calculatedAt": "ISODate"
    },
    "passenger": {
      "level": "HIGH | MEDIUM | LOW | null",
      "averageRating": "Double",
      "totalReviews": "Integer",
      "window": {
        "completedRides": "Integer",
        "lateCancellations": "Integer",
        "noShows": "Integer",
        "substantiatedReports": "Integer",
        "overdueRides": "Integer"
      },
      "calculatedAt": "ISODate"
    }
  },
  "badges": [
    {
      "badge": "TRUST | SUSTAINABILITY",
      "grantedAt": "ISODate"
    }
  ],
  "active": "Boolean",
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `email` | Único | Login e identificação — nunca duplicar |
| `firebaseUid` | Único | Chave de autenticação Firebase |
| `academicRecord.institutionalEmail` | Único Parcial (`$type: 'string'`) | Garante unicidade do e-mail institucional acadêmico |
| `academicRecord.university` | Simples | Filtro de matching por universidade |
| `academicRecord.status` | Simples | Consultas de moderação e verificação |
| `reputation.driver.level` | Simples | Consultas de reputação de motoristas |
| `isDefaulter` | Simples | Verificação rápida de inadimplência no matching |

### Regras de Negócio — Reputação (`reputation`)

A reputação é separada por papel (`reputation.driver` e `reputation.passenger`). Cada bloco guarda `level`, `averageRating`, `totalReviews`, `window` e `calculatedAt`. Um usuário novo começa com `averageRating = 0.0`, `totalReviews = 0`, `level = null` e a janela zerada.

**O que grava cada campo hoje:**

| Campo | Quem grava | Quando |
|---|---|---|
| `averageRating`, `totalReviews` | `CreateReviewUseCase` (módulo `review`), pelo caso de uso `RecordReviewRatingUseCase` (módulo `user`) | Logo depois de salvar uma avaliação. Só o papel avaliado (`reviewedRole`: `DRIVER` grava em `reputation.driver`, `PASSENGER` em `reputation.passenger`) |
| `window.completedRides` | `RideCompletedReputationListener` (módulo `user`), pelo caso de uso `RecordCompletedRideUseCase` | Quando uma viagem é concluída: +1 para o motorista (`driver.window`) e +1 para cada passageiro efetivo (`passenger.window`) |
| `window.driverInitiatedCancellations`, `window.lateCancellations`, `window.noShows` | `RecordReputationPenaltyUseCase` | Cancelamento do motorista, cancelamento tardio do passageiro e ausência |
| `level`, `calculatedAt` | Os mesmos casos de uso acima | A cada gravação de média, de viagem concluída ou de penalidade |
| `window.delays`, `window.substantiatedReports`, `window.overdueRides` | **Nenhum código grava** | Ficam em 0 (ver "Ainda não implementado") |

**Média de avaliação (`averageRating`, `totalReviews`):**
- A média e o total são **recalculados a partir de todas as avaliações recebidas no papel** (índice `reviewedId + reviewedRole` de `reviews`), nunca somados de forma incremental. Duas avaliações salvas ao mesmo tempo não se perdem: cada recálculo roda depois do próprio `save` e relê a coleção.
- A média é gravada **arredondada a 2 casas** e esse mesmo valor entra nas regras de `level` e de selo (≥ 4.2 e ≥ 4.0).
- Se a gravação da reputação falhar, a avaliação **continua salva**: o erro só vai para o log.

**Regra de `level` (igual para motorista e passageiro):**

| `level` | Quando |
|---|---|
| `LOW` | Algum limite de penalidade foi estourado. Motorista: mais de 2 cancelamentos iniciados por ele ou mais de 1 denúncia procedente. Passageiro: mais de 2 cancelamentos tardios, mais de 1 ausência, qualquer viagem vencida ou mais de 1 denúncia procedente |
| `HIGH` | Cumpre os critérios de confiança do papel (tabelas do "Selo de Confiança" abaixo) |
| `MEDIUM` | Todo o resto |

- A mesma regra roda quando entra uma média nova, uma viagem concluída ou uma penalidade. Por isso uma penalidade pode **rebaixar** `HIGH` para `MEDIUM` e `MEDIUM` não é mais "fixo": ele é recalculado a cada gravação.
- `level` só vira `HIGH` se `completedRides` for alto o bastante. Como `completedRides` só passou a ser contado com o listener de viagem concluída, **o `HIGH` só é alcançável com a gravação da média e a contagem de viagens funcionando juntas**.

**Contagem de viagens (`completedRides`):**
- O registro da viagem concluída roda **depois** que a conclusão da viagem é gravada, fora da transação da conclusão. Uma falha nele só vai para o log e **nunca desfaz nem impede** a conclusão.
- Idempotência: não há lista de `rideId` aplicados. O listener ouve só o `RideCompletedEvent`, que o `RideCompletionPublisher` publica uma vez por viagem (só o `CompleteRideUseCase` o chama, e uma viagem só conclui uma vez). O reconciliador de indicadores **não** republica o evento, então não há recontagem.
- Limitação: se o registro falhar, aquela viagem fica sem contagem (não há reprocessamento). Viagem sem passageiro efetivo não gera evento e não conta (RF49).

**Atualização atômica por papel:** a reputação é gravada **por papel** (`reputation.driver` ou `reputation.passenger`), sem regravar o usuário inteiro. Assim uma avaliação, uma penalidade e o job dos selos não sobrescrevem as alterações uns dos outros.

**Janela de 90 dias não é deslizante.** Os contadores de `window` **acumulam desde o início** e nunca são zerados nem descontados por data. Os limites "nos últimos 90 dias" das tabelas abaixo valem, na prática, sobre o total acumulado. A janela deslizante fica como evolução.

**Ainda não implementado:**
- Moderação das denúncias: `window.substantiatedReports` continua em 0 porque nenhuma denúncia chega a `RESOLVED` (ver coleção `reports`).
- `window.delays` e `window.overdueRides` não são incrementados por nenhum fluxo.
- Janela deslizante de 90 dias.

### Regras de Negócio — Selo de Confiança

O selo é recalculado **uma vez por semana** por um job (ver "Job semanal dos selos" abaixo). A tabela abaixo usa o texto "90 dias", mas a janela **não é deslizante**: os contadores acumulam (ver "Reputação").

**Motorista — critérios para ganhar `TRUST`:**

| Critério | Limiar |
|---|---|
| Média de avaliação | ≥ 4.2 estrelas |
| Viagens concluídas na janela | ≥ 10 |
| Cancelamentos iniciados pelo motorista | ≤ 2 nos últimos 90 dias |
| Denúncias procedentes | ≤ 1 nos últimos 90 dias |

**Passageiro — critérios para ganhar `TRUST`:**

| Critério | Limiar |
|---|---|
| Média de avaliação | ≥ 4.0 estrelas |
| Viagens concluídas na janela | ≥ 5 |
| Cancelamentos tardios (< 2h de antecedência) | ≤ 2 nos últimos 90 dias |
| Ausências | ≤ 1 nos últimos 90 dias |
| Viagens vencidas (não pagas no prazo) | 0 nos últimos 90 dias |

O selo é **removido automaticamente** se o usuário deixar de cumprir qualquer critério no próximo recálculo.

**Regra aplicada pelo código (`User.qualifiesForTrustBadge`):** o usuário tem `TRUST` quando não estourou nenhum limite de penalidade **e** cumpre os critérios de confiança em **pelo menos um** dos papéis (motorista **ou** passageiro). A revogação imediata por penalidade (`revokeTrustBadgeIfThresholdExceeded`) e o job semanal usam o mesmo predicado, então o selo não alterna entre concedido e revogado. Se uma penalidade derruba o selo no meio da semana, ele só volta no próximo job.

### Regras de Negócio — Selo de Sustentabilidade

O selo `SUSTAINABILITY` reconhece o impacto ambiental acumulado do usuário. É baseado em dados concretos dos indicadores, não apenas em "andar com mais gente". O critério usa o **CO₂ evitado acumulado** (somando os `indicators` mensais do usuário):

| Nível do selo | CO₂ evitado acumulado | Equivalência aproximada |
|---|---|---|
| Base para concessão | ≥ 50 kg de CO₂ evitado | ~2 árvores/ano |
| (opcional — exibição de destaque) | ≥ 200 kg de CO₂ evitado | ~9 árvores/ano |

- O cálculo de CO₂ evitado já é feito e armazenado em `indicators.co2AvoidedKg`, com base na energia economizada × fator de emissão da fonte do veículo (metodologia verídica — ver dicionário).
- O selo considera **apenas viagens concluídas com pelo menos um passageiro efetivo** — garante que o impacto seja real (compartilhamento de fato), não apenas o motorista rodando sozinho.
- Recalculado semanalmente junto com o selo de confiança, pelo mesmo job.
- O CO₂ acumulado vem do caso de uso de leitura do módulo `indicator` (`GetUserIndicatorsUseCase`, período `ALL_TIME`, que soma os `indicators` `MONTHLY`), por meio da porta `UserCo2Gateway`. O módulo `user` não acessa o repositório do `indicator`. O limite é inclusivo: **50,0 kg ou mais** concede o selo.
- O degrau opcional de 200 kg (destaque na exibição) **não** é gravado nem calculado pelo código.

### Job semanal dos selos (RF69)

- **Agendamento:** `BadgeRecalculationScheduler`, toda **segunda-feira às 03h**, fuso `America/Sao_Paulo`. A expressão cron pode ser trocada pela propriedade `converge.user.badge-recalculation-cron`.
- **Caso de uso:** `RecalculateBadgesUseCase` percorre os usuários **em páginas de 100**, ordenadas por `_id`, e para cada um concede ou revoga `TRUST` e `SUSTAINABILITY`.
- **Gravação:** só grava quando algum selo muda. Antes de gravar, relê o usuário (a página pode ter ficado velha). A data `grantedAt` original é preservada em um selo que continua válido.
- **Falha por usuário:** um erro em um usuário (por exemplo, ao ler o CO₂) é contado e logado, os selos dele **não** são revogados e o job segue com os demais.
- **Custo:** uma consulta de indicadores por usuário. Aceitável no volume atual do MVP.
- **Sem auditoria:** `AuditAction` não tem ação de selo e nenhuma foi criada. Concessão e revogação não geram linha em `audit_logs`.
- O nome do campo é `badge` (não `type`): o código usa `UserBadge(Badge badge, LocalDateTime grantedAt)`.

### Regras de Negócio — Inadimplência (pagamento por viagem)

O pagamento é **por viagem**. Cada viagem concluída gera uma cobrança que o passageiro paga por PIX.

**Estados de uma cobrança:**
- **Pendente (`PENDING`):** viagem concluída, ainda não paga, mas **dentro do prazo** (3 dias). Situação normal — não penaliza nem bloqueia.
- **Aguardando confirmação (`AWAITING_DRIVER_CONFIRMATION`):** o passageiro marcou que pagou e espera o motorista confirmar. **Enquanto está nesse estado, o relógio da dívida fica pausado** — a cobrança não vira vencida, protegendo o passageiro de boa-fé contra a lentidão do motorista.
- **Vencida (`overdue`):** passou de **3 dias** em `PENDING` sem o passageiro sequer marcar pagamento. Vira dívida e conta para o bloqueio.

**Ponte entre confirmação e dívida:**
- Se o motorista **confirmar** → `PAID`, cobrança encerrada.
- Se o motorista alegar que **não recebeu** → a cobrança volta para `PENDING` e o relógio dos 3 dias é retomado (ou abre disputa para moderação). Só assim o passageiro pode voltar a acumular dívida — nunca por mera inércia do motorista.

**Limite decrescente de tolerância** — quanto mais dívidas vencidas acumuladas, menor o prazo até o bloqueio. `overdueRidesCount` guarda a quantidade de viagens vencidas:

| Dívidas vencidas (`overdueRidesCount`) | Prazo para regularizar antes de bloquear | `paymentStanding` |
|---|---|---|
| 0 | — (tudo em dia) | `UP_TO_DATE` |
| 1 | 15 dias | `HAS_OVERDUE` |
| 2 | 7 dias | `HAS_OVERDUE` |
| 3 ou mais | bloqueio imediato | `BLOCKED` |

- `isDefaulter = true` e `paymentStanding = BLOCKED` quando atinge 3 dívidas vencidas **ou** estoura o prazo decrescente.
- Pendências dentro do prazo **não bloqueiam** — o passageiro continua pedindo e reservando caronas normalmente.
- Com **qualquer dívida vencida** (`HAS_OVERDUE`), o passageiro recebe aviso e é incentivado a quitar, mas só é efetivamente bloqueado ao atingir o limite da tabela acima.
- Enquanto `BLOCKED`, o usuário não aparece em combinações, não confirma novas caronas e não reserva vaga em vínculos recorrentes.
- **Regularização só ocorre quando TODAS as viagens vencidas estão pagas.** Pagar uma só não libera se ainda houver outra vencida. Quando `overdueRidesCount` volta a 0: `isDefaulter = false`, `defaulterSince = null`, `paymentStanding = UP_TO_DATE`.

### Regras de Negócio — Verificação Acadêmica

- **Método principal (MVP): e-mail institucional.** No cadastro, o usuário informa um e-mail institucional (`institutionalEmail`, ex.: `@puc-campinas.edu.br`) e confirma via link enviado. Ao confirmar, o `status` vai de `PENDING` → `VERIFIED` automaticamente, sem moderador humano. `verificationMethod = INSTITUTIONAL_EMAIL`.
- **Método alternativo: documento.** Caso a universidade não use e-mail institucional confiável, o usuário envia um documento (`documentUrl`) que passa por revisão (`status = UNDER_REVIEW`). `verificationMethod = DOCUMENT`.
- **Rejeição:** se o vínculo for recusado, `status = REJECTED`, `rejectionReason` descreve o motivo e `reviewedAt` registra quando. O usuário pode corrigir e reenviar.
- Somente usuários `VERIFIED` acessam as funcionalidades principais (criar trajeto, aparecer no matching, confirmar carona).

### Regras de Negócio — Habilitação (CNH)

O objeto `driverLicense` é **opcional** e só se aplica a quem atua como motorista (`DRIVER` em `participationRoles`). Passageiros não precisam.

- **Obrigatória para oferecer carona:** o usuário só pode criar trajetos do tipo `OFFER` e aparecer no matching como motorista se tiver `driverLicense.status = VERIFIED`.
- **MVP — autodeclaração com documento:** o motorista informa número, categoria e validade da CNH e envia uma foto (`documentUrl`). O vínculo entra em `UNDER_REVIEW` e um moderador aprova (`VERIFIED`) ou recusa (`REJECTED` com `rejectionReason`).
- **Validade:** quando a data `expiresAt` passa, o status vira `EXPIRED` automaticamente e o motorista é retirado do matching até renovar. Garante que motorista com CNH vencida não dirija pela plataforma.
- **Evolução futura:** validação automática via API oficial (SERPRO) ou serviço de KYC com biometria (compara selfie com a foto da CNH). Fica fora do MVP por exigir contrato e custo.

### Regras de Negócio — Chave PIX

- **Obrigatória para oferecer carona:** o usuário só pode criar trajetos `OFFER` se `pixKey` estiver preenchida, senão o passageiro chega ao fim da viagem sem ter para quem pagar.
- **`pixKeyType` é necessário** porque o formato no payload depende do tipo (uma chave de 11 dígitos pode ser CPF ou celular).
- **Normalização ao salvar:** CPF só com dígitos; `PHONE` no formato `+55DDDNÚMERO` (ex.: `+5511999999999`); `EMAIL` em minúsculas; `RANDOM` como UUID.
- O backend **não consulta o banco** para validar a chave. Quem faz isso é o app do banco do passageiro, ao ler o QR.

### Regras de Negócio — Desativação de Conta

Quando `active = false` (conta suspensa ou excluída):
- Os vínculos recorrentes do usuário passam a `TERMINATED`.
- Viagens futuras onde ele é motorista são canceladas (`CANCELLED`); onde é passageiro, sua vaga é liberada.
- **Dívidas em aberto permanecem** — desativar a conta não perdoa valores devidos.
- O usuário não aparece no matching, não loga e não recebe novas combinações.

---

## 2. Coleção `vehicles`

**Módulo:** `modules/user`
**Descrição:** Veículos cadastrados por motoristas. Um usuário pode ter mais de um veículo, mas apenas um ativo por vez em um trajeto. Suporta múltiplas fontes de energia por veículo (ex: flex tem gasolina e etanol como entradas separadas).

```json
{
  "_id": "ObjectId (String)",
  "userId": "String (ref: users._id)",
  "nickname": "String | null",
  "model": "String",
  "licensePlate": "String",
  "renavam": "String (11 dígitos)",
  "color": "String",
  "type": "HATCH | SEDAN | SUV | PICKUP | VAN | MOTORCYCLE | OTHER",
  "totalCapacity": "Integer",
  "fuelSources": [
    {
      "energySource": "GASOLINE | ETHANOL | DIESEL | ELECTRIC",
      "averageConsumption": "Double",
      "consumptionUnit": "KM_PER_LITER | KM_PER_KWH",
      "currentEnergyPrice": "Decimal128"
    }
  ],
  "active": "Boolean",
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `userId` | Simples | Buscar todos os veículos de um motorista |
| `userId + licensePlate` | Composto Único | Integridade — uma placa por proprietário |

### Regras de Negócio

- **Lugares (`totalCapacity`, contando o motorista):** carro (hatch, sedã, SUV, picape) até 7; van ou outro até 15; moto até 2. Validado no domínio (`Vehicle`).
- **Placa:** sempre gravada em maiúsculas, sem hífen e sem espaços.
- **Limite de consumo médio** (validado no service; fora da faixa retorna erro 400):

| Tipo | Unidade | Mín. | Máx. |
|---|---|---|---|
| HATCH / SEDAN | KM_PER_LITER | 6 | 25 |
| SUV / PICKUP / VAN | KM_PER_LITER | 4 | 18 |
| MOTORCYCLE | KM_PER_LITER | 15 | 50 |
| Carro elétrico | KM_PER_KWH | 3 | 9 |
| Moto elétrica | KM_PER_KWH | 15 | 40 |
| OTHER | qualquer | usar a faixa mais ampla | |

- **Limite de preço da energia** (informado pelo usuário, mesma validação):

| Energia | Mín. | Máx. |
|---|---|---|
| Gasolina / etanol / diesel (R$/L) | 3,00 | 12,00 |
| Eletricidade (R$/kWh) | 0,30 | 3,00 |

- **RENAVAM:** 11 dígitos, guardados só com números, não todos iguais, e o último é o dígito verificador (módulo 11). A conferência roda no **servidor de sockets** (`ValidateRenavamHandler`, ver "Cálculos no servidor de sockets"), chamada pelos casos de uso de criar e editar veículo **antes** de montar a entidade. As mensagens são `O RENAVAM deve ter 11 dígitos.` e `RENAVAM inválido.` (erro `BAD_REQUEST`, campo `renavam`). A entidade `Vehicle` só normaliza (tira a pontuação) e **não valida mais o dígito sozinha**: todo ponto novo que criar ou editar veículo precisa passar pela validação do servidor.
- **Itens de `fuelSources`:** não pode haver duas entradas da mesma `energySource` no mesmo veículo. Flex tem 2 itens (gasolina + etanol). Híbrido tem 1 ou 2.

---

## 3. Coleção `routes`

**Módulo:** `modules/route`
**Descrição:** Trajetos cadastrados pelos usuários para oferecer ou procurar carona. Base de dados do algoritmo de matching. Apenas trajetos com status `ACTIVE` entram no matching.

```json
{
  "_id": "ObjectId (String)",
  "userId": "String (ref: users._id)",
  "vehicleId": "String | null (ref: vehicles._id)",
  "type": "OFFER | SEARCH",
  "origin": {
    "address": "String",
    "neighborhood": "String | null",
    "city": "String",
    "location": {
      "type": "Point",
      "coordinates": ["Double (longitude)", "Double (latitude)"]
    }
  },
  "destination": {
    "address": "String",
    "neighborhood": "String | null",
    "city": "String",
    "location": {
      "type": "Point",
      "coordinates": ["Double (longitude)", "Double (latitude)"]
    }
  },
  "departureTime": "String (HH:mm)",
  "estimatedArrivalTime": "String (HH:mm)",
  "weekDays": ["MONDAY | TUESDAY | WEDNESDAY | THURSDAY | FRIDAY | SATURDAY | SUNDAY"],
  "recurrence": "ONCE | WEEKLY",
  "date": "String (yyyy-MM-dd) | null",
  "maxDetourKm": "Double",
  "toll": "Decimal128 | null",
  "parking": "Decimal128 | null",
  "totalSeats": "Integer | null",
  "availableSeats": "Integer | null",
  "routeDistanceKm": "Double | null",
  "encodedPolyline": "String | null",
  "status": "ACTIVE | PAUSED | INACTIVE",
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `userId` | Simples | Listar trajetos do usuário |
| `type` | Simples | Filtrar oferta vs procura no matching |
| `status` | Simples | Somente trajetos ACTIVE entram no matching |
| `origin.location` | Geoespacial 2dsphere | Busca por proximidade de origem (GeoJSON Point) |
| `destination.location` | Geoespacial 2dsphere | Busca por proximidade de destino (GeoJSON Point) |
| `weekDays` | Simples | Filtro de dias compatíveis no matching |

### Recorrência (carona de um dia só ou semanal)

- **`ONCE`** (padrão no app): o trajeto vale só em `date`. A data não pode ter passado, e `weekDays` vira o dia da semana dela.
  - A carona de um dia só termina junto com a viagem dela: quando a viagem é concluída ou cancelada, o trajeto fica `INACTIVE`. Ele também some da lista do usuário e da busca depois que a data passa.
- **`WEEKLY`** ("Repetir toda semana"): o trajeto vale toda semana nos `weekDays`, e `date` fica nulo.
  - A carona semanal continua ativa até o motorista cancelar. Cada semana em que alguém pede vaga gera uma viagem nova (uma por data), e as viagens concluídas vão para o histórico.
- Documentos antigos, sem o campo, valem como `WEEKLY`. Se o app não envia `recurrence`, o backend também usa `WEEKLY`.
- **Matching:** um trajeto `ONCE` com a data já passada sai da busca. Se o passageiro procura num dia específico, só aparecem motoristas que saem nesse dia.
- **Duplicidade:** é duplicado quando tudo bate: mesmo usuário, tipo, partida, destino, horário e recorrência, no mesmo dia. No `ONCE`, a mesma data; no `WEEKLY`, um dia da semana em comum (duas semanais que compartilham a segunda-feira dividiriam os passageiros da segunda). Vale entre os trajetos vivos (`ACTIVE` ou `PAUSED`), e a criação é recusada com `ROUTE_ALREADY_EXISTS` ("Você já tem uma carona igual a essa"). Se qualquer item mudar (outra data, nenhum dia em comum, outro horário, outra partida ou destino, ou outra recorrência), é outra carona.
- **Caronas que se sobrepõem:** um motorista não pode ter duas caronas vivas (`OFFER`) no mesmo dia com as viagens se sobrepondo, seja qual for o endereço (ele não faz as duas, e os passageiros se dividiriam). Cada viagem vai de `departureTime` até `estimatedArrivalTime` (o tempo da rota do Google), com no mínimo 15 min. Uma pode sair exatamente quando a outra chega (ex.: a volta). "Mesmo dia" é a mesma data, um dia da semana em comum, ou a data de uma de um dia só caindo num dos dias da semanal. A criação é recusada com `ROUTE_ALREADY_EXISTS` ("Você já tem uma carona das 07:00 às 07:40 nesse dia..."). Pedidos de passageiro (`SEARCH`) não entram nessa regra.

---

## 4. Coleção `rides`

**Módulo:** `modules/ride`
**Descrição:** Coleção central do sistema. Representa uma viagem concreta — desde a confirmação até a conclusão. Os participantes são subdocumentos embutidos para garantir atomicidade nas operações de vaga. Os dados de veículo são copiados como snapshot imutável no momento da criação.

```json
{
  "_id": "ObjectId (String)",
  "driverId": "String (ref: users._id)",
  "sequentialNumber": "Integer (sequencial por motorista)",
  "version": "Long (optimistic locking)",
  "vehicleId": "String (ref: vehicles._id)",
  "vehicleSnapshot": {
    "model": "String",
    "licensePlate": "String",
    "type": "String",
    "color": "String"
  },
  "driverRouteId": "String (ref: routes._id)",
  "origin": {
    "address": "String",
    "location": {
      "type": "Point",
      "coordinates": ["Double (longitude)", "Double (latitude)"]
    }
  },
  "destination": {
    "address": "String",
    "location": {
      "type": "Point",
      "coordinates": ["Double (longitude)", "Double (latitude)"]
    }
  },
  "departureDateTime": "ISODate",
  "estimatedArrivalDateTime": "ISODate",
  "directArrivalDateTime": "ISODate | null",
  "completedAt": "ISODate | null",
  "weekDays": ["MONDAY | TUESDAY | ..."],
  "totalSeats": "Integer",
  "availableSeats": "Integer",
  "status": "AWAITING_CONFIRMATION | CONFIRMED | IN_PROGRESS | COMPLETED | CANCELLED",
  "cancellationReason": "String | null",
  "cancelledBy": "String | null (ref: users._id)",
  "chatRoomId": "String | null (ref. sala no Firebase Realtime Database)",
  "costs": {
    "routeDistanceKm": "Double",
    "totalDistanceWithDetoursKm": "Double",
    "energySourceUsed": "GASOLINE | ETHANOL | DIESEL | ELECTRIC",
    "averageConsumptionUsed": "Double",
    "energyPriceUsed": "Decimal128",
    "energyCost": "Decimal128",
    "toll": "Decimal128",
    "parking": "Decimal128",
    "totalCost": "Decimal128",
    "totalShared": "Decimal128"
  },
  "participants": [
    {
      "userId": "String (ref: users._id)",
      "role": "DRIVER | PASSENGER",
      "passengerRouteId": "String | null (ref: routes._id)",
      "boardingLocation": {
        "address": "String",
        "location": {
          "type": "Point",
          "coordinates": ["Double (longitude)", "Double (latitude)"]
        }
      },
      "dropOffLocation": {
        "address": "String",
        "location": {
          "type": "Point",
          "coordinates": ["Double (longitude)", "Double (latitude)"]
        }
      },
      "detourGeneratedKm": "Double | null",
      "status": "REQUESTED | REJECTED | CONFIRMED | CANCELLED | NO_SHOW | COMPLETED | EXPIRED",
      "requestedAt": "ISODate",
      "respondedAt": "ISODate | null",
      "rejectionReason": "String | null",
      "cancellationReason": "String | null",
      "lateCancellation": "Boolean (default: false)",
      "estimatedAmount": "Decimal128",
      "maxAmount": "Decimal128",
      "finalAmount": "Decimal128 | null",
      "paymentStatus": "PENDING | AWAITING_DRIVER_CONFIRMATION | PAID | NOT_APPLICABLE",
      "paymentConfirmedByDriver": "Boolean",
      "requestedAmount": "Decimal128 | null (valor combinado no chat)",
      "paymentRequestedAt": "ISODate | null",
      "paidAt": "ISODate | null",
      "confirmedAt": "ISODate",
      "cancelledAt": "ISODate | null",
      "updatedAt": "ISODate"
    }
  ],
  "itinerary": [
    {
      "type": "PICKUP | DROPOFF",
      "passengerId": "String (ref: users._id)",
      "place": { "address": "String", "neighborhood": "String | null", "city": "String", "location": { "type": "Point", "coordinates": ["Double (longitude)", "Double (latitude)"] } }
    }
  ],
  "itineraryDistanceKm": "Double | null",
  "itineraryPolyline": "String | null",
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

### Chegada estimada

A chegada não usa mais um valor fixo enviado pelo app: ela sai do tempo de carro calculado pela Routes API (`routes.duration`).
- **Prévia no app:** antes de publicar ou pedir a carona, `GET /api/v1/routes/estimate?originLat=&originLng=&destinationLat=&destinationLng=` devolve a distância, o tempo de carro e o desenho do caminho, sem gravar nada. O app mostra o tempo na etapa de horário ("~25 min de viagem · 12,4 km").
- **Trajeto:** `estimatedArrivalTime` = `departureTime` + o tempo da rota entre a origem e o destino. Sem o Google, o tempo é estimado pela distância, a 30 km/h de média na cidade.
- **Viagem:** quando o itinerário é recalculado, `estimatedArrivalDateTime` = `departureDateTime` + o tempo da rota passando pelas paradas, porque buscar e deixar os passageiros leva mais tempo. Quando o último passageiro sai, volta a valer `directArrivalDateTime`, a chegada pela rota direta guardada na criação da viagem.

### Histórico de viagens (RF59)

`GET /api/v1/rides/history?role=&from=&to=` lista as viagens terminadas (`COMPLETED` ou `CANCELLED`) do usuário, da mais recente para a mais antiga. Não há coleção própria: as viagens já guardam o trajeto, o carro, os custos e os pagamentos.
- **Passageiro:** vê o próprio embarque e desembarque, a própria parte estimada (`estimatedAmount`), o valor pago e a situação do pagamento. Pedidos recusados, expirados ou nunca aceitos não entram, porque ele não fez a viagem.
- **Motorista:** vê o trajeto da carona, o custo total (`costs.totalCost`) e quanto recebeu (a soma dos pagamentos confirmados).
- **Pessoas:** o motorista mais os passageiros que concluíram a viagem.
- **Filtros:** `role` (`DRIVER` ou `PASSENGER`) e o intervalo de datas da saída (`from` e `to`, inclusive).

### Itinerário (rota com as paradas)

O motorista vai até cada passageiro: `itinerary` guarda as paradas na ordem em que ele busca e deixa os passageiros confirmados.
- **Quando muda:** é recalculado quando um passageiro é aceito (`AcceptPassengerUseCase`), quando um passageiro confirmado sai (`LeaveRideUseCase`) e quando o motorista remove um passageiro confirmado (`RejectPassengerUseCase`). Pedidos de vaga só existem antes de a viagem começar, então o itinerário não muda depois do `IN_PROGRESS`.
- **Ordem** (`RideItineraryPlanner`): o motorista sai da origem e busca todos os confirmados, sempre o embarque mais próximo de onde ele está. Depois deixa cada um, sempre o desembarque mais próximo, e segue para o destino. Buscar todos antes de deixar garante que ninguém é deixado antes de embarcar.
- **Rota:** `itineraryDistanceKm` e `itineraryPolyline` vêm da Routes API do Google, com as paradas como `intermediates`. Sem o Google, a distância é a soma das linhas retas entre os pontos e a polyline fica `null`. Uma falha no cálculo nunca impede o aceite nem a saída.
- **Privacidade:** os pontos de embarque e desembarque são endereços dos passageiros. Na API, o motorista recebe todas as paradas, a distância e a polyline; cada passageiro recebe só as próprias paradas, sem a polyline.

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `driverId` + `sequentialNumber` | Composto Único | Referência amigável por motorista (Ride #042). Sequencial por motorista — não vaza volume total da plataforma |
| `driverRouteId` + `departureDateTime` | Composto Único Parcial (`status in [AWAITING_CONFIRMATION, CONFIRMED, IN_PROGRESS, COMPLETED]`) | Garante que o motorista tenha apenas uma viagem aberta por data/horário em um trajeto |
| `driverId` | Simples | Listar viagens do motorista |
| `participants.userId` | Simples | Listar viagens de um passageiro |
| `status` | Simples | Filtrar viagens ativas, concluídas, etc. |
| `departureDateTime` | Simples | Ordenação e filtro por data |
| `driverRouteId` | Simples | Vincular viagens a um trajeto recorrente |

### Regras de Negócio Críticas

> **Onde o cálculo roda:** o cálculo e o rateio dos valores (`estimatedAmount`, `maxAmount`, `finalAmount`, divisão entre participantes e geração do payload PIX) são feitos no **Servidor Java de sockets** (`app/server`), acionado pelo backend via `SocketClient`. O backend reúne os dados dos participantes, pede o cálculo ao servidor e grava os valores resultantes nos campos abaixo. As regras a seguir descrevem o resultado esperado desse cálculo, independentemente de onde ele é executado.

- `availableSeats` é decrementado ao confirmar participante e incrementado ao cancelar — operação atômica obrigatória.
- **`estimatedAmount` (RF34, RF36):** valor estimado atual de cada participante, exibido durante a vida da carona. É **recalculado sempre que há entrada, cancelamento ou ausência** de participantes — o custo é dividido entre os participantes efetivos no momento. Nunca ultrapassa o `maxAmount`.
- `maxAmount` é o **teto de proteção do passageiro** (RF35/RF37): corresponde à **metade do custo da rota com o desvio do passageiro**, calculado e gravado no momento da confirmação. O `finalAmount` **nunca pode superá-lo**, mesmo que outros participantes cancelem/faltem ou que o custo mude depois.
- `finalAmount` só é calculado e gravado quando `status = COMPLETED` (RF38), com base nos participantes efetivos.
- **Ausência (RF23, RF24):** quando o motorista marca um passageiro como `NO_SHOW`, a vaga é liberada, o ausente **não é cobrado** (`finalAmount = 0`, `paymentStatus = NOT_APPLICABLE`), é **desconsiderado dos indicadores**, e o `estimatedAmount`/`finalAmount` dos demais participantes efetivos é recalculado.
- `totalCost / qtdParticipantesEfetivos = valorPorPessoa`. Diferença de centavos vai para o motorista.
- Se o passageiro cancelar com menos de 2h de antecedência (ou depois das 20h da véspera, no caso de vaga reservada recorrente), `lateCancellation = true` e a viagem é cobrada como se tivesse ido.
- Se entrar passageiro avulso, o valor daquele dia cai para todos — cada um paga o `finalAmount` real da viagem.

### Pagamento — sempre por viagem

- Todo pagamento no Converge é **por viagem**, via PIX, incluindo as viagens de passageiros com vínculo recorrente. Não há cobrança mensal consolidada nem valor fixo pré-combinado.
- A chave PIX do motorista (`users.pixKey`) e o valor de cada participante são exibidos ao fim da viagem. O passageiro paga e o motorista confirma o recebimento (`participants[].paymentConfirmedByDriver`).
- O prazo para pagar cada viagem é de **3 dias**; depois disso a cobrança vira dívida vencida (ver regra de inadimplência na coleção `users`).

### Cobrança combinada no chat (MVP)

Depois que o motorista aceita o passageiro, os dois combinam o valor no chat. A cobrança segue estes passos:

1. **Solicitar pagamento** — `POST /rides/{rideId}/passengers/{passengerId}/payment/request` com `{ "amount": "12.50" }`. Só o motorista da viagem, com chave PIX cadastrada. Grava `requestedAmount` e `paymentRequestedAt` e deixa `paymentStatus = PENDING`. O valor não pode passar do `maxAmount` quando este estiver definido. Pode ser repetido para corrigir o valor enquanto não estiver pago.
2. **Pix Copia e Cola** — gerado sob demanda pelo **Servidor Java de sockets** (`PixPayloadHandler`, padrão BR Code com CRC16) a partir da chave do motorista e do `requestedAmount`. Nada é armazenado; se o servidor estiver fora do ar, o backend executa o mesmo handler localmente.
3. **Passageiro já pagou** — `POST /rides/{rideId}/passengers/{passengerId}/payment/confirm`. Só o motorista. Grava `paymentStatus = PAID`, `paymentConfirmedByDriver = true` e `paidAt`.
4. **Consulta** — `GET /rides/{rideId}/passengers/{passengerId}/payment`. Só o motorista e o próprio passageiro. O código PIX só vem enquanto a cobrança está em aberto.

A cobrança é aceita com a viagem `CONFIRMED`, `IN_PROGRESS` ou `COMPLETED`, sempre de um passageiro com vaga (`CONFIRMED` ou `COMPLETED`). Passageiro ausente (`NO_SHOW`) não é cobrado.

### Geração do PIX Copia e Cola

> **MVP:** a cobrança segue o fluxo da seção anterior ("Cobrança combinada no chat"). O payload usa o `requestedAmount` combinado no chat, pode ser gerado a partir de `CONFIRMED` e é devolvido em `GET /rides/{rideId}/passengers/{passengerId}/payment`. O uso do `finalAmount` ao concluir a viagem, descrito abaixo, depende do cálculo de rateio no servidor de sockets e fica como evolução.

O backend monta o payload sob demanda (padrão BR Code do Banco Central, CRC16 incluso). **Nada é armazenado**: o payload é derivado de dados que já existem.

| Campo do payload | Origem |
|---|---|
| Chave PIX (campo 26) | `users.pixKey` do motorista, normalizada |
| Valor (campo 54) | `participants[].finalAmount` do passageiro, com 2 casas decimais |
| Nome do recebedor (campo 59) | `users.name` do motorista, sem acento, maiúsculas, máx. 25 caracteres |
| Cidade (campo 60) | Valor fixo ou cidade da origem da rota, máx. 15 caracteres |
| Identificador (campo 62) | `***` (PIX estático não tem txid útil) |

- **Endpoint:** `GET /rides/{rideId}/payment-qr` devolve a string; o app desenha o QR e oferece o botão de copiar.
- **Quando gera:** viagem `COMPLETED` e passageiro com `finalAmount > 0`. Para `NO_SHOW` e `NOT_APPLICABLE` não há cobrança, logo não há QR.
- **Autorização:** só participantes da viagem (`CONFIRMED` ou `COMPLETED`) acessam, porque a chave PIX do motorista é dado pessoal.
- **Valor travado:** o app do banco exibe o valor fixo e o passageiro não consegue alterá-lo.
- **Vínculo cobrança ↔ viagem:** como o PIX estático não devolve txid, o vínculo é feito pelo banco de dados (`rideId` + participante), e o `PAID` só acontece quando o motorista confirma.
- **Validação:** o formato foi testado escaneando um payload real no app de um banco; um teste unitário fixa o CRC16 para evitar regressão.

### Vínculo recorrente (vaga reservada)

- O vínculo recorrente entre um passageiro e um motorista (coleção `reserved_seats`) **não altera a forma de pagamento** — apenas garante vaga prioritária nos dias combinados. Cada viagem continua sendo paga individualmente.
- **Ambos os lados podem propor** o vínculo — o campo `proposedBy` aceita `PASSENGER` ou `DRIVER`.

---

## 5. Coleção `reviews`

**Módulo:** `modules/review`
**Descrição:** Avaliações registradas após a conclusão de uma viagem, **somente na relação motorista ↔ passageiro**. Passageiros **não avaliam outros passageiros** — apenas o motorista avalia cada passageiro e cada passageiro avalia o motorista. Cada par (avaliador, avaliado) registra no máximo uma avaliação por viagem.

```json
{
  "_id": "ObjectId (String)",
  "rideId": "String (ref: rides._id)",
  "reviewerId": "String (ref: users._id)",
  "reviewedId": "String (ref: users._id)",
  "reviewedRole": "DRIVER | PASSENGER",
  "rating": "Integer (1 a 5)",
  "comment": "String | null",
  "createdAt": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `rideId` + `reviewerId` + `reviewedId` | Composto Único | Impede avaliação duplicada do mesmo par na mesma viagem |
| `reviewedId` + `reviewedRole` | Composto | Buscar avaliações recebidas por papel (base da reputação) |
| `reviewerId` | Simples | Verificar se usuário já avaliou determinada viagem |

### Regra de validação

- Só é permitido avaliar se a viagem está `COMPLETED`, se avaliador e avaliado **concluíram** a mesma viagem (participante com `status = COMPLETED`) **e** um deles é `DRIVER` e o outro é `PASSENGER`. Avaliação `PASSENGER → PASSENGER` é rejeitada no service. Passageiro ausente (`NO_SHOW`), recusado, expirado, cancelado ou que saiu da viagem não avalia nem é avaliado.
- O `reviewedRole` é calculado pelo servidor a partir da viagem; o cliente não envia.
- A avaliação é imutável (sem edição nem exclusão) e não tem prazo no MVP.
- **Salvar a avaliação atualiza a reputação do avaliado.** Depois do `save`, o `CreateReviewUseCase` relê todas as avaliações recebidas no papel (índice `reviewedId + reviewedRole`), calcula a média e o total e os grava em `users.reputation.driver` (se `reviewedRole = DRIVER`) ou `users.reputation.passenger` (se `PASSENGER`), junto com o `level` e o `calculatedAt` (ver coleção `users`, "Reputação"). Cada recálculo parte de todas as avaliações, nunca soma de forma incremental.
- Se a gravação da reputação falhar, a avaliação **já salva continua valendo** (o erro só vai para o log). `REVIEW_ALREADY_EXISTS` e recusas de regra não chamam a reputação.
- A média exibida em telas como o matching continua sendo calculada na hora a partir desta coleção.
- Duas avaliações iguais enviadas ao mesmo tempo: o índice único barra a segunda, e a API responde `REVIEW_ALREADY_EXISTS`.

---

## 6. Coleção `reports`

**Módulo:** `modules/security`
**Descrição:** Denúncias realizadas por usuários contra outros. Mantém histórico completo para moderação.

> **Estado atual:** o **registro** e a **consulta das próprias denúncias** estão implementados (RF45, RF25). A **moderação** (análise, resolução, arquivamento) **não existe**: não há papel de moderador. Os campos `resolution` e `reviewedBy` e os status `UNDER_REVIEW`, `RESOLVED` e `ARCHIVED` existem no modelo, mas nenhum endpoint ou rotina os escreve. Por isso as denúncias nascem `PENDING` e ficam assim, e `substantiatedReports` das janelas de reputação continua em 0.

```json
{
  "_id": "ObjectId (String)",
  "reporterId": "String (ref: users._id)",
  "reportedId": "String (ref: users._id)",
  "rideId": "String | null (ref: rides._id)",
  "reason": "LATE_NO_SHOW | INAPPROPRIATE_CONDUCT | IMPROPER_CHARGE | OTHER",
  "description": "String | null",
  "status": "PENDING | UNDER_REVIEW | RESOLVED | ARCHIVED",
  "resolution": "String | null",
  "reviewedBy": "String | null (ref: users._id)",
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `reportedId` | Simples | Verificar histórico de denúncias de um usuário |
| `reporterId` | Simples | Listar denúncias feitas pelo usuário |
| `status` | Simples | Fila de moderação (PENDING, UNDER_REVIEW) |
| `rideId` | Simples | Denúncias relacionadas a uma viagem específica |

### Endpoints (módulo `security`)

| Método e rota | O que faz |
|---|---|
| `POST /api/v1/reports` | Registra uma denúncia. Corpo: `reportedId`, `reason`, `description` (opcional) e `rideId` (opcional). Devolve a denúncia criada |
| `GET /api/v1/reports/mine` | Lista as denúncias feitas pelo usuário logado, da mais recente para a mais antiga |

- Quem denuncia (`reporterId`) é sempre o **usuário logado**, nunca vem do corpo.
- A resposta traz `id`, `reportedId`, `rideId`, `reason`, `description`, `status` e `createdAt`. `resolution`, `reviewedBy` e `reporterId` **não são expostos**.

### Regras de Negócio

- `reportedId` diferente do denunciante (`BAD_REQUEST`) e o denunciado precisa existir (`USER_NOT_FOUND`). O `status` nasce `PENDING`.
- `LATE_NO_SHOW` (RF25) **exige** `rideId`. Para os outros motivos, `rideId` é opcional.
- Quando `rideId` vem, o denunciante **e** o denunciado precisam ter participado da viagem (`NOT_ALLOWED` se não); viagem inexistente dá `RIDE_NOT_FOUND`. "Participou" vale para o motorista e para passageiros que tiveram vaga em qualquer status da viagem (inclui ausente e cancelado) e **exclui** pedidos `REQUESTED`, `REJECTED` e `EXPIRED`. É uma leitura própria do módulo `ride` (`GetRideParticipantsUseCase`), porque a leitura de participantes concluídos barraria o motorista que denuncia um passageiro ausente. A regra **não** exige que o denunciado esteja `NO_SHOW`.
- `OTHER` exige `description`. A `description` tem no máximo **500 caracteres** (limite definido no código, validado no DTO e na entidade).
- Denúncia idêntica ainda `PENDING` (mesmo denunciante, denunciado, `rideId` e `reason`) é recusada com `BAD_REQUEST`. Não há índice único para isso: a checagem é uma consulta. Sem `rideId`, a consulta casa também denúncias sem o campo.
- `createdAt` e `updatedAt` são preenchidos pela auditoria do Spring Data (`Instant`).

### Fora do que está implementado

- Moderação: mudança de status, `resolution`, `reviewedBy`, fila de análise e `REPORT_RESOLVED` em `audit_logs`.
- Auditoria do registro da denúncia: nenhuma ação de `AuditAction` foi usada.
- Efeito automático sobre a reputação (`substantiatedReports`).
- Teste com MongoDB real: as consultas derivadas do Spring Data não foram executadas contra um banco nos testes (a suíte do projeto é unitária).

---

## 7. Coleção `blocks`

**Módulo:** `modules/security`
**Descrição:** Registros de bloqueio entre usuários. Usada para filtrar resultados de matching, aceites de carona e chat. O bloqueio é bidirecional para fins de interação.
- **Implementado (RF46, RF47):** as rotas para bloquear, desbloquear e listar, e o uso do bloqueio na busca (matching), no **pedido de vaga** e no **aceite** do passageiro. Tudo lê a coleção pela porta `BlockAccess`, nos dois sentidos.
- **Não coberto pelo backend:** o chat depois do aceite (ver "Onde o bloqueio vale").

```json
{
  "_id": "ObjectId (String)",
  "userId": "String (ref: users._id)",
  "blockedUserId": "String (ref: users._id)",
  "createdAt": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `userId` + `blockedUserId` | Composto Único | Impede bloqueio duplicado |
| `userId` | Simples | Listar quem o usuário bloqueou |
| `blockedUserId` | Simples | Verificar se um usuário está bloqueado (usado no matching) |

### Endpoints (módulo `security`)

| Método e rota | O que faz |
|---|---|
| `POST /api/v1/blocks` | Bloqueia um usuário. Corpo: `blockedUserId`. Resposta `201` com o bloqueio |
| `DELETE /api/v1/blocks/{blockedUserId}` | Desfaz o bloqueio feito pelo usuário logado. Resposta `204` |
| `GET /api/v1/blocks` | Lista os usuários que o logado bloqueou, do mais recente para o mais antigo. Cada item traz `id`, `blockedUserId`, `blockedUserName` (pode vir nulo se o usuário não existe mais) e `createdAt` |

- Quem bloqueia (`userId`) é sempre o **usuário logado**, nunca vem do corpo. O bloqueado precisa existir (`USER_NOT_FOUND`) e não pode ser o próprio usuário (`BAD_REQUEST`).
- `POST` é **idempotente**: se o bloqueio já existe, devolve o existente (também `201`). Uma corrida em que o índice único barra a segunda gravação (`DuplicateKeyException`) é tratada devolvendo o bloqueio já salvo.
- `DELETE` é **idempotente**: responde `204` mesmo sem bloqueio.
- `createdAt` é preenchido pela auditoria do Spring Data (`@EnableMongoAuditing` está ligado em `MongoConfig`).

### Onde o bloqueio vale

| Fluxo | Situação |
|---|---|
| Matching (busca de motoristas e passageiros) | Aplicado, nos dois sentidos |
| Pedido de vaga (`RequestRideSeatUseCase`) | Aplicado. Bloqueio entre passageiro e motorista dá `NOT_ALLOWED` ("Não é possível pedir vaga nesta carona."), antes de qualquer gravação |
| Aceite do passageiro (`AcceptPassengerUseCase`) | Aplicado. `NOT_ALLOWED` ("Não é possível aceitar este passageiro."), antes de gravar o aceite |
| Chat criado no aceite | O backend não cria a sala: só preenche `rides.chatRoomId` (`ride_<rideId>`) quando o passageiro é aceito. Como o aceite é barrado, a sala **não é liberada** para quem já está bloqueado |
| Chat depois do aceite | **Não coberto pelo backend.** As mensagens passam direto pelo Firebase Realtime Database, sem a API no meio. O app desabilita o envio no chat com um usuário bloqueado |
| Viagem já aceita, saída da viagem, cancelamentos | Não aplicado |

- **Desbloquear remove só o bloqueio do usuário logado.** Se o outro lado também bloqueou, o bloqueio dele continua e a interação segue barrada.
- Não há auditoria (`USER_BLOCKED` de `AuditAction` não é usado pelas rotas).

---

## 8. Coleção `reserved_seats`

> **Ainda sem código:** não existe entidade (`@Document`), repositório nem endpoint para esta coleção no backend. A descrição abaixo é o desenho previsto, ainda não implementado.

**Módulo:** `modules/ride`
**Descrição:** Registra a parceria habitual entre um passageiro e um motorista — a **vaga reservada recorrente**. Criado por opt-in manual: **qualquer um dos dois propõe** (`proposedBy`) e o outro aceita. Garante ao passageiro uma vaga prioritária no trajeto do motorista nos dias combinados. **Não envolve cobrança mensal nem valor fixo** — cada viagem efetivada é paga individualmente por PIX, como qualquer viagem avulsa (ver coleção `rides`). O vínculo opera em modelo **opt-out**: o sistema assume que o passageiro vai nos dias do vínculo e reserva a vaga automaticamente; o passageiro só age quando **não** vai.

```json
{
  "_id": "ObjectId (String)",
  "passengerId": "String (ref: users._id)",
  "driverId": "String (ref: users._id)",
  "driverRouteId": "String (ref: routes._id)",
  "passengerRouteId": "String (ref: routes._id)",
  "status": "PROPOSED | ACTIVE | TERMINATED",
  "proposedBy": "PASSENGER | DRIVER",
  "terminationReason": "String | null",
  "weekDays": ["MONDAY | TUESDAY | WEDNESDAY | THURSDAY | FRIDAY | SATURDAY | SUNDAY"],
  "skippedDates": ["ISODate (datas em que o passageiro avisou que não vai)"],
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `passengerId` + `driverRouteId` | Composto Único | Um vínculo por passageiro por trajeto do motorista |
| `passengerId` + `status` | Composto | Listar vínculos ativos do passageiro |
| `driverId` + `status` | Composto | Listar vínculos ativos do motorista |

### Regras de Negócio

- **Criação:** criado com `status = PROPOSED`. O outro lado aceita para mudar para `ACTIVE`. Ambos podem propor.
- **Vaga reservada (opt-out):** nos dias listados em `weekDays`, o sistema reserva a vaga do passageiro automaticamente — ele tem prioridade e não precisa confirmar nada.
- **Avisar que não vai:** o passageiro clica em "Não vou" para um dia específico. A data entra em `skippedDates` e a vaga daquele dia é **liberada para o matching** (outro passageiro pode pegar).
- **Horário limite:** cancelar até **20h da véspera** libera a vaga sem penalidade. Depois disso, cai na regra de cancelamento tardio da viagem (`rides.participants[].lateCancellation`), igual a qualquer passageiro.
- **Pagamento:** cada viagem efetivada do vínculo é paga individualmente por PIX, igual a qualquer viagem avulsa. A inadimplência segue a regra geral por viagem (ver coleção `users`).
- **Encerramento:** qualquer um dos dois pode encerrar (`status = TERMINATED`), com `terminationReason` opcional.
- **MVP:** o passageiro cancela um dia por vez. Cancelar um intervalo de datas de uma vez fica como evolução futura (não muda a estrutura — apenas adiciona várias datas a `skippedDates`).

---

## 9. Coleção `matchings`

**Módulo:** `modules/matching`
**Descrição:** Cache dos resultados do algoritmo de compatibilidade. Invalidado quando trajetos são alterados. TTL garante limpeza mesmo em falhas de invalidação.

> **Onde o cálculo roda:** o módulo `matching` do backend apenas orquestra (reúne os trajetos candidatos, aciona o cálculo e grava o resultado nesta coleção). O **cálculo de compatibilidade** (`compatibilityScore`, `calculatedDetourKm`, `scheduleDifferenceMin`, `estimatedCostPerPassenger`) é feito no **Servidor Java de sockets** (`app/server`), acionado via `SocketClient`. Esta coleção guarda apenas o resultado já calculado.

```json
{
  "_id": "ObjectId (String)",
  "passengerRouteId": "String (ref: routes._id)",
  "driverRouteId": "String (ref: routes._id)",
  "passengerId": "String (ref: users._id)",
  "driverId": "String (ref: users._id)",
  "compatibilityScore": "Double (0.0 a 1.0)",
  "calculatedDetourKm": "Double",
  "scheduleDifferenceMin": "Integer",
  "compatibleDays": ["MONDAY | TUESDAY | ..."],
  "estimatedCostPerPassenger": "Decimal128",
  "valid": "Boolean",
  "expiresAt": "ISODate | null",
  "createdAt": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `passengerRouteId` | Simples | Buscar combinações para o passageiro |
| `driverRouteId` | Simples | Buscar passageiros compatíveis com motorista |
| `passengerId` + `driverId` | Composto | Verificar se combinação específica existe |
| `valid` + `compatibilityScore` | Composto | Listar combinações válidas ordenadas por score |
| `expiresAt` | Simples (TTL) | Limpeza automática de resultados obsoletos |

### Como o cálculo funciona

O cálculo fica no `MatchRoutesHandler` (`app/server`, pacote `handler/matching`). O backend chama esse handler pelo `SocketMatchingGateway`, e quando o servidor de sockets está fora do ar ele roda o mesmo handler localmente. O cálculo compara um trajeto com N candidatos do tipo oposto.

`GET /api/v1/matchings?routeId=` lista as combinações de um trajeto do usuário, da maior para a menor `compatibilityScore`. O tipo do trajeto decide o lado da busca:
- **procura (`SEARCH`):** compara a procura do passageiro com as ofertas e lista os motoristas;
- **oferta (`OFFER`):** compara a oferta do motorista com as procuras e lista os passageiros, o "N passageiros × 1 motorista".

**Candidatos:** vêm de uma consulta geoespacial nos índices `2dsphere` (`$geoWithin` + `$centerSphere`). Entram os trajetos `ACTIVE` do tipo oposto com pelo menos um dia da semana em comum, desde que:
- a origem esteja dentro do raio em volta da origem do trajeto pesquisado;
- o destino esteja dentro do raio em volta do destino do trajeto pesquisado.

**Raio** (`MatchingSearchRadius`): para o desvio caber no `maxDetourKm`, a origem e o destino do passageiro ficam a no máximo (distância em linha reta da rota do motorista + `maxDetourKm`) dos pontos do motorista.
- **Oferta:** o raio é `routeDistanceKm + maxDetourKm`, que é um limite seguro, porque a distância pela rua nunca é menor que a em linha reta.
- **Procura:** cada motorista tem o próprio desvio máximo, então vale o teto.
- **Teto:** os dois lados respeitam o teto `converge.matching.max-candidate-radius-km` (padrão 50 km), para o par aparecer igual para os dois.
- **Filtro exato:** o raio só pré-seleciona; o desvio exato continua no servidor de sockets.

Ficam de fora:
- o próprio usuário;
- quem tem bloqueio ativo com o usuário, nos dois sentidos (coleção `blocks`, RF47);
- motoristas que não podem oferecer carona: conta inativa, vínculo não verificado, bloqueado por dívida (`paymentStanding = BLOCKED` ou `isDefaulter = true`, RF67), CNH não verificada ou vencida (RF63) ou sem chave PIX (RF72);
- passageiros que não podem pedir vaga: conta inativa, vínculo não verificado ou bloqueado por dívida (RF67);
- pares já recusados numa viagem em aberto.

**Filtros eliminatórios** (RF14, RF15). O par é descartado quando:

| Filtro | Regra |
|---|---|
| Dias | Nenhum dia da semana em comum |
| Vagas | `availableSeats` do motorista menor que 1 |
| Horário | Saídas com mais de **30 min** de diferença (considera a virada da meia-noite) |
| Embarque | Embarque do passageiro a mais de `maxDetourKm` do caminho do motorista (a rota pelas ruas, `encodedPolyline`; sem ela, a linha reta origem → destino) |
| Destino | Destino do passageiro a mais de `maxDetourKm` do destino do motorista |

**Distância até o caminho:** é a menor distância entre o embarque do passageiro e cada trecho da rota do motorista. O app chama o `maxDetourKm` de "Buscar passageiros a até X km" (padrão 3 km).

**Desvio** (RF31): não decide mais quem aparece; entra no custo estimado. É calculado em linha reta (Haversine), com `m` = motorista e `p` = passageiro:

`d(origem m → origem p) + d(origem p → destino p) + d(destino p → destino m) − d(origem m → destino m)`

O resultado vai para `calculatedDetourKm` e é o candidato natural ao `participants[].detourGeneratedKm` no aceite.

**Índice de compatibilidade** (RF16): fica entre 0 e 1 e é a combinação ponderada abaixo. Os pesos somam 1.

| Componente | Peso | Fórmula |
|---|---|---|
| Embarque | 0,4 | `1 − distância do embarque ao caminho / maxDetourKm` |
| Horário | 0,3 | `1 − diferença de saída / 30 min` |
| Dias | 0,2 | dias em comum ÷ dias do passageiro |
| Destino | 0,1 | `1 − d(destino p, destino m) / maxDetourKm` |

**Ordenação** (RF17): do maior para o menor índice. No empate, o critério passa a ser:
1. o menor desvio;
2. a menor diferença de horário;
3. o id do trajeto.

Assim o resultado não depende da ordem em que os candidatos chegaram.

**`estimatedCostPerPassenger`**: é `((routeDistanceKm + desvio) × preço da energia ÷ consumo + pedágio + estacionamento) ÷ (passageiros já confirmados + motorista + este passageiro)`, com 2 casas e arredondamento `HALF_EVEN`. A energia é a primeira `fuelSource` do veículo. O campo fica `null` se o veículo não tiver consumo e preço. É só uma estimativa: o valor final segue as regras de `rides`.

**`maxAmount`** (só na resposta da API, não é gravado em `matchings`): é uma **prévia** do teto do passageiro (RF35), calculada como a metade do mesmo custo da rota com o desvio: `((routeDistanceKm + desvio) × preço da energia ÷ consumo + pedágio + estacionamento) ÷ 2`. Representa o pior caso, em que só o motorista e esse passageiro dividem a viagem, e serve para o app avisar, antes do pedido, até quanto a estimativa pode subir se alguém desistir.

> **Não é o teto gravado.** O pedido de vaga (`POST /rides/request`) não envia esse valor. O `maxAmount` que vale para o passageiro é calculado de novo no **aceite**, pelo `RideCostHandler` do servidor de sockets (acionado pelo `RideCostCalculator`), e gravado no participante da viagem (seção de custos de `rides`). Como o aceite usa os dados do momento (distância da rota e desvio calculados ali), o teto gravado pode ser diferente da prévia mostrada no matching.

**Persistência:** cada busca recalcula o resultado e substitui o que havia para o trajeto pesquisado. O documento expira em 24 h (`expiresAt`). Quando um trajeto é editado ou excluído, o módulo `route` publica `RouteChangedEvent`, e todos os resultados que usam esse trajeto passam a `valid = false`.

**Próxima data:** a resposta da API traz `nextRideDate`, que é o primeiro dia em comum a partir de hoje, em America/Sao_Paulo. Se o motorista já saiu hoje, o dia de hoje é pulado. É essa data que o app manda em `POST /rides/request`.

---

## 10. Coleção `indicators`

**Módulo:** `modules/indicator`
**Descrição:** Indicadores de mobilidade e sustentabilidade por usuário e período (no MVP; o financeiro fica para evolução futura). Atualizados ao concluir uma viagem. Documentos pré-calculados por período (`WEEKLY`, `MONTHLY`, `SEMESTERLY`). O período `TOTAL` foi removido — o acumulado histórico é calculado **somando apenas os documentos `MONTHLY`** sob demanda, evitando o risco de estouro do limite de 16 MB do MongoDB.

```json
{
  "_id": "ObjectId (String)",
  "userId": "String (ref: users._id)",
  "period": "WEEKLY | MONTHLY | SEMESTERLY",
  "reference": "String (ex: '2025-05' para mensal, '2025-S1' para semestral, '2025-W20' para semanal)",
  "ridesAsDriver": "Integer",
  "ridesAsPassenger": "Integer",
  "sharedKm": "Double",
  "carsAvoided": "Integer",
  "co2AvoidedKg": "Double",
  "appliedRideIds": ["String (ref: rides._id — viagens já somadas neste documento)"],
  "updatedAt": "ISODate"
}
```

> **MVP (versão enxuta implementada):** o documento grava apenas mobilidade básica e CO₂ evitado, em campos planos (sem os subdocumentos `mobility`/`sustainability`). `totalRides` (= `ridesAsDriver + ridesAsPassenger`) e `equivalentTrees` (= `co2AvoidedKg / 22`) são **calculados na leitura**, não gravados. O período `ALL_TIME` **não** existe por usuário: o acumulado histórico é a soma dos documentos `MONTHLY`.
>
> **Evolução futura (ainda não implementada):** os subdocumentos `financial { totalPassengerSavings, totalDriverIncome }`, o array `series[]` (pontos para gráfico) e os campos `fuelSavedL`/`energySavedKwh` de sustentabilidade poderão ser adicionados numa versão futura, quando houver dados de pagamento e necessidade de gráfico de evolução.

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `userId` + `period` + `reference` | Composto Único | Um documento por usuário/período/referência |
| `userId` + `period` | Composto | Filtrar por tipo de período e ordenar por referência |

### Estratégia de leitura e acumulado histórico

A tela de Dados (ver wireframes `indicadores` e `indicadores ambientais`) exibe os indicadores com um **filtro de período** — o usuário escolhe entre semana, mês, 3 meses ou semestre. Isso substitui qualquer ideia de scroll infinito.

- **Documentos pré-calculados por período:** o backend mantém documentos `WEEKLY`, `MONTHLY` e `SEMESTERLY` prontos, para resposta imediata ao trocar o filtro.
- **Total histórico:** calculado **somando apenas os documentos `MONTHLY`** quando o usuário solicitar o acumulado. Nunca somar `WEEKLY` + `MONTHLY` + `SEMESTERLY` juntos — eles contêm os mesmos dados em granularidades diferentes e isso causaria contagem duplicada.
- **Paginação:** quando houver muitos meses, o histórico é carregado em páginas (ex: 12 meses por página), com opção de filtrar por intervalo de datas.
- **Sem cache em memória persistente:** o cálculo do acumulado é feito sob demanda na requisição. Como soma no máximo algumas dezenas de documentos mensais pequenos, é leve. Evita o risco de cache crescer indefinidamente e pesar com o tempo.
- **Série para os gráficos:** `GET /api/v1/indicators/me/series?period=WEEKLY|MONTHLY|SEMESTERLY&limit=6` devolve os últimos N períodos (até 12), do mais antigo ao mais recente. Período sem documento vem zerado, para o gráfico não pular a semana ou o mês. O app usa as semanas para "viagens por semana" e os meses para "CO₂ evitado por mês".

---

## 11. Coleção `platform_indicators`

**Módulo:** `modules/indicator`
**Descrição:** Indicadores **globais da plataforma** (não de um usuário específico), para atender ao RF51 ("quantidade de caronas realizadas no total da plataforma") e exibições públicas de impacto coletivo. Um documento por período, agregando os números de todos os usuários. Atualizado **a cada viagem concluída**, com operações atômicas do MongoDB, e **reconciliado a cada 15 minutos** por uma tarefa agendada que completa as viagens que ficaram sem contabilização (ver "Regras de Negócio").

```json
{
  "_id": "ObjectId (String)",
  "period": "WEEKLY | MONTHLY | SEMESTERLY | ALL_TIME",
  "reference": "String (ex: '2025-05', '2025-S1', '2025-W20', 'ALL_TIME')",
  "totalRides": "Integer",
  "totalSharedKm": "Double",
  "totalCarsAvoided": "Integer",
  "totalCo2AvoidedKg": "Double",
  "activeUsers": "Integer",
  "appliedRideIds": ["String (ref: rides._id — só nos períodos WEEKLY, MONTHLY e SEMESTERLY; ausente em ALL_TIME)"],
  "calculatedAt": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `period` + `reference` | Composto Único | Um documento por período/referência global |

### Regras de Negócio

- Diferente de `indicators` (por usuário), aqui o `ALL_TIME` **é um documento armazenado** — como é um único documento global e pequeno (só totais, sem array `series`), não há risco do limite de 16 MB.
- Atualizado ao concluir cada viagem, sempre com operações atômicas do MongoDB, sem ler e regravar o documento: `$inc` para somar, e `$max` para os usuários ativos e para o `ALL_TIME`, que é a soma dos semestres. Assim, viagens concluídas ao mesmo tempo não perdem somas.
- **Uma viagem é somada uma vez só em cada documento:** o update só casa se o `rideId` ainda não está em `appliedRideIds`, e o `$addToSet` registra a viagem no mesmo update. Quando a viagem termina de ser contabilizada, o `rideId` entra em `indicator_ride_registrations`. A mesma regra vale para a coleção `indicators`.
- **Reprocessamento:** a viagem gravada como `COMPLETED` é o registro durável. Uma tarefa agendada (a cada 15 minutos) refaz a contabilização das viagens concluídas nos últimos 7 dias que ainda não estão em `indicator_ride_registrations`. Graças à regra acima, uma contabilização que parou no meio é completada sem contar nada em dobro.
- Considera apenas viagens `COMPLETED` com ao menos um passageiro efetivo (RF49).
- **`ALL_TIME` não usa `appliedRideIds`.** O documento é recalculado a partir dos documentos `SEMESTERLY` (`refreshAllTime`, com `$max`), não por viagem, então não guarda a lista de viagens aplicadas. Nos demais períodos `appliedRideIds` é a trava de idempotência.

### Coleção `indicator_ride_registrations`

**Módulo:** `modules/indicator`
**Descrição:** Marca as viagens que já terminaram de ser contabilizadas nos indicadores, para a tarefa de reconciliação não reprocessar a mesma viagem. Existe em `MongoIndicatorStore` (`REGISTRATIONS_COLLECTION`), sem classe de entidade: o documento é gravado direto.

```json
{
  "_id": "String (rideId, ref: rides._id)",
  "registeredAt": "ISODate"
}
```

- O `_id` é o próprio `rideId`, então o índice único padrão do MongoDB impede registrar a mesma viagem duas vezes (`DuplicateKeyException` é ignorada).
- É gravado **depois** de todos os documentos de `indicators` e `platform_indicators` da viagem terem sido atualizados. A tarefa de reconciliação (a cada 15 minutos) busca as viagens concluídas nos últimos 7 dias que **não** estão aqui e completa o que faltar; `appliedRideIds` garante que nada seja somado em dobro.
- Não há índices adicionais nem TTL.

---

## 12. Coleção `audit_logs`

**Módulo:** `modules/shared`
**Descrição:** Trilha de auditoria de ações sensíveis da plataforma, para moderação, suporte e rastreabilidade. Registra **o quê, quem e quando** em eventos que têm impacto financeiro, de segurança ou de confiança — complementa os logs técnicos da aplicação (que ficam em arquivo/stdout, não no banco).

```json
{
  "_id": "ObjectId (String)",
  "action": "PAYMENT_CONFIRMED | NO_SHOW_REGISTERED | RIDE_CANCELLED | USER_BLOCKED | REPORT_RESOLVED | ACADEMIC_VERIFIED | LICENSE_VERIFIED | USER_DEACTIVATED | RESERVED_SEAT_TERMINATED",
  "actorId": "String (ref: users._id)",
  "targetUserId": "String | null (ref: users._id)",
  "entityType": "RIDE | USER | REPORT | RESERVED_SEAT | ...",
  "entityId": "String",
  "metadata": "Objeto (detalhes específicos da ação)",
  "createdAt": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `actorId` | Simples | Rastrear ações de um usuário |
| `targetUserId` | Simples | Rastrear ações sofridas por um usuário (moderação) |
| `entityType` + `entityId` | Composto | Histórico de uma entidade específica (ex: uma viagem) |
| `action` + `createdAt` | Composto | Auditoria por tipo de ação e período |
| `createdAt` | Simples (TTL opcional) | Expiração de logs antigos, se definida política de retenção |

### Regras de Negócio

- Registra apenas ações **sensíveis** (financeiras, de segurança, de moderação) — não é log de toda interação, para não inchar.
- É **append-only**: logs nunca são editados, só criados. Garante integridade da trilha.
- `metadata` guarda o contexto relevante de cada ação (ex: valor confirmado, motivo da recusa) sem esquema rígido.

---

## 13. Coleção `universities`

> **Ainda sem código:** não existe entidade (`@Document`), repositório nem endpoint para esta coleção no backend. Hoje `academicRecord.university` é gravado como texto. A descrição abaixo é o desenho previsto, ainda não implementado.

**Módulo:** `modules/shared`
**Descrição:** Lista de referência das universidades suportadas. Existe para **padronizar** o campo `academicRecord.university` — sem ela, "PUC-Campinas" e "PUC Campinas" seriam tratadas como instituições diferentes e o matching por universidade (RF14) falharia. É uma tabela simples e estável, pouco alterada.

```json
{
  "_id": "ObjectId (String)",
  "name": "String",
  "acronym": "String",
  "city": "String",
  "emailDomains": ["String (ex: 'puc-campinas.edu.br')"],
  "active": "Boolean"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `name` | Único | Evita universidade duplicada |
| `emailDomains` | Simples | Validar o e-mail institucional na verificação acadêmica |

### Regras de Negócio

- O `academicRecord.university` referencia uma universidade desta lista (não é texto livre).
- Os `emailDomains` são usados para validar automaticamente o e-mail institucional (RF03): se o e-mail do usuário bate com um domínio cadastrado, a verificação pode ser automática.

---

## 14. Coleção `notifications`

**Módulo:** `modules/shared`
**Descrição:** Avisos destinados ao usuário dentro do app (central de notificações). Mantida **simples**: guarda só o aviso, seu tipo e se já foi lido. O envio em tempo real (push) é feito pelo Firebase; esta coleção é o registro persistente para o usuário rever depois.

```json
{
  "_id": "ObjectId (String)",
  "userId": "String (ref: users._id)",
  "type": "RIDE_REQUEST | RIDE_CONFIRMED | RIDE_CANCELLED | REQUEST_EXPIRED | PAYMENT_DUE | PAYMENT_CONFIRMED | SEAT_RELEASED | GENERIC",
  "title": "String",
  "message": "String",
  "relatedEntityType": "RIDE | RESERVED_SEAT | REPORT | null",
  "relatedEntityId": "String | null",
  "read": "Boolean (default: false)",
  "createdAt": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `userId` + `read` | Composto | Listar notificações não lidas do usuário |
| `userId` + `createdAt` | Composto | Listar notificações em ordem cronológica |
| `createdAt` | Simples (TTL) | Expiração automática de notificações antigas (ex: 90 dias) |

### Regras de Negócio

- Mantida minimalista — sem templates, agendamento ou canais. Só o aviso e o status de leitura.
- O push em tempo real é responsabilidade do Firebase; o backend grava aqui para o histórico no app.

---

## 15. Coleção `idempotency_keys`

**Módulo:** `modules/shared`
**Descrição:** Registra as chaves de idempotência já processadas, conforme o cabeçalho `Idempotency-Key` enviado pelo app (previsto na arquitetura). Evita que uma operação crítica — confirmar carona, registrar pagamento — seja executada **duas vezes** se o app reenviar a requisição por falha de rede.

```json
{
  "_id": "ObjectId (String)",
  "key": "String (UUID v4 enviado pelo cliente no header Idempotency-Key)",
  "userId": "String (ref: users._id)",
  "method": "String (ex: 'POST')",
  "path": "String (ex: '/api/v1/rides/64d.../confirm')",
  "requestFingerprint": "String (hash SHA-256 do payload para validar consistência)",
  "status": "IN_PROGRESS | COMPLETED | UNKNOWN_OUTCOME",
  "responseStatus": "Integer (código HTTP retornado, ex: 200, 201)",
  "contentType": "String (ex: 'application/json')",
  "responseBody": "String (payload da resposta cacheada para replay)",
  "createdAt": "ISODate (TTL: expireAfter = '24h')"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `key` | Único | Garante que a mesma chave não seja processada duas vezes |
| `createdAt` | Simples (TTL) | Expiração automática em **24 h** (`expireAfter = "24h"`) — chaves antigas não precisam ser retidas |

### Regras de Negócio

- Antes de executar uma operação crítica, o backend verifica se a `key` já existe. Se existe, retorna o resultado anterior sem reexecutar.
- O TTL é de **24 h** — reenvios por falha de rede acontecem em minutos, não dias.

---

## Cálculos no servidor de sockets (RT04, RT20, RT22)

Cálculos em memória e validações determinísticas rodam no **Servidor Java de sockets** (`app/server`, pacote `com.converge.socket.handler`). O backend chama o servidor por `SocketClient`. Se o servidor estiver fora do ar, responder outro tipo de mensagem ou der erro, o backend registra um aviso no log e executa **o mesmo handler localmente** (a classe vem do artefato do servidor no classpath). O resultado é o mesmo nos dois caminhos. Nenhum desses cálculos grava dados: o resultado é usado pelo caso de uso, que grava nas coleções de sempre.

| Handler | O que calcula ou valida | Quem chama no backend | Onde o resultado aparece |
|---|---|---|---|
| `SustainabilityHandler` | Carros evitados, energia poupada, CO₂ evitado e árvores equivalentes de uma viagem | `RegisterCompletedRideUseCase` (módulo `indicator`), pela porta `SustainabilityGateway` | `indicators` e `platform_indicators` (`carsAvoided`, `co2AvoidedKg`) |
| `IndicatorReferenceHandler` | Texto de `reference` de um período: `AAAA-MM` (mensal), `AAAA-S1/S2` (semestral), `AAAA-W##` (semana ISO, com o ano ISO na virada) e `ALL_TIME`. Aceita **lote** (até 1000 datas por chamada) | `RegisterCompletedRideUseCase` (3 períodos por viagem, 1 chamada) e `GetUserIndicatorSeriesUseCase` (série do gráfico, 1 chamada), pela porta `IndicatorReferenceGateway` | `indicators.reference` e `platform_indicators.reference` |
| `DistanceHandler` | Distância Haversine em km (raio da Terra 6371 km). Aceita **lote** de pares (até 1000 por chamada) e devolve as distâncias na mesma ordem. Recusa latitude fora de [-90, 90], longitude fora de [-180, 180], `NaN` e par malformado | `RouteGeometryService`, `RideItineraryPlanner` e `RideCompletionPublisher`, pela porta `DistanceGateway` (módulo `route`) | `routes.routeDistanceKm` e `rides.itineraryDistanceKm` quando o Google não responde (soma dos trechos em linha reta), ordem das paradas de `rides.itinerary`, e a distância da viagem enviada ao registro de indicadores quando nem a viagem nem o trajeto têm distância |
| `ValidateRenavamHandler` | RENAVAM: 11 dígitos, não todos iguais, dígito verificador (módulo 11) | `CreateVehicleUseCase` e `UpdateVehicleUseCase`, pela porta `RenavamValidationGateway` | `vehicles.renavam` (só os dígitos) |
| `ValidateCpfHandler` | CPF: dígitos verificadores | `ValidateCpfUseCase` (módulo `user`), chamado por `POST /api/v1/users/validate-cpf` | Só responde se o CPF é válido; não grava nada |

Handlers que já existiam e seguem o mesmo padrão: `RideCostHandler` (rateio da viagem), `PixPayloadHandler` (Pix Copia e Cola) e `MatchRoutesHandler` (compatibilidade do matching).

**Sustentabilidade (fórmulas do `SustainabilityHandler`):**
- Carros evitados = passageiros efetivos (nunca negativo).
- Energia poupada = carros evitados × distância ÷ consumo médio. Distância, consumo ou carros inválidos dão 0, sem erro.
- CO₂ evitado (kg) = energia poupada × fator da fonte: gasolina 2,31 kg/L, etanol 1,50 kg/L, diesel 2,68 kg/L, elétrico 0,0385 kg/kWh. Fonte desconhecida dá 0.
- Árvores equivalentes = CO₂ evitado ÷ 22.
- O `RegisterCompletedRideUseCase` continua dividindo o CO₂ entre os participantes (`co2 ÷ (passageiros + 1)`) e gravando pelo `IndicatorStore`; essa divisão **não** foi para o servidor.

**Garantias:**
- Nenhum número calculado mudou com a migração: os adaptadores foram comparados com as classes antigas do backend em testes descartáveis, com igualdade exata (milhares de pares de pontos, todas as datas de 2010 a 2044 nos quatro períodos e casos aleatórios de sustentabilidade).
- Para não abrir uma conexão por par dentro de laços (RNF17), distância e referência de período usam **lote**. O `RideItineraryPlanner` abre uma conexão por passo da escolha gulosa, porque o passo seguinte depende do ponto escolhido.
- As classes `SustainabilityCalculator`, `IndicatorReference` e `HaversineCalculator` saíram do backend. A regra do dígito do RENAVAM também saiu da entidade `Vehicle`.
- As mensagens de erro de RENAVAM que o app recebe são as mesmas de antes (`O RENAVAM deve ter 11 dígitos.` e `RENAVAM inválido.`).
- Limitação: coordenadas com `NaN` passavam pelo `Coordinates` do backend e agora o servidor as recusa. Isso não acontece pela API (o JSON não carrega `NaN`).

---

## Diagrama de Relacionamentos

```
users (1) ──────────────────── (N) vehicles
    │                                  │
    │ (1)                              │ (N)
    │                                  │
    ├──── (N) routes ◄─────────────────┘
    │          │
    │          │ driverRouteId / passengerRouteId
    │          │
    │          ▼
    │      matchings (N) ────────► routes
    │
    ├──── (N) rides (como motorista)
    │          │
    │          │ participants[] (subdocumento embutido)
    │          │   └── userId, role, status, lateCancellation, amounts
    │          │
    │          ▼
    ├──── (N) reviews ──────► (reviewer → reviewed)
    │
    ├──── (N) reports ───────► (reporter → reported, ref. rideId)
    │
    ├──── (N) blocks ───────► (user → blockedUser)
    │
    ├──── (N) reserved_seats ──► (passenger ↔ driver, ref. routes)
    │          └── vaga reservada (opt-out) · weekDays · skippedDates[]
    │
    ├──── (N) indicators (por período WEEKLY | MONTHLY | SEMESTERLY)
    │
    ├──── (N) audit_logs (ações sensíveis: actorId → targetUserId)
    │
    └──── (N) notifications (avisos do usuário)

referência / infraestrutura (sem vínculo direto com um usuário):
  platform_indicators — totais globais agregados (por viagem concluída + reconciliação a cada 15 min)
  indicator_ride_registrations — viagens já contabilizadas nos indicadores (idempotência)
  universities        — lista de referência (academicRecord.university aponta pra cá)
  idempotency_keys    — chaves processadas (TTL de 24 h)
```

---

## Estratégia de Embedding vs. Referência

| Situação | Decisão | Justificativa |
|---|---|---|
| `participants[]` dentro de `rides` | **Embedded** | Atomicidade nas operações de vaga e valores. Sempre consultados juntos com a viagem. |
| `location` (GeoJSON Point) dentro de `routes` e `rides` | **Embedded** | Necessária para índice geoespacial 2dsphere no próprio documento. Formato `[longitude, latitude]`. |
| `costs` dentro de `rides` | **Embedded** | Snapshot imutável — não deve mudar se o veículo for editado depois. |
| `vehicleSnapshot` dentro de `rides` | **Embedded** | Snapshot imutável dos dados do veículo no momento da viagem. |
| `reviews`, `reports`, `blocks` | **Coleção própria** | Volume variável, consultados de forma independente. |
| `skippedDates[]` dentro de `reserved_seats` | **Embedded** | Lista pequena de datas que o passageiro marcou "não vou"; sempre lida junto ao vínculo. |
| `reserved_seats` | **Coleção própria** | Ciclo de vida independente — vínculo persistente entre par fixo. |
| `series[]` dentro de `indicators` | **Embedded** | Volume controlado por período (máx. ~31 pontos para mensal). Período `TOTAL` removido para evitar crescimento ilimitado. |

---

## Enums Consolidados

### `ParticipationRole` (users.participationRoles[])
| Valor | Descrição |
|---|---|
| `PASSENGER` | Procura vagas em caronas |
| `DRIVER` | Oferece carona com veículo próprio |

### `AcademicStatus` (users.academicRecord.status)
| Valor | Descrição |
|---|---|
| `PENDING` | Recém cadastrado, aguarda envio de documentação |
| `UNDER_REVIEW` | Documentação enviada, em revisão |
| `VERIFIED` | Vínculo confirmado |
| `REJECTED` | Documentação inválida ou vínculo não confirmado |

### `DriverLicenseStatus` (users.driverLicense.status)
| Valor | Descrição |
|---|---|
| `PENDING` | CNH ainda não enviada |
| `UNDER_REVIEW` | Foto da CNH enviada, aguardando aprovação |
| `VERIFIED` | CNH aprovada — motorista pode oferecer carona |
| `REJECTED` | CNH recusada (ilegível, inválida); ver `rejectionReason` |
| `EXPIRED` | CNH venceu (`expiresAt` passou) — motorista fora do matching até renovar |

### `DriverLicenseCategory` (users.driverLicense.category)
| Valor | Descrição |
|---|---|
| `A` | Motos |
| `B` | Carros de passeio |
| `AB` | Motos e carros |
| `C` | Veículos de carga |
| `D` | Veículos de transporte de passageiros (vans, ônibus) |
| `E` | Veículos com reboque/articulados |

### `PaymentStanding` (users.paymentStanding)
| Valor | Descrição |
|---|---|
| `UP_TO_DATE` | Sem dívidas vencidas (`overdueRidesCount = 0`) |
| `HAS_OVERDUE` | Tem 1 ou 2 viagens vencidas; recebe aviso mas ainda não bloqueado |
| `BLOCKED` | Bloqueado do matching — atingiu 3 dívidas vencidas ou estourou o prazo decrescente |

### `ReputationLevel` (users.reputation.*.level)
| Valor | Descrição |
|---|---|
| `HIGH` | Cumpre os critérios de confiança do papel (média, viagens concluídas e limites de penalidade) |
| `MEDIUM` | Nem estourou limites de penalidade nem cumpre os critérios de confiança |
| `LOW` | Estourou algum limite de penalidade (cancelamentos, ausências, viagens vencidas ou denúncias procedentes) |

### `Badge` (users.badges[].badge)
O enum no código chama-se `Badge` e o campo gravado em cada item de `badges` chama-se `badge` (`UserBadge(Badge badge, LocalDateTime grantedAt)`).

| Valor | Descrição |
|---|---|
| `TRUST` | Concedido pelo job semanal quando o usuário cumpre os critérios de confiança em algum papel e não estourou limites de penalidade |
| `SUSTAINABILITY` | Concedido pelo job semanal quando o CO₂ evitado acumulado chega a 50 kg |

### `VehicleType` (vehicles.type)
| Valor | Descrição |
|---|---|
| `HATCH` | Carro hatch |
| `SEDAN` | Carro sedan |
| `SUV` | SUV |
| `PICKUP` | Camionete/Pickup |
| `VAN` | Van |
| `MOTORCYCLE` | Moto |
| `OTHER` | Outro tipo |

### `EnergySource` (vehicles.fuelSources[].energySource)
| Valor | Descrição |
|---|---|
| `GASOLINE` | Gasolina |
| `ETHANOL` | Etanol |
| `DIESEL` | Diesel |
| `ELECTRIC` | Energia elétrica (km/kWh) |

### `ConsumptionUnit` (vehicles.fuelSources[].consumptionUnit)
| Valor | Descrição |
|---|---|
| `KM_PER_LITER` | Quilômetros por litro (combustão) |
| `KM_PER_KWH` | Quilômetros por kWh (elétrico) |

### `RouteType` (routes.type)
| Valor | Descrição |
|---|---|
| `OFFER` | Motorista oferecendo vagas |
| `SEARCH` | Passageiro procurando carona |

### `RouteStatus` (routes.status)
| Valor | Descrição |
|---|---|
| `ACTIVE` | Elegível para matching |
| `PAUSED` | Temporariamente fora do matching |
| `INACTIVE` | Desativado pelo usuário |

### `GeoJsonPoint` (routes.origin/destination.location, rides.*.location)
Formato geoespacial padrão do MongoDB para o índice `2dsphere`.

| Campo | Valor |
|---|---|
| `type` | Sempre `"Point"` |
| `coordinates` | Array `[longitude, latitude]` — **longitude primeiro**, latitude depois |

### `WeekDay` (routes.weekDays[], rides.weekDays[], matchings.compatibleDays[])
| Valor | Descrição |
|---|---|
| `MONDAY` | Segunda-feira |
| `TUESDAY` | Terça-feira |
| `WEDNESDAY` | Quarta-feira |
| `THURSDAY` | Quinta-feira |
| `FRIDAY` | Sexta-feira |
| `SATURDAY` | Sábado |
| `SUNDAY` | Domingo |

### `RideStatus` (rides.status)
| Valor | Descrição |
|---|---|
| `AWAITING_CONFIRMATION` | Solicitação enviada, aguardando aceite do motorista |
| `CONFIRMED` | Motorista aceitou; vagas sendo preenchidas |
| `IN_PROGRESS` | Viagem iniciada |
| `COMPLETED` | Viagem finalizada com sucesso |
| `CANCELLED` | Cancelada por motorista ou passageiro |

### `RideStopType` (rides.itinerary[].type)
| Valor | Descrição |
|---|---|
| `PICKUP` | Parada para buscar o passageiro (embarque) |
| `DROPOFF` | Parada para deixar o passageiro (desembarque) |

### `ParticipantRole` (rides.participants[].role)
| Valor | Descrição |
|---|---|
| `DRIVER` | Condutor da carona |
| `PASSENGER` | Passageiro na carona |

### `ParticipantStatus` (rides.participants[].status)
| Valor | Descrição |
|---|---|
| `REQUESTED` | Passageiro solicitou a vaga, aguardando resposta do motorista (RF19) |
| `REJECTED` | Motorista recusou a solicitação (ver `rejectionReason`) |
| `CONFIRMED` | Participação ativa confirmada (motorista aceitou) |
| `CANCELLED` | Cancelou antes da viagem |
| `NO_SHOW` | Não compareceu (registrado pelo motorista) |
| `COMPLETED` | Participou até o fim |
| `EXPIRED` | Solicitação expirada por inatividade do motorista após a janela de timeout |

### `PaymentStatus` (rides.participants[].paymentStatus)
| Valor | Descrição |
|---|---|
| `PENDING` | Valor calculado, pagamento não realizado |
| `AWAITING_DRIVER_CONFIRMATION` | Passageiro marcou como pago; aguarda motorista confirmar |
| `PAID` | Motorista confirmou recebimento |
| `NOT_APPLICABLE` | Participante cancelou ou foi ausente sem cobrança |

### `ReportReason` (reports.reason)
| Valor | Descrição |
|---|---|
| `LATE_NO_SHOW` | Atraso ou ausência injustificada |
| `INAPPROPRIATE_CONDUCT` | Comportamento inadequado |
| `IMPROPER_CHARGE` | Cobrança incorreta ou abusiva |
| `OTHER` | Outro motivo descrito na descrição |

### `ReportStatus` (reports.status)
| Valor | Descrição |
|---|---|
| `PENDING` | Recebida, aguarda análise |
| `UNDER_REVIEW` | Em revisão pela moderação |
| `RESOLVED` | Analisada com resolução registrada |
| `ARCHIVED` | Arquivada sem ação (improcedente) |

### `ReservedSeatStatus` (reserved_seats.status)
| Valor | Descrição |
|---|---|
| `PROPOSED` | Proposta enviada, aguardando aceite |
| `ACTIVE` | Vínculo em vigor; vaga prioritária reservada nos dias combinados |
| `TERMINATED` | Encerrado por iniciativa de um dos participantes |

### `IndicatorPeriod` (indicators.period)
| Valor | Descrição |
|---|---|
| `WEEKLY` | Dados da semana referência |
| `MONTHLY` | Dados do mês referência |
| `SEMESTERLY` | Dados do semestre referência |

### `PlatformIndicatorPeriod` (platform_indicators.period)
| Valor | Descrição |
|---|---|
| `WEEKLY` | Agregado da semana |
| `MONTHLY` | Agregado do mês |
| `SEMESTERLY` | Agregado do semestre |
| `ALL_TIME` | Acumulado total da plataforma (documento global único) |

### `AuditAction` (audit_logs.action)
| Valor | Descrição |
|---|---|
| `PAYMENT_CONFIRMED` | Motorista confirmou recebimento de pagamento |
| `NO_SHOW_REGISTERED` | Motorista registrou ausência de passageiro |
| `RIDE_CANCELLED` | Viagem cancelada |
| `USER_BLOCKED` | Usuário bloqueou outro |
| `REPORT_RESOLVED` | Denúncia analisada e resolvida |
| `ACADEMIC_VERIFIED` | Vínculo acadêmico verificado |
| `LICENSE_VERIFIED` | CNH verificada |
| `USER_DEACTIVATED` | Conta desativada |
| `RESERVED_SEAT_TERMINATED` | Vínculo de vaga reservada encerrado |

### `NotificationType` (notifications.type)
| Valor | Descrição |
|---|---|
| `RIDE_REQUEST` | Passageiro solicitou vaga (aviso ao motorista) |
| `RIDE_CONFIRMED` | Carona confirmada |
| `RIDE_CANCELLED` | Carona cancelada |
| `REQUEST_EXPIRED` | Aviso ao passageiro de que sua solicitação de vaga expirou por tempo limite |
| `PAYMENT_DUE` | Lembrete de pagamento pendente |
| `PAYMENT_CONFIRMED` | Motorista confirmou recebimento |
| `SEAT_RELEASED` | Vaga reservada foi liberada (passageiro recorrente não vai) |
| `GENERIC` | Aviso geral da plataforma |

### `PixKeyType` (users.pixKeyType)
| Valor | Descrição |
|---|---|
| `CPF` | CPF, somente dígitos |
| `PHONE` | Celular, formato +55DDDNÚMERO |
| `EMAIL` | E-mail, em minúsculas |
| `RANDOM` | Chave aleatória (UUID) |
