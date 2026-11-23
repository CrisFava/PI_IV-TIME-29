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
| 11/10/2026 | Sara Monteiro | 1.2.0 | Detalhamento do pagamento PIX: campo `pixKeyType` e enum no Usuário; regras da chave PIX; geração do QR Code "Pix Copia e Cola" estático (sem gateway); confirmação manual pelo motorista |
| 21/10/2026 | Gustavo Alves de Siqueira Costa | 1.3.0 | `encodedPolyline` no Trajeto; `routeDistanceKm` calculada pela Routes API, com Haversine como alternativa |
| 05/11/2026 | Daniela Mikie Kikuchi Gonçalves | 1.4.0 | Indicador por usuário (`indicators`) ajustado para a versão enxuta do MVP (campos planos, `carsAvoided`); `financial` e `series` marcados como evolução futura |
| 12/11/2026 | Cristian Eduardo Fava | 1.5.0 | Cobrança combinada no chat na Participação (`requestedAmount`, `paymentRequestedAt`, `paidAt`); estrutura do chat no Firebase Realtime Database; indicadores atualizados ao concluir a viagem |
| 13/11/2026 | Daniela Mikie Kikuchi Gonçalves | 1.5.1 | Avaliação: quem pode avaliar (participante que concluiu a viagem), avaliação imutável e sem prazo no MVP, média calculada a partir das avaliações; índice de duplicidade corrigido para `rideId + reviewerId + reviewedId` |
| 15/11/2026 | Allan Giovanni Matias Paes | 1.6.0 | Alinhamento com as entidades Java: `toll` e `parking` no Trajeto; `EXPIRED` e `version` na Viagem; `appliedRideIds` nos Indicadores e Plataforma; `IdempotencyKey` completo com replay e fingerprint; `REQUEST_EXPIRED` em Notificação; complementação dos schemas JSON 4.11 a 4.15 e índices de integridade |
| 18/11/2026 | Daniela Mikie Kikuchi Gonçalves | 1.7.0 | Itinerário da Viagem (`itinerary`, `itineraryDistanceKm`, `itineraryPolyline`, `RideStop`, `RideStopType`); `IndicatorRideRegistration` documentada; reputação gravada a partir das avaliações e regra de `level`; selos automáticos (job semanal) e `completedRides` contado ao concluir a viagem; denúncias e bloqueios implementados (endpoints, onde valem, moderação fora); campo do selo `badge`; `appliedRideIds` ausente em `ALL_TIME`; indicadores na versão enxuta; atualização por viagem com reconciliação a cada 15 min; TTL de idempotência de 24 h; avisos de "sem código" para `reserved_seats` e `universities`; cálculos migrados para o servidor de sockets (sustentabilidade, referência de período, distância, RENAVAM); registro de `completedRides` e dos indicadores depois do commit da conclusão da viagem; reputação e selos gravados por update parcial atômico |
| 20/11/2026 | Daniela Mikie Kikuchi Gonçalves | 1.8.0 | Inadimplência implementada (RF67): `overdueAt` e `nonPaymentReportedAt` na Participação; prazo de 5 minutos e limite de 2 dívidas; denúncia `NON_PAYMENT` aceita só depois do "Já paguei" e contada como dívida; `overdueRides` passa a ser incrementado |
| 20/11/2026 | Daniela Mikie Kikuchi Gonçalves | 1.9.0 | Janela de 90 dias da reputação recalculada a partir das viagens (job semanal); campos `cancelledAt` e `driverCancellationPenalized` na Viagem e `removedByDriver` na Participação |
| 20/11/2026 | Daniela Mikie Kikuchi Gonçalves | 1.9.1 | Reputação: a nota da avaliação só é gravada se o total for maior que o já gravado, e penalidade e viagem concluída deixam de regravar a média e o total (avaliações simultâneas não se sobrescrevem) |
| 22/11/2026 | Daniela Mikie Kikuchi Gonçalves | 1.9.2 | Alinhamento com o código: avaliação também em viagem `IN_PROGRESS` para quem já desceu; `startedAt` na Viagem; datas como `LocalDateTime` no horário de Brasília, convertidas para UTC pelo fuso da máquina; ausências antigas sem `startedAt` fora da janela de 90 dias; na modelagem, pedido de vaga expira em 1 h e `PAYMENT_CONFIRMED` descrito pelo fluxo do "Já paguei"; índices sugeridos separados dos que o código cria |
| 22/11/2026 | Daniela Mikie Kikuchi Gonçalves | 1.9.3 | Regras de negócio alinhadas com o código: o cadastro do trajeto não confere vínculo, CNH e PIX (a conferência é no matching, no pedido e no aceite); desativação de conta só marca `active = false`; cancelamento tardio pesa só na reputação; bloqueio separa os dois em viagens não iniciadas; chat sem regra de participante no Firebase; `finalAmount` gravado na chegada ou no encerramento; vagas por data em `rides.availableSeats`; auditoria e notificações reservadas marcadas; notificações sem TTL; idempotência só em `/rides`; e ajustes em e-mail, telefone, moto, desvio, `totalSeats`, CNH vencida, `NON_PAYMENT` e histórico |

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

Usuário é toda pessoa que se cadastra na plataforma Converge. Pode assumir um ou mais papéis: passageiro (`PASSENGER`) ou motorista (`DRIVER`). O usuário deve ter vínculo acadêmico verificado com uma universidade para utilizar as funcionalidades principais da plataforma. O sistema mantém indicadores de reputação separados por papel e concede selos de reconhecimento com base em comportamento ao longo do tempo (ver tipos 3.2 e 3.3 para o que o código grava hoje).

#### Atributos de User

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | vazio, nulo | Gerado automaticamente pelo banco. Imutável após criação. |
| `firebaseUid` | Texto alfanumérico | Sim | Simples | `UID123abc` | vazio, nulo | UID gerado pelo Firebase Auth no primeiro login. Nunca alterado. Usado para validar tokens JWT. |
| `name` | Texto | Sim | Simples | `Ana Souza`, `João da Silva` | `""`, menos de 3 caracteres | Nome completo da pessoa. Exibido em perfis, combinações e detalhes de viagem. |
| `email` | Endereço de e-mail | Sim | Simples | `ana@puc-campinas.edu.br` | `ana@`, vazio | Formato de e-mail válido e único no sistema. O backend **não confere** se o domínio é institucional. Imutável após criação. |
| `phone` | Texto numérico | Sim | Simples | `(19) 91234-5678` | vazio | Formato `(00) 90000-0000`, conferido na edição do perfil; no cadastro só não pode ser vazio. Exibido no perfil. |
| `photoUrl` | URL | Não | Simples | `https://storage.firebase...` | URL inválida | URL da imagem armazenada no Firebase Storage. Nulo até o upload. |
| `participationRoles` | Elemento de lista enumerada | Sim | Multivalorado (M) | `["PASSENGER"]`, `["DRIVER", "PASSENGER"]` | lista vazia, valor fora do enum | Mínimo 1 valor. Valores: `PASSENGER`, `DRIVER`. Selecionado no onboarding. |
| `academicRecord` | Dados de vínculo universitário | Sim | Composto (C) | ver tipo especial 3.1 | incompleto, nulo | Obrigatório no cadastro. Contém universidade, curso, matrícula e status da verificação. |
| `driverLicense` | Dados de habilitação (CNH) | Não | Composto (C) | ver tipo especial 3.13 | — | Obrigatório apenas para quem atua como motorista. Deve estar `VERIFIED` e dentro da validade para aparecer no matching, receber pedidos e aceitar passageiros (o cadastro do trajeto não confere). Nulo para passageiros. |
| `pixKey` | Texto | Não | Simples | `+5519912345678` (PHONE), `ana@email.com` (EMAIL) | vazio para quem oferece carona | Chave PIX do motorista, normalizada ao salvar conforme o `pixKeyType`. Obrigatória para aparecer no matching como motorista, receber pedidos e aceitar passageiros (senão o passageiro não tem para quem pagar). O cadastro do trajeto não confere. Usada para gerar o QR Code "Pix Copia e Cola". |
| `pixKeyType` | Elemento de lista enumerada | Não | Simples | `CPF`, `PHONE`, `EMAIL`, `RANDOM` | tipo que não corresponde à chave | Tipo da chave PIX. Necessário porque o formato no payload depende dele (uma chave de 11 dígitos pode ser CPF ou celular). Nulo se não houver chave. |
| `paymentStanding` | Elemento de lista enumerada | Sim | Simples | `UP_TO_DATE`, `HAS_OVERDUE`, `BLOCKED` | valor fora do enum | Situação de pagamento do passageiro. `UP_TO_DATE` sem dívidas vencidas; `HAS_OVERDUE` com dívidas vencidas abaixo do limite; `BLOCKED` ao atingir o limite (padrão 2). Ver "Inadimplência" na modelagem. |
| `overdueRidesCount` | Número inteiro | Sim | Simples | `0`, `1`, `2` | negativo | Quantidade de dívidas vencidas: cobranças não marcadas como pagas em 5 minutos (`converge.payment.due-after`) e denúncias `NON_PAYMENT`. Soma 1 a cada dívida e tira 1 quando uma cobrança vencida é paga. Ao chegar em `converge.payment.overdue-limit` (padrão 2), o passageiro é bloqueado. |
| `isDefaulter` | Booleano | Sim | Simples | `true`, `false` | nulo | `false` por padrão. Muda para `true` quando o passageiro é bloqueado (atingiu o limite de dívidas vencidas, padrão 2). Só volta a `false` quando `overdueRidesCount` chega a 0, ou seja, quando **todas** as dívidas por prazo forem pagas. A dívida de denúncia `NON_PAYMENT` só sai pela moderação (fora do MVP). |
| `defaulterSince` | Data e hora | Não | Simples | `2025-05-20T00:00:00Z` | data futura | Preenchido quando `isDefaulter = true`. Nulo quando em dia. |
| `reputation` | Dados de reputação por papel | Sim | Composto (C) | ver tipo especial 3.2 | nulo | Separada por papel (driver e passenger). A média e o `level` são gravados quando uma avaliação é salva; `completedRides` quando a viagem é concluída; cancelamentos e ausências quando ocorrem. A janela de 90 dias é **recalculada semanalmente** pelo job dos selos a partir das viagens. Ver tipo especial 3.2. |
| `badges` | Selos de reconhecimento | Não | Multivalorado e Composto (MC) | ver tipo especial 3.3 | — | Concedidos e removidos automaticamente por um job semanal (segunda-feira, 03h, America/Sao_Paulo) quando o usuário atinge ou deixa de atender os critérios. Cada item grava o selo no campo `badge` (não `type`). Ver tipo especial 3.3. |
| `active` | Booleano | Sim | Simples | `true`, `false` | nulo | `true` por padrão. `false` ao desativar a conta (`DELETE /api/v1/users/me`): a conta inativa não pede vaga, não aceita passageiros e não aparece no matching. Ver "Desativação de Conta" na modelagem. |
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
| `renavam` | Texto numérico | Sim | Simples | `14283256656` | `1428325665` (10 dígitos), `14283256657` (dígito verificador errado), `11111111111` | Registro Nacional de Veículos Automotores. 11 dígitos, guardados sem pontuação; o último é o dígito verificador (soma dos 10 primeiros com os pesos 3, 2, 9, 8, 7, 6, 5, 4, 3, 2; dígito = soma × 10 mod 11, com 10 virando 0). Pode faltar em veículos cadastrados antes do campo existir. A conferência é feita pelo **servidor de sockets** (`ValidateRenavamHandler`) ao criar e ao editar o veículo, antes de montar a entidade; o `Vehicle` só guarda os dígitos e não valida o dígito verificador sozinho. Mensagens de erro: `O RENAVAM deve ter 11 dígitos.` e `RENAVAM inválido.` |
| `color` | Texto | Sim | Simples | `Branco`, `Prata`, `Azul` | vazio | Texto livre. Auxilia o passageiro a identificar o veículo. |
| `type` | Elemento de lista enumerada | Sim | Simples | `HATCH`, `VAN`, `MOTORCYCLE` | valor fora do enum | Valores: `HATCH`, `SEDAN`, `SUV`, `PICKUP`, `VAN`, `MOTORCYCLE`, `OTHER`. Moto: até 2 lugares (contando o piloto). |
| `totalCapacity` | Número inteiro | Sim | Simples | `5`, `7`, `15` (van), `2` (moto) | `0`, negativo, carro com mais de 7, maior que 15 | Inclui o motorista. Carro (hatch, sedã, SUV, picape): até 7. Van ou outro: até 15. Moto: até 2. |
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
| `weekDays` | Elemento de lista enumerada | Sim | Multivalorado (M) | `["MONDAY","TUESDAY"]` | lista vazia | Mínimo 1 dia. Valores: `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`. Em `ONCE`, é o dia da semana de `date`. |
| `recurrence` | Enumerado | Não | Simples | `ONCE`, `WEEKLY` | outro valor | `ONCE`: só na data de `date`. `WEEKLY`: toda semana nos `weekDays`. Ausente vale `WEEKLY`. |
| `date` | Data | Não | Simples | `2026-11-21`, `null` | data passada | Formato `yyyy-MM-dd`, no fuso **America/Sao_Paulo**. Obrigatória em `ONCE` e nula em `WEEKLY`. |
| `maxDetourKm` | Número decimal | Sim | Simples | `2.0`, `0.5`, `5.0` | negativo, zero | Somente para `OFFER`. Distância máxima de desvio aceita pelo motorista. Usado no matching. |
| `toll` | Número decimal monetário | Não | Simples | `12.50`, `null` | negativo | Somente para `OFFER`. Valor estimado de pedágio informado pelo motorista na oferta. `Decimal128`. |
| `parking` | Número decimal monetário | Não | Simples | `20.00`, `null` | negativo | Somente para `OFFER`. Valor estimado de estacionamento informado pelo motorista na oferta. `Decimal128`. |
| `totalSeats` | Número inteiro | Não | Simples | `3`, `8` | `0`, negativo | Somente para `OFFER`. Informado pelo app (o app usa `totalCapacity do veículo - 1`). O backend só exige maior que 0 e não confere com a capacidade do veículo. |
| `availableSeats` | Número inteiro | Não | Simples | `3`, `1`, `0` | negativo, maior que totalSeats | Somente para `OFFER`. Copia `totalSeats` na criação e não muda depois. As vagas que restam ficam em `rides.availableSeats`, por data. |
| `routeDistanceKm` | Número decimal | Não | Simples | `12.4`, `5.8` | negativo, zero | Distância pelas ruas, calculada pela Routes API (Google Maps) na criação e em cada edição. Se o Google estiver indisponível, recebe a distância em linha reta (Haversine), calculada pelo **servidor de sockets** (`DistanceHandler`, em lote). Base para custos e desvios. |
| `encodedPolyline` | Texto | Não | Simples | `x~ijCzbe_HnAkBtCwE` | `""` (texto vazio) | Desenho do caminho no formato *encoded polyline* do Google, usado pelo app para traçar o trajeto no mapa. Calculado junto com `routeDistanceKm`. Nulo quando a distância veio do Haversine. |
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
| `version` | Número inteiro longo | Não | Simples | `0`, `1`, `2` | negativo | Controle de concorrência otimista (`@Version` no Spring Data MongoDB). Incrementado a cada mutação concorrente. |
| `driverId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao `_id` do User motorista. |
| `vehicleId` | Texto alfanumérico | Sim | Simples | `64b2e1d3...` | nulo | Referência ao `_id` do Vehicle. Dados copiados no snapshot. |
| `vehicleSnapshot` | Dados do veículo | Sim | Composto (C) | ver tipo especial 3.6 | nulo | Cópia imutável dos dados do veículo no momento da criação da viagem. |
| `driverRouteId` | Texto alfanumérico | Sim | Simples | `64c3a2b1...` | nulo | Referência ao Route que originou esta viagem. |
| `origin` | Localização | Sim | Composto (C) | ver tipo especial 3.5 | nulo | Snapshot da origem. Imutável. |
| `destination` | Localização | Sim | Composto (C) | ver tipo especial 3.5 | nulo | Snapshot do destino. Imutável. |
| `departureDateTime` | Data e hora | Sim | Simples | `2025-05-12T10:30:00Z` | data passada no cadastro | Combinação da data concreta com `route.departureTime`, no horário de Brasília. No código é um `LocalDateTime` (sem fuso); o Mongo guarda em UTC, e a conversão usa o **fuso da máquina** onde o backend roda. Por isso o backend precisa rodar com o fuso America/Sao_Paulo (ex.: 07:30 BRT = 10:30 UTC). Numa máquina em UTC, os horários e os prazos ficam 3 horas errados (limitação conhecida). |
| `estimatedArrivalDateTime` | Data e hora | Sim | Simples | `2025-05-12T11:10:00Z` | anterior à partida | Armazenado em UTC. Calculada a partir da partida + duração estimada. |
| `directArrivalDateTime` | Data e hora | Não | Simples | `2025-05-12T11:00:00Z` | anterior à partida | Armazenado em UTC. Chegada pela rota direta, sem paradas, guardada na criação. Volta a ser a `estimatedArrivalDateTime` quando a viagem fica sem passageiros. |
| `startedAt` | Data e hora | Não | Simples | `2025-05-12T10:32:00Z` | — | Armazenado em UTC. Hora real em que o motorista iniciou a viagem (`POST /rides/{id}/start`, que muda o status para `IN_PROGRESS`). Nulo até iniciar. O histórico mostra o início e o fim reais, e a janela de reputação usa esta data como a data das ausências (`NO_SHOW`). Viagens iniciadas antes de o campo existir (19/11/2026) não têm a data, e as ausências delas não entram na janela de 90 dias. |
| `completedAt` | Data e hora | Não | Simples | `2025-05-12T11:15:00Z` | anterior à partida | Armazenado em UTC. Nulo até o motorista concluir a viagem. |
| `weekDays` | Elemento de lista enumerada | Sim | Multivalorado (M) | `["MONDAY","WEDNESDAY"]` | lista vazia | Cópia dos dias do trajeto no momento da criação. |
| `totalSeats` | Número inteiro | Sim | Simples | `3`, `8` | `0`, negativo | Copiado do trajeto. Não se altera após a criação. |
| `availableSeats` | Número inteiro | Sim | Simples | `2`, `0` | negativo, maior que totalSeats | Atualizado atomicamente ao confirmar ou cancelar participante. |
| `status` | Elemento de lista enumerada | Sim | Simples | `CONFIRMED`, `COMPLETED` | valor fora do enum | Valores: `AWAITING_CONFIRMATION`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`. |
| `cancellationReason` | Texto | Não | Simples | `Imprevisto pessoal` | — | Presente somente quando `status = CANCELLED`. |
| `cancelledBy` | Texto alfanumérico | Não | Simples | `64a1f3c2...` | — | Referência ao User que cancelou. |
| `cancelledAt` | Data e hora | Não | Simples | `2025-05-12T09:00:00Z` | — | Armazenado em UTC. Momento em que a viagem foi cancelada (pelo motorista, pelo job de não iniciada, por falta de participantes ou por todos os passageiros ausentes). Nulo em viagens não canceladas e nas canceladas antes da versão 1.9.0. Usado para datar o cancelamento do motorista na janela de reputação. |
| `driverCancellationPenalized` | Booleano | Sim | Simples | `false`, `true` | — | `false` por padrão. `true` só quando o motorista cancelou a viagem com passageiro confirmado, o mesmo caso que soma em `driverInitiatedCancellations`. Cancelamentos automáticos ficam `false`. |
| `chatRoomId` | Texto | Não | Simples | `ride_64d4c3b2` | — | Identificador da sala de chat no **Firebase Realtime Database**. As mensagens não ficam no MongoDB — só a referência da sala. |
| `costs` | Detalhamento de custos | Sim | Composto (C) | ver tipo especial 3.7 | nulo | Recalculado a cada aceite, saída ou ausência; os valores finais vêm do encerramento da viagem. Todos os valores monetários em `Decimal128`. |
| `participants` | Participação na viagem | Sim | Multivalorado e Composto (MC) | ver tipo especial 3.8 | lista vazia | Mínimo 1 elemento (o motorista). |
| `itinerary` | Parada do itinerário | Não | Multivalorado e Composto (MC) | ver tipo especial 3.14 | — | Paradas na ordem em que o motorista busca (`PICKUP`) e deixa (`DROPOFF`) os passageiros confirmados. Lista vazia quando não há passageiro confirmado. Recalculada ao aceitar um passageiro, quando um confirmado sai e quando o motorista remove um confirmado. Não muda depois de `IN_PROGRESS`. |
| `itineraryDistanceKm` | Número decimal | Não | Simples | `15.8`, `null` | negativo | Distância da rota passando pelas paradas. Vem da Routes API (Google); sem o Google, é a soma das linhas retas entre os pontos (calculada pelo servidor de sockets). Nulo quando não há paradas. `Double`. |
| `itineraryPolyline` | Texto | Não | Simples | `x~ijCzbe_HnAkBtCwE`, `null` | `""` | Desenho da rota com as paradas, em *encoded polyline* do Google. Nulo sem o Google ou sem paradas. O motorista sempre a recebe na API; o passageiro com vaga, só com a viagem em andamento. |
| `itineraryLegPolylines` | Lista de textos | Não | Multivalorado | `["a~b", "c~d", "e~f"]`, `[]` | quantidade diferente de paradas + 1 | O mesmo caminho em trechos (*encoded polyline* do Google): um até cada parada e o último até o destino. O app desenha forte o trecho atual e apagados os próximos. Vazio sem o Google, sem paradas ou se a quantidade não bater. Mesma visibilidade de `itineraryPolyline`. |
| `createdAt` | Data e hora | Sim | Simples | `2025-05-10T20:00:00Z` | data futura | Gerado automaticamente. |
| `updatedAt` | Data e hora | Sim | Simples | `2025-05-12T08:15:00Z` | anterior à criação | Atualizado a cada mudança de status ou participante. |

---

### 2.5. Review (Avaliação)

Review é o registro de nota e comentário na relação **motorista ↔ passageiro** após a conclusão de uma viagem. Passageiros **não avaliam outros passageiros** — apenas o motorista avalia cada passageiro e cada passageiro avalia o motorista. Cada par (avaliador, avaliado) registra no máximo uma avaliação por viagem (unicidade por `rideId + reviewerId + reviewedId`). As notas alimentam a reputação por papel do usuário.

**Regras do MVP:**
- **Quem pode avaliar e ser avaliado:** apenas quem **concluiu** a viagem, ou seja, o participante com `status = COMPLETED`. A viagem pode estar `COMPLETED` ou ainda `IN_PROGRESS`: o passageiro que já desceu (chegada confirmada pelo motorista) avalia na hora e pode ser avaliado, sem esperar o fim da viagem dos outros (`GetCompletedRideParticipantsUseCase`). Passageiro ausente (`NO_SHOW`), recusado (`REJECTED`), expirado (`EXPIRED`), que cancelou ou saiu (`CANCELLED`) e quem só pediu a vaga (`REQUESTED`) não avaliam nem são avaliados. Viagem cancelada ou sem passageiro efetivo não gera avaliação.
- **Sem edição e sem exclusão:** a avaliação é imutável depois de criada.
- **Sem prazo:** não há limite de tempo para avaliar depois que a viagem termina. Um prazo pode ser definido numa versão futura.
- **Papel do avaliado:** o servidor calcula o `reviewedRole` a partir da viagem. O cliente não envia esse campo.
- **Média de avaliação:** depois de salvar a avaliação, o sistema recalcula a média e a quantidade a partir de **todas** as avaliações recebidas no papel (`reviewedId + reviewedRole`) e as grava em `reputation.driver` (se `reviewedRole = DRIVER`) ou `reputation.passenger` (se `PASSENGER`) do avaliado, junto com o `level` (ver tipo 3.2). A média gravada tem 2 casas decimais. Se essa gravação falhar, a avaliação continua salva e o erro só vai para o log. A média exibida no matching continua calculada na hora a partir desta coleção.

#### Atributos de Review

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64e5d4c3...` | vazio, nulo | Gerado pelo banco. |
| `rideId` | Texto alfanumérico | Sim | Simples | `64d4c3b2...` | nulo | Referência ao Ride. A viagem precisa estar `COMPLETED` ou `IN_PROGRESS` (avaliação ao descer). |
| `reviewerId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao User que avalia. |
| `reviewedId` | Texto alfanumérico | Sim | Simples | `64a9f8e7...` | igual ao reviewerId, nulo | Referência ao User avaliado. Deve ter concluído a mesma viagem (participante `COMPLETED`). |
| `reviewedRole` | Elemento de lista enumerada | Sim | Simples | `DRIVER`, `PASSENGER` | valor fora do enum | Papel do avaliado naquela viagem. Alimenta a reputação correta. |
| `rating` | Número inteiro | Sim | Simples | `1`, `3`, `5` | `0`, `6`, negativo | Escala de 1 a 5 estrelas. O app deve enviar apenas inteiros: o servidor não valida decimais e descarta a parte fracionária (`4.5` vira `4`). Entra na média do avaliado no papel correspondente. |
| `comment` | Texto | Não | Simples | `Ótima viagem!`, `Pontual e educado.` | — | Texto livre. Exibido no histórico de avaliações. |
| `createdAt` | Data e hora | Sim | Simples | `2025-05-12T09:00:00Z` | data anterior à conclusão da viagem | Gerado automaticamente. |

---

### 2.6. Report (Denúncia)

Report é o registro formal de uma ocorrência reportada por um usuário contra outro. Mantém histórico completo para suporte à moderação da plataforma.

**Estado atual:** o registro (`POST /api/v1/reports`) e a consulta das próprias denúncias (`GET /api/v1/reports/mine`) estão implementados (RF45, RF25). A **moderação não existe**: não há papel de moderador, então `resolution`, `reviewedBy` e os status `UNDER_REVIEW`, `RESOLVED` e `ARCHIVED` não são escritos por nenhum fluxo. As denúncias ficam `PENDING` e `substantiatedReports` das janelas de reputação continua em 0.

**Regras do registro:**
- Quem denuncia é o usuário logado; não pode denunciar a si mesmo; o denunciado precisa existir.
- `LATE_NO_SHOW` e `NON_PAYMENT` exigem `rideId`. `OTHER` exige `description`. Os demais motivos aceitam `rideId` opcional.
- Quando `rideId` vem, denunciante e denunciado precisam ter participado da viagem (motorista ou passageiro que teve vaga, em qualquer status da viagem, inclusive ausente ou cancelado; pedido `REQUESTED`, `REJECTED` ou `EXPIRED` não conta).
- Denúncia idêntica (mesmo denunciante, denunciado, `rideId` e `reason`) ainda `PENDING` é recusada.

#### Atributos de Report

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64f6e5d4...` | vazio, nulo | Gerado pelo banco. |
| `reporterId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao User que registra a denúncia. |
| `reportedId` | Texto alfanumérico | Sim | Simples | `64a9f8e7...` | igual ao reporterId, nulo | Referência ao User denunciado. |
| `rideId` | Texto alfanumérico | Não | Simples | `64d4c3b2...` | `LATE_NO_SHOW` sem viagem | Referência ao Ride relacionado. **Obrigatório quando `reason = LATE_NO_SHOW`**; opcional nos demais. Quando vem, os dois usuários precisam ter participado da viagem. |
| `reason` | Elemento de lista enumerada | Sim | Simples | `INAPPROPRIATE_CONDUCT`, `LATE_NO_SHOW` | valor fora do enum | Valores: `LATE_NO_SHOW`, `INAPPROPRIATE_CONDUCT`, `IMPROPER_CHARGE`, `NON_PAYMENT`, `OTHER`. `NON_PAYMENT` exige `rideId`, só o motorista da viagem denuncia, o passageiro precisa ter sido cobrado e já ter marcado "Já paguei"; vale uma por passageiro em cada viagem e conta como uma dívida (RF67). |
| `description` | Texto | Não | Simples | `O motorista cobrou mais do combinado.` | mais de 500 caracteres, vazio com `OTHER` | Detalhamento livre, **máximo 500 caracteres** (limite definido no código). **Obrigatório quando `reason = OTHER`.** |
| `status` | Elemento de lista enumerada | Sim | Simples | `PENDING`, `UNDER_REVIEW` | valor fora do enum | Valores: `PENDING`, `UNDER_REVIEW`, `RESOLVED`, `ARCHIVED`. Nasce `PENDING`. Hoje só `PENDING` é gravado; os demais dependem da moderação, que não existe. |
| `resolution` | Texto | Não | Simples | `Usuário advertido.` | — | **Pendente (sem moderação):** nenhum fluxo escreve este campo e a API não o expõe. Seria preenchido pela moderação ao encerrar a análise. |
| `reviewedBy` | Texto alfanumérico | Não | Simples | `64mod1...` | — | **Pendente (sem moderação):** nenhum fluxo escreve este campo e a API não o expõe. Seria a referência ao User moderador que analisou. |
| `createdAt` | Data e hora | Sim | Simples | `2025-05-12T10:00:00Z` | data futura | Gerado automaticamente. |
| `updatedAt` | Data e hora | Sim | Simples | `2025-05-15T14:00:00Z` | anterior à criação | Atualizado ao mudar o status. |

---

### 2.7. Block (Bloqueio)

Block é o registro de que um usuário optou por impedir interações com outro. O bloqueio é bidirecional para fins de interação — se A bloqueou B, nenhum dos dois verá o outro no matching nem consegue pedir ou aceitar vaga com o outro.

**Endpoints (RF46):** `POST /api/v1/blocks` (idempotente, `201`), `DELETE /api/v1/blocks/{blockedUserId}` (idempotente, `204`) e `GET /api/v1/blocks` (lista de quem o logado bloqueou, com o nome do bloqueado). Quem bloqueia é sempre o usuário logado.

**Onde o bloqueio vale (RF47):**
- Matching: sim, nos dois sentidos.
- Pedido de vaga e aceite do passageiro: sim, nos dois sentidos (`NOT_ALLOWED`). Como a sala de chat só é liberada no aceite, quem já está bloqueado não chega a ter a conversa criada.
- Viagem já aceita e ainda não iniciada: sim. Ao bloquear, o `BlockedUsersRideListener` separa os dois (se o motorista bloqueou, o passageiro sai; se o passageiro bloqueou, ele sai), sem penalidade, com a vaga liberada e aviso `RIDE_CANCELLED`. Viagem em andamento não é afetada.
- Chat **depois** do aceite: **não coberto pelo backend**. As mensagens vivem no Firebase Realtime Database, sem a API no meio. O app desabilita o envio no chat com um usuário bloqueado.
- Desbloquear remove só o bloqueio do usuário logado; se o outro lado também bloqueou, a interação segue barrada.

#### Atributos de Block

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64g7f6e5...` | vazio, nulo | Gerado pelo banco. |
| `userId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao User que realizou o bloqueio. |
| `blockedUserId` | Texto alfanumérico | Sim | Simples | `64a9f8e7...` | igual ao userId, nulo | Referência ao User bloqueado. O par (userId + blockedUserId) é único no sistema. |
| `createdAt` | Data e hora | Sim | Simples | `2025-05-13T08:00:00Z` | data futura | Gerado automaticamente no ato do bloqueio. |

---

### 2.8. ReservedSeat (Vaga Reservada Recorrente)

> **Ainda sem código:** não existe entidade (`@Document`), repositório nem endpoint para `reserved_seats` no backend. Esta seção descreve o desenho previsto.

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
| `estimatedCostPerPassenger` | Número decimal monetário | Não | Simples | `6.00`, `4.50`, `null` | negativo | `Decimal128`. Estimativa do valor que o passageiro pagará. `null` quando o veículo não tem consumo e preço. Exibido na lista de combinações. |
| `valid` | Booleano | Sim | Simples | `true`, `false` | nulo | `false` quando algum dos trajetos é editado ou inativado. Somente `true` é exibido ao usuário. |
| `expiresAt` | Data e hora | Não | Simples | `2025-06-01T00:00:00Z` | anterior à criação | Data de expiração automática (TTL). MongoDB deleta o documento após esta data. |
| `createdAt` | Data e hora | Sim | Simples | `2025-05-12T06:00:00Z` | data futura | Gerado automaticamente quando o algoritmo calcula o resultado. |

---

### 2.10. Indicator (Indicadores)

Indicator é o conjunto de métricas de mobilidade e sustentabilidade de um usuário em um período específico (o financeiro fica para evolução futura). São pré-calculados por período (`WEEKLY`, `MONTHLY`, `SEMESTERLY`) e atualizados ao concluir cada viagem. O CO₂ e os carros evitados vêm do `SustainabilityHandler` e o texto de `reference` do `IndicatorReferenceHandler`, ambos no servidor de sockets. A tela de Dados usa um **filtro de período** (semana / mês / 3 meses / semestre). O período `TOTAL` foi removido — o acumulado histórico é calculado **somando apenas os documentos `MONTHLY`** sob demanda (nunca misturar granularidades, pois têm dados sobrepostos), com paginação quando houver muitos meses. Sem cache persistente — o cálculo soma poucos documentos pequenos, é leve.

#### Atributos de Indicator

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64i9h8g7...` | vazio, nulo | Gerado pelo banco. |
| `userId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao `_id` do User dono dos indicadores. |
| `period` | Elemento de lista enumerada | Sim | Simples | `MONTHLY`, `WEEKLY` | `TOTAL`, valor fora do enum | Valores: `WEEKLY`, `MONTHLY`, `SEMESTERLY`. O período `TOTAL` não existe mais como documento — é calculado somando os meses na leitura. |
| `reference` | Texto alfanumérico | Sim | Simples | `2025-05`, `2025-S1`, `2025-W20` | vazio | Para mensal: `AAAA-MM`. Para semestral: `AAAA-S1/S2`. Para semanal: `AAAA-W##`. |
| `ridesAsDriver` | Número inteiro | Sim | Simples | `12`, `0` | negativo | Total de viagens concluídas como motorista no período. |
| `ridesAsPassenger` | Número inteiro | Sim | Simples | `22`, `0` | negativo | Total de viagens concluídas como passageiro no período. |
| `sharedKm` | Número decimal | Sim | Simples | `272.8`, `0.0` | negativo | Total de quilômetros compartilhados no período (`Double`). |
| `carsAvoided` | Número inteiro | Sim | Simples | `22`, `0` | negativo | Quantidade estimada de veículos individuais evitados no trânsito. |
| `co2AvoidedKg` | Número decimal | Sim | Simples | `50.7`, `0.0` | negativo | Estimativa de emissão de CO₂ evitada em quilogramas (`Double`). |
| `appliedRideIds` | Texto alfanumérico | Sim | Multivalorado (M) | `["64d4c3b2..."]` | lista nula | Lista de `rides._id` cujos dados já foram agregados neste indicador (garante idempotência). |
| `updatedAt` | Data e hora | Sim | Simples | `2025-05-12T08:20:00Z` | anterior à criação | Atualizado sempre que uma viagem do período é concluída. |

> **Nota de compatibilidade conceitual:** Para o MVP, a entidade utiliza campos planos acima para simplicidade e performance. Estruturas aninhadas como `financial` e `series` (seções 3.9 a 3.12) representam expansões planejadas para versões futuras.

---

### 2.11. PlatformIndicator (Indicadores da Plataforma)

Indicadores **globais** agregados de toda a plataforma, por período, para atender ao RF51 (totais da plataforma) e exibições de impacto coletivo. Diferente de Indicator, não tem `userId` — representa o somatório de todos os usuários. Atualizado **a cada viagem concluída**, com operações atômicas, e **reconciliado a cada 15 minutos** por uma tarefa agendada que completa as viagens que ficaram sem contabilização.

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
| `appliedRideIds` | Texto alfanumérico | Sim nos períodos `WEEKLY`, `MONTHLY` e `SEMESTERLY`; **não existe em `ALL_TIME`** | Multivalorado (M) | `["64d4c3b2..."]` | lista nula (nos três períodos que a usam) | Lista de IDs de caronas (`rides._id`) já somadas neste documento, para idempotência. O documento `ALL_TIME` não a guarda: é recalculado a partir dos documentos `SEMESTERLY`, não por viagem. |
| `calculatedAt` | Data e hora | Sim | Simples | `2025-05-31T23:00:00Z` | — | Timestamp da última atualização do documento (por viagem ou pela reconciliação). |

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

> **Ainda sem código:** não existe entidade (`@Document`), repositório nem endpoint para `universities` no backend. Hoje `academicRecord.university` é gravado como texto. Esta seção descreve o desenho previsto.

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
| `type` | Elemento de lista enumerada | Sim | Simples | `PAYMENT_DUE`, `REQUEST_EXPIRED` | valor fora do enum | Ver enum `NotificationType` (`RIDE_REQUEST`, `RIDE_CONFIRMED`, `RIDE_CANCELLED`, `REQUEST_EXPIRED`, `PAYMENT_DUE`, `PAYMENT_CONFIRMED`, `SEAT_RELEASED`, `GENERIC`). Hoje são gravados `RIDE_CONFIRMED`, `RIDE_CANCELLED`, `REQUEST_EXPIRED`, `PAYMENT_DUE` e `PAYMENT_CONFIRMED`; `RIDE_REQUEST`, `SEAT_RELEASED` e `GENERIC` estão reservados. |
| `title` | Texto | Sim | Simples | `Pagamento pendente` | vazio | Título curto do aviso. |
| `message` | Texto | Sim | Simples | `Você tem uma carona a pagar.` | vazio | Corpo do aviso. |
| `relatedEntityType` | Texto | Não | Simples | `RIDE`, `RESERVED_SEAT` | — | Tipo da entidade relacionada, para navegação ao tocar no aviso. |
| `relatedEntityId` | Texto alfanumérico | Não | Simples | `64d4c3b2...` | — | Id da entidade relacionada. |
| `read` | Booleano | Sim | Simples | `true`, `false` | nulo | `false` por padrão. Marcado `true` quando o usuário vê. |
| `createdAt` | Data e hora | Sim | Simples | `2025-05-12T08:20:00Z` | — | Momento do aviso. Notificações **não expiram** (não há índice TTL); expiração em ~90 dias é evolução futura. |

---

### 2.15. IdempotencyKey (Chave de Idempotência)

Registra as chaves de idempotência já processadas, para evitar que uma operação crítica rode duas vezes se o app reenviar a requisição por falha de rede.

#### Atributos de IdempotencyKey

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64i1d2k3...` | vazio, nulo | Gerado pelo banco. |
| `key` | Texto alfanumérico | Sim | Simples | `c3f8e562-b13c...` | duplicada, vazio | Chave UUID v4 enviada pelo cliente no cabeçalho `Idempotency-Key`. Única. |
| `userId` | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao `_id` do User que originou a requisição. |
| `method` | Texto | Sim | Simples | `POST`, `PUT`, `PATCH` | vazio, `DELETE` | Método HTTP da operação protegida. Só `POST`, `PUT` e `PATCH` em `/api/v1/rides/**` passam pelo filtro de idempotência. |
| `path` | Texto | Sim | Simples | `/api/v1/rides/64d.../accept` | vazio | URI do recurso acionado. |
| `requestFingerprint` | Texto | Sim | Simples | hash SHA-256 | vazio | Hash do corpo da requisição para verificar consistência em caso de repetição. |
| `status` | Elemento de lista enumerada | Sim | Simples | `IN_PROGRESS`, `COMPLETED`, `UNKNOWN_OUTCOME` | fora do enum | Estado da execução da operação associada à chave. |
| `responseStatus` | Número inteiro | Não | Simples | `200`, `201`, `400` | — | Código HTTP de retorno para replay transparente ao cliente. |
| `contentType` | Texto | Não | Simples | `application/json` | — | Tipo de mídia da resposta cacheada. |
| `responseBody` | Texto | Não | Simples | payload JSON serializado | — | Resposta retornada para responder requisições repetidas sem reprocessar mutações. |
| `createdAt` | Data e hora | Sim | Simples | `2025-05-12T08:20:00Z` | — | Momento do primeiro recebimento. Expira por TTL de 24 horas. |

### 2.16. IndicatorRideRegistration (Viagem Contabilizada nos Indicadores)

Marca as viagens que já terminaram de ser contabilizadas nos indicadores (`indicators` e `platform_indicators`). A tarefa de reconciliação, que roda a cada 15 minutos, usa esta coleção para achar as viagens concluídas nos últimos 7 dias que ficaram sem contabilização. Existe só em `MongoIndicatorStore` (`REGISTRATIONS_COLLECTION`): não tem classe de entidade, o documento é gravado direto. Módulo dono: `modules/indicator`.

#### Atributos de IndicatorRideRegistration

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| `_id` | Texto alfanumérico | Sim | Simples | `64d4c3b2...` | duplicado, nulo | É o próprio `rides._id`. Como `_id` é único, a mesma viagem não é registrada duas vezes (a duplicata é ignorada). |
| `registeredAt` | Data e hora | Sim | Simples | `2025-05-12T08:20:00Z` | — | Momento em que a viagem terminou de ser contabilizada. Gravado só depois de todos os documentos de indicadores da viagem terem sido atualizados. Sem TTL. |

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

Estrutura composta que representa os indicadores de comportamento do usuário separados por papel.

**Como cada campo é gravado hoje:**
- `averageRating` e `totalReviews`: recalculados a partir de **todas** as avaliações recebidas no papel, logo depois de salvar uma avaliação (só o papel avaliado), e só são gravados se o total for maior que o já gravado. A média é gravada com 2 casas decimais, e esse valor entra nas regras de `level` e de selo. Se a gravação falhar, a avaliação continua salva (só log).
- `window.completedRides`: +1 para o motorista e +1 para cada passageiro efetivo quando a viagem é concluída. O registro roda **depois do commit** da conclusão da viagem (`@TransactionalEventListener` com `AFTER_COMMIT`, fora da transação); uma falha nele é capturada e só vai para o log, então não desfaz nem impede a conclusão. Cada conclusão conta uma vez (o evento é publicado uma vez por viagem). Viagem sem passageiro efetivo não gera evento e não conta.
- `window.driverInitiatedCancellations`, `window.lateCancellations` e `window.noShows`: incrementados pelas penalidades.
- `level` e `calculatedAt`: recalculados a cada gravação acima.
- Gravação: a reputação é gravada por **update parcial atômico** (`$set`), com filtro por `_id`. O documento inteiro do usuário não é regravado. Os selos são gravados por `$push`/`$pull` em `badges`. Assim, avaliação, penalidade, viagem concluída e job dos selos não sobrescrevem os outros campos do usuário nem o outro papel.
- Gravação da nota (avaliação): `$set` só em `reputation.<papel>.averageRating`, `totalReviews`, `level` e `calculatedAt` (mais `updatedAt`), sem tocar em `window`. Só aplica se o `totalReviews` gravado estiver ausente ou for menor que o novo. Penalidade e viagem concluída gravam só `reputation.<papel>.window`, `level` e `calculatedAt` (mais `updatedAt`), sem tocar em `averageRating` e `totalReviews`. Assim uma não desfaz a outra.
- Avaliações simultâneas no mesmo papel: uma nota calculada com menos avaliações que a já gravada é ignorada (só log), então a mais antiga não sobrescreve a mais nova. Limite: o `level` usa a janela lida antes da gravação, então uma penalidade simultânea pode deixar o `level` defasado até a próxima gravação de reputação.

**Regra de `level` (igual nos dois papéis):** `LOW` quando algum limite de penalidade é estourado; `HIGH` quando cumpre os critérios de confiança do papel (tabelas abaixo); `MEDIUM` no resto. A regra roda de novo a cada média, viagem concluída ou penalidade, então uma penalidade pode rebaixar `HIGH` para `MEDIUM`.

**Limitações conhecidas:**
- `HIGH` só é alcançável com a gravação da média e a contagem de `completedRides` funcionando juntas.
- Nenhum fluxo grava `window.delays` nem `window.substantiatedReports` (depende da moderação de denúncias, que não existe); ficam em 0. O job semanal não os recalcula.
- `noShows` é datado pelo `startedAt` da viagem, porque a participação não guarda a data da ausência. Viagens iniciadas antes de 19/11/2026 não têm `startedAt`, então as ausências delas **não entram** na janela: no primeiro job, quem tinha essas ausências antigas volta a 0 em `noShows`.
- Cancelamentos do motorista e remoções de passageiro confirmado gravados antes da versão 1.9.0 não têm `cancelledAt`, `driverCancellationPenalized` nem `removedByDriver` e **não entram** na janela: no primeiro job, o contador de quem tinha essas penalidades antigas volta a 0.
- Entre duas execuções do job os contadores seguem somando em tempo real; o job reescreve os valores uma vez por semana.
- O job lê as contagens uma vez, no início. Um incremento em tempo real entre essa leitura e a gravação do usuário é sobrescrito e só volta a ficar certo no job seguinte.

**Janela deslizante de 90 dias:** toda segunda-feira, antes de avaliar os selos, o job semanal recalcula `completedRides`, `driverInitiatedCancellations`, `lateCancellations`, `noShows` e `overdueRides` de cada usuário a partir dos fatos datados das viagens (coleção `rides`), em `[agora − N dias, agora]`. `N` é a propriedade `converge.user.reputation-window-days` (padrão 90; menor que 1 volta para 90). Um fato com exatamente `N` dias ainda conta. Quem não tem fato na janela volta a 0. Depois o job recalcula o `level` e grava só `window`, `level` e `calculatedAt` do papel, sem tocar em `averageRating` e `totalReviews`. Datas usadas: `Ride.completedAt` (viagens concluídas), `Participant.cancelledAt` (cancelamento tardio), `Ride.startedAt` (ausência), `Ride.cancelledAt` (cancelamento da viagem pelo motorista), `Participant.cancelledAt` com `removedByDriver` (remoção de passageiro confirmado, que também soma em `driverInitiatedCancellations`) e `Participant.overdueAt` (dívida).

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
| `level` | Elemento de lista enumerada | Sim | Valores: `HIGH`, `MEDIUM`, `LOW`, `null` (sem histórico ainda). Ver a regra de `level` acima. |
| `averageRating` | Número decimal | Sim | Média das notas recebidas no papel, com 2 casas. Começa em `0.0`. |
| `totalReviews` | Número inteiro | Sim | Quantidade total de avaliações recebidas no papel. |
| `calculatedAt` | Data e hora | Sim | Timestamp da última gravação da reputação do papel (média, viagem concluída ou penalidade). |

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
| `overdueRides` | Número inteiro | Sim | Dívidas vencidas registradas (cobrança não paga no prazo ou denúncia `NON_PAYMENT`). Soma 1 a cada dívida. Pagar não diminui: só sai da janela depois de 90 dias, quando o job semanal recalcula. Critério do selo de confiança: deve ser 0. |

---

### 3.3. Badge (Selo de Reconhecimento)

Estrutura composta que representa um selo concedido ao usuário. Concedido e removido automaticamente pelo **job semanal** (`BadgeRecalculationScheduler`, toda segunda-feira às 03h, fuso America/Sao_Paulo; expressão trocável pela propriedade `converge.user.badge-recalculation-cron`).

**Critérios de concessão:**

| Selo | Critério |
| :--- | :--- |
| `TRUST` | Não estourou nenhum limite de penalidade **e** cumpre os critérios de confiança (ver tipo 3.2 Reputation) em **pelo menos um** dos papéis, motorista ou passageiro. A janela de 90 dias é recalculada pelo job a partir das viagens antes de o selo ser avaliado. |
| `SUSTAINABILITY` | CO₂ evitado acumulado **≥ 50 kg** (soma dos `indicators` `MONTHLY` do usuário, lida pelo caso de uso de leitura do módulo `indicator`, período `ALL_TIME`). Considera apenas viagens concluídas com ao menos um passageiro efetivo — impacto real de compartilhamento, não motorista rodando sozinho. |

**Como o job funciona:** pede ao módulo `ride`, uma vez, os fatos da janela de todos os usuários (se isso falhar, o job inteiro para). Depois percorre os usuários em páginas de 100 (ordenadas por `_id`), recalcula a janela e o `level` de cada um (grava só se mudaram), concede ou revoga os dois selos e só grava quando algo muda, relendo o usuário antes. A data `grantedAt` original é preservada em um selo que continua válido. Um erro em um usuário é contado e logado, os selos dele não são revogados e o job segue. Uma penalidade que estoura o limite revoga o `TRUST` na hora; ele só volta no próximo job. Não há auditoria de concessão ou revogação (não existe ação de selo em `AuditAction`).

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `badge` | Elemento de lista enumerada | Sim | Valores: `TRUST`, `SUSTAINABILITY`. O nome do campo no código e no banco é `badge` (record `UserBadge(Badge badge, LocalDateTime grantedAt)`), não `type`. |
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
| `detourGeneratedKm` | Número decimal | Não | Desvio gerado por este passageiro. `Double`. Calculado no aceite pelo `RideCostHandler` (3 trechos pelas ruas menos a rota base); é outro valor que o desvio em linha reta do matching. Nulo para o motorista e antes do aceite. |
| `status` | Elemento de lista enumerada | Sim | Valores: `REQUESTED`, `REJECTED`, `CONFIRMED`, `CANCELLED`, `NO_SHOW`, `COMPLETED`, `EXPIRED`. |
| `requestedAt` | Data e hora | Sim | Momento em que o passageiro solicitou a vaga (RF19). |
| `respondedAt` | Data e hora | Não | Momento em que o motorista aceitou ou recusou. Nulo enquanto `REQUESTED`. |
| `rejectionReason` | Texto | Não | Motivo da recusa pelo motorista. Presente somente quando `status = REJECTED`. |
| `cancellationReason` | Texto | Não | Preenchido pelo participante ao cancelar. |
| `lateCancellation` | Booleano | Sim | `false` por padrão. `true` quando o passageiro confirmado cancela com menos de 2h de antecedência. Pesa só na reputação (`lateCancellations`); não gera cobrança. |
| `removedByDriver` | Booleano | Sim | `false` por padrão. `true` só quando o motorista remove um passageiro já `CONFIRMED` (o status vai para `CANCELLED` e `cancelledAt` é gravado), o mesmo caso que soma em `driverInitiatedCancellations`. Recusar pedido pendente (`REJECTED`) não marca. Remoções anteriores à versão 1.9.0 ficam `false`. |
| `estimatedAmount` | Número decimal monetário | Sim | `Decimal128`. Valor estimado atual (RF34/RF36). Recalculado a cada entrada, cancelamento ou ausência de participantes. Nunca ultrapassa o `maxAmount`. |
| `maxAmount` | Número decimal monetário | Sim | `Decimal128`. Teto de proteção do passageiro (RF35/RF37): custo inteiro da rota com o desvio do passageiro, gravado na confirmação. Nunca pode ser superado pelo `finalAmount`. |
| `finalAmount` | Número decimal monetário | Não | `Decimal128`. Gravado quando o passageiro termina a viagem (confirmação de chegada ou encerramento). Ausente fica com `0`. Nulo antes disso. |
| `paymentStatus` | Elemento de lista enumerada | Sim | Valores: `PENDING`, `PAID`, `NOT_APPLICABLE`. Todo passageiro nasce `PENDING`; só com `paymentRequestedAt` é uma cobrança em aberto, que bloqueia pedir vaga e cadastrar trajeto (RF74). |
| `requestedAmount` | Número decimal monetário | Não | `Decimal128`. Valor cobrado: o `finalAmount` do rateio, gravado automaticamente no fim da viagem do passageiro (RF39). Nunca ultrapassa o `maxAmount` quando este estiver definido. Nulo até a cobrança. |
| `paymentRequestedAt` | Data e hora | Não | Momento da última cobrança. Nulo até a primeira cobrança. |
| `paidAt` | Data e hora | Não | Momento em que o passageiro informou o pagamento ("Já paguei"). |
| `confirmedAt` | Data e hora | Sim | Timestamp de entrada do participante na viagem. |
| `overdueAt` | Data e hora | Não | Quando a cobrança virou dívida vencida (prazo de 5 minutos sem "Já paguei", ou denúncia `NON_PAYMENT`). Nulo = não vencida. A cobrança continua `PENDING`. Só é marcado de novo quando o passageiro deixou vencer, marcou "Já paguei" e depois chega uma denúncia `NON_PAYMENT` da mesma viagem. |
| `nonPaymentReportedAt` | Data e hora | Não | Quando a dívida veio de uma denúncia `NON_PAYMENT`. Nulo para dívida por prazo. "Já paguei" não desfaz essa dívida. |
| `boardedAt` | Data e hora | Não | Quando o motorista confirmou que o passageiro entrou no carro (`POST /rides/{id}/passengers/{passengerId}/board`, só com a viagem em andamento). Nulo até o embarque. Quem já embarcou não pode ser marcado como ausente. |
| `cancelledAt` | Data e hora | Não | Nulo enquanto não cancelar. |
| `updatedAt` | Data e hora | Sim | Atualizado a cada mudança no subdocumento. |

---

> **Nota:** as seções 3.9 a 3.12 descrevem a **estrutura completa prevista**. No MVP, o indicador por usuário grava uma versão **enxuta e plana** (ver seção 2.10 e o JSON 4.10): só `ridesAsDriver`, `ridesAsPassenger`, `sharedKm`, `carsAvoided` e `co2AvoidedKg`. Não existem os subdocumentos `mobility` e `sustainability` no banco. As seções 3.9 e 3.10 mostram, cada uma, **o que já existe** (campos que no MVP ficam planos no documento) e **o que é evolução futura**. As seções 3.11 e 3.12 são inteiramente evolução futura.

### 3.9. MobilityData (Dados de Mobilidade) — campos planos no MVP; subdocumento é evolução futura

> No MVP estes campos ficam **direto no documento `indicators`**, sem o subdocumento `mobility`. `totalRides` não é gravado: é calculado na leitura (`ridesAsDriver + ridesAsPassenger`).

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `ridesAsDriver` | Número inteiro | Sim | Viagens concluídas no período em que o usuário foi motorista. |
| `ridesAsPassenger` | Número inteiro | Sim | Viagens concluídas no período em que o usuário foi passageiro. |
| `sharedKm` | Número decimal | Sim | Soma dos km das viagens participadas no período. `Double`. |
| `carsAvoided` | Número inteiro | Sim | Cada passageiro efetivo = 1 carro evitado. (No MVP o campo chama-se `carsAvoided`.) |

---

### 3.10. SustainabilityData (Dados de Sustentabilidade) — só `co2AvoidedKg` existe no MVP

> No MVP só `co2AvoidedKg` é gravado, direto no documento `indicators` (sem o subdocumento `sustainability`). `equivalentTrees` é calculado na leitura (`co2AvoidedKg / 22`). `fuelSavedL` e `energySavedKwh` **não são gravados** e ficam como evolução futura. O cálculo vem do `SustainabilityHandler` do servidor de sockets.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `fuelSavedL` | Número decimal | Não | Para veículos de combustão. Nulo para elétricos. `Double`. |
| `energySavedKwh` | Número decimal | Não | Para veículos elétricos. Nulo para combustão. `Double`. |
| `co2AvoidedKg` | Número decimal | Sim | `energia economizada × fator de emissão`. Fator varia pela fonte de energia. `Double`. |
| `equivalentTrees` | Número decimal | Sim | `co2AvoidedKg / 22`. Referência: 1 árvore absorve ~22 kg CO₂/ano. `Double`. |

---

### 3.11. FinancialData (Dados Financeiros do Indicador) — evolução futura

> Não implementado no MVP. Depende dos dados de pagamento das viagens.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `totalPassengerSavings` | Número decimal monetário | Sim | `Decimal128`. Diferença entre o custo de ir sozinho e o valor pago no rateio. |
| `totalDriverIncome` | Número decimal monetário | Sim | `Decimal128`. Soma dos `finalAmount` recebidos pelo motorista no período. |

---

### 3.12. SeriesPoint (Ponto de Série Temporal) — evolução futura

> Não implementado no MVP. Será usado quando a tela tiver gráfico de evolução.

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
| `expiresAt` | Data | Sim | Data de validade da CNH. Depois dela, a CNH vale como `EXPIRED` na leitura (o status gravado continua `VERIFIED`; não há job que regrave) e o motorista não oferece carona. |
| `documentUrl` | URL | Não | URL da foto da CNH no Firebase Storage, para conferência. |
| `status` | Elemento de lista enumerada | Sim | Valores: `PENDING`, `UNDER_REVIEW`, `VERIFIED`, `REJECTED`, `EXPIRED`. |
| `verifiedAt` | Data e hora | Não | Preenchido quando a CNH é aprovada (`VERIFIED`). |
| `rejectionReason` | Texto | Não | Motivo da recusa. Presente somente quando `status = REJECTED`. |

---

### 3.14. RideStop (Parada do Itinerário)

Estrutura composta e multivalorada usada no array `itinerary` da Ride. Cada item é uma parada em que o motorista busca ou deixa um passageiro confirmado. A ordem do array é a ordem de visita.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| `type` | Elemento de lista enumerada | Sim | Enum `RideStopType`. Valores: `PICKUP` (buscar o passageiro no embarque) e `DROPOFF` (deixar o passageiro no desembarque). |
| `passengerId` | Texto alfanumérico | Sim | Referência ao `_id` do User passageiro da parada. |
| `place` | Location (C) | Sim | Local da parada (endereço, bairro, cidade e coordenadas GeoJSON; ver tipo 3.5). |

**Ordem (`RideItineraryPlanner`):** o motorista sai da origem e busca todos os confirmados, sempre o embarque mais próximo de onde ele está. Depois deixa cada um, sempre o desembarque mais próximo, e segue para o destino. Em empate de distância vence o menor `userId`. As distâncias vêm do `DistanceHandler` do servidor de sockets, em lote.

**Privacidade:** os pontos são endereços dos passageiros. Na API, o motorista recebe todas as paradas, a distância e a polyline; cada passageiro recebe só as próprias paradas, sem a polyline.

---

## 4. Desdobramento físico — banco orientado a documentos (MongoDB)

| Entidade conceitual | Coleção MongoDB | Módulo dono | Estratégia |
| :--- | :--- | :--- | :--- |
| User | `users` | `modules/user` | `academicRecord`, `reputation` e `badges[]` embutidos (sempre consultados juntos). |
| Vehicle | `vehicles` | `modules/user` | `fuelSources[]` como MC. Referenciado por id em routes e rides. |
| Route | `routes` | `modules/route` | `origin` e `destination` embutidos com coordenadas (índice geoespacial). `weekDays` como array de strings. |
| Ride | `rides` | `modules/ride` | `participants[]` embutidos (atomicidade). `costs` e `vehicleSnapshot` embutidos (snapshot imutável). `itinerary[]` (`RideStop`), `itineraryDistanceKm` e `itineraryPolyline` embutidos (paradas na ordem de visita). |
| Review | `reviews` | `modules/review` | Documento simples com referências por id. |
| Report | `reports` | `modules/security` | Documento simples com referências por id. Registro e consulta das próprias denúncias implementados; moderação ainda não existe. |
| Block | `blocks` | `modules/security` | Documento simples com referências por id. Endpoints de bloquear, desbloquear e listar implementados. |
| ReservedSeat | `reserved_seats` | `modules/ride` | **Ainda sem código** (sem `@Document`). Vínculo de vaga reservada. `weekDays` e `skippedDates[]` como arrays simples. |
| Matching | `matchings` | `modules/matching` | `compatibleDays` como array de strings. `passengerId` e `driverId` denormalizados. |
| Indicator | `indicators` | `modules/indicator` | Versão enxuta: campos planos (`ridesAsDriver`, `ridesAsPassenger`, `sharedKm`, `carsAvoided`, `co2AvoidedKg`) e `appliedRideIds[]`, sem subdocumentos. `financial` e `series[]` são evolução futura. Sem período `TOTAL` nem `ALL_TIME` por usuário (o acumulado soma os `MONTHLY`). |
| PlatformIndicator | `platform_indicators` | `modules/indicator` | Documento global por período. `ALL_TIME` é armazenado (pequeno, só totais) e não tem `appliedRideIds`. Atualizado a cada viagem concluída e reconciliado a cada 15 minutos. |
| IndicatorRideRegistration | `indicator_ride_registrations` | `modules/indicator` | Sem classe de entidade. `_id` é o `rideId` (impede registrar a viagem duas vezes). Marca as viagens já contabilizadas nos indicadores. |
| AuditLog | `audit_logs` | `modules/shared` | Append-only. Referências por id. `metadata` flexível sem esquema rígido. |
| University | `universities` | `modules/shared` | **Ainda sem código** (sem `@Document`). Lista de referência. `emailDomains` como array. Pouco alterada. |
| Notification | `notifications` | `modules/shared` | Documento simples por aviso. Sem TTL: os avisos ficam guardados (expiração é evolução futura). |
| IdempotencyKey | `idempotency_keys` | `modules/shared` | Documento simples com `key` única e TTL de 24 h. |

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
  "pixKey": "+5519912345678",
  "pixKeyType": "PHONE",
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
    { "badge": "TRUST", "grantedAt": "2025-09-15T00:00:00Z" }
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
  "renavam": "14283256656",
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
  "recurrence": "WEEKLY",
  "date": null,
  "maxDetourKm": 2.0,
  "toll": { "$numberDecimal": "12.50" },
  "parking": { "$numberDecimal": "0.00" },
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
  "version": 1,
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
  "directArrivalDateTime": "2025-05-12T11:00:00Z",
  "completedAt": "2025-05-12T11:15:00Z",
  "weekDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
  "totalSeats": 3,
  "availableSeats": 0,
  "status": "COMPLETED",
  "cancellationReason": null,
  "cancelledBy": null,
  "cancelledAt": null,
  "driverCancellationPenalized": false,
  "chatRoomId": "ride_64d4c3b2f7e3d4c5b6a7f8e9",
  "itinerary": [
    {
      "type": "PICKUP",
      "passengerId": "64a9f8e7d6c5b4a3f2e1d0c9",
      "place": {
        "address": "Rua das Flores, 50",
        "neighborhood": "Centro",
        "city": "Campinas",
        "location": { "type": "Point", "coordinates": [-47.0650, -22.9100] }
      }
    },
    {
      "type": "DROPOFF",
      "passengerId": "64a9f8e7d6c5b4a3f2e1d0c9",
      "place": {
        "address": "Campus UNIV — Bloco B",
        "neighborhood": "Jardim Universitário",
        "city": "Campinas",
        "location": { "type": "Point", "coordinates": [-47.0500, -22.8333] }
      }
    }
  ],
  "itineraryDistanceKm": 14.5,
  "itineraryPolyline": "x~ijCzbe_HnAkBtCwE",
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
      "requestedAt": "2025-05-10T20:00:00Z",
      "respondedAt": "2025-05-10T20:00:00Z",
      "rejectionReason": null,
      "cancellationReason": null,
      "lateCancellation": false,
      "estimatedAmount": { "$numberDecimal": "2.51" },
      "maxAmount": { "$numberDecimal": "2.51" },
      "finalAmount": { "$numberDecimal": "2.51" },
      "paymentStatus": "NOT_APPLICABLE",
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
      "requestedAt": "2025-05-11T06:30:00Z",
      "respondedAt": "2025-05-11T07:00:00Z",
      "rejectionReason": null,
      "cancellationReason": null,
      "lateCancellation": false,
      "estimatedAmount": { "$numberDecimal": "2.51" },
      "maxAmount": { "$numberDecimal": "6.00" },
      "finalAmount": { "$numberDecimal": "2.51" },
      "paymentStatus": "PAID",
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
  "ridesAsDriver": 0,
  "ridesAsPassenger": 22,
  "sharedKm": 272.8,
  "carsAvoided": 22,
  "co2AvoidedKg": 50.7,
  "appliedRideIds": [
    "64d4c3b2f7e3d4c5b6a7f8e9"
  ],
  "updatedAt": "2025-05-30T09:00:00Z"
}
```

> **MVP (versão enxuta implementada):** campos planos, sem os subdocumentos `mobility`/`sustainability`. `totalRides` (= `ridesAsDriver + ridesAsPassenger`) e `equivalentTrees` (= `co2AvoidedKg / 22`) são **calculados na leitura**, não gravados. O campo de carros evitados chama-se **`carsAvoided`**. Não há documento `ALL_TIME` por usuário — o acumulado é a soma dos meses.
>
> **Evolução futura (ainda não implementada):** `financial { totalPassengerSavings, totalDriverIncome }`, `series[]` e os campos `fuelSavedL`/`energySavedKwh` ficam para uma versão futura. As seções 3.9 a 3.12 abaixo descrevem essa estrutura futura completa.

---

### 4.11. Coleção `platform_indicators`

```json
{
  "_id": "64f1a2b3f4e5d6c7b8a9f0e1",
  "period": "MONTHLY",
  "reference": "2025-05",
  "totalRides": 1248,
  "totalSharedKm": 18420.5,
  "totalCarsAvoided": 340,
  "totalCo2AvoidedKg": 3100.0,
  "activeUsers": 214,
  "appliedRideIds": [
    "64d4c3b2f7e3d4c5b6a7f8e9"
  ],
  "calculatedAt": "2025-05-31T23:00:00Z"
}
```

> `appliedRideIds` existe nos períodos `WEEKLY`, `MONTHLY` e `SEMESTERLY`. O documento `ALL_TIME` (`reference = "ALL_TIME"`) não tem esse campo: é recalculado a partir dos documentos `SEMESTERLY`.

---

### 4.12. Coleção `audit_logs`

```json
{
  "_id": "64a1d2e3f4e5d6c7b8a9f0e2",
  "action": "PAYMENT_CONFIRMED",
  "actorId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "targetUserId": "64a9f8e7d6c5b4a3f2e1d0c9",
  "entityType": "RIDE",
  "entityId": "64d4c3b2f7e3d4c5b6a7f8e9",
  "metadata": {
    "amount": "2.51",
    "method": "PIX_MANUAL"
  },
  "createdAt": "2025-05-12T08:20:00Z"
}
```

---

### 4.13. Coleção `universities`

```json
{
  "_id": "64b1c2d3f4e5d6c7b8a9f0e3",
  "name": "Pontifícia Universidade Católica de Campinas",
  "acronym": "PUC-Campinas",
  "city": "Campinas",
  "emailDomains": [
    "puc-campinas.edu.br"
  ],
  "active": true
}
```

---

### 4.14. Coleção `notifications`

```json
{
  "_id": "64c1e2f3f4e5d6c7b8a9f0e4",
  "userId": "64a9f8e7d6c5b4a3f2e1d0c9",
  "type": "REQUEST_EXPIRED",
  "title": "Solicitação expirada",
  "message": "Sua solicitação de vaga na carona expirou por falta de resposta do motorista dentro do prazo.",
  "relatedEntityType": "RIDE",
  "relatedEntityId": "64d4c3b2f7e3d4c5b6a7f8e9",
  "read": false,
  "createdAt": "2025-05-12T08:20:00Z"
}
```

---

### 4.15. Coleção `idempotency_keys`

```json
{
  "_id": "64d1e2f3f4e5d6c7b8a9f0e5",
  "key": "c3f8e562-b13c-4e89-9a2d-22fa10b98091",
  "userId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "method": "POST",
  "path": "/api/v1/rides/64d4c3b2f7e3d4c5b6a7f8e9/passengers/64a9f8e7d6c5b4a3f2e1d0c9/accept",
  "requestFingerprint": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "status": "COMPLETED",
  "responseStatus": 200,
  "contentType": "application/json",
  "responseBody": "{\"success\":true,\"data\":{\"status\":\"CONFIRMED\"}}",
  "createdAt": "2025-05-12T08:20:00Z"
}
```

---

### 4.16. Coleção `indicator_ride_registrations`

```json
{
  "_id": "64d4c3b2f7e3d4c5b6a7f8e9",
  "registeredAt": "2025-05-12T08:20:05Z"
}
```

> O `_id` é o `rides._id` da viagem contabilizada. Não há classe de entidade: o documento é gravado direto pelo `MongoIndicatorStore`.

---

## 5. Consultas, índices e observações de arquitetura

> **Criados de fato:** só os índices declarados nas entidades Java (`@Indexed` e `@CompoundIndex`), que o Spring cria ao subir (`auto-index-creation: true`). Os índices desta tabela que não aparecem nas entidades são **sugestões ainda não criadas**: `participants.userId` (sozinho ou com `participants.paymentStatus`), `academicRecord.status`, `driverLicense.status`, `driverLicense.expiresAt + driverLicense.status`, `weekDays + status`, `driverId + status`, `driverId + participants.status` e `userId + period` (este último já é atendido pelo índice único `userId + period + reference`, que começa pelos mesmos campos). No volume do MVP as consultas funcionam sem eles; ficam para quando houver mais dados.

| Consulta esperada | Campos envolvidos | Índices sugeridos | Observações |
| :--- | :--- | :--- | :--- |
| Buscar usuário por e-mail (login) | `email` | `{ email: 1 }` único | Consulta crítica no fluxo de autenticação. |
| Validar unicidade de e-mail institucional | `academicRecord.institutionalEmail` | `{ "academicRecord.institutionalEmail": 1 }` único parcial (`$type: 'string'`) | Garante que cada e-mail universitário pertença a no máximo uma conta. |
| Buscar usuário por UID Firebase | `firebaseUid` | `{ firebaseUid: 1 }` único | Executada a cada requisição autenticada. |
| Verificar inadimplência no matching | `isDefaulter` | `{ isDefaulter: 1 }` | Checagem rápida antes de exibir combinações. |
| Tela de pendências do passageiro | `participants.userId`, `participants.paymentStatus` | `{ "participants.userId": 1, "participants.paymentStatus": 1 }` | Lista as cobranças emitidas ainda `PENDING` (`GET /rides/payments/pending`). |
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
| Garantir apenas uma carona aberta por trajeto e horário | `driverRouteId`, `departureDateTime` | `{ driverRouteId: 1, departureDateTime: 1 }` único parcial (`status in [AWAITING_CONFIRMATION, CONFIRMED, IN_PROGRESS, COMPLETED]`) | Garante que o motorista tenha apenas uma viagem aberta por data/horário em um trajeto. |
| Buscar combinações válidas por passageiro | `passengerRouteId`, `valid`, `compatibilityScore` | `{ passengerRouteId: 1, valid: 1, compatibilityScore: -1 }` | Lista ordenada por score. |
| Verificar bloqueio entre dois usuários | `userId`, `blockedUserId` | `{ userId: 1, blockedUserId: 1 }` único | Executada no matching, aceite de carona e chat. |
| Verificar avaliação duplicada | `rideId`, `reviewerId`, `reviewedId` | `{ rideId: 1, reviewerId: 1, reviewedId: 1 }` único | Impede dupla avaliação do mesmo par na mesma viagem. O `reviewedId` é necessário porque o motorista avalia cada passageiro da mesma viagem. Também atende "avaliações que o usuário já fez na viagem" (`rideId + reviewerId`). |
| Listar avaliações dadas por um usuário | `reviewerId` | `{ reviewerId: 1 }` | Verificar se o usuário já avaliou determinada viagem. |
| Buscar avaliações recebidas por papel | `reviewedId`, `reviewedRole` | `{ reviewedId: 1, reviewedRole: 1 }` | Base do cálculo de reputação por papel. |
| Buscar fila de denúncias pendentes | `status` | `{ status: 1 }` | Moderação — filtro por `PENDING` e `UNDER_REVIEW`. A fila de moderação ainda não existe; o índice existe na entidade `Report`. |
| Listar denúncias feitas por um usuário | `reporterId` | `{ reporterId: 1 }` | `GET /api/v1/reports/mine`, ordenado por `createdAt` decrescente. |
| Denúncias contra um usuário e por viagem | `reportedId`, `rideId` | `{ reportedId: 1 }`, `{ rideId: 1 }` | Histórico de denúncias de um usuário e de uma viagem. Também apoia a checagem de denúncia idêntica pendente. |
| Listar bloqueios feitos por um usuário | `userId` | `{ userId: 1 }` | `GET /api/v1/blocks`, do mais recente para o mais antigo. |
| Viagens já contabilizadas nos indicadores | `_id` (= `rideId`) | índice `_id` padrão | `indicator_ride_registrations`: o `_id` único impede registrar a mesma viagem duas vezes e a reconciliação busca as ausentes. |
| Percorrer usuários no job semanal de selos | `_id` | índice `_id` padrão | Leitura paginada (100 por página, ordenada por `_id`) feita pelo `RecalculateBadgesUseCase`. |
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

### Cálculos no servidor de sockets (RT04, RT20, RT22)

Cálculos em memória e validações determinísticas rodam no **servidor de sockets** (`app/server`). O backend chama o servidor por `SocketClient` e, se ele estiver fora do ar ou responder outro tipo de mensagem, executa o **mesmo handler localmente**, com o mesmo resultado. Nenhum handler grava dados; o caso de uso do backend grava o resultado nas coleções.

| Handler | Calcula ou valida | Campos afetados |
| :--- | :--- | :--- |
| `SustainabilityHandler` | Carros evitados, energia poupada, CO₂ evitado e árvores equivalentes | `indicators.carsAvoided`, `indicators.co2AvoidedKg` e os totais de `platform_indicators` |
| `IndicatorReferenceHandler` | Texto de `reference` (`AAAA-MM`, `AAAA-S1/S2`, semana ISO `AAAA-W##`, `ALL_TIME`), em lote | `indicators.reference`, `platform_indicators.reference` |
| `DistanceHandler` | Distância Haversine em km, em lote (até 1000 pares por chamada) | `routes.routeDistanceKm` e `rides.itineraryDistanceKm` (sem o Google), ordem de `rides.itinerary` |
| `ValidateRenavamHandler` | RENAVAM com dígito verificador módulo 11 | `vehicles.renavam` |
| `ValidateCpfHandler` | CPF com dígitos verificadores | Nenhum (só a resposta de `POST /api/v1/users/validate-cpf`) |

Já existiam e seguem o mesmo padrão: `RideCostHandler` (rateio da viagem), `PixPayloadHandler` (Pix Copia e Cola) e `MatchRoutesHandler` (compatibilidade do matching). Os números calculados não mudaram com a migração (comparação exata com as classes antigas do backend, que saíram do backend: `SustainabilityCalculator`, `IndicatorReference` e `HaversineCalculator`).

### Pagamento PIX — geração do "Copia e Cola"

No MVP o pagamento usa **QR Code PIX estático** ("Pix Copia e Cola") gerado pelo próprio backend, **sem gateway e sem API externa**. O dinheiro vai direto da conta do passageiro para a do motorista — o Converge nunca toca no dinheiro. Como o banco não devolve confirmação ao servidor, o pagamento é **informado pelo próprio passageiro** ("Já paguei", que grava `paymentStatus = PAID`); o motorista não confirma o recebimento.

- O payload (padrão BR Code do Banco Central, com CRC16) é **montado sob demanda e não armazenado** — é derivado de dados que já existem (`users.pixKey`, `users.name`, `participants[].requestedAmount`, que é o `finalAmount` cobrado).
- **Endpoints:** `GET /rides/{rideId}/passengers/{passengerId}/payment` e `GET /rides/payments/pending` devolvem a string em `pixCopyPaste`; o app desenha o QR.
- **Quando gera:** cobrança emitida e em aberto (`requestedAmount` preenchido e `paymentStatus = PENDING`). Não há código depois do "Já paguei" (`PAID`), nem para `NO_SHOW` e `NOT_APPLICABLE`.
- **Autorização:** só o motorista e o próprio passageiro acessam, pois a chave PIX do motorista é dado pessoal.
- **Vínculo cobrança ↔ viagem:** como o PIX estático não tem txid, o vínculo é feito no banco (`rideId` + participante); o status `PAID` muda quando o passageiro informa o pagamento (`POST .../payment/mark-paid`).
