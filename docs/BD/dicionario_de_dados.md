# Dicionário de Dados: Sistema Converge de Mobilidade Universitária Compartilhada

---

## 1. Histórico de versões

| Data | Autor | Versão | Comentários |
| :---: | :---: | ---: | :--- |
| 06/10/2026 | Daniela Mikie Kikuchi Gonçalves | 1.0.0-alfa1 | Criação do documento e estrutura base das entidades |
| 07/10/2026 | Sara Monteiro | 1.0.0-alfa2 | Reputação e selos no Usuário; `reviewedRole` na Avaliação; snapshot de cálculo nos Custos da Viagem |
| 07/10/2026 | Sara Monteiro | 1.0.0-alfa3 | `fuelSources` (MC) no Veículo para flex/híbrido; `MOTORCYCLE` em `VehicleType`; validações de faixa de consumo e preço |
| 08/10/2026 | Daniela Mikie Kikuchi Gonçalves | 1.0.0-alfa4 | Acordo recorrente, inadimplência progressiva e snapshot do veículo na Viagem |
| 09/10/2026 | Daniela Mikie Kikuchi Gonçalves | 1.0.0-alfa5 | Revisão técnica: dinheiro em `Decimal128`, coordenadas em GeoJSON, datas em UTC, nomes em inglês |
| 09/10/2026 | Daniela Mikie Kikuchi Gonçalves | 1.0.0-alfa6 | Recorrência redefinida como vaga reservada (opt-out); pagamento sempre por viagem |
| 09/10/2026 | Daniela Mikie Kikuchi Gonçalves | 1.0.0-alfa7 | Verificação acadêmica, regra de desativação de conta e ajustes de pagamento/inadimplência |
| 09/10/2026 | Daniela Mikie Kikuchi Gonçalves | 1.0.0-alfa8 | Verificação de CNH no Usuário |
| 09/10/2026 | Daniela Mikie Kikuchi Gonçalves | 1.0.0-alfa9 | Alinhamento com os requisitos funcionais (RF23–RF37, RF40, RF58, RF63–RF69) |
| 09/10/2026 | Daniela Mikie Kikuchi Gonçalves | 1.0.0 | Solicitação de vaga com recusa registrada; coleções de indicadores da plataforma e auditoria |
| 09/10/2026 | Daniela Mikie Kikuchi Gonçalves | 1.1.0 | Coleções de apoio: universidades, notificações e chaves de idempotência |
| 09/10/2026 | Daniela Mikie Kikuchi Gonçalves | 1.1.1 | Renomeação de `recurring_agreements` → `reserved_seats` |
| 09/10/2026 | Daniela Mikie Kikuchi Gonçalves | 1.1.2 | Revisão final e padronização de nomes |

---

## 2. Entidades e atributos conceituais

A seguir são descritas as entidades que o projeto Converge contempla, seus atributos, tipos conceituais, características e regras de negócio. As entidades estão ordenadas da mais central para as de suporte.

> **Padrão de atributos especiais:**
> - **(C)** — Composto: estrutura formada por partes que juntas compõem o atributo.
> - **(M)** — Multivalorado: admite múltiplos valores para um mesmo registro.
> - **(MC)** — Multivalorado e Composto: lista de estruturas compostas (array de objetos).

> **Tipos monetários:**
> Todos os campos de valor monetário usam `Decimal128` no MongoDB e `BigDecimal` no Java — nunca `Double` ou `Float` para dinheiro.

---

### 2.1. User (Usuário)

Usuário é toda pessoa que se cadastra na plataforma Converge. Pode assumir um ou mais papéis: passageiro (`PASSENGER`) ou motorista (`DRIVER`). O usuário deve ter vínculo acadêmico verificado com uma universidade para utilizar as funcionalidades principais da plataforma. O sistema mantém indicadores de reputação separados por papel e concede selos de reconhecimento com base em comportamento ao longo do tempo.

#### Atributos de User

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | vazio, nulo | Gerado automaticamente pelo banco. Imutável após criação. |
| `firebaseUid` | Texto alfanumérico | Sim | Simples | `UID123abc` | vazio, nulo | UID gerado pelo Firebase Auth no primeiro login. Nunca alterado. Usado para validar tokens JWT. |
| `name` | Texto | Sim | Simples | `Ana Souza`, `João da Silva` | `""`, menos de 3 caracteres | Nome completo da pessoa. Exibido em perfis, combinações e detalhes de viagem. |
| `email` | Endereço de e-mail | Sim | Simples | `ana@puc-campinas.edu.br` | `ana@gmail.com`, vazio | Deve ser e-mail institucional acadêmico. Único no sistema. Imutável após criação. |
| `phone` | Texto numérico | Sim | Simples | `(19) 91234-5678` | `19912345678`, vazio | Formato com DDD. Exibido no perfil. |
| `photoUrl` | URL | Não | Simples | `https://storage.firebase...` | URL inválida | URL da imagem armazenada no Firebase Storage. Nulo até o upload. |
| `participationRoles` | Elemento de lista enumerada | Sim | Multivalorado (M) | `["PASSENGER"]`, `["DRIVER", "PASSENGER"]` | lista vazia, valor fora do enum | Mínimo 1 valor. Valores: `PASSENGER`, `DRIVER`. Selecionado no onboarding. |
| `academicRecord` | Dados de vínculo universitário | Sim | Composto (C) | ver tipo especial 3.1 | incompleto, nulo | Obrigatório no cadastro. Contém universidade, curso, matrícula e status da verificação. |
| `driverLicense` | Dados de habilitação (CNH) | Não | Composto (C) | ver tipo especial 3.13 | — | Obrigatório apenas para quem atua como motorista. Deve estar `VERIFIED` para oferecer carona. Nulo para passageiros. |
| `pixKey` | Texto | Não | Simples | `(19) 91234-5678`, `cpf@email.com` | vazio com cobrança ativa | Obrigatório para motoristas que desejam cobrar. Exibida na tela de Pagamento via PIX. |
| `paymentStanding` | Elemento de lista enumerada | Sim | Simples | `UP_TO_DATE`, `HAS_OVERDUE`, `BLOCKED` | valor fora do enum | Situação de pagamento do passageiro. `UP_TO_DATE` sem dívidas vencidas; `HAS_OVERDUE` com 1-2 viagens vencidas; `BLOCKED` ao atingir o limite. Ver regra de inadimplência decrescente. |
| `overdueRidesCount` | Número inteiro | Sim | Simples | `0`, `1`, `2`, `3` | negativo | Quantidade de viagens vencidas (não pagas após o prazo de 3 dias). Define o prazo decrescente até o bloqueio. |
| `isDefaulter` | Booleano | Sim | Simples | `true`, `false` | nulo | `false` por padrão. Muda para `true` quando o passageiro é bloqueado (3 dívidas vencidas ou prazo estourado). Só volta a `false` quando **todas** as viagens vencidas forem pagas. |
| `defaulterSince` | Data e hora | Não | Simples | `2025-05-20T00:00:00Z` | data futura | Preenchido quando `isDefaulter = true`. Nulo quando em dia. |
| `reputation` | Dados de reputação por papel | Sim | Composto (C) | ver tipo especial 3.2 | nulo | Calculada semanalmente com base na janela de 90 dias. Separada por papel (driver e passenger). |
| `badges` | Selos de reconhecimento | Não | Multivalorado e Composto (MC) | ver tipo especial 3.3 | — | Concedidos automaticamente quando o usuário atinge os critérios da janela de 90 dias. Removidos se deixar de atender. |
| `active` | Booleano | Sim | Simples | `true`, `false` | nulo | `true` por padrão. `false` para contas suspensas ou excluídas. |
| `createdAt` | Data e hora | Sim | Simples | `2025-05-01T10:00:00Z` | data futura | Gerado automaticamente no cadastro. |
| `updatedAt` | Data e hora | Sim | Simples | `2025-09-15T08:30:00Z` | data anterior à criação | Atualizado a cada alteração no documento. |

---

### 2.2. Vehicle (Veículo)

Veículo é o automóvel cadastrado por um motorista. Um usuário pode ter múltiplos veículos, mas apenas um é associado a cada trajeto de oferta. Os dados são copiados como snapshot imutável no momento da criação de uma viagem. Suporta múltiplas fontes de energia por veículo.

#### Atributos de Vehicle

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64b2e1d3...` | vazio, nulo | Gerado pelo banco. Imutável. |
| `userId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao `_id` do User motorista dono do veículo. |
| `nickname` | Texto | Não | Simples | `Gol Azul`, `Van da Manhã` | — | Facilita a seleção quando o motorista tem mais de um veículo. |
| `model` | Texto | Sim | Simples | `Volkswagen Gol`, `Fiat Uno` | vazio | Exibido nos detalhes da viagem. |
| `licensePlate` | Texto alfanumérico | Sim | Simples | `ABC1234`, `ABC1D23` | placa duplicada, vazio | Única por proprietário. Armazenada em maiúsculas sem hífen. |
| `color` | Texto | Sim | Simples | `Branco`, `Prata`, `Azul` | vazio | Texto livre. Auxilia o passageiro a identificar o veículo. |
| `type` | Elemento de lista enumerada | Sim | Simples | `HATCH`, `VAN`, `MOTORCYCLE` | valor fora do enum | Valores: `HATCH`, `SEDAN`, `SUV`, `PICKUP`, `VAN`, `MOTORCYCLE`, `OTHER`. Moto tem capacidade máxima de 1. |
| `totalCapacity` | Número inteiro | Sim | Simples | `5`, `15`, `1` (moto) | `0`, negativo, maior que 15 | Inclui o motorista. Mínimo 1. Máximo 15. |
| `fuelSources` | Dados de consumo por fonte de energia | Sim | Multivalorado e Composto (MC) | ver tipo especial 3.4 | lista vazia | Não pode haver dois itens da mesma fonte no mesmo veículo. Flex tem 2 itens. |
| `active` | Booleano | Sim | Simples | `true`, `false` | nulo | Veículo inativo não aparece na seleção de trajeto. |
| `createdAt` | Data e hora | Sim | Simples | `2025-04-10T09:00:00Z` | data futura | Gerado automaticamente. |
| `updatedAt` | Data e hora | Sim | Simples | `2025-09-01T11:00:00Z` | data anterior à criação | Atualizado a cada edição. |

---

### 2.3. Route (Trajeto)

Route é o registro de um deslocamento recorrente cadastrado por um usuário. Pode ser do tipo oferta (`OFFER`) ou procura (`SEARCH`). É a base de dados consumida pelo algoritmo de matching. Apenas trajetos com status `ACTIVE` são elegíveis para o matching.

#### Atributos de Route

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64c3a2b1...` | vazio, nulo | Gerado pelo banco. Referenciado em rides, matchings e acordos recorrentes. |
| `userId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao `_id` do User dono do trajeto. |
| `vehicleId` | Texto alfanumérico | Não | Simples | `64b2e1d3...` | — | Obrigatório somente para `type = OFFER`. Nulo para `SEARCH`. |
| `type` | Elemento de lista enumerada | Sim | Simples | `OFFER`, `SEARCH` | valor fora do enum | Define o papel do usuário neste trajeto. |
| `origin` | Localização | Sim | Composto (C) | ver tipo especial 3.5 | nulo, incompleto | Ponto de partida com endereço e coordenadas geográficas. |
| `destination` | Localização | Sim | Composto (C) | ver tipo especial 3.5 | nulo, incompleto | Ponto de chegada com endereço e coordenadas geográficas. |
| `departureTime` | Hora | Sim | Simples | `07:30`, `18:00` | `7:3`, `25:00` | Formato `HH:mm`. Hora recorrente do dia, sem data, interpretada no fuso **America/Sao_Paulo**. |
| `estimatedArrivalTime` | Hora | Sim | Simples | `08:10`, `19:00` | hora anterior à partida | Formato `HH:mm`. Calculado ou informado pelo usuário. |
| `weekDays` | Elemento de lista enumerada | Sim | Multivalorado (M) | `["MONDAY","TUESDAY"]` | lista vazia | Mínimo 1 dia. Valores: `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`. |
| `maxDetourKm` | Número decimal | Sim | Simples | `2.0`, `0.5`, `5.0` | negativo, zero | Somente para `OFFER`. Distância máxima de desvio aceita pelo motorista. Usado no matching. |
| `totalSeats` | Número inteiro | Não | Simples | `3`, `8` | `0`, negativo | Somente para `OFFER`. Calculado como `totalCapacity do veículo - 1`. |
| `availableSeats` | Número inteiro | Não | Simples | `3`, `1`, `0` | negativo, maior que totalSeats | Somente para `OFFER`. Decrementado ao confirmar; incrementado ao cancelar. |
| `routeDistanceKm` | Número decimal | Não | Simples | `12.4`, `5.8` | negativo, zero | Calculada via Google Maps na criação. Base para custos e desvios. |
| `status` | Elemento de lista enumerada | Sim | Simples | `ACTIVE`, `PAUSED`, `INACTIVE` | valor fora do enum | Somente `ACTIVE` entra no matching. |
| `createdAt` | Data e hora | Sim | Simples | `2025-03-01T08:00:00Z` | data futura | Gerado automaticamente. |
| `updatedAt` | Data e hora | Sim | Simples | `2025-09-20T07:00:00Z` | data anterior à criação | Atualizado a cada edição. |

---

### 2.4. Ride (Viagem/Carona)

Ride é o evento central do sistema. Representa uma viagem concreta com data, horário, motorista, passageiros e custos. Criada a partir de uma combinação do matching. Os dados de veículo, origem e destino são copiados como snapshot imutável no momento da criação.

#### Atributos de Ride

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64d4c3b2...` | vazio, nulo | Gerado pelo banco. Referenciado por reviews e reports. |
| `sequentialNumber` | Número inteiro | Sim | Simples | `42`, `1`, `1000` | negativo, zero, duplicado para o mesmo motorista | Número amigável exibido na UI: `Ride #042`. **Sequencial por motorista** (único em `driverId + sequentialNumber`), não global — evita vazar o volume total de viagens da plataforma. |
| `driverId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao `_id` do User motorista. |
| `vehicleId` | Texto alfanumérico | Sim | Simples | `64b2e1d3...` | nulo | Referência ao `_id` do Vehicle. Dados copiados no snapshot. |
| `vehicleSnapshot` | Dados do veículo | Sim | Composto (C) | ver tipo especial 3.6 | nulo | Cópia imutável dos dados do veículo no momento da criação da viagem. |
| `driverRouteId` | Texto alfanumérico | Sim | Simples | `64c3a2b1...` | nulo | Referência ao Route que originou esta viagem. |
| `origin` | Localização | Sim | Composto (C) | ver tipo especial 3.5 | nulo | Snapshot da origem. Imutável. |
| `destination` | Localização | Sim | Composto (C) | ver tipo especial 3.5 | nulo | Snapshot do destino. Imutável. |
| `departureDateTime` | Data e hora | Sim | Simples | `2025-05-12T10:30:00Z` | data passada no cadastro | Armazenado em **UTC**. Combinação da data concreta com `route.departureTime`, convertida de America/Sao_Paulo para UTC (ex.: 07:30 BRT = 10:30 UTC). Nunca usar o fuso do servidor na conversão. |
| `estimatedArrivalDateTime` | Data e hora | Sim | Simples | `2025-05-12T11:10:00Z` | anterior à partida | Armazenado em UTC. Calculada a partir da partida + duração estimada. |
| `completedAt` | Data e hora | Não | Simples | `2025-05-12T11:15:00Z` | anterior à partida | Armazenado em UTC. Nulo até o motorista concluir a viagem. |
| `weekDays` | Elemento de lista enumerada | Sim | Multivalorado (M) | `["MONDAY","WEDNESDAY"]` | lista vazia | Cópia dos dias do trajeto no momento da criação. |
| `totalSeats` | Número inteiro | Sim | Simples | `3`, `8` | `0`, negativo | Copiado do trajeto. Não se altera após a criação. |
| `availableSeats` | Número inteiro | Sim | Simples | `2`, `0` | negativo, maior que totalSeats | Atualizado atomicamente ao confirmar ou cancelar participante. |
| `status` | Elemento de lista enumerada | Sim | Simples | `CONFIRMED`, `COMPLETED` | valor fora do enum | Valores: `AWAITING_CONFIRMATION`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`. |
| `cancellationReason` | Texto | Não | Simples | `Imprevisto pessoal` | — | Presente somente quando `status = CANCELLED`. |
| `cancelledBy` | Texto alfanumérico | Não | Simples | `64a1f3c2...` | — | Referência ao User que cancelou. |
| `chatRoomId` | Texto | Não | Simples | `ride_64d4c3b2` | — | Identificador da sala de chat no **Firebase Realtime Database**. As mensagens não ficam no MongoDB — só a referência da sala. |
| `costs` | Detalhamento de custos | Sim | Composto (C) | ver tipo especial 3.7 | nulo | Calculado ao concluir a viagem. Todos os valores monetários em `Decimal128`. |
| `participants` | Participação na viagem | Sim | Multivalorado e Composto (MC) | ver tipo especial 3.8 | lista vazia | Mínimo 1 elemento (o motorista). |
| `createdAt` | Data e hora | Sim | Simples | `2025-05-10T20:00:00Z` | data futura | Gerado automaticamente. |
| `updatedAt` | Data e hora | Sim | Simples | `2025-05-12T08:15:00Z` | anterior à criação | Atualizado a cada mudança de status ou participante. |

---

### 2.5. Review (Avaliação)

Review é o registro de nota e comentário na relação **motorista ↔ passageiro** após a conclusão de uma viagem. Passageiros **não avaliam outros passageiros** — apenas o motorista avalia cada passageiro e cada passageiro avalia o motorista. Cada par (avaliador, avaliado) registra no máximo uma avaliação por viagem (unicidade por `rideId + reviewerId + reviewedId`). As notas alimentam a reputação por papel do usuário.

#### Atributos de Review

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64e5d4c3...` | vazio, nulo | Gerado pelo banco. |
| `rideId` | Texto alfanumérico | Sim | Simples | `64d4c3b2...` | nulo | Referência ao Ride. Somente viagens com `status = COMPLETED` podem ser avaliadas. |
| `reviewerId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao User que avalia. |
| `reviewedId` | Texto alfanumérico | Sim | Simples | `64a9f8e7...` | igual ao reviewerId, nulo | Referência ao User avaliado. Deve ser participante da mesma viagem. |
| `reviewedRole` | Elemento de lista enumerada | Sim | Simples | `DRIVER`, `PASSENGER` | valor fora do enum | Papel do avaliado naquela viagem. Alimenta a reputação correta. |
| `rating` | Número inteiro | Sim | Simples | `1`, `3`, `5` | `0`, `6`, negativo, decimal | Escala de 1 a 5 estrelas. Ao salvar, recalcula a reputação do avaliado no papel correspondente. |
| `comment` | Texto | Não | Simples | `Ótima viagem!`, `Pontual e educado.` | — | Texto livre. Exibido no histórico de avaliações. |
| `createdAt` | Data e hora | Sim | Simples | `2025-05-12T09:00:00Z` | data anterior à conclusão da viagem | Gerado automaticamente. |

---

### 2.6. Report (Denúncia)

Report é o registro formal de uma ocorrência reportada por um usuário contra outro. Mantém histórico completo para suporte à moderação da plataforma.

#### Atributos de Report

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64f6e5d4...` | vazio, nulo | Gerado pelo banco. |
| `reporterId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao User que registra a denúncia. |
| `reportedId` | Texto alfanumérico | Sim | Simples | `64a9f8e7...` | igual ao reporterId, nulo | Referência ao User denunciado. |
| `rideId` | Texto alfanumérico | Não | Simples | `64d4c3b2...` | — | Referência ao Ride relacionado, quando aplicável. |
| `reason` | Elemento de lista enumerada | Sim | Simples | `INAPPROPRIATE_CONDUCT`, `LATE_NO_SHOW` | valor fora do enum | Valores: `LATE_NO_SHOW`, `INAPPROPRIATE_CONDUCT`, `IMPROPER_CHARGE`, `OTHER`. |
| `description` | Texto | Não | Simples | `O motorista cobrou mais do combinado.` | — | Campo opcional para detalhamento livre. |
| `status` | Elemento de lista enumerada | Sim | Simples | `PENDING`, `UNDER_REVIEW` | valor fora do enum | Valores: `PENDING`, `UNDER_REVIEW`, `RESOLVED`, `ARCHIVED`. Default: `PENDING`. |
| `resolution` | Texto | Não | Simples | `Usuário advertido.` | — | Preenchido pela moderação ao encerrar a análise. |
| `reviewedBy` | Texto alfanumérico | Não | Simples | `64mod1...` | — | Referência ao User moderador que analisou. |
| `createdAt` | Data e hora | Sim | Simples | `2025-05-12T10:00:00Z` | data futura | Gerado automaticamente. |
| `updatedAt` | Data e hora | Sim | Simples | `2025-05-15T14:00:00Z` | anterior à criação | Atualizado ao mudar o status. |

---

### 2.7. Block (Bloqueio)

Block é o registro de que um usuário optou por impedir interações com outro. O bloqueio é bidirecional — se A bloqueou B, nenhum dos dois verá o outro em matching, caronas ou chat.

#### Atributos de Block

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64g7f6e5...` | vazio, nulo | Gerado pelo banco. |
| `userId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao User que realizou o bloqueio. |
| `blockedUserId` | Texto alfanumérico | Sim | Simples | `64a9f8e7...` | igual ao userId, nulo | Referência ao User bloqueado. O par (userId + blockedUserId) é único no sistema. |
| `createdAt` | Data e hora | Sim | Simples | `2025-05-13T08:00:00Z` | data futura | Gerado automaticamente no ato do bloqueio. |

---

### 2.8. ReservedSeat (Vaga Reservada Recorrente)

ReservedSeat é o vínculo de **vaga reservada recorrente** entre um passageiro e um motorista. Criado por opt-in: **qualquer um dos dois propõe** (`proposedBy`) e o outro aceita. Garante ao passageiro vaga prioritária nos dias combinados (`weekDays`). **Não envolve cobrança mensal nem valor fixo** — cada viagem é paga individualmente por PIX, como qualquer viagem avulsa. Opera em modelo **opt-out**: o sistema assume que o passageiro vai e reserva a vaga; ele só age para avisar que **não** vai num dia (entra em `skippedDates`).

#### Atributos de ReservedSeat

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64h1i2j3...` | vazio, nulo | Gerado pelo banco. |
| `passengerId` | Texto alfanumérico | Sim | Simples | `64a9f8e7...` | nulo | Referência ao User passageiro do vínculo. |
| `driverId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao User motorista do vínculo. |
| `driverRouteId` | Texto alfanumérico | Sim | Simples | `64c3a2b1...` | nulo | Referência ao Route de oferta base do vínculo. |
| `passengerRouteId` | Texto alfanumérico | Sim | Simples | `64c5d4e3...` | nulo | Referência ao Route de procura base do vínculo. |
| `status` | Elemento de lista enumerada | Sim | Simples | `PROPOSED`, `ACTIVE`, `TERMINATED` | valor fora do enum | Valores: `PROPOSED` (aguardando aceite), `ACTIVE` (vigente), `TERMINATED` (encerrado). |
| `proposedBy` | Elemento de lista enumerada | Sim | Simples | `PASSENGER`, `DRIVER` | valor fora do enum | Quem iniciou o vínculo. **Ambos podem propor.** |
| `terminationReason` | Texto | Não | Simples | `Motorista mudou de rota.` | — | Presente somente quando `status = TERMINATED`. |
| `weekDays` | Elemento de lista enumerada | Sim | Multivalorado (M) | `["MONDAY","WEDNESDAY","FRIDAY"]` | lista vazia | Dias em que a vaga é reservada automaticamente para o passageiro. |
| `skippedDates` | Data | Não | Multivalorado (M) | `["2025-05-14T00:00:00Z"]` | — | Datas específicas em que o passageiro avisou que não vai (botão "Não vou"). A vaga daquele dia é liberada para o matching. |
| `createdAt` | Data e hora | Sim | Simples | `2025-05-01T00:00:00Z` | data futura | Gerado automaticamente na criação do vínculo. |
| `updatedAt` | Data e hora | Sim | Simples | `2025-05-15T08:00:00Z` | anterior à criação | Atualizado a cada mudança de status ou de `skippedDates`. |

---

### 2.9. Matching

Matching é o resultado calculado pelo algoritmo de compatibilidade entre dois trajetos. Serve como cache e base de auditoria. Invalidado quando trajetos são editados ou inativados. TTL garante limpeza automática mesmo em falhas de invalidação.

#### Atributos de Matching

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64h8g7f6...` | vazio, nulo | Gerado pelo banco. |
| `passengerRouteId` | Texto alfanumérico | Sim | Simples | `64c3a2b1...` | nulo | Referência ao Route de procura. |
| `driverRouteId` | Texto alfanumérico | Sim | Simples | `64c4b3a2...` | nulo | Referência ao Route de oferta. |
| `passengerId` | Texto alfanumérico | Sim | Simples | `64a9f8e7...` | nulo | Referência ao User passageiro. Denormalizado para agilizar consultas. |
| `driverId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao User motorista. Denormalizado para agilizar consultas. |
| `compatibilityScore` | Número decimal | Sim | Simples | `0.96`, `0.84`, `0.50` | negativo, maior que 1 | Escala de 0.0 a 1.0. Exibido como porcentagem na UI: `96%`. Não é monetário — `Double` é adequado. |
| `calculatedDetourKm` | Número decimal | Sim | Simples | `2.1`, `0.8` | negativo | Quilômetros adicionais que o motorista percorreria. |
| `scheduleDifferenceMin` | Número inteiro | Sim | Simples | `5`, `0`, `15` | negativo | Diferença entre os horários de partida dos dois trajetos. Fator de penalização no score. |
| `compatibleDays` | Elemento de lista enumerada | Sim | Multivalorado (M) | `["MONDAY","TUESDAY"]` | lista vazia | Interseção dos dias dos dois trajetos. |
| `estimatedCostPerPassenger` | Número decimal monetário | Sim | Simples | `6.00`, `4.50` | negativo, zero | `Decimal128`. Estimativa do valor que o passageiro pagará. Exibido na lista de combinações. |
| `valid` | Booleano | Sim | Simples | `true`, `false` | nulo | `false` quando algum dos trajetos é editado ou inativado. Somente `true` é exibido ao usuário. |
| `expiresAt` | Data e hora | Não | Simples | `2025-06-01T00:00:00Z` | anterior à criação | Data de expiração automática (TTL). MongoDB deleta o documento após esta data. |
| `createdAt` | Data e hora | Sim | Simples | `2025-05-12T06:00:00Z` | data futura | Gerado automaticamente quando o algoritmo calcula o resultado. |

---

### 2.10. Indicator (Indicadores)

Indicator é o conjunto de métricas de mobilidade, sustentabilidade e financeiro de um usuário em um período específico. São pré-calculados por período (`WEEKLY`, `MONTHLY`, `SEMESTERLY`) e atualizados ao concluir cada viagem. A tela de Dados usa um **filtro de período** (semana / mês / 3 meses / semestre). O período `TOTAL` foi removido — o acumulado histórico é calculado **somando apenas os documentos `MONTHLY`** sob demanda (nunca misturar granularidades, pois têm dados sobrepostos), com paginação quando houver muitos meses. Sem cache persistente — o cálculo soma poucos documentos pequenos, é leve.

#### Atributos de Indicator

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64i9h8g7...` | vazio, nulo | Gerado pelo banco. |
| `userId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao `_id` do User dono dos indicadores. |
| `period` | Elemento de lista enumerada | Sim | Simples | `MONTHLY`, `WEEKLY` | `TOTAL`, valor fora do enum | Valores: `WEEKLY`, `MONTHLY`, `SEMESTERLY`. O período `TOTAL` não existe mais como documento — é calculado somando os meses na leitura. |
| `reference` | Texto alfanumérico | Sim | Simples | `2025-05`, `2025-S1`, `2025-W20` | vazio | Para mensal: `AAAA-MM`. Para semestral: `AAAA-S1/S2`. Para semanal: `AAAA-W##`. |
| `mobility` | Dados de mobilidade | Sim | Composto (C) | ver tipo especial 3.9 | nulo | Totais de viagens, km e carros evitados no período. |
| `sustainability` | Dados ambientais | Sim | Composto (C) | ver tipo especial 3.10 | nulo | Energia economizada, CO₂ evitado e equivalência em árvores. |
| `financial` | Dados financeiros | Sim | Composto (C) | ver tipo especial 3.11 | nulo | Economia do passageiro e receita do motorista no período. Valores em `Decimal128`. |
| `series` | Ponto de série temporal | Sim | Multivalorado e Composto (MC) | ver tipo especial 3.12 | lista vazia | Array de pontos para os gráficos de evolução. Máximo controlado por período. |
| `updatedAt` | Data e hora | Sim | Simples | `2025-05-12T08:20:00Z` | anterior à criação | Atualizado sempre que uma viagem do período é concluída. |

---

### 2.11. PlatformIndicator (Indicadores da Plataforma)

Indicadores **globais** agregados de toda a plataforma, por período, para atender ao RF51 (totais da plataforma) e exibições de impacto coletivo. Diferente de Indicator, não tem `userId` — representa o somatório de todos os usuários. Atualizado por job periódico.

#### Atributos de PlatformIndicator

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64p1a2t3...` | vazio, nulo | Gerado pelo banco. |
| `period` | Elemento de lista enumerada | Sim | Simples | `MONTHLY`, `ALL_TIME` | valor fora do enum | Valores: `WEEKLY`, `MONTHLY`, `SEMESTERLY`, `ALL_TIME`. |
| `reference` | Texto alfanumérico | Sim | Simples | `2025-05`, `ALL_TIME` | vazio | Identifica o período. Para `ALL_TIME`, o valor é `"ALL_TIME"`. |
| `totalRides` | Número inteiro | Sim | Simples | `1248` | negativo | Total de viagens concluídas na plataforma no período. |
| `totalSharedKm` | Número decimal | Sim | Simples | `18420.5` | negativo | Total de km compartilhados. `Double`. |
| `totalCarsAvoided` | Número inteiro | Sim | Simples | `340` | negativo | Estimativa de carros evitados no período. |
| `totalCo2AvoidedKg` | Número decimal | Sim | Simples | `3100.0` | negativo | Total de CO₂ evitado. `Double`. |
| `activeUsers` | Número inteiro | Sim | Simples | `214` | negativo | Usuários ativos no período. |
| `calculatedAt` | Data e hora | Sim | Simples | `2025-05-31T23:00:00Z` | — | Timestamp do último recálculo pelo job. |

---

### 2.12. AuditLog (Trilha de Auditoria)

Registro append-only de ações sensíveis (financeiras, de segurança e de moderação) para rastreabilidade e suporte. Complementa os logs técnicos da aplicação, que ficam em arquivo/stdout e não no banco.

#### Atributos de AuditLog

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64au1d2it...` | vazio, nulo | Gerado pelo banco. |
| `action` | Elemento de lista enumerada | Sim | Simples | `PAYMENT_CONFIRMED`, `USER_BLOCKED` | valor fora do enum | Tipo da ação registrada. Ver enum `AuditAction`. |
| `actorId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao User que executou a ação. |
| `targetUserId` | Texto alfanumérico | Não | Simples | `64a9f8e7...` | — | Referência ao User afetado pela ação, quando houver. |
| `entityType` | Texto | Sim | Simples | `RIDE`, `USER`, `REPORT` | vazio | Tipo da entidade envolvida. |
| `entityId` | Texto alfanumérico | Sim | Simples | `64d4c3b2...` | vazio | Id da entidade envolvida. |
| `metadata` | Objeto | Não | Composto (C) | `{ "valor": 6.00 }` | — | Detalhes específicos da ação, sem esquema rígido. |
| `createdAt` | Data e hora | Sim | Simples | `2025-05-12T08:20:00Z` | — | Momento do registro. Logs nunca são editados (append-only). |

---

### 2.13. University (Universidade)

Lista de referência das universidades suportadas. Padroniza o campo `university` do vínculo acadêmico — sem ela o matching por instituição falharia por diferenças de digitação.

#### Atributos de University

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64u1n2i3...` | vazio, nulo | Gerado pelo banco. |
| `name` | Texto | Sim | Simples | `PUC-Campinas` | duplicado, vazio | Nome oficial. Único no sistema. |
| `acronym` | Texto | Sim | Simples | `PUC-SP`, `USP` | vazio | Sigla da instituição. |
| `city` | Texto | Sim | Simples | `Campinas` | vazio | Cidade principal. |
| `emailDomains` | Texto | Sim | Multivalorado (M) | `["puc-campinas.edu.br"]` | lista vazia | Domínios de e-mail institucional, usados na verificação acadêmica automática. |
| `active` | Booleano | Sim | Simples | `true`, `false` | nulo | Universidades inativas não aparecem na seleção do cadastro. |

---

### 2.14. Notification (Notificação)

Aviso destinado ao usuário dentro do app. Mantida simples: só o aviso, o tipo e se foi lido. O push em tempo real é do Firebase; esta coleção é o histórico persistente.

#### Atributos de Notification

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64n1o2t3...` | vazio, nulo | Gerado pelo banco. |
| `userId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao User destinatário. |
| `type` | Elemento de lista enumerada | Sim | Simples | `PAYMENT_DUE`, `RIDE_CONFIRMED` | valor fora do enum | Ver enum `NotificationType`. |
| `title` | Texto | Sim | Simples | `Pagamento pendente` | vazio | Título curto do aviso. |
| `message` | Texto | Sim | Simples | `Você tem uma carona a pagar.` | vazio | Corpo do aviso. |
| `relatedEntityType` | Texto | Não | Simples | `RIDE`, `RESERVED_SEAT` | — | Tipo da entidade relacionada, para navegação ao tocar no aviso. |
| `relatedEntityId` | Texto alfanumérico | Não | Simples | `64d4c3b2...` | — | Id da entidade relacionada. |
| `read` | Booleano | Sim | Simples | `true`, `false` | nulo | `false` por padrão. Marcado `true` quando o usuário vê. |
| `createdAt` | Data e hora | Sim | Simples | `2025-05-12T08:20:00Z` | — | Momento do aviso. Expira por TTL após ~90 dias. |

---

### 2.15. IdempotencyKey (Chave de Idempotência)

Registra as chaves de idempotência já processadas, para evitar que uma operação crítica rode duas vezes se o app reenviar a requisição por falha de rede.

#### Atributos de IdempotencyKey

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64i1d2k3...` | vazio, nulo | Gerado pelo banco. |
| `key` | Texto alfanumérico | Sim | Simples | `uuid-v4...` | duplicada, vazio | Chave enviada pelo app no cabeçalho `Idempotency-Key`. Única. |
| `endpoint` | Texto | Sim | Simples | `POST /rides/{id}/confirm` | vazio | Operação à qual a chave se refere. |
| `userId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao User que fez a requisição. |
| `createdAt` | Data e hora | Sim | Simples | `2025-05-12T08:20:00Z` | — | Momento do processamento. Expira por TTL em 24-48h. |

---

## 3. Tipos especiais, compostos ou específicos

### 3.1. AcademicRecord (Vínculo Acadêmico)

Estrutura composta que representa os dados universitários do usuário e o status de verificação do vínculo com a instituição.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `university` | Texto | Sim | Nome da instituição. Selecionado de lista cadastrada na plataforma. |
| `course` | Texto | Sim | Nome do curso. Texto livre ou lista pré-definida por universidade. |
| `enrollmentNumber` | Texto alfanumérico | Sim | Número de matrícula. Ex.: `2024-00123`. |
| `institutionalEmail` | Endereço de e-mail | Sim | E-mail institucional usado na verificação. Ex.: `ana@puc-campinas.edu.br`. |
| `status` | Elemento de lista enumerada | Sim | Valores: `PENDING`, `UNDER_REVIEW`, `VERIFIED`, `REJECTED`. Default: `PENDING`. |
| `verificationMethod` | Elemento de lista enumerada | Sim | Valores: `INSTITUTIONAL_EMAIL` (padrão, automático via link) ou `DOCUMENT` (envio revisado). |
| `documentUrl` | URL | Não | URL do documento comprobatório no Firebase Storage. Usado quando `verificationMethod = DOCUMENT`. Nulo caso contrário. |
| `verifiedAt` | Data e hora | Não | Preenchido quando o status muda para `VERIFIED`. |
| `reviewedAt` | Data e hora | Não | Preenchido quando o vínculo é analisado (verificado ou rejeitado). |
| `rejectionReason` | Texto | Não | Motivo da recusa. Presente somente quando `status = REJECTED`, para o usuário corrigir e reenviar. |

---

### 3.2. Reputation (Reputação por Papel)

Estrutura composta que representa os indicadores de comportamento do usuário separados por papel. Calculada semanalmente com base na janela deslizante de **90 dias**.

**Critérios para ganhar o selo `TRUST` — Driver:**

| Critério | Limiar |
| :--- | :--- |
| Média de avaliação | ≥ 4.2 estrelas |
| Viagens concluídas na janela | ≥ 10 |
| Cancelamentos iniciados pelo motorista | ≤ 2 nos últimos 90 dias |
| Denúncias procedentes | ≤ 1 nos últimos 90 dias |

**Critérios para ganhar o selo `TRUST` — Passenger:**

| Critério | Limiar |
| :--- | :--- |
| Média de avaliação | ≥ 4.0 estrelas |
| Viagens concluídas na janela | ≥ 5 |
| Cancelamentos tardios (< 2h de antecedência) | ≤ 2 nos últimos 90 dias |
| Ausências | ≤ 1 nos últimos 90 dias |
| Viagens vencidas (não pagas no prazo) | 0 nos últimos 90 dias |

A estrutura `reputation` tem dois blocos: `reputation.driver` e `reputation.passenger`. Ambos compartilham `level`, `averageRating`, `totalReviews` e `calculatedAt`, mas têm uma `window` (janela de 90 dias) com campos específicos por papel.

**Campos comuns aos dois papéis:**

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `level` | Elemento de lista enumerada | Sim | Valores: `HIGH`, `MEDIUM`, `LOW`, `null` (sem histórico ainda). |
| `averageRating` | Número decimal | Sim | Média das notas recebidas no papel. |
| `totalReviews` | Número inteiro | Sim | Quantidade total de avaliações recebidas no papel. |
| `calculatedAt` | Data e hora | Sim | Timestamp do último recálculo semanal. |

**`driver.window` — janela de 90 dias do motorista:**

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `completedRides` | Número inteiro | Sim | Viagens concluídas como motorista nos últimos 90 dias. |
| `driverInitiatedCancellations` | Número inteiro | Sim | Cancelamentos iniciados pelo motorista nos últimos 90 dias. |
| `delays` | Número inteiro | Sim | Atrasos registrados nos últimos 90 dias. |
| `substantiatedReports` | Número inteiro | Sim | Denúncias marcadas como `RESOLVED` contra o motorista nos últimos 90 dias. |

**`passenger.window` — janela de 90 dias do passageiro:**

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `completedRides` | Número inteiro | Sim | Viagens concluídas como passageiro nos últimos 90 dias. |
| `lateCancellations` | Número inteiro | Sim | Cancelamentos com menos de 2h de antecedência nos últimos 90 dias. |
| `noShows` | Número inteiro | Sim | Vezes marcado como ausente nos últimos 90 dias. |
| `substantiatedReports` | Número inteiro | Sim | Denúncias marcadas como `RESOLVED` contra o passageiro nos últimos 90 dias. |
| `overdueRides` | Número inteiro | Sim | Viagens vencidas (não pagas no prazo) nos últimos 90 dias. Critério do selo de confiança: deve ser 0. |

---

### 3.3. Badge (Selo de Reconhecimento)

Estrutura composta que representa um selo concedido ao usuário. Removido automaticamente se o usuário deixar de atender os critérios no próximo recálculo semanal.

**Critérios de concessão:**

| Selo | Critério |
| :--- | :--- |
| `TRUST` | Atinge os limiares de comportamento da janela de 90 dias (ver tipo 3.2 Reputation). |
| `SUSTAINABILITY` | CO₂ evitado acumulado ≥ 50 kg (somando os `indicators` mensais do usuário). Considera apenas viagens concluídas com ao menos um passageiro efetivo — impacto real de compartilhamento, não motorista rodando sozinho. |

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `type` | Elemento de lista enumerada | Sim | Valores: `TRUST`, `SUSTAINABILITY`. |
| `grantedAt` | Data e hora | Sim | Data em que o selo foi concedido. |

---

### 3.4. FuelSource (Consumo por Fonte de Energia)

Estrutura composta usada no array `fuelSources` do Vehicle. Cada item representa uma fonte de energia e seus parâmetros de consumo e preço.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `energySource` | Elemento de lista enumerada | Sim | Valores: `GASOLINE`, `ETHANOL`, `DIESEL`, `ELECTRIC`. Não pode haver dois itens da mesma fonte no mesmo veículo. |
| `averageConsumption` | Número decimal | Sim | Para combustão: km/L. Para elétrico: km/kWh. Informado pelo motorista. Não é monetário — `Double`. |
| `consumptionUnit` | Elemento de lista enumerada | Sim | Valores: `KM_PER_LITER`, `KM_PER_KWH`. Determinado automaticamente pela fonte. |
| `currentEnergyPrice` | Número decimal monetário | Sim | `Decimal128`. R$/litro para combustão; R$/kWh para elétrico. Informado pelo motorista. |

---

### 3.5. Location (Localização)

Estrutura composta que representa um ponto geográfico com endereço e coordenadas em formato GeoJSON. Usada em Route, Ride e nos pontos de embarque/desembarque de participantes. O campo `location` segue o formato exigido pelo índice `2dsphere` do MongoDB.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `address` | Texto | Sim | Endereço textual legível. Ex.: `Av. Central, 100 — Centro`. |
| `neighborhood` | Texto | Não | Extraído do geocoding via Google Maps. |
| `city` | Texto | Sim | Extraída do geocoding. Ex.: `Campinas`. |
| `location.type` | Texto | Sim | Sempre `"Point"`. Exigido pelo GeoJSON do MongoDB. |
| `location.coordinates` | Array de número decimal | Sim | Formato `[longitude, latitude]` — **longitude primeiro**. Ex.: `[-47.0616, -22.9064]`. Não é monetário — `Double`. Atenção: ordem invertida em relação ao senso comum (lat, lng). |

---

### 3.6. VehicleSnapshot (Snapshot do Veículo)

Cópia imutável dos dados relevantes do veículo no momento da criação da viagem. Garante que edições futuras no cadastro não afetem o histórico.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `model` | Texto | Sim | Ex.: `Volkswagen Gol`. |
| `licensePlate` | Texto alfanumérico | Sim | Exibida nos detalhes da viagem. |
| `type` | Elemento de lista enumerada | Sim | Ex.: `HATCH`, `VAN`. |
| `color` | Texto | Sim | Ex.: `Branco`. |

---

### 3.7. RideCosts (Custos da Viagem)

Estrutura composta que detalha todos os valores financeiros de uma viagem. Os campos de snapshot (`energySourceUsed`, `averageConsumptionUsed`, `energyPriceUsed`) registram os parâmetros exatos usados no cálculo para auditoria futura.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `routeDistanceKm` | Número decimal | Sim | Distância direta do trajeto do motorista. Copiada do Route na criação. `Double`. |
| `totalDistanceWithDetoursKm` | Número decimal | Sim | Rota base + soma de todos os desvios dos passageiros. `Double`. |
| `energySourceUsed` | Elemento de lista enumerada | Sim | Snapshot da fonte de energia usada na viagem. |
| `averageConsumptionUsed` | Número decimal | Sim | Snapshot do consumo médio. `Double`. |
| `energyPriceUsed` | Número decimal monetário | Sim | `Decimal128`. Snapshot do preço da energia no momento da viagem. |
| `energyCost` | Número decimal monetário | Sim | `Decimal128`. `(distância total / consumo médio) × preço da energia`. |
| `toll` | Número decimal monetário | Sim | `Decimal128`. Informado pelo motorista. `0.00` quando não há pedágio. |
| `parking` | Número decimal monetário | Sim | `Decimal128`. Informado pelo motorista. `0.00` quando não há estacionamento. |
| `totalCost` | Número decimal monetário | Sim | `Decimal128`. Custo total da viagem: `energyCost + toll + parking`. (antes `grossTotal`) |
| `totalShared` | Número decimal monetário | Sim | `Decimal128`. Valor efetivamente rateado entre os participantes. Diferença de centavos atribuída ao motorista. (antes `splitTotal`) |

---

### 3.8. Participant (Participação na Viagem)

Estrutura composta e multivalorada que representa cada participante de uma viagem — motorista e passageiros.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `userId` | Texto alfanumérico | Sim | Referência ao `_id` do User. |
| `role` | Elemento de lista enumerada | Sim | Valores: `DRIVER`, `PASSENGER`. |
| `passengerRouteId` | Texto alfanumérico | Não | Referência ao Route de procura. Nulo para o motorista. |
| `boardingLocation` | Location (C) | Não | Ponto de embarque do passageiro. Nulo para o motorista. |
| `dropOffLocation` | Location (C) | Não | Ponto de desembarque do passageiro. Nulo para o motorista. |
| `detourGeneratedKm` | Número decimal | Não | Desvio gerado por este passageiro. `Double`. Calculado no matching. Nulo para o motorista. |
| `status` | Elemento de lista enumerada | Sim | Valores: `REQUESTED`, `REJECTED`, `CONFIRMED`, `CANCELLED`, `NO_SHOW`, `COMPLETED`. |
| `requestedAt` | Data e hora | Sim | Momento em que o passageiro solicitou a vaga (RF19). |
| `respondedAt` | Data e hora | Não | Momento em que o motorista aceitou ou recusou. Nulo enquanto `REQUESTED`. |
| `rejectionReason` | Texto | Não | Motivo da recusa pelo motorista. Presente somente quando `status = REJECTED`. |
| `cancellationReason` | Texto | Não | Preenchido pelo participante ao cancelar. |
| `lateCancellation` | Booleano | Sim | `false` por padrão. `true` quando cancela com menos de 2h de antecedência. |
| `estimatedAmount` | Número decimal monetário | Sim | `Decimal128`. Valor estimado atual (RF34/RF36). Recalculado a cada entrada, cancelamento ou ausência de participantes. Nunca ultrapassa o `maxAmount`. |
| `maxAmount` | Número decimal monetário | Sim | `Decimal128`. Teto de proteção do passageiro (RF35/RF37): metade do custo da rota com o desvio do passageiro, gravado na confirmação. Nunca pode ser superado pelo `finalAmount`. |
| `finalAmount` | Número decimal monetário | Não | `Decimal128`. Calculado somente ao concluir a viagem. Nulo até a conclusão. |
| `paymentStatus` | Elemento de lista enumerada | Sim | Valores: `PENDING`, `AWAITING_DRIVER_CONFIRMATION`, `PAID`, `NOT_APPLICABLE`. Em `AWAITING_DRIVER_CONFIRMATION` o relógio da dívida fica **pausado** (a viagem não vira vencida enquanto espera o motorista confirmar). |
| `paymentConfirmedByDriver` | Booleano | Sim | `false` por padrão. Atualizado pelo motorista ao confirmar recebimento. |
| `confirmedAt` | Data e hora | Sim | Timestamp de entrada do participante na viagem. |
| `cancelledAt` | Data e hora | Não | Nulo enquanto não cancelar. |
| `updatedAt` | Data e hora | Sim | Atualizado a cada mudança no subdocumento. |

---

### 3.9. MobilityData (Dados de Mobilidade)

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `ridesAsDriver` | Número inteiro | Sim | Viagens concluídas no período em que o usuário foi motorista. |
| `ridesAsPassenger` | Número inteiro | Sim | Viagens concluídas no período em que o usuário foi passageiro. |
| `sharedKm` | Número decimal | Sim | Soma dos km das viagens participadas no período. `Double`. |
| `estimatedCarsAvoided` | Número inteiro | Sim | Cada passageiro efetivo = 1 carro evitado. |

---

### 3.10. SustainabilityData (Dados de Sustentabilidade)

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `fuelSavedL` | Número decimal | Não | Para veículos de combustão. Nulo para elétricos. `Double`. |
| `energySavedKwh` | Número decimal | Não | Para veículos elétricos. Nulo para combustão. `Double`. |
| `co2AvoidedKg` | Número decimal | Sim | `energia economizada × fator de emissão`. Fator varia pela fonte de energia. `Double`. |
| `equivalentTrees` | Número decimal | Sim | `co2AvoidedKg / 22`. Referência: 1 árvore absorve ~22 kg CO₂/ano. `Double`. |

---

### 3.11. FinancialData (Dados Financeiros do Indicador)

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `totalPassengerSavings` | Número decimal monetário | Sim | `Decimal128`. Diferença entre o custo de ir sozinho e o valor pago no rateio. |
| `totalDriverIncome` | Número decimal monetário | Sim | `Decimal128`. Soma dos `finalAmount` recebidos pelo motorista no período. |

---

### 3.12. SeriesPoint (Ponto de Série Temporal)

Estrutura usada no array `series` dos Indicadores. Cada ponto representa um dia ou semana. O período `TOTAL` não gera pontos — o acumulado histórico é calculado somando os documentos mensais na leitura.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `date` | Data | Sim | Data do ponto da série. |
| `rides` | Número inteiro | Sim | Quantidade de viagens naquele ponto. |
| `sharedKm` | Número decimal | Sim | Km compartilhados naquele ponto. `Double`. |
| `co2AvoidedKg` | Número decimal | Sim | CO₂ evitado naquele ponto. `Double`. |

---

### 3.13. DriverLicense (Habilitação / CNH)

Estrutura composta que representa a CNH do motorista. Opcional — só preenchida por quem atua como `DRIVER`. O motorista precisa de `status = VERIFIED` para oferecer carona e aparecer no matching.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `number` | Texto alfanumérico | Sim | Número de registro da CNH. |
| `category` | Elemento de lista enumerada | Sim | Valores: `A`, `B`, `AB`, `C`, `D`, `E`. Define que tipo de veículo o motorista pode dirigir. |
| `expiresAt` | Data | Sim | Data de validade da CNH. Ao vencer, o `status` vira `EXPIRED` automaticamente. |
| `documentUrl` | URL | Não | URL da foto da CNH no Firebase Storage, para conferência. |
| `status` | Elemento de lista enumerada | Sim | Valores: `PENDING`, `UNDER_REVIEW`, `VERIFIED`, `REJECTED`, `EXPIRED`. |
| `verifiedAt` | Data e hora | Não | Preenchido quando a CNH é aprovada (`VERIFIED`). |
| `rejectionReason` | Texto | Não | Motivo da recusa. Presente somente quando `status = REJECTED`. |

---

## 4. Desdobramento físico — banco orientado a documentos (MongoDB)

| Entidade conceitual | Coleção MongoDB | Módulo dono | Estratégia |
| :--- | :--- | :--- | :--- |
| User | `users` | `modules/user` | `academicRecord`, `reputation` e `badges[]` embutidos (sempre consultados juntos). |
| Vehicle | `vehicles` | `modules/user` | `fuelSources[]` como MC. Referenciado por id em routes e rides. |
| Route | `routes` | `modules/route` | `origin` e `destination` embutidos com coordenadas (índice geoespacial). `weekDays` como array de strings. |
| Ride | `rides` | `modules/ride` | `participants[]` embutidos (atomicidade). `costs` e `vehicleSnapshot` embutidos (snapshot imutável). |
| Review | `reviews` | `modules/review` | Documento simples com referências por id. |
| Report | `reports` | `modules/security` | Documento simples com referências por id. |
| Block | `blocks` | `modules/security` | Documento simples com referências por id. |
| ReservedSeat | `reserved_seats` | `modules/ride` | Vínculo de vaga reservada. `weekDays` e `skippedDates[]` como arrays simples. |
| Matching | `matchings` | `modules/matching` | `compatibleDays` como array de strings. `passengerId` e `driverId` denormalizados. |
| Indicator | `indicators` | `modules/indicator` | `mobility`, `sustainability` e `financial` embutidos. `series[]` embutida com volume controlado. Sem período `TOTAL`. |
| PlatformIndicator | `platform_indicators` | `modules/indicator` | Documento global por período. `ALL_TIME` é armazenado (pequeno, só totais). Atualizado por job. |
| AuditLog | `audit_logs` | `modules/shared` | Append-only. Referências por id. `metadata` flexível sem esquema rígido. |
| University | `universities` | `modules/shared` | Lista de referência. `emailDomains` como array. Pouco alterada. |
| Notification | `notifications` | `modules/shared` | Documento simples por aviso. TTL para expirar antigos. |
| IdempotencyKey | `idempotency_keys` | `modules/shared` | Documento simples com `key` única e TTL curto (24-48h). |

---

### 4.1. Coleção `users`

```json
{
  "_id": "64a1f3c2e4b0a1b2c3d4e5f6",
  "firebaseUid": "UID123abc",
  "name": "Ana Souza",
  "email": "ana@puc-campinas.edu.br",
  "phone": "(19) 91234-5678",
  "photoUrl": "https://storage.firebase.../foto_ana.jpg",
  "participationRoles": ["PASSENGER", "DRIVER"],
  "academicRecord": {
    "university": "PUC-Campinas",
    "course": "Engenharia de Software",
    "enrollmentNumber": "2024-00123",
    "institutionalEmail": "ana@puc-campinas.edu.br",
    "status": "VERIFIED",
    "verificationMethod": "INSTITUTIONAL_EMAIL",
    "documentUrl": null,
    "verifiedAt": "2025-03-10T14:00:00Z",
    "reviewedAt": "2025-03-10T14:00:00Z",
    "rejectionReason": null
  },
  "driverLicense": {
    "number": "01234567890",
    "category": "B",
    "expiresAt": "2029-08-20T00:00:00Z",
    "documentUrl": "https://storage.firebase.../cnh_ana.jpg",
    "status": "VERIFIED",
    "verifiedAt": "2025-03-11T10:00:00Z",
    "rejectionReason": null
  },
  "pixKey": "(19) 91234-5678",
  "paymentStanding": "UP_TO_DATE",
  "overdueRidesCount": 0,
  "isDefaulter": false,
  "defaulterSince": null,
  "reputation": {
    "driver": {
      "level": "HIGH",
      "averageRating": 4.8,
      "totalReviews": 32,
      "window": {
        "completedRides": 22,
        "driverInitiatedCancellations": 0,
        "delays": 1,
        "substantiatedReports": 0
      },
      "calculatedAt": "2025-09-15T00:00:00Z"
    },
    "passenger": {
      "level": "HIGH",
      "averageRating": 4.9,
      "totalReviews": 10,
      "window": {
        "completedRides": 10,
        "lateCancellations": 0,
        "noShows": 0,
        "substantiatedReports": 0,
        "overdueRides": 0
      },
      "calculatedAt": "2025-09-15T00:00:00Z"
    }
  },
  "badges": [
    { "type": "TRUST", "grantedAt": "2025-09-15T00:00:00Z" }
  ],
  "active": true,
  "createdAt": "2025-03-01T09:00:00Z",
  "updatedAt": "2025-09-15T08:30:00Z"
}
```

---

### 4.2. Coleção `vehicles`

```json
{
  "_id": "64b2e1d3f5c1b2a3d4e5f6a7",
  "userId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "nickname": "Gol Azul",
  "model": "Volkswagen Gol",
  "licensePlate": "ABC1234",
  "color": "Azul",
  "type": "HATCH",
  "totalCapacity": 5,
  "fuelSources": [
    {
      "energySource": "GASOLINE",
      "averageConsumption": 12.5,
      "consumptionUnit": "KM_PER_LITER",
      "currentEnergyPrice": { "$numberDecimal": "6.49" }
    },
    {
      "energySource": "ETHANOL",
      "averageConsumption": 9.0,
      "consumptionUnit": "KM_PER_LITER",
      "currentEnergyPrice": { "$numberDecimal": "4.29" }
    }
  ],
  "active": true,
  "createdAt": "2025-03-05T10:00:00Z",
  "updatedAt": "2025-09-01T11:00:00Z"
}
```

---

### 4.3. Coleção `routes`

```json
{
  "_id": "64c3a2b1e6d2c3b4a5f6e7d8",
  "userId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "vehicleId": "64b2e1d3f5c1b2a3d4e5f6a7",
  "type": "OFFER",
  "origin": {
    "address": "Av. Central, 100",
    "neighborhood": "Centro",
    "city": "Campinas",
    "location": { "type": "Point", "coordinates": [-47.0616, -22.9064] }
  },
  "destination": {
    "address": "Campus UNIV — Bloco B",
    "neighborhood": "Jardim Universitário",
    "city": "Campinas",
    "location": { "type": "Point", "coordinates": [-47.0500, -22.8333] }
  },
  "departureTime": "07:30",
  "estimatedArrivalTime": "08:10",
  "weekDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
  "maxDetourKm": 2.0,
  "totalSeats": 3,
  "availableSeats": 1,
  "routeDistanceKm": 12.4,
  "status": "ACTIVE",
  "createdAt": "2025-03-10T08:00:00Z",
  "updatedAt": "2025-09-20T07:00:00Z"
}
```

---

### 4.4. Coleção `rides`

```json
{
  "_id": "64d4c3b2f7e3d4c5b6a7f8e9",
  "sequentialNumber": 42,
  "driverId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "vehicleId": "64b2e1d3f5c1b2a3d4e5f6a7",
  "vehicleSnapshot": {
    "model": "Volkswagen Gol",
    "licensePlate": "ABC1234",
    "type": "HATCH",
    "color": "Azul"
  },
  "driverRouteId": "64c3a2b1e6d2c3b4a5f6e7d8",
  "origin": {
    "address": "Av. Central, 100",
    "location": { "type": "Point", "coordinates": [-47.0616, -22.9064] }
  },
  "destination": {
    "address": "Campus UNIV — Bloco B",
    "location": { "type": "Point", "coordinates": [-47.0500, -22.8333] }
  },
  "departureDateTime": "2025-05-12T10:30:00Z",
  "estimatedArrivalDateTime": "2025-05-12T11:10:00Z",
  "completedAt": "2025-05-12T11:15:00Z",
  "weekDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
  "totalSeats": 3,
  "availableSeats": 0,
  "status": "COMPLETED",
  "cancellationReason": null,
  "cancelledBy": null,
  "chatRoomId": "ride_64d4c3b2f7e3d4c5b6a7f8e9",
  "costs": {
    "routeDistanceKm": 12.4,
    "totalDistanceWithDetoursKm": 14.5,
    "energySourceUsed": "GASOLINE",
    "averageConsumptionUsed": 12.5,
    "energyPriceUsed": { "$numberDecimal": "6.49" },
    "energyCost": { "$numberDecimal": "7.53" },
    "toll": { "$numberDecimal": "0.00" },
    "parking": { "$numberDecimal": "0.00" },
    "totalCost": { "$numberDecimal": "7.53" },
    "totalShared": { "$numberDecimal": "7.53" }
  },
  "participants": [
    {
      "userId": "64a1f3c2e4b0a1b2c3d4e5f6",
      "role": "DRIVER",
      "passengerRouteId": null,
      "boardingLocation": null,
      "dropOffLocation": null,
      "detourGeneratedKm": null,
      "status": "COMPLETED",
      "cancellationReason": null,
      "lateCancellation": false,
      "estimatedAmount": { "$numberDecimal": "2.51" },
      "maxAmount": { "$numberDecimal": "2.51" },
      "finalAmount": { "$numberDecimal": "2.51" },
      "paymentStatus": "NOT_APPLICABLE",
      "paymentConfirmedByDriver": true,
      "confirmedAt": "2025-05-10T20:00:00Z",
      "cancelledAt": null,
      "updatedAt": "2025-05-12T08:15:00Z"
    },
    {
      "userId": "64a9f8e7d6c5b4a3f2e1d0c9",
      "role": "PASSENGER",
      "passengerRouteId": "64c5d4e3f2a1b0c9d8e7f6a5",
      "boardingLocation": {
        "address": "Rua das Flores, 50",
        "location": { "type": "Point", "coordinates": [-47.0650, -22.9100] }
      },
      "dropOffLocation": {
        "address": "Campus UNIV — Bloco B",
        "location": { "type": "Point", "coordinates": [-47.0500, -22.8333] }
      },
      "detourGeneratedKm": 2.1,
      "status": "COMPLETED",
      "cancellationReason": null,
      "lateCancellation": false,
      "estimatedAmount": { "$numberDecimal": "2.51" },
      "maxAmount": { "$numberDecimal": "6.00" },
      "finalAmount": { "$numberDecimal": "2.51" },
      "paymentStatus": "PAID",
      "paymentConfirmedByDriver": true,
      "confirmedAt": "2025-05-11T07:00:00Z",
      "cancelledAt": null,
      "updatedAt": "2025-05-12T08:20:00Z"
    }
  ],
  "createdAt": "2025-05-10T20:00:00Z",
  "updatedAt": "2025-05-12T08:20:00Z"
}
```

---

### 4.5. Coleção `reviews`

```json
{
  "_id": "64e5d4c3b2a1f0e9d8c7b6a5",
  "rideId": "64d4c3b2f7e3d4c5b6a7f8e9",
  "reviewerId": "64a9f8e7d6c5b4a3f2e1d0c9",
  "reviewedId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "reviewedRole": "DRIVER",
  "rating": 5,
  "comment": "Motorista pontual e carro limpo!",
  "createdAt": "2025-05-12T09:00:00Z"
}
```

---

### 4.6. Coleção `reports`

```json
{
  "_id": "64f6e5d4c3b2a1f0e9d8c7b6",
  "reporterId": "64a9f8e7d6c5b4a3f2e1d0c9",
  "reportedId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "rideId": "64d4c3b2f7e3d4c5b6a7f8e9",
  "reason": "INAPPROPRIATE_CONDUCT",
  "description": "O motorista fez comentários inapropriados durante a viagem.",
  "status": "PENDING",
  "resolution": null,
  "reviewedBy": null,
  "createdAt": "2025-05-12T10:00:00Z",
  "updatedAt": "2025-05-12T10:00:00Z"
}
```

---

### 4.7. Coleção `blocks`

```json
{
  "_id": "64g7f6e5d4c3b2a1f0e9d8c7",
  "userId": "64a9f8e7d6c5b4a3f2e1d0c9",
  "blockedUserId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "createdAt": "2025-05-13T08:00:00Z"
}
```

---

### 4.8. Coleção `reserved_seats`

```json
{
  "_id": "64h1i2j3k4l5m6n7o8p9q0r1",
  "passengerId": "64a9f8e7d6c5b4a3f2e1d0c9",
  "driverId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "driverRouteId": "64c3a2b1e6d2c3b4a5f6e7d8",
  "passengerRouteId": "64c5d4e3f2a1b0c9d8e7f6a5",
  "status": "ACTIVE",
  "proposedBy": "PASSENGER",
  "terminationReason": null,
  "weekDays": ["MONDAY", "WEDNESDAY", "FRIDAY"],
  "skippedDates": ["2025-05-14T00:00:00Z"],
  "createdAt": "2025-05-01T00:00:00Z",
  "updatedAt": "2025-05-14T08:00:00Z"
}
```

---

### 4.9. Coleção `matchings`

```json
{
  "_id": "64h8g7f6e5d4c3b2a1f0e9d8",
  "passengerRouteId": "64c5d4e3f2a1b0c9d8e7f6a5",
  "driverRouteId": "64c3a2b1e6d2c3b4a5f6e7d8",
  "passengerId": "64a9f8e7d6c5b4a3f2e1d0c9",
  "driverId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "compatibilityScore": 0.96,
  "calculatedDetourKm": 2.1,
  "scheduleDifferenceMin": 0,
  "compatibleDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
  "estimatedCostPerPassenger": { "$numberDecimal": "6.00" },
  "valid": true,
  "expiresAt": "2025-06-12T00:00:00Z",
  "createdAt": "2025-05-12T06:00:00Z"
}
```

---

### 4.10. Coleção `indicators`

```json
{
  "_id": "64i9h8g7f6e5d4c3b2a1f0e9",
  "userId": "64a9f8e7d6c5b4a3f2e1d0c9",
  "period": "MONTHLY",
  "reference": "2025-05",
  "mobility": {
    "ridesAsDriver": 0,
    "ridesAsPassenger": 22,
    "sharedKm": 272.8,
    "estimatedCarsAvoided": 22
  },
  "sustainability": {
    "fuelSavedL": 21.8,
    "energySavedKwh": null,
    "co2AvoidedKg": 50.7,
    "equivalentTrees": 2.3
  },
  "financial": {
    "totalPassengerSavings": { "$numberDecimal": "132.00" },
    "totalDriverIncome": { "$numberDecimal": "0.00" }
  },
  "series": [
    { "date": "2025-05-01T00:00:00Z", "rides": 5, "sharedKm": 62.0, "co2AvoidedKg": 11.5 },
    { "date": "2025-05-08T00:00:00Z", "rides": 5, "sharedKm": 62.0, "co2AvoidedKg": 11.5 }
  ],
  "updatedAt": "2025-05-30T09:00:00Z"
}
```

---

## 5. Consultas, índices e observações de arquitetura

| Consulta esperada | Campos envolvidos | Índices sugeridos | Observações |
| :--- | :--- | :--- | :--- |
| Buscar usuário por e-mail (login) | `email` | `{ email: 1 }` único | Consulta crítica no fluxo de autenticação. |
| Buscar usuário por UID Firebase | `firebaseUid` | `{ firebaseUid: 1 }` único | Executada a cada requisição autenticada. |
| Verificar inadimplência no matching | `isDefaulter` | `{ isDefaulter: 1 }` | Checagem rápida antes de exibir combinações. |
| Tela de pendências do passageiro | `participants.userId`, `participants.paymentStatus` | `{ "participants.userId": 1, "participants.paymentStatus": 1 }` | Lista viagens com pagamento `PENDING` ou `AWAITING_DRIVER_CONFIRMATION`. |
| Fila de verificação acadêmica (método documento) | `academicRecord.status` | `{ "academicRecord.status": 1 }` | Moderação dos cadastros `UNDER_REVIEW`. |
| Fila de verificação de CNH | `driverLicense.status` | `{ "driverLicense.status": 1 }` | Moderação das CNHs `UNDER_REVIEW`. |
| Detectar CNHs vencidas (job) | `driverLicense.expiresAt`, `driverLicense.status` | `{ "driverLicense.expiresAt": 1, "driverLicense.status": 1 }` | Job agendado que marca `EXPIRED` e tira o motorista do matching. |
| Listar veículos de um motorista | `userId` | `{ userId: 1 }` | Consulta na tela de cadastro de trajeto. |
| Listar trajetos ativos de um usuário | `userId`, `status` | `{ userId: 1, status: 1 }` | Dashboard e perfil. |
| Buscar trajetos por proximidade de origem | `origin.location` | `{ "origin.location": "2dsphere" }` | Consulta geoespacial central do matching (GeoJSON). |
| Buscar trajetos por proximidade de destino | `destination.location` | `{ "destination.location": "2dsphere" }` | Filtra por destino próximo (GeoJSON). |
| Listar trajetos por dia da semana | `weekDays`, `status` | `{ weekDays: 1, status: 1 }` | Pré-filtro do matching. |
| Listar viagens de um motorista | `driverId`, `status` | `{ driverId: 1, status: 1 }` | Histórico e painel do motorista. |
| Listar viagens de um passageiro | `participants.userId` | `{ "participants.userId": 1 }` | Histórico e viagem ativa do passageiro. |
| Buscar viagem por número sequencial do motorista | `driverId`, `sequentialNumber` | `{ driverId: 1, sequentialNumber: 1 }` único | Referência amigável `Ride #042` por motorista. |
| Buscar combinações válidas por passageiro | `passengerRouteId`, `valid`, `compatibilityScore` | `{ passengerRouteId: 1, valid: 1, compatibilityScore: -1 }` | Lista ordenada por score. |
| Verificar bloqueio entre dois usuários | `userId`, `blockedUserId` | `{ userId: 1, blockedUserId: 1 }` único | Executada no matching, aceite de carona e chat. |
| Verificar avaliação duplicada | `rideId`, `reviewerId` | `{ rideId: 1, reviewerId: 1 }` único | Impede dupla avaliação na mesma viagem. |
| Buscar avaliações recebidas por papel | `reviewedId`, `reviewedRole` | `{ reviewedId: 1, reviewedRole: 1 }` | Base do cálculo de reputação por papel. |
| Buscar fila de denúncias pendentes | `status` | `{ status: 1 }` | Moderação — filtro por `PENDING` e `UNDER_REVIEW`. |
| Buscar vínculo recorrente entre par | `passengerId`, `driverRouteId` | `{ passengerId: 1, driverRouteId: 1 }` único | Verificar se já existe vínculo e seu status. |
| Listar vínculos ativos de um usuário | `passengerId`/`driverId`, `status` | `{ passengerId: 1, status: 1 }` | Montar a lista de caronas reservadas (opt-out). |
| Buscar indicadores de um usuário por período | `userId`, `period`, `reference` | `{ userId: 1, period: 1, reference: 1 }` único | Tela de Dados. |
| Acumulado histórico (filtro de período) | `userId`, `period` | `{ userId: 1, period: 1 }` | Soma **apenas** os documentos `MONTHLY`, paginado. Nunca misturar com `WEEKLY`/`SEMESTERLY`. |
| Indicadores globais da plataforma (RF51) | `period`, `reference` | `{ period: 1, reference: 1 }` único | Totais da plataforma por período. |
| Solicitações de vaga pendentes para o motorista | `driverId`, `participants.status` | `{ driverId: 1, "participants.status": 1 }` | Listar participantes `REQUESTED` aguardando aceite/recusa. |
| Histórico de auditoria de uma entidade | `entityType`, `entityId` | `{ entityType: 1, entityId: 1 }` | Trilha de ações de uma viagem, usuário, etc. |
| Auditoria de ações sofridas por um usuário | `targetUserId` | `{ targetUserId: 1 }` | Suporte à moderação. |
| Listar notificações não lidas | `userId`, `read` | `{ userId: 1, read: 1 }` | Central de notificações do app. |
| Validar chave de idempotência | `key` | `{ key: 1 }` único | Verificar se a operação já foi processada. |
| Validar e-mail institucional | `emailDomains` | `{ emailDomains: 1 }` | Verificação acadêmica automática pelo domínio. |
| Limpeza automática de matchings obsoletos | `expiresAt` | `{ expiresAt: 1 }` TTL | MongoDB remove automaticamente. |
