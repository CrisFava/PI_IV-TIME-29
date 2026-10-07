# Dicionário de Dados: Sistema Converge de Mobilidade Universitária Compartilhada

---

## 1. Histórico de versões

| Data | Autor | Versão | Comentários |
| :---: | :---: | ---: | :--- |
| 06/10/2026 | Daniela Mikie Kikuchi Gonçalves | 1.0.0-alfa1 | Criação do documento — estrutura base do dicionário de dados |
| 06/10/2026 | Daniela Mikie Kikuchi Gonçalves | 1.0.0-alfa2 | Versão inicial — título, estrutura do documento, descrição das 9 entidades e seus atributos conceituais |
| 07/10/2026 | Sara Monteiro | 1.0.0-alfa3 | Adição de `papelAvaliado` na entidade Avaliação; adição de `fonteEnergiaUsada`, `consumoMedioUsado` e `precoEnergiaUsado` no tipo composto Custos da Carona |
| 07/10/2026 | Sara Monteiro | 1.0.0-alfa4 | Adição dos atributos compostos `reputacao` e `selos` na entidade Usuário |
| 07/10/2026 | Sara Monteiro | 1.0.0-alfa5 | Reestruturação da entidade Veículo: atributo `consumos` como MC para suportar flex e híbrido |
| 07/10/2026 | Sara Monteiro | 1.0.0-alfa6 | Adição de `MOTO` em `TipoVeiculo`; validações de faixa de consumo e preço |
| 07/10/2026 | Daniela Mikie Kikuchi Gonçalves | 1.0.0-alfa7 | Adição da entidade Acordo Recorrente com extrato mensal, valor fixado e fluxo de proposta/aceite; adição de `statusCobranca`, `inadimplente`, `inadimplenteDesde` e critérios do Selo de Confiança com limiares toleráveis na entidade Usuário; adição de `cancelamentoTardio` e `veiculoSnapshot` na entidade Carona |

---

## 2. Entidades e atributos conceituais

A seguir são descritas as entidades que o projeto Converge contempla, seus atributos, tipos conceituais, características e regras de negócio. As entidades estão ordenadas da mais central para as de suporte.

> **Padrão de atributos especiais:**
> - **(C)** — Composto: estrutura formada por partes que juntas compõem o atributo.
> - **(M)** — Multivalorado: admite múltiplos valores para um mesmo registro.
> - **(MC)** — Multivalorado e Composto: lista de estruturas compostas (array de objetos).

---

### 2.1. Usuário

Usuário é toda pessoa que se cadastra na plataforma Converge. Pode assumir um ou mais papéis: passageiro (procura carona) ou motorista (oferece carona com veículo próprio). O usuário deve ter vínculo acadêmico verificado com uma universidade para utilizar as funcionalidades principais da plataforma. O sistema mantém indicadores de reputação por papel e concede selos de reconhecimento com base em comportamento ao longo do tempo.

#### Atributos de Usuário

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| identificador | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | vazio, nulo | Gerado automaticamente pelo banco. Imutável após criação. |
| identificador Firebase | Texto alfanumérico | Sim | Simples | `UID123abc` | vazio, nulo | UID gerado pelo Firebase Auth no primeiro login. Nunca alterado. Usado para validar tokens JWT. |
| nome completo | Texto | Sim | Simples | `Ana Souza`, `João da Silva` | `""`, menos de 3 caracteres | Nome completo da pessoa. Exibido em perfis, combinações e detalhes de viagem. |
| e-mail | Endereço de e-mail | Sim | Simples | `ana@puc-campinas.edu.br` | `ana@gmail.com`, vazio | Deve ser e-mail institucional acadêmico. Único no sistema. Imutável após criação. |
| telefone | Texto numérico | Sim | Simples | `(19) 91234-5678` | `19912345678`, vazio | Formato com DDD. Exibido no perfil. |
| foto de perfil | URL | Não | Simples | `https://storage.firebase...` | URL inválida | URL da imagem armazenada no Firebase Storage. Nulo até o upload. |
| tipos de participação | Elemento de lista enumerada | Sim | Multivalorado (M) | `["PASSAGEIRO"]`, `["MOTORISTA", "PASSAGEIRO"]` | lista vazia, valor fora do enum | Mínimo 1 valor. Valores possíveis: `PASSAGEIRO`, `MOTORISTA`. Selecionado no onboarding. |
| vínculo acadêmico | Dados de vínculo universitário | Sim | Composto (C) | ver tipo especial 3.1 | incompleto, nulo | Obrigatório no cadastro. Contém universidade, curso, matrícula e status da verificação. |
| chave PIX | Texto | Não | Simples | `(19) 91234-5678`, `cpf@email.com` | vazio com cobrança ativa | Obrigatório para motoristas que desejam cobrar. Exibida na tela de Pagamento via PIX. |
| frequência de cobrança preferida | Elemento de lista enumerada | Sim | Simples | `POR_VIAGEM`, `MENSAL` | valor fora do enum | Preferência padrão do motorista. Pode ser sobrescrita por carona. |
| inadimplente | Booleano | Sim | Simples | `true`, `false` | nulo | `false` por padrão. Muda para `true` no dia 20 sem pagamento do extrato mensal (após 3 avisos). Enquanto `true`, o usuário é bloqueado do matching. |
| inadimplente desde | Data e hora | Não | Simples | `2025-05-20T00:00:00Z` | data futura | Preenchido quando `inadimplente` muda para `true`. Nulo quando em dia. |
| status de cobrança | Elemento de lista enumerada | Sim | Simples | `EM_DIA`, `AVISO_1`, `INADIMPLENTE` | valor fora do enum | Controla a progressão de avisos. Valores: `EM_DIA`, `AVISO_1` (dia 5), `AVISO_2` (dia 10), `AVISO_3` (dia 15), `INADIMPLENTE` (dia 20). |
| reputação | Dados de reputação por papel | Sim | Composto (C) | ver tipo especial 3.2 | nulo | Calculada semanalmente com base na janela de 90 dias. Separada por papel (motorista e passageiro). |
| selos | Selos de reconhecimento | Não | Multivalorado e Composto (MC) | ver tipo especial 3.3 | — | Concedidos automaticamente quando o usuário atinge os critérios da janela de 90 dias. Removidos se deixar de atender. |
| conta ativa | Booleano | Sim | Simples | `true`, `false` | nulo | `true` por padrão. `false` para contas suspensas ou excluídas. |
| criado em | Data e hora | Sim | Simples | `2025-05-01T10:00:00Z` | data futura | Gerado automaticamente no cadastro. |
| atualizado em | Data e hora | Sim | Simples | `2025-09-15T08:30:00Z` | data anterior à criação | Atualizado a cada alteração no documento. |

---

### 2.2. Veículo

Veículo é o automóvel cadastrado por um motorista para ser utilizado nas caronas. Um usuário pode ter múltiplos veículos cadastrados, mas apenas um é associado a cada trajeto de oferta. Os dados do veículo são copiados como snapshot imutável no momento da confirmação de uma carona. O veículo suporta múltiplas fontes de energia (ex: carro flex tem gasolina e etanol como entradas separadas).

#### Atributos de Veículo

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| identificador | Texto alfanumérico | Sim | Simples | `64b2e1d3...` | vazio, nulo | Gerado pelo banco. Imutável. |
| id do proprietário | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao identificador do Usuário motorista dono do veículo. |
| apelido | Texto | Não | Simples | `Gol Azul`, `Van da Manhã` | — | Facilita a seleção quando o motorista tem mais de um veículo. |
| modelo | Texto | Sim | Simples | `Volkswagen Gol`, `Fiat Uno` | vazio | Exibido nos detalhes da carona. |
| placa | Texto alfanumérico | Sim | Simples | `ABC1234`, `ABC1D23` | placa duplicada, vazio | Única por proprietário. Armazenada em maiúsculas sem hífen. Aceita formato antigo e Mercosul. |
| cor | Texto | Sim | Simples | `Branco`, `Prata`, `Azul` | vazio | Texto livre. Auxilia o passageiro a identificar o veículo. |
| tipo | Elemento de lista enumerada | Sim | Simples | `HATCH`, `VAN`, `MOTO` | valor fora do enum | Valores: `HATCH`, `SEDAN`, `SUV`, `PICKUP`, `VAN`, `MOTO`, `OUTRO`. Moto tem capacidade máxima de 1. |
| capacidade total | Número inteiro | Sim | Simples | `5`, `15`, `1` (moto) | `0`, negativo, maior que 15 | Inclui o motorista. Mínimo 1 (moto). Máximo 15. |
| consumos | Dados de consumo por fonte de energia | Sim | Multivalorado e Composto (MC) | ver tipo especial 3.4 | lista vazia | Gasolina/etanol/diesel: 1 item cada. Flex: 2 itens (gasolina + etanol). Elétrico: 1 item (kWh). Não pode haver dois itens da mesma fonte. |
| ativo | Booleano | Sim | Simples | `true`, `false` | nulo | Veículo inativo não aparece na seleção de trajeto. |
| criado em | Data e hora | Sim | Simples | `2025-04-10T09:00:00Z` | data futura | Gerado automaticamente. |
| atualizado em | Data e hora | Sim | Simples | `2025-09-01T11:00:00Z` | data anterior à criação | Atualizado a cada edição. |

---

### 2.3. Trajeto

Trajeto é o registro de um deslocamento recorrente cadastrado por um usuário. Pode ser do tipo oferta (motorista disponibiliza vagas) ou procura (passageiro busca carona). O trajeto é a base de dados consumida pelo algoritmo de matching. Apenas trajetos com status `ATIVO` são elegíveis para o matching.

#### Atributos de Trajeto

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| identificador | Texto alfanumérico | Sim | Simples | `64c3a2b1...` | vazio, nulo | Gerado pelo banco. Referenciado em caronas, matchings e acordos recorrentes. |
| id do usuário | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao Usuário dono do trajeto. |
| id do veículo | Texto alfanumérico | Não | Simples | `64b2e1d3...` | — | Obrigatório somente para tipo `OFERECER`. Nulo para `PROCURAR`. |
| tipo | Elemento de lista enumerada | Sim | Simples | `OFERECER`, `PROCURAR` | valor fora do enum | Define o papel do usuário neste trajeto. |
| origem | Localização | Sim | Composto (C) | ver tipo especial 3.5 | nulo, incompleto | Ponto de partida do trajeto com endereço e coordenadas geográficas. |
| destino | Localização | Sim | Composto (C) | ver tipo especial 3.5 | nulo, incompleto | Ponto de chegada do trajeto com endereço e coordenadas geográficas. |
| horário de partida | Hora | Sim | Simples | `07:30`, `18:00` | `7:3`, `25:00` | Formato `HH:mm`. Armazenado sem data pois representa a hora do dia recorrente. |
| horário de chegada estimado | Hora | Sim | Simples | `08:10`, `19:00` | hora anterior à partida | Formato `HH:mm`. Calculado ou informado pelo usuário. |
| dias da semana | Elemento de lista enumerada | Sim | Multivalorado (M) | `["SEGUNDA","TERCA"]` | lista vazia | Mínimo 1 dia. Valores: `SEGUNDA`, `TERCA`, `QUARTA`, `QUINTA`, `SEXTA`, `SABADO`, `DOMINGO`. |
| desvio máximo aceitável (km) | Número decimal | Sim | Simples | `2.0`, `0.5`, `5.0` | negativo, zero | Somente para `OFERECER`. Distância máxima que o motorista aceita se desviar. Usado no matching. |
| vagas totais | Número inteiro | Não | Simples | `3`, `8` | `0`, negativo | Somente para `OFERECER`. Calculado como `capacidadeTotal do veículo - 1`. |
| vagas disponíveis | Número inteiro | Não | Simples | `3`, `1`, `0` | negativo, maior que vagas totais | Somente para `OFERECER`. Decrementado ao confirmar passageiro; incrementado ao cancelar. |
| distância da rota (km) | Número decimal | Não | Simples | `12.4`, `5.8` | negativo, zero | Calculada via Google Maps na criação. Base para cálculo de custos e desvios. |
| status | Elemento de lista enumerada | Sim | Simples | `ATIVO`, `PAUSADO`, `INATIVO` | valor fora do enum | Somente `ATIVO` entra no matching. |
| criado em | Data e hora | Sim | Simples | `2025-03-01T08:00:00Z` | data futura | Gerado automaticamente. |
| atualizado em | Data e hora | Sim | Simples | `2025-09-20T07:00:00Z` | data anterior à criação | Atualizado a cada edição. |

---

### 2.4. Carona

Carona (ou Viagem) é o evento central do sistema. Representa um deslocamento concreto, com data, horário, motorista, passageiros confirmados e custos. É criada a partir de uma combinação identificada pelo matching. Os dados de veículo, origem e destino são copiados como snapshot imutável no momento da criação.

#### Atributos de Carona

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| identificador | Texto alfanumérico | Sim | Simples | `64d4c3b2...` | vazio, nulo | Gerado pelo banco. Referenciado por avaliações, denúncias e extrato de acordos. |
| número sequencial | Número inteiro | Sim | Simples | `42`, `1`, `1000` | negativo, zero, duplicado | Número amigável exibido na UI: `Viagem #042`. Gerado por sequência no backend. |
| id do motorista | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao Usuário motorista da carona. |
| id do veículo | Texto alfanumérico | Sim | Simples | `64b2e1d3...` | nulo | Referência ao Veículo usado. Dados relevantes copiados no snapshot. |
| snapshot do veículo | Dados do veículo | Sim | Composto (C) | ver tipo especial 3.6 | nulo | Cópia imutável dos dados do veículo no momento da criação. Garante integridade do histórico mesmo que o veículo seja editado depois. |
| id do trajeto do motorista | Texto alfanumérico | Sim | Simples | `64c3a2b1...` | nulo | Referência ao Trajeto que originou esta carona. |
| origem | Localização | Sim | Composto (C) | ver tipo especial 3.5 | nulo | Snapshot da origem no momento da confirmação. Imutável. |
| destino | Localização | Sim | Composto (C) | ver tipo especial 3.5 | nulo | Snapshot do destino. Imutável. |
| data e hora de partida | Data e hora | Sim | Simples | `2025-05-12T07:30:00Z` | data passada no cadastro | Combinação da data concreta com o horário do trajeto. |
| data e hora de chegada estimada | Data e hora | Sim | Simples | `2025-05-12T08:10:00Z` | anterior à partida | Calculada a partir da partida + duração estimada da rota. |
| data e hora de conclusão | Data e hora | Não | Simples | `2025-05-12T08:15:00Z` | anterior à partida | Nulo até o motorista concluir a viagem. |
| dias da semana | Elemento de lista enumerada | Sim | Multivalorado (M) | `["SEGUNDA","QUARTA"]` | lista vazia | Cópia dos dias do trajeto no momento da criação. |
| vagas totais | Número inteiro | Sim | Simples | `3`, `8` | `0`, negativo | Copiado do trajeto. Não se altera após a criação. |
| vagas disponíveis | Número inteiro | Sim | Simples | `2`, `0` | negativo, maior que vagas totais | Atualizado atomicamente ao confirmar ou cancelar participante. |
| status | Elemento de lista enumerada | Sim | Simples | `CONFIRMADA`, `CONCLUIDA` | valor fora do enum | Valores: `AGUARDANDO_CONFIRMACAO`, `CONFIRMADA`, `EM_ANDAMENTO`, `CONCLUIDA`, `CANCELADA`. |
| motivo do cancelamento | Texto | Não | Simples | `Imprevisto pessoal` | — | Presente somente quando `status = CANCELADA`. |
| cancelado por | Texto alfanumérico | Não | Simples | `64a1f3c2...` | — | Referência ao Usuário que cancelou a carona. |
| frequência de cobrança | Elemento de lista enumerada | Sim | Simples | `POR_VIAGEM`, `MENSAL` | valor fora do enum | Definida pelo motorista para esta carona específica. |
| custos | Detalhamento de custos | Sim | Composto (C) | ver tipo especial 3.7 | nulo | Calculado e atualizado ao concluir a viagem. |
| participantes | Participação na carona | Sim | Multivalorado e Composto (MC) | ver tipo especial 3.8 | lista vazia | Mínimo 1 elemento (o motorista). Inclui dados individuais de embarque, status, cancelamento tardio e pagamento. |
| criado em | Data e hora | Sim | Simples | `2025-05-10T20:00:00Z` | data futura | Gerado automaticamente. |
| atualizado em | Data e hora | Sim | Simples | `2025-05-12T08:15:00Z` | anterior à criação | Atualizado a cada mudança de status ou de participante. |

---

### 2.5. Avaliação

Avaliação é o registro de nota e comentário que um participante faz sobre outro após a conclusão de uma viagem. Motorista avalia passageiros e passageiros avaliam o motorista. Cada par avaliador/avaliado pode registrar apenas uma avaliação por viagem. As notas recebidas alimentam a reputação do usuário por papel.

#### Atributos de Avaliação

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| identificador | Texto alfanumérico | Sim | Simples | `64e5d4c3...` | vazio, nulo | Gerado pelo banco. |
| id da carona | Texto alfanumérico | Sim | Simples | `64d4c3b2...` | nulo | Referência à Carona. Somente caronas com `status = CONCLUIDA` podem ser avaliadas. |
| id do avaliador | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao Usuário que avalia. |
| id do avaliado | Texto alfanumérico | Sim | Simples | `64a9f8e7...` | igual ao avaliador, nulo | Referência ao Usuário avaliado. Deve ser participante da mesma carona. |
| papel do avaliado | Elemento de lista enumerada | Sim | Simples | `MOTORISTA`, `PASSAGEIRO` | valor fora do enum | Indica o papel do avaliado naquela viagem. Usado para alimentar a reputação correta (motorista ou passageiro). |
| nota | Número inteiro | Sim | Simples | `1`, `3`, `5` | `0`, `6`, negativo, decimal | Escala de 1 a 5 estrelas. Ao salvar, recalcula a média da reputação do avaliado no papel correspondente. |
| comentário | Texto | Não | Simples | `Ótima viagem!`, `Pontual e educado.` | — | Texto livre. Exibido no histórico de avaliações do perfil do avaliado. |
| criado em | Data e hora | Sim | Simples | `2025-05-12T09:00:00Z` | data anterior à conclusão da carona | Gerado automaticamente. |

---

### 2.6. Denúncia

Denúncia é o registro formal de uma ocorrência reportada por um usuário contra outro. Pode estar vinculada a uma carona específica ou não. Mantém histórico completo para suporte à moderação da plataforma.

#### Atributos de Denúncia

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| identificador | Texto alfanumérico | Sim | Simples | `64f6e5d4...` | vazio, nulo | Gerado pelo banco. |
| id do denunciante | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao Usuário que registra a denúncia. |
| id do denunciado | Texto alfanumérico | Sim | Simples | `64a9f8e7...` | igual ao denunciante, nulo | Referência ao Usuário denunciado. |
| id da carona | Texto alfanumérico | Não | Simples | `64d4c3b2...` | — | Referência à Carona relacionada, quando aplicável. |
| motivo | Elemento de lista enumerada | Sim | Simples | `CONDUTA_INADEQUADA`, `ATRASO_FALTA` | valor fora do enum | Valores: `ATRASO_FALTA`, `CONDUTA_INADEQUADA`, `COBRANCA_INDEVIDA`, `OUTRO`. |
| descrição | Texto | Não | Simples | `O motorista cobrou mais do combinado.` | — | Campo opcional para detalhamento livre. |
| status | Elemento de lista enumerada | Sim | Simples | `PENDENTE`, `EM_ANALISE` | valor fora do enum | Valores: `PENDENTE`, `EM_ANALISE`, `RESOLVIDA`, `ARQUIVADA`. Default: `PENDENTE`. |
| resolução | Texto | Não | Simples | `Usuário advertido.` | — | Preenchido pela moderação ao encerrar a análise. |
| analisado por | Texto alfanumérico | Não | Simples | `64mod1...` | — | Referência ao Usuário moderador que analisou. |
| criado em | Data e hora | Sim | Simples | `2025-05-12T10:00:00Z` | data futura | Gerado automaticamente. |
| atualizado em | Data e hora | Sim | Simples | `2025-05-15T14:00:00Z` | anterior à criação | Atualizado ao mudar o status. |

---

### 2.7. Bloqueio

Bloqueio é o registro de que um usuário optou por impedir interações com outro usuário na plataforma. O bloqueio é bidirecional para fins de interação: se A bloqueou B, nenhum dos dois verá o outro em resultados de matching, não poderão compartilhar caronas nem trocar mensagens.

#### Atributos de Bloqueio

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| identificador | Texto alfanumérico | Sim | Simples | `64g7f6e5...` | vazio, nulo | Gerado pelo banco. |
| id do usuário | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao Usuário que realizou o bloqueio. |
| id do bloqueado | Texto alfanumérico | Sim | Simples | `64a9f8e7...` | igual ao usuário, nulo | Referência ao Usuário bloqueado. O par (usuário + bloqueado) é único no sistema. |
| criado em | Data e hora | Sim | Simples | `2025-05-13T08:00:00Z` | data futura | Gerado automaticamente no ato do bloqueio. |

---

### 2.8. Acordo Recorrente

Acordo Recorrente é a formalização da parceria habitual entre um passageiro e um motorista específico. É criado por **opt-in manual** — um dos lados propõe e o outro aceita. Garante ao passageiro uma vaga prioritária no trajeto do motorista com um valor fixado e cobrança mensal consolidada. O valor fixado no acordo **não é alterado** quando um passageiro avulso entra na mesma viagem — o avulso paga pelo valor calculado normalmente pelo sistema. O motorista pode propor renegociação do valor a qualquer momento.

#### Atributos de Acordo Recorrente

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| identificador | Texto alfanumérico | Sim | Simples | `64h1i2j3...` | vazio, nulo | Gerado pelo banco. |
| id do passageiro | Texto alfanumérico | Sim | Simples | `64a9f8e7...` | nulo | Referência ao Usuário passageiro do acordo. |
| id do motorista | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao Usuário motorista do acordo. |
| id do trajeto do motorista | Texto alfanumérico | Sim | Simples | `64c3a2b1...` | nulo | Referência ao Trajeto de oferta base do acordo. |
| id do trajeto do passageiro | Texto alfanumérico | Sim | Simples | `64c5d4e3...` | nulo | Referência ao Trajeto de procura base do acordo. |
| status do acordo | Elemento de lista enumerada | Sim | Simples | `PROPOSTO`, `ATIVO`, `SUSPENSO` | valor fora do enum | Valores: `PROPOSTO`, `ATIVO`, `SUSPENSO`, `ENCERRADO`. |
| proposto por | Elemento de lista enumerada | Sim | Simples | `PASSAGEIRO`, `MOTORISTA` | valor fora do enum | Indica quem iniciou o acordo. |
| motivo de suspensão | Elemento de lista enumerada | Não | Simples | `INADIMPLENCIA` | — | Presente somente quando `statusAcordo = SUSPENSO`. |
| motivo de encerramento | Texto | Não | Simples | `Motorista mudou de rota.` | — | Presente somente quando `statusAcordo = ENCERRADO`. |
| valor fixado no acordo (R$) | Número decimal | Sim | Simples | `6.00`, `8.50` | negativo, zero | Calculado no momento da criação com base na divisão atual do custo. Não muda quando passageiros avulsos entram. Pode ser renegociado pelo motorista. |
| mês de referência | Texto alfanumérico | Sim | Simples | `2025-05`, `2025-10` | vazio, formato inválido | Identifica o mês do extrato. Formato `AAAA-MM`. |
| cancelamentos tardios no mês | Número inteiro | Sim | Simples | `0`, `1`, `2`, `3` | negativo | Incrementado a cada cancelamento com menos de 2h de antecedência no mês corrente. A partir do 3º, todos os cancelamentos seguintes são cobrados. |
| avisos enviados | Número inteiro | Sim | Simples | `0`, `1`, `2`, `3` | negativo, maior que 3 | Contador de avisos de cobrança enviados no ciclo atual. Incrementado nos dias 5, 10 e 15 após o vencimento. |
| extrato do mês | Item de extrato mensal | Sim | Multivalorado e Composto (MC) | ver tipo especial 3.9 | — | Uma entrada por viagem ocorrida ou cancelamento tardio cobrado no mês. Máximo de ~31 itens. |
| total devido no mês (R$) | Número decimal | Sim | Simples | `132.00`, `0.00` | negativo | Soma de todos os `valorCobrado` do extrato do mês, incluindo cancelamentos tardios cobrados. |
| data limite de pagamento | Data | Sim | Simples | `2025-06-05` | data anterior ao fim do mês | Sempre o dia 5 do mês seguinte. A progressão de avisos e bloqueio é contada a partir desta data. |
| criado em | Data e hora | Sim | Simples | `2025-05-01T00:00:00Z` | data futura | Gerado automaticamente no início de cada mês para pares ativos. |
| atualizado em | Data e hora | Sim | Simples | `2025-05-15T08:00:00Z` | anterior à criação | Atualizado a cada viagem, cancelamento ou mudança de status. |

---

### 2.9. Matching

Matching é o resultado calculado pelo algoritmo de compatibilidade entre dois trajetos — um de oferta e um de procura. Serve como cache de resultados e base para auditoria. Um resultado é invalidado quando qualquer um dos trajetos envolvidos é editado, pausado ou inativado. Expiração automática por TTL garante limpeza mesmo em falhas de invalidação.

#### Atributos de Matching

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| identificador | Texto alfanumérico | Sim | Simples | `64h8g7f6...` | vazio, nulo | Gerado pelo banco. |
| id do trajeto do passageiro | Texto alfanumérico | Sim | Simples | `64c3a2b1...` | nulo | Referência ao Trajeto de procura. |
| id do trajeto do motorista | Texto alfanumérico | Sim | Simples | `64c4b3a2...` | nulo | Referência ao Trajeto de oferta. |
| id do passageiro | Texto alfanumérico | Sim | Simples | `64a9f8e7...` | nulo | Referência ao Usuário passageiro. Denormalizado para agilizar consultas. |
| id do motorista | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao Usuário motorista. Denormalizado para agilizar consultas. |
| índice de compatibilidade | Número decimal | Sim | Simples | `0.96`, `0.84`, `0.50` | negativo, maior que 1 | Escala de 0.0 a 1.0. Exibido como porcentagem na UI: `96%`. |
| desvio calculado (km) | Número decimal | Sim | Simples | `2.1`, `0.8` | negativo | Quilômetros adicionais que o motorista percorreria para buscar/deixar o passageiro. |
| diferença de horário (min) | Número inteiro | Sim | Simples | `5`, `0`, `15` | negativo | Diferença entre os horários de partida dos dois trajetos. Fator de penalização no índice. |
| dias compatíveis | Elemento de lista enumerada | Sim | Multivalorado (M) | `["SEGUNDA","TERCA"]` | lista vazia | Interseção dos dias da semana dos dois trajetos. |
| custo estimado por passageiro | Número decimal | Sim | Simples | `6.00`, `4.50` | negativo, zero | Estimativa do valor que o passageiro pagará caso confirme a carona. |
| válido | Booleano | Sim | Simples | `true`, `false` | nulo | `false` quando algum dos trajetos é editado ou inativado. Somente `true` é exibido ao usuário. |
| expira em | Data e hora | Não | Simples | `2025-06-01T00:00:00Z` | anterior à criação | Data de expiração automática (TTL). Após esta data o MongoDB deleta o documento automaticamente. |
| criado em | Data e hora | Sim | Simples | `2025-05-12T06:00:00Z` | data futura | Gerado automaticamente quando o algoritmo calcula o resultado. |

---

### 2.10. Indicadores

Indicadores é o conjunto de métricas de mobilidade, sustentabilidade e financeiro de um usuário em um período específico. São pré-calculados e atualizados ao concluir cada viagem. Consideram apenas viagens com status `CONCLUIDA` que tiveram ao menos um passageiro efetivo.

#### Atributos de Indicadores

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| identificador | Texto alfanumérico | Sim | Simples | `64i9h8g7...` | vazio, nulo | Gerado pelo banco. |
| id do usuário | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao Usuário dono dos indicadores. |
| período | Elemento de lista enumerada | Sim | Simples | `MENSAL`, `TOTAL` | valor fora do enum | Valores: `SEMANAL`, `MENSAL`, `SEMESTRAL`, `TOTAL`. |
| referência | Texto alfanumérico | Sim | Simples | `2025-05`, `2025-S1`, `TOTAL` | vazio | Para mensal: `AAAA-MM`. Para semestral: `AAAA-S1/S2`. Para semanal: `AAAA-W##`. |
| mobilidade | Dados de mobilidade | Sim | Composto (C) | ver tipo especial 3.10 | nulo | Totais de caronas, viagens, km e carros evitados no período. |
| sustentabilidade | Dados ambientais | Sim | Composto (C) | ver tipo especial 3.11 | nulo | Energia economizada, CO₂ evitado e equivalência em árvores. |
| financeiro | Dados financeiros | Sim | Composto (C) | ver tipo especial 3.12 | nulo | Economia do passageiro e receita do motorista no período. |
| série histórica | Ponto de série temporal | Sim | Multivalorado e Composto (MC) | ver tipo especial 3.13 | lista vazia | Array de pontos para os gráficos de evolução. |
| atualizado em | Data e hora | Sim | Simples | `2025-05-12T08:20:00Z` | anterior à criação | Atualizado sempre que uma viagem do período é concluída. |

---

## 3. Tipos especiais, compostos ou específicos

### 3.1. Vínculo Acadêmico

Estrutura composta que representa os dados universitários do usuário e o status de verificação do vínculo com a instituição.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| universidade | Texto | Sim | Nome da instituição. Selecionado de lista cadastrada na plataforma. |
| curso | Texto | Sim | Nome do curso. Texto livre ou lista pré-definida por universidade. |
| matrícula | Texto alfanumérico | Sim | Número de matrícula. Ex.: `2024-00123`. |
| status | Elemento de lista enumerada | Sim | Valores: `PENDENTE`, `EM_ANALISE`, `VERIFICADO`, `REJEITADO`. Default: `PENDENTE`. |
| URL do documento | URL | Não | URL do arquivo comprobatório enviado ao Firebase Storage. Nulo até o upload. |
| verificado em | Data e hora | Não | Preenchido automaticamente quando o status muda para `VERIFICADO`. |

---

### 3.2. Reputação por Papel

Estrutura composta que representa os indicadores de comportamento do usuário separados por papel (motorista e passageiro). Calculada semanalmente com base na janela deslizante de **90 dias**.

**Critérios para ganhar o Selo de Confiança — Motorista:**

| Critério | Limiar |
| :--- | :--- |
| Média de avaliação | ≥ 4.2 estrelas |
| Viagens concluídas na janela | ≥ 10 |
| Cancelamentos iniciados pelo motorista | ≤ 2 nos últimos 90 dias |
| Denúncias procedentes | ≤ 1 nos últimos 90 dias |

**Critérios para ganhar o Selo de Confiança — Passageiro:**

| Critério | Limiar |
| :--- | :--- |
| Média de avaliação | ≥ 4.0 estrelas |
| Viagens concluídas na janela | ≥ 5 |
| Cancelamentos tardios (< 2h de antecedência) | ≤ 2 nos últimos 90 dias |
| Ausências | ≤ 1 nos últimos 90 dias |
| Pagamentos não confirmados pelo motorista | 0 nos últimos 90 dias |

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| nível | Elemento de lista enumerada | Sim | Valores: `ALTA`, `MEDIA`, `BAIXA`, `null` (sem histórico ainda). |
| média de estrelas | Número decimal | Sim | Média das notas recebidas no papel. |
| total de avaliações | Número inteiro | Sim | Quantidade total de avaliações recebidas no papel. |
| janela — viagens concluídas | Número inteiro | Sim | Viagens concluídas nos últimos 90 dias no papel. |
| janela — cancelamentos | Número inteiro | Sim | Para motorista: cancelamentos iniciados por ele. Para passageiro: cancelamentos tardios. |
| janela — ausências | Número inteiro | Sim | Somente para passageiro. Vezes marcado como ausente nos últimos 90 dias. |
| janela — denúncias procedentes | Número inteiro | Sim | Denúncias marcadas como `RESOLVIDA` contra o usuário nos últimos 90 dias. |
| janela — pagamentos não confirmados | Número inteiro | Sim | Somente para passageiro. Pagamentos não confirmados pelo motorista nos últimos 90 dias. |
| calculado em | Data e hora | Sim | Timestamp do último recálculo semanal. |

---

### 3.3. Selo de Reconhecimento

Estrutura composta que representa um selo concedido ao usuário. O selo é removido automaticamente se o usuário deixar de atender os critérios no próximo recálculo semanal.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| tipo | Elemento de lista enumerada | Sim | Valores: `CONFIANCA`, `SUSTENTABILIDADE`. |
| concedido em | Data e hora | Sim | Data em que o selo foi concedido. |

---

### 3.4. Consumo por Fonte de Energia

Estrutura composta usada no array `consumos` do Veículo. Cada item representa uma fonte de energia e seus parâmetros de consumo e preço.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| fonte de energia | Elemento de lista enumerada | Sim | Valores: `GASOLINA`, `ETANOL`, `DIESEL`, `ELETRICO`. Não pode haver dois itens da mesma fonte no mesmo veículo. |
| consumo médio | Número decimal | Sim | Para combustão: km/L. Para elétrico: km/kWh. Informado pelo motorista. |
| unidade de consumo | Elemento de lista enumerada | Sim | Valores: `KM_POR_LITRO`, `KM_POR_KWH`. Determinado automaticamente pela fonte. |
| preço da energia | Número decimal | Sim | R$/litro para combustão; R$/kWh para elétrico. Informado pelo motorista. |

---

### 3.5. Localização

Estrutura composta que representa um ponto geográfico com endereço textual e coordenadas. Usada em Trajeto, Carona e nos pontos de embarque/desembarque de participantes.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| endereço | Texto | Sim | Endereço textual legível. Ex.: `Av. Central, 100 — Centro`. |
| bairro | Texto | Não | Extraído do geocoding via Google Maps. |
| cidade | Texto | Sim | Extraída do geocoding. Ex.: `Campinas`. |
| latitude | Número decimal | Sim | Coordenada geográfica. Ex.: `-22.9064`. Usada no matching geoespacial. |
| longitude | Número decimal | Sim | Coordenada geográfica. Ex.: `-47.0616`. Usada no matching geoespacial. |

---

### 3.6. Snapshot do Veículo

Cópia imutável dos dados relevantes do veículo no momento da criação da carona. Garante que edições futuras no cadastro do veículo não afetem o histórico de viagens passadas.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| modelo | Texto | Sim | Ex.: `Volkswagen Gol`. |
| placa | Texto alfanumérico | Sim | Exibida nos detalhes da carona. |
| tipo | Elemento de lista enumerada | Sim | Ex.: `HATCH`, `VAN`. |
| cor | Texto | Sim | Ex.: `Branco`. |

---

### 3.7. Custos da Carona

Estrutura composta que detalha todos os valores financeiros de uma carona. Os campos de snapshot (`fonteEnergiaUsada`, `consumoMedioUsado`, `precoEnergiaUsado`) registram os parâmetros exatos usados no cálculo — garantindo que o histórico seja auditável mesmo que o veículo seja editado depois.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| distância da rota (km) | Número decimal | Sim | Distância direta do trajeto do motorista. Copiada do trajeto na criação. |
| distância total com desvios (km) | Número decimal | Sim | Rota base + soma de todos os desvios gerados pelos passageiros. Calculada ao concluir. |
| fonte de energia usada | Elemento de lista enumerada | Sim | Snapshot da fonte de energia do veículo no momento da viagem. |
| consumo médio usado | Número decimal | Sim | Snapshot do consumo médio do veículo no momento da viagem. |
| preço da energia usado | Número decimal | Sim | Snapshot do preço da energia no momento da viagem. |
| custo de energia (R$) | Número decimal | Sim | `(distância total / consumo médio) × preço da energia`. |
| pedágio (R$) | Número decimal | Sim | Informado pelo motorista. `0.00` quando não há pedágio. |
| estacionamento (R$) | Número decimal | Sim | Informado pelo motorista. `0.00` quando não há estacionamento. |
| total bruto (R$) | Número decimal | Sim | `custo energia + pedágio + estacionamento`. |
| total rateado (R$) | Número decimal | Sim | Valor dividido entre os participantes efetivos. O motorista sempre participa. Diferença de centavos atribuída ao motorista. |

---

### 3.8. Participação na Carona

Estrutura composta e multivalorada que representa cada participante de uma carona — motorista e passageiros — com dados individuais de localização, status, cancelamento tardio e pagamento.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| id do usuário | Texto alfanumérico | Sim | Referência ao Usuário. |
| papel | Elemento de lista enumerada | Sim | Valores: `MOTORISTA`, `PASSAGEIRO`. |
| id do trajeto do passageiro | Texto alfanumérico | Não | Referência ao Trajeto de procura. Nulo para o motorista. |
| origem de embarque | Localização (C) | Não | Ponto de embarque do passageiro. Nulo para o motorista. |
| destino de desembarque | Localização (C) | Não | Ponto de desembarque do passageiro. Nulo para o motorista. |
| desvio gerado (km) | Número decimal | Não | Desvio que este passageiro gera na rota do motorista. Calculado no matching. Nulo para o motorista. |
| status | Elemento de lista enumerada | Sim | Valores: `CONFIRMADO`, `CANCELADO`, `AUSENTE`, `CONCLUIDO`. |
| motivo do cancelamento | Texto | Não | Preenchido pelo participante ao cancelar. |
| cancelamento tardio | Booleano | Sim | `false` por padrão. Muda para `true` quando o passageiro cancela com menos de 2h de antecedência. Determina se a viagem entra no extrato do acordo mesmo sem presença. |
| valor máximo (R$) | Número decimal | Sim | Gravado no momento da confirmação. Nunca pode ser superado pelo valor final. |
| valor final (R$) | Número decimal | Não | Calculado somente ao concluir a viagem. Nulo até a conclusão. |
| status do pagamento | Elemento de lista enumerada | Sim | Valores: `PENDENTE`, `AGUARDANDO_CONFIRMACAO`, `PAGO`, `NAO_APLICAVEL`. |
| confirmação de recebimento pelo motorista | Booleano | Sim | `false` por padrão. Atualizado pelo motorista ao confirmar que recebeu. |
| confirmado em | Data e hora | Sim | Timestamp de entrada do participante na carona. |
| cancelado em | Data e hora | Não | Nulo enquanto não cancelar. |
| atualizado em | Data e hora | Sim | Atualizado a cada mudança no subdocumento. |

---

### 3.9. Item de Extrato Mensal

Estrutura composta usada no array `extratoMes` do Acordo Recorrente. Cada item representa uma viagem do mês ou um cancelamento tardio cobrado.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| id da carona | Texto alfanumérico | Sim | Referência à Carona correspondente. |
| data da viagem | Data e hora | Sim | Data e hora da viagem. |
| valor cobrado (R$) | Número decimal | Sim | Valor cobrado neste item. Para cancelamento tardio: valor fixado no acordo. Para viagem normal: `valorFinal` do participante. |
| cancelamento tardio | Booleano | Sim | `true` quando o item representa um cancelamento cobrado sem presença. |
| status do pagamento | Elemento de lista enumerada | Sim | Valores: `PENDENTE`, `PAGO`, `COBRADO_SEM_PRESENCA`. |

---

### 3.10. Dados de Mobilidade

Estrutura composta com os indicadores de mobilidade de um período.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| total de caronas | Número inteiro | Sim | Participações individuais no período (motorista + passageiro). |
| total de viagens | Número inteiro | Sim | Eventos únicos de deslocamento no período. |
| km compartilhados | Número decimal | Sim | Soma dos km das viagens participadas no período. |
| carros evitados (estimativa) | Número inteiro | Sim | Cada passageiro efetivo = 1 carro evitado. |

---

### 3.11. Dados de Sustentabilidade

Estrutura composta com os indicadores ambientais de um período.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| combustível economizado (L) | Número decimal | Não | Para veículos de combustão. Nulo para elétricos. |
| energia economizada (kWh) | Número decimal | Não | Para veículos elétricos. Nulo para combustão. |
| CO₂ evitado (kg) | Número decimal | Sim | `energia economizada × fator de emissão`. Fator varia pela fonte de energia do veículo. |
| árvores equivalentes | Número decimal | Sim | `CO₂ evitado / 22`. Referência: 1 árvore absorve ~22 kg CO₂/ano. |

---

### 3.12. Dados Financeiros do Indicador

Estrutura composta com os indicadores financeiros de um período.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| economia total como passageiro (R$) | Número decimal | Sim | Diferença entre o custo de ir sozinho e o valor pago no rateio. `0.00` se não foi passageiro no período. |
| receita total como motorista (R$) | Número decimal | Sim | Soma dos valores finais recebidos nas viagens concluídas do período. `0.00` se não foi motorista. |

---

### 3.13. Ponto de Série Temporal

Estrutura composta usada no array de série histórica dos indicadores. Cada ponto representa um dia ou semana, dependendo do período do documento.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| data | Data | Sim | Data do ponto da série. |
| caronas | Número inteiro | Sim | Quantidade de caronas naquele ponto. |
| km compartilhados | Número decimal | Sim | Km compartilhados naquele ponto. |
| CO₂ evitado (kg) | Número decimal | Sim | CO₂ evitado naquele ponto. |

---

## 4. Desdobramento físico — banco orientado a documentos (MongoDB)

O projeto Converge utiliza **MongoDB** como banco de dados principal. Todas as entidades descritas na seção 2 são implementadas como coleções de documentos JSON.

| Entidade conceitual | Coleção MongoDB | Módulo dono | Estratégia de atributos compostos e multivalorados |
| :--- | :--- | :--- | :--- |
| Usuário | `usuarios` | `modules/usuario` | `vinculoAcademico`, `reputacao` e `selos[]` embutidos (sempre consultados juntos). `tiposParticipacao` como array de strings. |
| Veículo | `veiculos` | `modules/usuario` | `consumos[]` como array de subdocumentos (MC) para suportar flex/híbrido. Referenciado por id em trajetos e caronas. |
| Trajeto | `trajetos` | `modules/trajeto` | `origem` e `destino` embutidos com coordenadas (índice geoespacial). `diasSemana` como array de strings. |
| Carona | `caronas` | `modules/carona` | `participantes[]` embutidos (atomicidade). `custos` e `veiculoSnapshot` embutidos (snapshot imutável). |
| Avaliação | `avaliacoes` | `modules/avaliacao` | Documento simples com referências por id. |
| Denúncia | `denuncias` | `modules/seguranca` | Documento simples com referências por id. |
| Bloqueio | `bloqueios` | `modules/seguranca` | Documento simples com referências por id. |
| Acordo Recorrente | `acordos_recorrentes` | `modules/carona` | `extratoMes[]` embutido (volume máximo de ~31 itens/mês, sempre lido junto). |
| Matching | `matchings` | `modules/matching` | `diasCompativeis` como array de strings. `passageiroId` e `motoristaId` denormalizados. |
| Indicadores | `indicadores` | `modules/indicador` | `mobilidade`, `sustentabilidade` e `financeiro` embutidos. `serie[]` embutida como array de pontos. |

---

### 4.1. Coleção `usuarios`

```json
{
  "_id": "64a1f3c2e4b0a1b2c3d4e5f6",
  "firebaseUid": "UID123abc",
  "nome": "Ana Souza",
  "email": "ana@puc-campinas.edu.br",
  "telefone": "(19) 91234-5678",
  "fotoUrl": "https://storage.firebase.../foto_ana.jpg",
  "tiposParticipacao": ["PASSAGEIRO", "MOTORISTA"],
  "vinculoAcademico": {
    "universidade": "PUC-Campinas",
    "curso": "Engenharia de Software",
    "matricula": "2024-00123",
    "status": "VERIFICADO",
    "documentoUrl": "https://storage.firebase.../doc_ana.pdf",
    "verificadoEm": "2025-03-10T14:00:00Z"
  },
  "chavePix": "(19) 91234-5678",
  "frequenciaCobrancaPreferida": "POR_VIAGEM",
  "inadimplente": false,
  "inadimplenteDesde": null,
  "statusCobranca": "EM_DIA",
  "reputacao": {
    "motorista": {
      "nivel": "ALTA",
      "mediaEstrelas": 4.8,
      "totalAvaliacoes": 32,
      "janela": {
        "viagensConcluidas": 22,
        "cancelamentosPeloMotorista": 0,
        "atrasos": 1,
        "denunciasProcedentes": 0
      },
      "calculadoEm": "2025-09-15T00:00:00Z"
    },
    "passageiro": {
      "nivel": "ALTA",
      "mediaEstrelas": 4.9,
      "totalAvaliacoes": 10,
      "janela": {
        "viagensConcluidas": 10,
        "cancelamentosTardios": 0,
        "ausencias": 0,
        "denunciasProcedentes": 0,
        "pagamentosNaoConfirmados": 0
      },
      "calculadoEm": "2025-09-15T00:00:00Z"
    }
  },
  "selos": [
    { "tipo": "CONFIANCA", "concedidoEm": "2025-09-15T00:00:00Z" }
  ],
  "ativo": true,
  "criadoEm": "2025-03-01T09:00:00Z",
  "atualizadoEm": "2025-09-15T08:30:00Z"
}
```

**Justificativa:** `vinculoAcademico`, `reputacao` e `selos` embutidos pois são sempre consultados junto ao usuário. `reputacao` separada por papel evita misturar métricas de motorista com as de passageiro.

---

### 4.2. Coleção `veiculos`

```json
{
  "_id": "64b2e1d3f5c1b2a3d4e5f6a7",
  "usuarioId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "apelido": "Gol Azul",
  "modelo": "Volkswagen Gol",
  "placa": "ABC1234",
  "cor": "Azul",
  "tipo": "HATCH",
  "capacidadeTotal": 5,
  "consumos": [
    {
      "fonteEnergia": "GASOLINA",
      "consumoMedio": 12.5,
      "unidadeConsumo": "KM_POR_LITRO",
      "precoEnergiaAtual": 6.49
    },
    {
      "fonteEnergia": "ETANOL",
      "consumoMedio": 9.0,
      "unidadeConsumo": "KM_POR_LITRO",
      "precoEnergiaAtual": 4.29
    }
  ],
  "ativo": true,
  "criadoEm": "2025-03-05T10:00:00Z",
  "atualizadoEm": "2025-09-01T11:00:00Z"
}
```

**Justificativa:** `consumos[]` como array para suportar veículos flex (2 combustíveis) e híbridos. Coleção separada de `usuarios` pois veículos são consultados de forma independente na tela de cadastro de trajeto.

---

### 4.3. Coleção `trajetos`

```json
{
  "_id": "64c3a2b1e6d2c3b4a5f6e7d8",
  "usuarioId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "veiculoId": "64b2e1d3f5c1b2a3d4e5f6a7",
  "tipo": "OFERECER",
  "origem": {
    "endereco": "Av. Central, 100",
    "bairro": "Centro",
    "cidade": "Campinas",
    "coordenadas": { "latitude": -22.9064, "longitude": -47.0616 }
  },
  "destino": {
    "endereco": "Campus UNIV — Bloco B",
    "bairro": "Jardim Universitário",
    "cidade": "Campinas",
    "coordenadas": { "latitude": -22.8333, "longitude": -47.0500 }
  },
  "horarioPartida": "07:30",
  "horarioChegadaEstimado": "08:10",
  "diasSemana": ["SEGUNDA", "TERCA", "QUARTA", "QUINTA", "SEXTA"],
  "desvioMaximoKm": 2.0,
  "vagasTotal": 3,
  "vagasDisponiveis": 1,
  "distanciaRotaKm": 12.4,
  "status": "ATIVO",
  "criadoEm": "2025-03-10T08:00:00Z",
  "atualizadoEm": "2025-09-20T07:00:00Z"
}
```

**Justificativa:** `origem` e `destino` embutidos pois as coordenadas precisam de índice geoespacial 2dsphere no próprio documento. `diasSemana` como array simples para o operador `$in` no matching.

---

### 4.4. Coleção `caronas`

```json
{
  "_id": "64d4c3b2f7e3d4c5b6a7f8e9",
  "numero": 42,
  "motoristaId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "veiculoId": "64b2e1d3f5c1b2a3d4e5f6a7",
  "veiculoSnapshot": {
    "modelo": "Volkswagen Gol",
    "placa": "ABC1234",
    "tipo": "HATCH",
    "cor": "Azul"
  },
  "trajetoMotoristaId": "64c3a2b1e6d2c3b4a5f6e7d8",
  "origem": {
    "endereco": "Av. Central, 100",
    "coordenadas": { "latitude": -22.9064, "longitude": -47.0616 }
  },
  "destino": {
    "endereco": "Campus UNIV — Bloco B",
    "coordenadas": { "latitude": -22.8333, "longitude": -47.0500 }
  },
  "dataHoraPartida": "2025-05-12T07:30:00Z",
  "dataHoraChegadaEstimada": "2025-05-12T08:10:00Z",
  "dataHoraConclusao": "2025-05-12T08:15:00Z",
  "diasSemana": ["SEGUNDA", "TERCA", "QUARTA", "QUINTA", "SEXTA"],
  "vagasTotal": 3,
  "vagasDisponiveis": 0,
  "status": "CONCLUIDA",
  "motivoCancelamento": null,
  "canceladoPor": null,
  "frequenciaCobranca": "MENSAL",
  "custos": {
    "distanciaRotaKm": 12.4,
    "distanciaTotalComDesviosKm": 14.5,
    "fonteEnergiaUsada": "GASOLINA",
    "consumoMedioUsado": 12.5,
    "precoEnergiaUsado": 6.49,
    "custoEnergia": 7.53,
    "pedagio": 0.00,
    "estacionamento": 0.00,
    "totalBruto": 7.53,
    "totalRateado": 7.53
  },
  "participantes": [
    {
      "usuarioId": "64a1f3c2e4b0a1b2c3d4e5f6",
      "papel": "MOTORISTA",
      "trajetoPassageiroId": null,
      "origemEmbarque": null,
      "destinoDesembarque": null,
      "desvioGeradoKm": null,
      "status": "CONCLUIDO",
      "motivoCancelamento": null,
      "cancelamentoTardio": false,
      "valorMaximo": 2.51,
      "valorFinal": 2.51,
      "statusPagamento": "NAO_APLICAVEL",
      "confirmacaoRecebimentoPeloMotorista": true,
      "confirmadoEm": "2025-05-10T20:00:00Z",
      "canceladoEm": null,
      "atualizadoEm": "2025-05-12T08:15:00Z"
    },
    {
      "usuarioId": "64a9f8e7d6c5b4a3f2e1d0c9",
      "papel": "PASSAGEIRO",
      "trajetoPassageiroId": "64c5d4e3f2a1b0c9d8e7f6a5",
      "origemEmbarque": {
        "endereco": "Rua das Flores, 50",
        "coordenadas": { "latitude": -22.9100, "longitude": -47.0650 }
      },
      "destinoDesembarque": {
        "endereco": "Campus UNIV — Bloco B",
        "coordenadas": { "latitude": -22.8333, "longitude": -47.0500 }
      },
      "desvioGeradoKm": 2.1,
      "status": "CONCLUIDO",
      "motivoCancelamento": null,
      "cancelamentoTardio": false,
      "valorMaximo": 6.00,
      "valorFinal": 2.51,
      "statusPagamento": "PAGO",
      "confirmacaoRecebimentoPeloMotorista": true,
      "confirmadoEm": "2025-05-11T07:00:00Z",
      "canceladoEm": null,
      "atualizadoEm": "2025-05-12T08:20:00Z"
    }
  ],
  "criadoEm": "2025-05-10T20:00:00Z",
  "atualizadoEm": "2025-05-12T08:20:00Z"
}
```

**Justificativa:** `participantes[]` embutidos para atomicidade nas operações de vaga. `veiculoSnapshot` e `custos` embutidos como snapshots imutáveis do momento da viagem.

---

### 4.5. Coleção `avaliacoes`

```json
{
  "_id": "64e5d4c3b2a1f0e9d8c7b6a5",
  "caronaId": "64d4c3b2f7e3d4c5b6a7f8e9",
  "avaliadorId": "64a9f8e7d6c5b4a3f2e1d0c9",
  "avaliadoId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "papelAvaliado": "MOTORISTA",
  "nota": 5,
  "comentario": "Motorista pontual e carro limpo!",
  "criadoEm": "2025-05-12T09:00:00Z"
}
```

**Justificativa:** Coleção separada para consultas independentes do histórico de avaliações por papel. Índice único composto `{ caronaId, avaliadorId }` impede avaliação duplicada.

---

### 4.6. Coleção `denuncias`

```json
{
  "_id": "64f6e5d4c3b2a1f0e9d8c7b6",
  "denuncianteId": "64a9f8e7d6c5b4a3f2e1d0c9",
  "denunciadoId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "caronaId": "64d4c3b2f7e3d4c5b6a7f8e9",
  "motivo": "CONDUTA_INADEQUADA",
  "descricao": "O motorista fez comentários inapropriados durante a viagem.",
  "status": "PENDENTE",
  "resolucao": null,
  "analisadoPor": null,
  "criadoEm": "2025-05-12T10:00:00Z",
  "atualizadoEm": "2025-05-12T10:00:00Z"
}
```

---

### 4.7. Coleção `bloqueios`

```json
{
  "_id": "64g7f6e5d4c3b2a1f0e9d8c7",
  "usuarioId": "64a9f8e7d6c5b4a3f2e1d0c9",
  "bloqueadoId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "criadoEm": "2025-05-13T08:00:00Z"
}
```

---

### 4.8. Coleção `acordos_recorrentes`

```json
{
  "_id": "64h1i2j3k4l5m6n7o8p9q0r1",
  "passageiroId": "64a9f8e7d6c5b4a3f2e1d0c9",
  "motoristaId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "trajetoMotoristaId": "64c3a2b1e6d2c3b4a5f6e7d8",
  "trajetoPassageiroId": "64c5d4e3f2a1b0c9d8e7f6a5",
  "statusAcordo": "ATIVO",
  "propostoPor": "PASSAGEIRO",
  "motivoSuspensao": null,
  "motivoEncerramento": null,
  "valorFixadoNoAcordo": 6.00,
  "mesReferencia": "2025-05",
  "cancelamentosTardiosNoMes": 1,
  "avisosEnviados": 0,
  "extratoMes": [
    {
      "caronaId": "64d4c3b2f7e3d4c5b6a7f8e9",
      "dataViagem": "2025-05-12T07:30:00Z",
      "valorCobrado": 6.00,
      "cancelamentoTardio": false,
      "statusPagamento": "PENDENTE"
    },
    {
      "caronaId": "64d5c4b3a2f1e0d9c8b7a6f5",
      "dataViagem": "2025-05-14T07:30:00Z",
      "valorCobrado": 6.00,
      "cancelamentoTardio": true,
      "statusPagamento": "COBRADO_SEM_PRESENCA"
    }
  ],
  "totalDevidoMes": 12.00,
  "dataLimitePagamento": "2025-06-05",
  "criadoEm": "2025-05-01T00:00:00Z",
  "atualizadoEm": "2025-05-14T08:00:00Z"
}
```

**Justificativa:** `extratoMes[]` embutido pois é sempre lido junto ao acordo para exibir o extrato mensal, e tem volume máximo controlado (~31 itens por mês). Coleção separada da carona pois tem ciclo de vida próprio — existe por mês, entre par fixo, com controle de pagamento independente.

---

### 4.9. Coleção `matchings`

```json
{
  "_id": "64h8g7f6e5d4c3b2a1f0e9d8",
  "trajetoPassageiroId": "64c5d4e3f2a1b0c9d8e7f6a5",
  "trajetoMotoristaId": "64c3a2b1e6d2c3b4a5f6e7d8",
  "passageiroId": "64a9f8e7d6c5b4a3f2e1d0c9",
  "motoristaId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "indiceCompatibilidade": 0.96,
  "desvioCalculadoKm": 2.1,
  "diferencaHorarioMin": 0,
  "diasCompativeis": ["SEGUNDA", "TERCA", "QUARTA", "QUINTA", "SEXTA"],
  "custoEstimadoPorPassageiro": 6.00,
  "valido": true,
  "expiradoEm": "2025-06-12T00:00:00Z",
  "criadoEm": "2025-05-12T06:00:00Z"
}
```

---

### 4.10. Coleção `indicadores`

```json
{
  "_id": "64i9h8g7f6e5d4c3b2a1f0e9",
  "usuarioId": "64a9f8e7d6c5b4a3f2e1d0c9",
  "periodo": "MENSAL",
  "referencia": "2025-05",
  "mobilidade": {
    "totalCaronas": 22,
    "totalViagens": 22,
    "kmCompartilhados": 272.8,
    "carrosEvitadosEstimado": 22
  },
  "sustentabilidade": {
    "combustivelEconomizadoL": 21.8,
    "energiaEconomizadaKwh": null,
    "co2EvitadoKg": 50.7,
    "arvoresEquivalentes": 2.3
  },
  "financeiro": {
    "economiaTotalPassageiro": 132.00,
    "receitaTotalMotorista": 0.00
  },
  "serie": [
    { "data": "2025-05-01T00:00:00Z", "caronas": 5, "kmCompartilhados": 62.0, "co2EvitadoKg": 11.5 },
    { "data": "2025-05-08T00:00:00Z", "caronas": 5, "kmCompartilhados": 62.0, "co2EvitadoKg": 11.5 }
  ],
  "atualizadoEm": "2025-05-30T09:00:00Z"
}
```

---

## 5. Consultas, índices e observações de arquitetura

| Consulta esperada | Campos envolvidos | Índices sugeridos | Observações |
| :--- | :--- | :--- | :--- |
| Buscar usuário por e-mail (login) | `email` | `{ email: 1 }` único | Consulta crítica no fluxo de autenticação. |
| Buscar usuário por UID Firebase | `firebaseUid` | `{ firebaseUid: 1 }` único | Executada a cada requisição autenticada. |
| Verificar inadimplência no matching | `inadimplente` | `{ inadimplente: 1 }` | Checagem rápida antes de exibir combinações. |
| Listar veículos de um motorista | `usuarioId` | `{ usuarioId: 1 }` | Consulta na tela de cadastro de trajeto. |
| Listar trajetos ativos de um usuário | `usuarioId`, `status` | `{ usuarioId: 1, status: 1 }` | Dashboard e perfil. |
| Buscar trajetos por proximidade de origem | `origem.coordenadas` | `{ "origem.coordenadas": "2dsphere" }` | Consulta geoespacial central do matching. |
| Buscar trajetos por proximidade de destino | `destino.coordenadas` | `{ "destino.coordenadas": "2dsphere" }` | Filtra por destino próximo. |
| Listar trajetos por dia da semana | `diasSemana`, `status` | `{ diasSemana: 1, status: 1 }` | Pré-filtro do matching. |
| Listar caronas de um motorista | `motoristaId`, `status` | `{ motoristaId: 1, status: 1 }` | Histórico e painel do motorista. |
| Listar caronas de um passageiro | `participantes.usuarioId` | `{ "participantes.usuarioId": 1 }` | Histórico e viagem ativa do passageiro. |
| Buscar carona por número sequencial | `numero` | `{ numero: 1 }` único | Referência amigável `Viagem #042`. |
| Buscar combinações válidas por passageiro | `trajetoPassageiroId`, `valido`, `indiceCompatibilidade` | `{ trajetoPassageiroId: 1, valido: 1, indiceCompatibilidade: -1 }` | Lista ordenada por score. |
| Verificar bloqueio entre dois usuários | `usuarioId`, `bloqueadoId` | `{ usuarioId: 1, bloqueadoId: 1 }` único | Executada no matching, aceite de carona e chat. |
| Verificar avaliação duplicada | `caronaId`, `avaliadorId` | `{ caronaId: 1, avaliadorId: 1 }` único | Impede dupla avaliação na mesma viagem. |
| Buscar avaliações recebidas por papel | `avaliadoId`, `papelAvaliado` | `{ avaliadoId: 1, papelAvaliado: 1 }` | Base do cálculo de reputação por papel. |
| Buscar fila de denúncias pendentes | `status` | `{ status: 1 }` | Moderação — filtro por `PENDENTE` e `EM_ANALISE`. |
| Buscar acordo ativo entre par | `passageiroId`, `motoristaId`, `statusAcordo` | `{ passageiroId: 1, motoristaId: 1, mesReferencia: 1 }` único | Verificar existência e carregar extrato do mês. |
| Listar acordos vencidos para avisos | `dataLimitePagamento`, `statusAcordo` | `{ dataLimitePagamento: 1, statusAcordo: 1 }` | Job agendado que dispara avisos e bloqueios. |
| Buscar indicadores de um usuário por período | `usuarioId`, `periodo`, `referencia` | `{ usuarioId: 1, periodo: 1, referencia: 1 }` único | Tela de Dados. |
| Limpeza automática de matchings obsoletos | `expiradoEm` | `{ expiradoEm: 1 }` TTL | MongoDB remove automaticamente. |
