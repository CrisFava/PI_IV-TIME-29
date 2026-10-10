# Modelagem de Banco de Dados — Converge

> **Banco de dados:** MongoDB (Atlas em produção)
> **Estratégia:** Monólito Modular — cada módulo de negócio é dono exclusivo de suas coleções.
> Referências entre coleções de módulos distintos são feitas **exclusivamente por `String id`**, sem `@DBRef`.
> Valores monetários são armazenados como **`Decimal128`** no MongoDB e **`BigDecimal`** no Java. A diferença de centavos no rateio é sempre atribuída ao motorista.
> Coordenadas geográficas são armazenadas no formato **GeoJSON `Point`** exigido pelo índice `2dsphere` do MongoDB: `{ "type": "Point", "coordinates": [longitude, latitude] }` — **longitude sempre antes da latitude**.
> **Datas e horas:** todos os campos `ISODate` são armazenados em **UTC**. Campos de hora recorrente (`departureTime`, `estimatedArrivalTime`) são `String "HH:mm"` interpretados no fuso oficial da aplicação (**America/Sao_Paulo**). Ao combinar uma data concreta com um horário recorrente para gerar `departureDateTime`, a conversão para UTC deve usar explicitamente America/Sao_Paulo — nunca o fuso do servidor.
> **Chat:** as mensagens entre participantes **não ficam no MongoDB** — vivem no **Firebase Realtime Database**, conforme a arquitetura do projeto (comunicação em tempo real). O MongoDB guarda apenas o identificador da sala de conversa quando necessário (ex.: um `chatRoomId` na viagem para localizar o histórico no Firebase). Por isso não há coleção `messages`/`chat` nesta modelagem.
> **Pagamento PIX:** no MVP a confirmação de recebimento é **manual** (o motorista marca que recebeu). A verificação automática via gateway de pagamento (QR Code dinâmico + webhook) fica como evolução futura.

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
      "type": "TRUST | SUSTAINABILITY",
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
| `academicRecord.university` | Simples | Filtro de matching por universidade |
| `academicRecord.status` | Simples | Consultas de moderação e verificação |
| `reputation.driver.level` | Simples | Consultas de reputação de motoristas |
| `isDefaulter` | Simples | Verificação rápida de inadimplência no matching |

### Regras de Negócio — Selo de Confiança

O selo é recalculado semanalmente com base na janela deslizante de **90 dias**.

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

### Regras de Negócio — Selo de Sustentabilidade

O selo `SUSTAINABILITY` reconhece o impacto ambiental acumulado do usuário. É baseado em dados concretos dos indicadores, não apenas em "andar com mais gente". O critério usa o **CO₂ evitado acumulado** (somando os `indicators` mensais do usuário):

| Nível do selo | CO₂ evitado acumulado | Equivalência aproximada |
|---|---|---|
| Base para concessão | ≥ 50 kg de CO₂ evitado | ~2 árvores/ano |
| (opcional — exibição de destaque) | ≥ 200 kg de CO₂ evitado | ~9 árvores/ano |

- O cálculo de CO₂ evitado já é feito e armazenado em `indicators.sustainability.co2AvoidedKg`, com base na energia economizada × fator de emissão da fonte do veículo (metodologia verídica — ver dicionário).
- O selo considera **apenas viagens concluídas com pelo menos um passageiro efetivo** — garante que o impacto seja real (compartilhamento de fato), não apenas o motorista rodando sozinho.
- Recalculado semanalmente junto com o selo de confiança.

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

- **Moto:** `totalCapacity = 1`, validado no service.
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
  "maxDetourKm": "Double",
  "totalSeats": "Integer | null",
  "availableSeats": "Integer | null",
  "routeDistanceKm": "Double | null",
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

---

## 4. Coleção `rides`

**Módulo:** `modules/ride`
**Descrição:** Coleção central do sistema. Representa uma viagem concreta — desde a confirmação até a conclusão. Os participantes são subdocumentos embutidos para garantir atomicidade nas operações de vaga. Os dados de veículo são copiados como snapshot imutável no momento da criação.

```json
{
  "_id": "ObjectId (String)",
  "driverId": "String (ref: users._id)",
  "sequentialNumber": "Integer (sequencial por motorista)",
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
      "status": "REQUESTED | REJECTED | CONFIRMED | CANCELLED | NO_SHOW | COMPLETED",
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
      "confirmedAt": "ISODate",
      "cancelledAt": "ISODate | null",
      "updatedAt": "ISODate"
    }
  ],
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `driverId` + `sequentialNumber` | Composto Único | Referência amigável por motorista (Ride #042). Sequencial por motorista — não vaza volume total da plataforma |
| `driverId` | Simples | Listar viagens do motorista |
| `participants.userId` | Simples | Listar viagens de um passageiro |
| `status` | Simples | Filtrar viagens ativas, concluídas, etc. |
| `departureDateTime` | Simples | Ordenação e filtro por data |
| `driverRouteId` | Simples | Vincular viagens a um trajeto recorrente |

### Regras de Negócio Críticas

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

- Só é permitido avaliar se avaliador e avaliado participaram da mesma viagem **e** um deles é `DRIVER` e o outro é `PASSENGER`. Avaliação `PASSENGER → PASSENGER` é rejeitada no service.

---

## 6. Coleção `reports`

**Módulo:** `modules/security`
**Descrição:** Denúncias realizadas por usuários contra outros. Mantém histórico completo para moderação.

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

---

## 7. Coleção `blocks`

**Módulo:** `modules/security`
**Descrição:** Registros de bloqueio entre usuários. Usada para filtrar resultados de matching, aceites de carona e chat. O bloqueio é bidirecional para fins de interação.

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

---

## 8. Coleção `reserved_seats`

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

---

## 10. Coleção `indicators`

**Módulo:** `modules/indicator`
**Descrição:** Indicadores de mobilidade, sustentabilidade e financeiro por usuário e período. Atualizados ao concluir uma viagem. Documentos pré-calculados por período (`WEEKLY`, `MONTHLY`, `SEMESTERLY`). O período `TOTAL` foi removido — o acumulado histórico é calculado **somando apenas os documentos `MONTHLY`** sob demanda, evitando o risco de estouro do limite de 16 MB do MongoDB.

```json
{
  "_id": "ObjectId (String)",
  "userId": "String (ref: users._id)",
  "period": "WEEKLY | MONTHLY | SEMESTERLY",
  "reference": "String (ex: '2025-05' para mensal, '2025-S1' para semestral, '2025-W20' para semanal)",
  "mobility": {
    "ridesAsDriver": "Integer",
    "ridesAsPassenger": "Integer",
    "sharedKm": "Double",
    "estimatedCarsAvoided": "Integer"
  },
  "sustainability": {
    "fuelSavedL": "Double | null",
    "energySavedKwh": "Double | null",
    "co2AvoidedKg": "Double",
    "equivalentTrees": "Double"
  },
  "financial": {
    "totalPassengerSavings": "Decimal128",
    "totalDriverIncome": "Decimal128"
  },
  "series": [
    {
      "date": "ISODate",
      "rides": "Integer",
      "sharedKm": "Double",
      "co2AvoidedKg": "Double"
    }
  ],
  "updatedAt": "ISODate"
}
```

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

---

## 11. Coleção `platform_indicators`

**Módulo:** `modules/indicator`
**Descrição:** Indicadores **globais da plataforma** (não de um usuário específico), para atender ao RF51 ("quantidade de caronas realizadas no total da plataforma") e exibições públicas de impacto coletivo. Um documento por período, agregando os números de todos os usuários. Atualizado por job periódico, não a cada viagem (evita contenção de escrita num documento único de alto tráfego).

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
  "calculatedAt": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `period` + `reference` | Composto Único | Um documento por período/referência global |

### Regras de Negócio

- Diferente de `indicators` (por usuário), aqui o `ALL_TIME` **é um documento armazenado** — como é um único documento global e pequeno (só totais, sem array `series`), não há risco do limite de 16 MB.
- Atualizado por job agendado (ex: diário), não em tempo real, para não gerar concorrência de escrita no mesmo documento a cada viagem concluída.
- Considera apenas viagens `COMPLETED` com ao menos um passageiro efetivo (RF49).

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
  "type": "RIDE_REQUEST | RIDE_CONFIRMED | RIDE_CANCELLED | PAYMENT_DUE | PAYMENT_CONFIRMED | SEAT_RELEASED | GENERIC",
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
  "key": "String (UUID v4 enviado pelo cliente)",
  "endpoint": "String (ex: 'POST /rides/{id}/confirm')",
  "userId": "String (ref: users._id)",
  "createdAt": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `key` | Único | Garante que a mesma chave não seja processada duas vezes |
| `createdAt` | Simples (TTL) | Expiração automática (ex: 24-48h) — chaves antigas não precisam ser retidas |

### Regras de Negócio

- Antes de executar uma operação crítica, o backend verifica se a `key` já existe. Se existe, retorna o resultado anterior sem reexecutar.
- TTL curto (24-48h) é suficiente — reenvios por falha de rede acontecem em minutos, não dias.

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
  platform_indicators — totais globais agregados por job periódico
  universities        — lista de referência (academicRecord.university aponta pra cá)
  idempotency_keys    — chaves processadas (TTL curto)
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
| `HIGH` | Boa avaliação e baixa taxa de problemas |
| `MEDIUM` | Desempenho intermediário |
| `LOW` | Avaliação baixa ou alta taxa de problemas |

### `BadgeType` (users.badges[].type)
| Valor | Descrição |
|---|---|
| `TRUST` | Concedido quando atinge os critérios de comportamento na janela de 90 dias |
| `SUSTAINABILITY` | Concedido com base nos indicadores de impacto ambiental acumulados |

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
| `PAYMENT_DUE` | Lembrete de pagamento pendente |
| `PAYMENT_CONFIRMED` | Motorista confirmou recebimento |
| `SEAT_RELEASED` | Vaga reservada foi liberada (passageiro recorrente não vai) |
| `GENERIC` | Aviso geral da plataforma |
