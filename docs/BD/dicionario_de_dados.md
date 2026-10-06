# Dicionário de Dados: Sistema Converge de Mobilidade Universitária Compartilhada

---

## 1. Histórico de versões

| Data | Autor | Versão | Comentários |
| :---: | :---: | ---: | :--- |
| 29/09/2026 | Daniela Mikie Kikuchi Gonçalves | 1.0.0 | Publicação da versão 1.0.0|
| 29/09/2026 | Daniela Mikie Kikuchi Gonçalves | 1.0.1 | Versão inicial — título, estrutura do documento, citação das entidades e descrições conceituais |

---

## 2. Entidades e atributos conceituais

A seguir são descritas as entidades que o projeto Converge contempla, seus atributos, tipos conceituais, características e regras de negócio. As entidades estão ordenadas da mais central para as de suporte.

> **Padrão de atributos especiais:**
> - **(C)** — Composto: estrutura formada por partes que juntas compõem o atributo.
> - **(M)** — Multivalorado: admite múltiplos valores para um mesmo registro.
> - **(MC)** — Multivalorado e Composto: lista de estruturas compostas (array de objetos).

---

### 2.1. Usuário

Usuário é toda pessoa que se cadastra na plataforma Converge. Pode assumir um ou mais papéis: passageiro (procura carona), motorista (oferece carona com carro próprio) ou motorista de van (oferece carona com van, rota fixa e maior capacidade). O usuário deve ter vínculo acadêmico verificado com uma universidade para utilizar as funcionalidades principais da plataforma.

#### Atributos de Usuário

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| identificador | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | vazio, nulo | Gerado automaticamente pelo banco. Imutável após criação. |
| identificador Firebase | Texto alfanumérico | Sim | Simples | `UID123abc` | vazio, nulo | UID gerado pelo Firebase Auth no primeiro login. Nunca alterado. Usado para validar tokens JWT. |
| nome completo | Texto | Sim | Simples | `Ana Souza`, `João da Silva` | `""`, menos de 3 caracteres | Nome completo da pessoa. Exibido em perfis, combinações e detalhes de viagem. |
| e-mail | Endereço de e-mail | Sim | Simples | `ana@puc-campinas.edu.br` | `ana@gmail.com`, vazio | Deve ser e-mail institucional acadêmico. Único no sistema. Imutável após criação. |
| telefone | Texto numérico | Sim | Simples | `(19) 91234-5678` | `19912345678`, vazio | Formato com DDD. Exibido no perfil. |
| foto de perfil | URL | Não | Simples | `https://storage.firebase...` | URL inválida | URL da imagem armazenada no Firebase Storage. Nulo até o upload. |
| tipos de participação | Elemento de lista enumerada | Sim | Multivalorado (M) | `["PASSAGEIRO"]`, `["MOTORISTA", "PASSAGEIRO"]` | lista vazia, valor fora do enum | Mínimo 1 valor. Valores possíveis: `PASSAGEIRO`, `MOTORISTA`, `MOTORISTA_VAN`. Selecionado no onboarding. |
| vínculo acadêmico | Dados de vínculo universitário | Sim | Composto (C) | ver tipo especial 3.1 | incompleto, nulo | Obrigatório no cadastro. Contém universidade, curso, matrícula e status da verificação. |
| chave PIX | Texto | Não | Simples | `(19) 91234-5678`, `cpf@email.com` | vazio com cobrança ativa | Obrigatório para motoristas que desejam cobrar. Exibida na tela de Pagamento via PIX. |
| frequência de cobrança preferida | Elemento de lista enumerada | Sim | Simples | `POR_VIAGEM`, `MENSAL` | valor fora do enum | Preferência padrão do motorista. Pode ser sobrescrita por carona. |
| média de avaliação | Número decimal | Sim | Simples | `4.8`, `3.0`, `5.0` | valor negativo, maior que 5 | Calculada a cada nova avaliação recebida. Exibida como `★ 4.8`. Inicia em `0.0`. |
| total de avaliações | Número inteiro | Sim | Simples | `32`, `0` | valor negativo | Exibido ao lado da nota: `★ 4.8 (32)`. Incrementado a cada avaliação recebida. |
| total de viagens | Número inteiro | Sim | Simples | `86`, `0` | valor negativo | Quantidade de viagens concluídas. Exibido no perfil. |
| total de ausências | Número inteiro | Sim | Simples | `2`, `0` | valor negativo | Quantidade de vezes marcado como ausente pelo motorista. Exibido no perfil. |
| conta ativa | Booleano | Sim | Simples | `true`, `false` | nulo | `true` por padrão. `false` para contas suspensas ou excluídas. |
| criado em | Data e hora | Sim | Simples | `2025-05-01T10:00:00Z` | data futura | Gerado automaticamente no cadastro. |
| atualizado em | Data e hora | Sim | Simples | `2025-09-15T08:30:00Z` | data anterior à criação | Atualizado a cada alteração no documento. |

---

### 2.2. Veículo

Veículo é o automóvel cadastrado por um motorista para ser utilizado nas caronas. Um usuário pode ter múltiplos veículos cadastrados, mas apenas um é associado a cada trajeto de oferta. Os dados do veículo são copiados como snapshot imutável no momento da confirmação de uma carona, para garantir que edições futuras não alterem o histórico de viagens passadas.

#### Atributos de Veículo

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| identificador | Texto alfanumérico | Sim | Simples | `64b2e1d3...` | vazio, nulo | Gerado pelo banco. Imutável. |
| id do proprietário | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao identificador do Usuário motorista dono do veículo. |
| apelido | Texto | Não | Simples | `Gol Azul`, `Van da Manhã` | — | Facilita a seleção quando o motorista tem mais de um veículo. |
| modelo | Texto | Sim | Simples | `Volkswagen Gol`, `Fiat Uno` | vazio | Exibido nos detalhes da carona. |
| placa | Texto alfanumérico | Sim | Simples | `ABC-1234`, `ABC1D23` | placa duplicada, vazio | Única no sistema. Aceita formato antigo e Mercosul. |
| cor | Texto | Sim | Simples | `Branco`, `Prata`, `Azul` | vazio | Texto livre. Auxilia o passageiro a identificar o veículo. |
| tipo | Elemento de lista enumerada | Sim | Simples | `HATCH`, `VAN` | valor fora do enum | Valores: `HATCH`, `SEDAN`, `SUV`, `PICKUP`, `VAN`, `OUTRO`. |
| capacidade total | Número inteiro | Sim | Simples | `5`, `15` | `1`, `0`, valor negativo, maior que 15 | Inclui o motorista. Mínimo 2. Máximo 15. Define o limite de vagas disponíveis. |
| fonte de energia | Elemento de lista enumerada | Sim | Simples | `GASOLINA`, `ELETRICO`, `FLEX` | valor fora do enum | Valores: `GASOLINA`, `ETANOL`, `FLEX`, `DIESEL`, `ELETRICO`, `HIBRIDO`. Determina o fator de emissão de CO₂ e a unidade de consumo. |
| consumo médio | Número decimal | Sim | Simples | `10.5`, `6.2` | `0`, negativo | Para combustão: km/L. Para elétrico: km/kWh. Usado no cálculo de custo de energia. |
| unidade de consumo | Elemento de lista enumerada | Sim | Simples | `KM_POR_LITRO`, `KM_POR_KWH` | valor fora do enum | Determinado automaticamente pela fonte de energia. |
| preço da energia | Número decimal | Sim | Simples | `6.49`, `0.85` | `0`, negativo | R$/litro para combustão; R$/kWh para elétrico. Informado pelo motorista. |
| ativo | Booleano | Sim | Simples | `true`, `false` | nulo | Veículo inativo não aparece na seleção de trajeto. |
| criado em | Data e hora | Sim | Simples | `2025-04-10T09:00:00Z` | data futura | Gerado automaticamente. |
| atualizado em | Data e hora | Sim | Simples | `2025-09-01T11:00:00Z` | data anterior à criação | Atualizado a cada edição. |

---

### 2.3. Trajeto

Trajeto é o registro de um deslocamento recorrente cadastrado por um usuário. Pode ser do tipo oferta (motorista disponibiliza vagas) ou procura (passageiro busca carona). O trajeto é a base de dados consumida pelo algoritmo de matching. Apenas trajetos com status `ATIVO` são elegíveis para o matching.

#### Atributos de Trajeto

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| identificador | Texto alfanumérico | Sim | Simples | `64c3a2b1...` | vazio, nulo | Gerado pelo banco. Referenciado em caronas e matchings. |
| id do usuário | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao Usuário dono do trajeto. |
| id do veículo | Texto alfanumérico | Não | Simples | `64b2e1d3...` | — | Obrigatório somente para tipo `OFERECER`. Nulo para `PROCURAR`. |
| tipo | Elemento de lista enumerada | Sim | Simples | `OFERECER`, `PROCURAR` | valor fora do enum | Define o papel do usuário neste trajeto. |
| origem | Localização | Sim | Composto (C) | ver tipo especial 3.2 | nulo, incompleto | Ponto de partida do trajeto com endereço e coordenadas geográficas. |
| destino | Localização | Sim | Composto (C) | ver tipo especial 3.2 | nulo, incompleto | Ponto de chegada do trajeto com endereço e coordenadas geográficas. |
| horário de partida | Hora | Sim | Simples | `07:30`, `18:00` | `7:3`, `25:00` | Formato `HH:mm`. Armazenado sem data pois representa a hora do dia recorrente. |
| horário de chegada estimado | Hora | Sim | Simples | `08:10`, `19:00` | hora anterior à partida | Formato `HH:mm`. Calculado ou informado pelo usuário. |
| dias da semana | Elemento de lista enumerada | Sim | Multivalorado (M) | `["SEGUNDA","TERCA"]` | lista vazia | Mínimo 1 dia. Valores: `SEGUNDA`, `TERCA`, `QUARTA`, `QUINTA`, `SEXTA`, `SABADO`, `DOMINGO`. |
| desvio máximo aceitável (km) | Número decimal | Sim | Simples | `2.0`, `0.5`, `5.0` | negativo, zero | Somente para `OFERECER`. Distância máxima que o motorista aceita se desviar para buscar/deixar passageiro. Usado no matching. |
| vagas totais | Número inteiro | Não | Simples | `3`, `8` | `0`, negativo | Somente para `OFERECER`. Calculado como `capacidadeTotal do veículo - 1`. |
| vagas disponíveis | Número inteiro | Não | Simples | `3`, `1`, `0` | negativo, maior que vagas totais | Somente para `OFERECER`. Decrementado ao confirmar passageiro; incrementado ao cancelar. |
| distância da rota (km) | Número decimal | Não | Simples | `12.4`, `5.8` | negativo, zero | Calculada via Google Maps na criação. Base para cálculo de custos e desvios. |
| status | Elemento de lista enumerada | Sim | Simples | `ATIVO`, `PAUSADO`, `INATIVO` | valor fora do enum | Somente `ATIVO` entra no matching. |
| criado em | Data e hora | Sim | Simples | `2025-03-01T08:00:00Z` | data futura | Gerado automaticamente. |
| atualizado em | Data e hora | Sim | Simples | `2025-09-20T07:00:00Z` | data anterior à criação | Atualizado a cada edição. |

---

### 2.4. Carona

Carona (ou Viagem) é o evento central do sistema. Representa um deslocamento concreto, com data, horário, motorista, passageiros confirmados e custos. É criada a partir de uma combinação identificada pelo matching e persiste durante todo o ciclo de vida da viagem — da confirmação à conclusão e pagamento. Os dados de veículo, origem e destino são copiados como snapshot imutável no momento da criação da carona.

#### Atributos de Carona

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| identificador | Texto alfanumérico | Sim | Simples | `64d4c3b2...` | vazio, nulo | Gerado pelo banco. Referenciado por avaliações e denúncias. |
| número sequencial | Número inteiro | Sim | Simples | `42`, `1`, `1000` | negativo, zero, duplicado | Número amigável exibido na UI: `Viagem #042`. Gerado por sequência no backend. |
| id do motorista | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao Usuário motorista da carona. |
| id do veículo | Texto alfanumérico | Sim | Simples | `64b2e1d3...` | nulo | Referência ao Veículo usado. Os dados relevantes também são copiados como snapshot. |
| snapshot do veículo | Dados do veículo | Sim | Composto (C) | ver tipo especial 3.3 | nulo | Cópia imutável dos dados do veículo no momento da criação da carona. Garante integridade do histórico. |
| id do trajeto do motorista | Texto alfanumérico | Sim | Simples | `64c3a2b1...` | nulo | Referência ao Trajeto que originou esta carona. |
| origem | Localização | Sim | Composto (C) | ver tipo especial 3.2 | nulo | Snapshot da origem no momento da confirmação. Imutável. |
| destino | Localização | Sim | Composto (C) | ver tipo especial 3.2 | nulo | Snapshot do destino. Imutável. |
| data e hora de partida | Data e hora | Sim | Simples | `2025-05-12T07:30:00Z` | data passada no cadastro | Combinação da data concreta com o horário do trajeto. |
| data e hora de chegada estimada | Data e hora | Sim | Simples | `2025-05-12T08:10:00Z` | anterior à partida | Calculada a partir da partida + duração estimada da rota. |
| data e hora de conclusão | Data e hora | Não | Simples | `2025-05-12T08:15:00Z` | anterior à partida | Nulo até o motorista concluir a viagem. Preenchido ao encerrar. |
| dias da semana | Elemento de lista enumerada | Sim | Multivalorado (M) | `["SEGUNDA","QUARTA"]` | lista vazia | Cópia dos dias do trajeto no momento da criação. |
| vagas totais | Número inteiro | Sim | Simples | `3`, `8` | `0`, negativo | Copiado do trajeto. Não se altera após a criação. |
| vagas disponíveis | Número inteiro | Sim | Simples | `2`, `0` | negativo, maior que vagas totais | Atualizado atomicamente ao confirmar ou cancelar participante. |
| status | Elemento de lista enumerada | Sim | Simples | `CONFIRMADA`, `CONCLUIDA` | valor fora do enum | Valores: `AGUARDANDO_CONFIRMACAO`, `CONFIRMADA`, `EM_ANDAMENTO`, `CONCLUIDA`, `CANCELADA`. |
| motivo do cancelamento | Texto | Não | Simples | `Imprevisto pessoal` | — | Presente somente quando `status = CANCELADA`. |
| cancelado por | Texto alfanumérico | Não | Simples | `64a1f3c2...` | — | Referência ao Usuário que cancelou a carona. |
| frequência de cobrança | Elemento de lista enumerada | Sim | Simples | `POR_VIAGEM`, `MENSAL` | valor fora do enum | Definida pelo motorista para esta carona específica. |
| custos | Detalhamento de custos | Sim | Composto (C) | ver tipo especial 3.4 | nulo | Calculado e atualizado ao concluir a viagem. |
| participantes | Participação na carona | Sim | Multivalorado e Composto (MC) | ver tipo especial 3.5 | lista vazia | Mínimo 1 elemento (o motorista). Inclui dados individuais de embarque, status e pagamento. |
| criado em | Data e hora | Sim | Simples | `2025-05-10T20:00:00Z` | data futura | Gerado automaticamente. |
| atualizado em | Data e hora | Sim | Simples | `2025-05-12T08:15:00Z` | anterior à criação | Atualizado a cada mudança de status ou de participante. |

---

### 2.5. Avaliação

Avaliação é o registro de nota e comentário que um participante faz sobre outro após a conclusão de uma viagem. Motorista avalia passageiros e passageiros avaliam o motorista. Cada par avaliador/avaliado pode registrar apenas uma avaliação por viagem. As notas recebidas alimentam a média de avaliação exibida no perfil do usuário.

#### Atributos de Avaliação

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| identificador | Texto alfanumérico | Sim | Simples | `64e5d4c3...` | vazio, nulo | Gerado pelo banco. |
| id da carona | Texto alfanumérico | Sim | Simples | `64d4c3b2...` | nulo | Referência à Carona. Somente caronas com `status = CONCLUIDA` podem ser avaliadas. |
| id do avaliador | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao Usuário que avalia. |
| id do avaliado | Texto alfanumérico | Sim | Simples | `64a9f8e7...` | igual ao avaliador, nulo | Referência ao Usuário avaliado. Deve ser participante da mesma carona. |
| nota | Número inteiro | Sim | Simples | `1`, `3`, `5` | `0`, `6`, negativo, decimal | Escala de 1 a 5 estrelas. Ao salvar, recalcula a média e o total de avaliações do avaliado. |
| comentário | Texto | Não | Simples | `Ótima viagem!`, `Pontual e educado.` | — | Texto livre. Exibido no histórico de avaliações do perfil do avaliado. |
| criado em | Data e hora | Sim | Simples | `2025-05-12T09:00:00Z` | data anterior à conclusão da carona | Gerado automaticamente. |

---

### 2.6. Denúncia

Denúncia é o registro formal de uma ocorrência reportada por um usuário contra outro. Pode estar vinculada a uma carona específica ou não. Mantém histórico completo para suporte à moderação da plataforma, incluindo motivo, descrição e status de análise.

#### Atributos de Denúncia

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| identificador | Texto alfanumérico | Sim | Simples | `64f6e5d4...` | vazio, nulo | Gerado pelo banco. |
| id do denunciante | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao Usuário que registra a denúncia. |
| id do denunciado | Texto alfanumérico | Sim | Simples | `64a9f8e7...` | igual ao denunciante, nulo | Referência ao Usuário denunciado. |
| id da carona | Texto alfanumérico | Não | Simples | `64d4c3b2...` | — | Referência à Carona relacionada, quando aplicável. |
| motivo | Elemento de lista enumerada | Sim | Simples | `CONDUTA_INADEQUADA`, `ATRASO_FALTA` | valor fora do enum | Valores: `ATRASO_FALTA`, `CONDUTA_INADEQUADA`, `COBRANCA_INDEVIDA`, `OUTRO`. Pré-selecionado na tela de Segurança. |
| descrição | Texto | Não | Simples | `O motorista cobrou mais do combinado.` | — | Campo opcional para detalhamento livre. |
| status | Elemento de lista enumerada | Sim | Simples | `PENDENTE`, `EM_ANALISE`, `RESOLVIDA` | valor fora do enum | Valores: `PENDENTE`, `EM_ANALISE`, `RESOLVIDA`, `ARQUIVADA`. Default: `PENDENTE`. |
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
| id do bloqueado | Texto alfanumérico | Sim | Simples | `64a9f8e7...` | igual ao usuário, nulo | Referência ao Usuário que foi bloqueado. O par (usuário + bloqueado) é único no sistema. |
| criado em | Data e hora | Sim | Simples | `2025-05-13T08:00:00Z` | data futura | Gerado automaticamente no ato do bloqueio. |

---

### 2.8. Matching

Matching é o resultado calculado pelo algoritmo de compatibilidade entre dois trajetos — um de oferta e um de procura. Armazena o índice de compatibilidade, o desvio calculado, os dias compatíveis e o custo estimado. Serve como cache de resultados e base para auditoria do algoritmo. Um resultado é invalidado quando qualquer um dos trajetos envolvidos é editado, pausado ou inativado.

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
| dias compatíveis | Elemento de lista enumerada | Sim | Multivalorado (M) | `["SEGUNDA","TERCA"]` | lista vazia | Interseção dos dias da semana dos dois trajetos. A combinação só é válida nesses dias. |
| custo estimado por passageiro | Número decimal | Sim | Simples | `6.00`, `4.50` | negativo, zero | Estimativa do valor que o passageiro pagará caso confirme a carona. Exibido na lista de combinações. |
| válido | Booleano | Sim | Simples | `true`, `false` | nulo | `false` quando algum dos trajetos é editado ou inativado. Somente `true` é exibido ao usuário. |
| expira em | Data e hora | Não | Simples | `2025-06-01T00:00:00Z` | anterior à criação | Data de expiração automática do documento pelo banco. |
| criado em | Data e hora | Sim | Simples | `2025-05-12T06:00:00Z` | data futura | Gerado automaticamente quando o algoritmo calcula o resultado. |

---

### 2.9. Indicadores

Indicadores é o conjunto de métricas de mobilidade, sustentabilidade e financeiro de um usuário em um período específico. São pré-calculados e atualizados ao concluir cada viagem, garantindo resposta rápida na tela de Dados. Consideram apenas viagens com status `CONCLUIDA` que tiveram ao menos um passageiro efetivo.

#### Atributos de Indicadores

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| identificador | Texto alfanumérico | Sim | Simples | `64i9h8g7...` | vazio, nulo | Gerado pelo banco. |
| id do usuário | Texto alfanumérico | Sim | Simples | `64a1f3c2...` | nulo | Referência ao Usuário dono dos indicadores. |
| período | Elemento de lista enumerada | Sim | Simples | `MENSAL`, `TOTAL` | valor fora do enum | Valores: `SEMANAL`, `MENSAL`, `SEMESTRAL`, `TOTAL`. |
| referência | Texto alfanumérico | Sim | Simples | `2025-05`, `2025-S1`, `TOTAL` | vazio | Identifica o período específico. Para mensal: `AAAA-MM`. Para semestral: `AAAA-S1/S2`. Para semanal: `AAAA-W##`. |
| mobilidade | Dados de mobilidade | Sim | Composto (C) | ver tipo especial 3.6 | nulo | Totais de caronas, viagens, km e carros evitados no período. |
| sustentabilidade | Dados ambientais | Sim | Composto (C) | ver tipo especial 3.7 | nulo | Energia economizada, CO₂ evitado e equivalência em árvores. |
| financeiro | Dados financeiros | Sim | Composto (C) | ver tipo especial 3.8 | nulo | Economia do passageiro e receita do motorista no período. |
| série histórica | Ponto de série temporal | Sim | Multivalorado e Composto (MC) | ver tipo especial 3.9 | lista vazia | Array de pontos para os gráficos de evolução. Atualizado ao concluir cada viagem. |
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

### 3.2. Localização

Estrutura composta que representa um ponto geográfico com endereço textual e coordenadas. Usada em Trajeto, Carona e nos pontos de embarque/desembarque de participantes.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| endereço | Texto | Sim | Endereço textual legível. Ex.: `Av. Central, 100 — Centro`. |
| bairro | Texto | Não | Extraído do geocoding via Google Maps. |
| cidade | Texto | Sim | Extraída do geocoding. Ex.: `Campinas`. |
| latitude | Número decimal | Sim | Coordenada geográfica. Ex.: `-22.9064`. Usada no matching geoespacial. |
| longitude | Número decimal | Sim | Coordenada geográfica. Ex.: `-47.0616`. Usada no matching geoespacial. |

---

### 3.3. Snapshot do Veículo

Cópia imutável dos dados relevantes do veículo no momento da criação da carona. Garante que o histórico de viagens não seja afetado por edições futuras no cadastro do veículo.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| modelo | Texto | Sim | Ex.: `Volkswagen Gol`. |
| placa | Texto alfanumérico | Sim | Exibida nos detalhes da carona: `Hatch · ABC-0000`. |
| tipo | Elemento de lista enumerada | Sim | Ex.: `HATCH`, `VAN`. |
| cor | Texto | Sim | Ex.: `Branco`. |

---

### 3.4. Custos da Carona

Estrutura composta que detalha todos os valores financeiros de uma carona. Calculada ao concluir a viagem.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| distância da rota (km) | Número decimal | Sim | Distância direta do trajeto do motorista. Copiada do trajeto na criação. |
| distância total com desvios (km) | Número decimal | Sim | Rota base + soma de todos os desvios gerados pelos passageiros. Calculada ao concluir. |
| custo de energia (R$) | Número decimal | Sim | `(distância total / consumo médio) × preço da energia`. |
| pedágio (R$) | Número decimal | Sim | Informado pelo motorista. `0.00` quando não há pedágio. |
| estacionamento (R$) | Número decimal | Sim | Informado pelo motorista. `0.00` quando não há estacionamento. |
| total bruto (R$) | Número decimal | Sim | `custo energia + pedágio + estacionamento`. |
| total rateado (R$) | Número decimal | Sim | Calculado ao concluir com base nos participantes efetivos. O motorista sempre participa da divisão. Diferença de centavos é atribuída ao motorista. |

---

### 3.5. Participação na Carona

Estrutura composta e multivalorada que representa cada participante de uma carona. Inclui o motorista e todos os passageiros com dados individuais de localização, status e pagamento.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| id do usuário | Texto alfanumérico | Sim | Referência ao Usuário. |
| papel | Elemento de lista enumerada | Sim | Valores: `MOTORISTA`, `PASSAGEIRO`. |
| id do trajeto do passageiro | Texto alfanumérico | Não | Referência ao Trajeto de procura. Nulo para o motorista. |
| origem de embarque | Localização (C) | Não | Ponto de embarque do passageiro. Pode diferir da origem do trajeto. Nulo para o motorista. |
| destino de desembarque | Localização (C) | Não | Ponto de desembarque do passageiro. Nulo para o motorista. |
| desvio gerado (km) | Número decimal | Não | Desvio que este passageiro gera na rota do motorista. Calculado no matching e registrado ao confirmar. Nulo para o motorista. |
| status | Elemento de lista enumerada | Sim | Valores: `CONFIRMADO`, `CANCELADO`, `AUSENTE`, `CONCLUIDO`. |
| motivo do cancelamento | Texto | Não | Preenchido pelo participante ao cancelar. |
| valor máximo (R$) | Número decimal | Sim | Gravado no momento da confirmação. Nunca pode ser superado pelo valor final. Corresponde à metade do custo da rota com o desvio do passageiro. |
| valor final (R$) | Número decimal | Não | Calculado somente ao concluir a viagem. Para o motorista: valor recebido de volta. Nulo até a conclusão. |
| status do pagamento | Elemento de lista enumerada | Sim | Valores: `PENDENTE`, `AGUARDANDO_CONFIRMACAO`, `PAGO`, `NAO_APLICAVEL`. |
| confirmação de recebimento pelo motorista | Booleano | Sim | `false` por padrão. Atualizado pelo motorista ao confirmar que recebeu. |
| confirmado em | Data e hora | Sim | Timestamp de entrada do participante na carona. |
| cancelado em | Data e hora | Não | Nulo enquanto não cancelar. |
| atualizado em | Data e hora | Sim | Atualizado a cada mudança no subdocumento. |

---

### 3.6. Dados de Mobilidade

Estrutura composta com os indicadores de mobilidade de um período.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| total de caronas | Número inteiro | Sim | Participações individuais (como motorista e como passageiro) no período. |
| total de viagens | Número inteiro | Sim | Eventos únicos de deslocamento no período. |
| km compartilhados | Número decimal | Sim | Soma dos km das viagens participadas no período. |
| carros evitados (estimativa) | Número inteiro | Sim | Cada passageiro efetivo = 1 carro evitado. |

---

### 3.7. Dados de Sustentabilidade

Estrutura composta com os indicadores ambientais de um período.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| combustível economizado (L) | Número decimal | Não | Para veículos de combustão. Nulo para elétricos. |
| energia economizada (kWh) | Número decimal | Não | Para veículos elétricos. Nulo para combustão. |
| CO₂ evitado (kg) | Número decimal | Sim | `energia economizada × fator de emissão`. Fator varia pela fonte de energia do veículo. |
| árvores equivalentes | Número decimal | Sim | `CO₂ evitado / 22`. Referência: 1 árvore absorve ~22 kg CO₂/ano. |

---

### 3.8. Dados Financeiros do Indicador

Estrutura composta com os indicadores financeiros de um período.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :---: | :--- | :---: | :--- |
| economia total como passageiro (R$) | Número decimal | Sim | Diferença entre o custo de ir sozinho e o valor pago no rateio. `0.00` se o usuário não foi passageiro no período. |
| receita total como motorista (R$) | Número decimal | Sim | Soma dos valores finais recebidos nas viagens concluídas do período. `0.00` se o usuário não foi motorista. |

---

### 3.9. Ponto de Série Temporal

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
| Usuário | `usuarios` | `modules/usuario` | `vinculoAcademico` embutido (composto simples, sempre consultado junto). `tiposParticipacao` como array de strings simples. |
| Veículo | `veiculos` | `modules/usuario` | Documento simples. Referenciado por id em `trajetos` e `caronas`. |
| Trajeto | `trajetos` | `modules/trajeto` | `origem` e `destino` embutidos com coordenadas (sempre consultados juntos; necessários para índice geoespacial). `diasSemana` como array de strings. |
| Carona | `caronas` | `modules/carona` | `participantes` embutidos como array de subdocumentos (atomicidade de vagas e valores). `custos` e `veiculoSnapshot` embutidos (snapshot imutável). |
| Avaliação | `avaliacoes` | `modules/avaliacao` | Documento simples com referências por id. |
| Denúncia | `denuncias` | `modules/seguranca` | Documento simples com referências por id. |
| Bloqueio | `bloqueios` | `modules/seguranca` | Documento simples com referências por id. |
| Matching | `matchings` | `modules/matching` | `diasCompativeis` como array de strings. Referências denormalizadas (`passageiroId`, `motoristaId`) para evitar lookups extras. |
| Indicadores | `indicadores` | `modules/indicador` | `mobilidade`, `sustentabilidade` e `financeiro` embutidos. `serie` embutida como array de subdocumentos (volume limitado e sempre lida em conjunto). |

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
  "mediaAvaliacao": 4.8,
  "totalAvaliacoes": 32,
  "totalViagens": 32,
  "totalAusencias": 0,
  "ativo": true,
  "criadoEm": "2025-03-01T09:00:00Z",
  "atualizadoEm": "2025-09-15T08:30:00Z"
}
```

**Justificativa:** `vinculoAcademico` é embutido pois é sempre consultado junto ao usuário (perfil, matching, moderação) e tem estrutura fixa e tamanho limitado. `tiposParticipacao` é array de strings simples pois os valores são atômicos e a lista é pequena.

---

### 4.2. Coleção `veiculos`

```json
{
  "_id": "64b2e1d3f5c1b2a3d4e5f6a7",
  "usuarioId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "apelido": "Gol Azul",
  "modelo": "Volkswagen Gol",
  "placa": "ABC-1234",
  "cor": "Azul",
  "tipo": "HATCH",
  "capacidadeTotal": 5,
  "fonteEnergia": "FLEX",
  "consumoMedio": 12.5,
  "unidadeConsumo": "KM_POR_LITRO",
  "precoEnergiaAtual": 6.49,
  "ativo": true,
  "criadoEm": "2025-03-05T10:00:00Z",
  "atualizadoEm": "2025-09-01T11:00:00Z"
}
```

**Justificativa:** Coleção separada de `usuarios` pois um usuário pode ter múltiplos veículos e veículos são consultados de forma independente. Referenciado por id em `trajetos` e `caronas` (onde os dados relevantes são copiados como snapshot).

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

**Justificativa:** `origem` e `destino` embutidos pois são sempre consultados junto ao trajeto e as coordenadas precisam de índice geoespacial 2dsphere no próprio documento. `diasSemana` como array simples para facilitar o operador `$in` do MongoDB no matching.

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
    "placa": "ABC-1234",
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
  "frequenciaCobranca": "POR_VIAGEM",
  "custos": {
    "distanciaRotaKm": 12.4,
    "distanciaTotalComDesviosKm": 14.5,
    "custoEnergia": 12.00,
    "pedagio": 4.00,
    "estacionamento": 8.00,
    "totalBruto": 24.00,
    "totalRateado": 24.00
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
      "valorMaximo": 6.00,
      "valorFinal": 6.00,
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
      "valorMaximo": 6.00,
      "valorFinal": 6.00,
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

**Justificativa:** `participantes` embutidos pois são sempre lidos junto à carona e as operações de vaga precisam ser atômicas (incremento/decremento com `$inc` em um único documento). `custos` e `veiculoSnapshot` embutidos por serem snapshots imutáveis do momento da viagem.

---

### 4.5. Coleção `avaliacoes`

```json
{
  "_id": "64e5d4c3b2a1f0e9d8c7b6a5",
  "caronaId": "64d4c3b2f7e3d4c5b6a7f8e9",
  "avaliadorId": "64a9f8e7d6c5b4a3f2e1d0c9",
  "avaliadoId": "64a1f3c2e4b0a1b2c3d4e5f6",
  "nota": 5,
  "comentario": "Motorista pontual e carro limpo!",
  "criadoEm": "2025-05-12T09:00:00Z"
}
```

**Justificativa:** Coleção separada para permitir consultas independentes do histórico de avaliações de um usuário sem carregar a carona completa. Índice único composto em `{ caronaId, avaliadorId }` para impedir avaliação duplicada.

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

**Justificativa:** Coleção separada para suporte à moderação. Consultada de forma independente — fila de análise, histórico do denunciado — sem necessidade de acessar a carona.

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

**Justificativa:** Coleção mínima e de leitura frequente. Consultada em todas as operações de matching, aceite de carona e envio de mensagem para verificar bloqueios bidirecionais. Índice único em `{ usuarioId, bloqueadoId }`.

---

### 4.8. Coleção `matchings`

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

**Justificativa:** `passageiroId` e `motoristaId` denormalizados (duplicados) para evitar lookup extra nos trajetos ao listar combinações. `diasCompativeis` como array simples para filtro direto. Índice TTL em `expiradoEm` para limpeza automática de resultados obsoletos.

---

### 4.9. Coleção `indicadores`

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

**Justificativa:** Subdocumentos `mobilidade`, `sustentabilidade` e `financeiro` embutidos pois são sempre lidos em conjunto na tela de Dados. `serie` embutida como array com volume controlado (máx. ~52 pontos para mensal semanal). Índice único composto `{ usuarioId, periodo, referencia }` garante um documento por usuário/período.

---

## 5. Consultas, índices e observações de arquitetura

| Consulta esperada | Campos envolvidos | Índices sugeridos | Observações |
| :--- | :--- | :--- | :--- |
| Buscar usuário por e-mail (login) | `email` | `{ email: 1 }` único | Consulta crítica no fluxo de autenticação. |
| Buscar usuário por UID Firebase | `firebaseUid` | `{ firebaseUid: 1 }` único | Executada a cada requisição autenticada para carregar o contexto do usuário. |
| Listar veículos de um motorista | `usuarioId` | `{ usuarioId: 1 }` | Consulta simples na tela de cadastro de trajeto. |
| Listar trajetos ativos de um usuário | `usuarioId`, `status` | `{ usuarioId: 1, status: 1 }` | Exibidos no perfil e no dashboard. |
| Buscar trajetos por proximidade de origem | `origem.coordenadas` | `{ "origem.coordenadas": "2dsphere" }` | Consulta geoespacial central do matching. |
| Buscar trajetos por proximidade de destino | `destino.coordenadas` | `{ "destino.coordenadas": "2dsphere" }` | Filtra trajetos com destino próximo ao destino do passageiro. |
| Listar trajetos por dia da semana | `diasSemana`, `status` | `{ diasSemana: 1, status: 1 }` | Pré-filtro do matching antes do cálculo geoespacial. |
| Listar caronas de um motorista | `motoristaId`, `status` | `{ motoristaId: 1, status: 1 }` | Histórico de viagens e painel do motorista. |
| Listar caronas de um passageiro | `participantes.usuarioId` | `{ "participantes.usuarioId": 1 }` | Histórico de viagens e viagem ativa do passageiro. |
| Buscar carona por número sequencial | `numero` | `{ numero: 1 }` único | Referência amigável exibida na UI como `Viagem #042`. |
| Buscar combinações válidas por passageiro | `trajetoPassageiroId`, `valido`, `indiceCompatibilidade` | `{ trajetoPassageiroId: 1, valido: 1, indiceCompatibilidade: -1 }` | Lista de combinações ordenada por score. |
| Verificar bloqueio entre dois usuários | `usuarioId`, `bloqueadoId` | `{ usuarioId: 1, bloqueadoId: 1 }` único | Executada no matching, no aceite de carona e no chat. |
| Verificar avaliação duplicada | `caronaId`, `avaliadorId` | `{ caronaId: 1, avaliadorId: 1 }` único | Impede dupla avaliação na mesma viagem. |
| Buscar histórico de avaliações de um usuário | `avaliadoId` | `{ avaliadoId: 1 }` | Exibido no perfil. |
| Buscar fila de denúncias pendentes | `status` | `{ status: 1 }` | Para moderação. Filtro por `PENDENTE` e `EM_ANALISE`. |
| Buscar indicadores de um usuário por período | `usuarioId`, `periodo`, `referencia` | `{ usuarioId: 1, periodo: 1, referencia: 1 }` único | Consulta principal da tela de Dados. |
| Limpeza automática de matchings obsoletos | `expiradoEm` | `{ expiradoEm: 1 }` TTL | MongoDB remove automaticamente documentos expirados. |
