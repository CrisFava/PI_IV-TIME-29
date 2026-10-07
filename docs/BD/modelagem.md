# Modelagem de Banco de Dados — Converge

> **Banco de dados:** MongoDB (Atlas em produção)
> **Estratégia:** Monólito Modular — cada módulo de negócio é dono exclusivo de suas coleções.
> Referências entre coleções de módulos distintos são feitas **exclusivamente por `String id`**, sem `@DBRef`.
> Valores monetários são armazenados como `Double` com 2 casas decimais. A diferença de centavos no rateio é sempre atribuída ao motorista.

---

## Visão Geral das Coleções

| Coleção | Módulo Dono | Descrição |
|---|---|---|
| `usuarios` | usuario | Dados de cadastro, perfil, vínculo acadêmico e configurações |
| `veiculos` | usuario | Veículos cadastrados pelo motorista |
| `trajetos` | trajeto | Trajetos cadastrados para oferecer ou procurar carona |
| `caronas` | carona | Viagens confirmadas, participantes, custos e status |
| `avaliacoes` | avaliacao | Avaliações mútuas após conclusão de viagem |
| `denuncias` | seguranca | Denúncias entre usuários com motivo e status |
| `bloqueios` | seguranca | Registros de bloqueio entre usuários |
| `matchings` | matching | Resultados calculados do algoritmo de compatibilidade |
| `indicadores` | indicador | Indicadores de mobilidade e sustentabilidade por usuário e período |

---

## 1. Coleção `usuarios`

**Módulo:** `modules/usuario`
**Descrição:** Armazena todos os dados de um usuário da plataforma — cadastro, perfil, vínculo acadêmico, estatísticas e configurações.

```json
{
  "_id": "ObjectId (String)",
  "firebaseUid": "String",
  "nome": "String",
  "email": "String",
  "telefone": "String",
  "fotoUrl": "String | null",
  "tiposParticipacao": ["PASSAGEIRO | MOTORISTA"],
  "vinculoAcademico": {
    "universidade": "String",
    "curso": "String",
    "matricula": "String",
    "status": "PENDENTE | EM_ANALISE | VERIFICADO | REJEITADO",
    "documentoUrl": "String | null",
    "verificadoEm": "ISODate | null"
  },
  "chavePix": "String | null",
  "frequenciaCobrancaPreferida": "POR_VIAGEM | MENSAL",
  "reputacao": {
    "motorista": {
      "nivel": "ALTA | MEDIA | BAIXA | null",
      "mediaEstrelas": "Double",
      "totalAvaliacoes": "Integer",
      "janela": {
        "viagensConcluidas": "Integer",
        "cancelamentosPeloMotorista": "Integer",
        "atrasos": "Integer",
        "denunciasProcedentes": "Integer"
      },
      "calculadoEm": "ISODate"
    },
    "passageiro": {
      "nivel": "ALTA | MEDIA | BAIXA | null",
      "mediaEstrelas": "Double",
      "totalAvaliacoes": "Integer",
      "janela": {
        "viagensConcluidas": "Integer",
        "cancelamentosTardios": "Integer",
        "ausencias": "Integer",
        "denunciasProcedentes": "Integer"
      },
      "calculadoEm": "ISODate"
    }
  },
  "selos": [
    {
      "tipo": "CONFIANCA | SUSTENTABILIDADE", "concedidoEm": "ISODate"
    }
  ],
  "ativo": "Boolean",
  "criadoEm": "ISODate",
  "atualizadoEm": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `email` | Único | Login e identificação — nunca duplicar |
| `firebaseUid` | Único | Chave de autenticação Firebase |
| `vinculoAcademico.universidade` | Simples | Filtro de matching por universidade |
| `vinculoAcademico.status` | Simples | Consultas de moderação e verificação |
| `reputacao.motorista.nivel` | Simples | Consultas de reputação de motoristas |

---

## 2. Coleção `veiculos`

**Módulo:** `modules/usuario`
**Descrição:** Veículos cadastrados por motoristas. Um usuário pode ter mais de um veículo, mas apenas um ativo por vez em um trajeto.

```json
{
  "_id": "ObjectId (String)",
  "usuarioId": "String (ref: usuarios._id)",
  "apelido": "String | null",
  "modelo": "String",
  "placa": "String",
  "cor": "String",
  "tipo": "HATCH | SEDAN | SUV | PICKUP | VAN | MOTO | OUTRO",
  "capacidadeTotal": "Integer",
  "consumos": [
    {
      "fonteEnergia": "GASOLINA | ETANOL | DIESEL | ELETRICO",
      "consumoMedio": "Double",
      "unidadeConsumo": "KM_POR_LITRO | KM_POR_KWH",
      "precoEnergiaAtual": "Double"
    }
  ],
  "ativo": "Boolean",
  "criadoEm": "ISODate",
  "atualizadoEm": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `usuarioId` | Simples | Buscar todos os veículos de um motorista |
| `usuarioId + placa` | Composto Único | Integridade — uma placa por cadastro |

### Regras de Negócio

- **Moto:** `capacidadeTotal = 1`, validado no service.
- **Placa:** sempre gravada em maiúsculas, sem hífen e sem espaços.
- **Limite de consumo médio** (validado no service; fora da faixa retorna erro 400). Valores sugeridos, a calibrar:

| Tipo | Unidade | Mín. | Máx. |
|---|---|---|---|
| HATCH / SEDAN | KM_POR_LITRO | 6 | 25 |
| SUV / PICKUP / VAN | KM_POR_LITRO | 4 | 18 |
| MOTO | KM_POR_LITRO | 15 | 50 |
| Carro elétrico | KM_POR_KWH | 3 | 9 |
| Moto elétrica | KM_POR_KWH | 15 | 40 |
| OUTRO | qualquer | usar a faixa mais ampla | |

- **Limite de preço da energia** (informado pelo usuário, mesma validação):

| Energia | Mín. | Máx. |
|---|---|---|
| Gasolina / etanol / diesel (R$/L) | 3,00 | 12,00 |
| Eletricidade (R$/kWh) | 0,30 | 3,00 |

- **Itens de `consumos`:** `GASOLINA`, `ETANOL` e `DIESEL` têm 1 item; `ELETRICO` tem 1 item; carro flex tem 2 itens (gasolina e etanol); híbrido tem 1 ou 2. Não pode haver duas entradas da mesma `fonteEnergia` no mesmo veículo.

---

## 3. Coleção `trajetos`

**Módulo:** `modules/trajeto`
**Descrição:** Trajetos cadastrados pelos usuários para oferecer ou procurar carona. Base de dados do algoritmo de matching.

```json
{
  "_id": "ObjectId (String)",
  "usuarioId": "String (ref: usuarios._id)",
  "veiculoId": "String | null (ref: veiculos._id)",
  "tipo": "OFERECER | PROCURAR",
  "origem": {
    "endereco": "String",
    "bairro": "String | null",
    "cidade": "String",
    "coordenadas": {
      "latitude": "Double",
      "longitude": "Double"
    }
  },
  "destino": {
    "endereco": "String",
    "bairro": "String | null",
    "cidade": "String",
    "coordenadas": {
      "latitude": "Double",
      "longitude": "Double"
    }
  },
  "horarioPartida": "String (HH:mm)",
  "horarioChegadaEstimado": "String (HH:mm)",
  "diasSemana": ["SEGUNDA | TERCA | QUARTA | QUINTA | SEXTA | SABADO | DOMINGO"],
  "desvioMaximoKm": "Double",
  "vagasTotal": "Integer | null",
  "vagasDisponiveis": "Integer | null",
  "distanciaRotaKm": "Double | null",
  "status": "ATIVO | PAUSADO | INATIVO",
  "criadoEm": "ISODate",
  "atualizadoEm": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `usuarioId` | Simples | Listar trajetos do usuário |
| `tipo` | Simples | Filtrar oferta vs procura no matching |
| `status` | Simples | Somente trajetos ATIVO entram no matching |
| `origem.coordenadas` | Geoespacial 2dsphere | Busca por proximidade de origem |
| `destino.coordenadas` | Geoespacial 2dsphere | Busca por proximidade de destino |
| `diasSemana` | Simples | Filtro de dias compatíveis no matching |

---

## 4. Coleção `caronas`

**Módulo:** `modules/carona`
**Descrição:** Coleção central do sistema. Representa uma viagem concreta — desde a confirmação até a conclusão. Contém participantes, custos detalhados e histórico de status. Os participantes são subdocumentos embutidos para garantir atomicidade nas operações de vaga.

```json
{
  "_id": "ObjectId (String)",
  "numero": "Integer (sequencial, ex: 42)",
  "motoristaId": "String (ref: usuarios._id)",
  "veiculoId": "String (ref: veiculos._id)",
  "trajetoMotoristaId": "String (ref: trajetos._id)",
  "origem": {
    "endereco": "String",
    "coordenadas": {
      "latitude": "Double",
      "longitude": "Double"
    }
  },
  "destino": {
    "endereco": "String",
    "coordenadas": {
      "latitude": "Double",
      "longitude": "Double"
    }
  },
  "dataHoraPartida": "ISODate",
  "dataHoraChegadaEstimada": "ISODate",
  "dataHoraConclusao": "ISODate | null",
  "diasSemana": ["SEGUNDA | TERCA | ..."],
  "vagasTotal": "Integer",
  "vagasDisponiveis": "Integer",
  "status": "AGUARDANDO_CONFIRMACAO | CONFIRMADA | EM_ANDAMENTO | CONCLUIDA | CANCELADA",
  "motivoCancelamento": "String | null",
  "canceladoPor": "String | null (ref: usuarios._id)",
  "frequenciaCobranca": "POR_VIAGEM | MENSAL",
  "custos": {
    "distanciaRotaKm": "Double",
    "distanciaTotalComDesviosKm": "Double",
    "fonteEnergiaUsada": "GASOLINA | ETANOL | DIESEL | ELETRICO",
    "consumoMedioUsado": "Double",
    "precoEnergiaUsado": "Double",
    "custoEnergia": "Double",
    "pedagio": "Double",
    "estacionamento": "Double",
    "totalBruto": "Double",
    "totalRateado": "Double"
  },
  "participantes": [
    {
      "usuarioId": "String (ref: usuarios._id)",
      "papel": "MOTORISTA | PASSAGEIRO",
      "trajetoPassageiroId": "String | null (ref: trajetos._id)",
      "origemEmbarque": {
        "endereco": "String",
        "coordenadas": {
          "latitude": "Double",
          "longitude": "Double"
        }
      },
      "destinoDesembarque": {
        "endereco": "String",
        "coordenadas": {
          "latitude": "Double",
          "longitude": "Double"
        }
      },
      "desvioGeradoKm": "Double",
      "status": "CONFIRMADO | CANCELADO | AUSENTE | CONCLUIDO",
      "motivoCancelamento": "String | null",
      "valorMaximo": "Double",
      "valorFinal": "Double | null",
      "statusPagamento": "PENDENTE | AGUARDANDO_CONFIRMACAO | PAGO | NAO_APLICAVEL",
      "confirmacoRecebimentoPeloMotorista": "Boolean",
      "confirmadoEm": "ISODate",
      "canceladoEm": "ISODate | null",
      "atualizadoEm": "ISODate"
    }
  ],
  "criadoEm": "ISODate",
  "atualizadoEm": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `numero` | Único | Referência amigável exibida na UI (Viagem #042) |
| `motoristaId` | Simples | Listar viagens do motorista |
| `participantes.usuarioId` | Simples | Listar viagens de um passageiro |
| `status` | Simples | Filtrar viagens ativas, concluídas, etc. |
| `dataHoraPartida` | Simples | Ordenação e filtro por data |
| `trajetoMotoristaId` | Simples | Vincular viagens a um trajeto recorrente |

### Regras de Negócio Críticas (mapeadas nos RFs)

- **RF21:** `vagasDisponiveis` é decrementado ao confirmar participante e incrementado ao cancelar. Quando `vagasDisponiveis == 0`, o status da carona muda para `LOTADA` (subvalor de `CONFIRMADA`).
- **RF37:** `valorMaximo` de cada participante é gravado no momento da confirmação e **nunca pode ser superado** pelo `valorFinal`.
- **RF38:** `valorFinal` só é calculado e gravado quando `status = CONCLUIDA`.
- **RNF20:** A transição de status de vaga e participante deve ser atômica (usando operações MongoDB como `$inc` e `findAndModify`/transações).
- **RNF22:** `totalBruto / qtdParticipantesEfetivos = valorPorPessoa`. Diferença de centavos vai para o motorista.

---

## 5. Coleção `avaliacoes`

**Módulo:** `modules/avaliacao`
**Descrição:** Avaliações mútuas registradas após a conclusão de uma viagem. Um usuário avalia o outro apenas uma vez por viagem.

```json
{
  "_id": "ObjectId (String)",
  "caronaId": "String (ref: caronas._id)",
  "avaliadorId": "String (ref: usuarios._id)",
  "avaliadoId": "String (ref: usuarios._id)",
  "papelAvaliado": "MOTORISTA | PASSAGEIRO",
  "nota": "Integer (1 a 5)",
  "comentario": "String | null",
  "criadoEm": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `caronaId` + `avaliadorId` | Composto Único | Impede avaliação duplicada na mesma viagem |
| `avaliadoId + papelAvaliado` | Composto | Buscar avaliações recebidas por papel (base da reputação) |
| `avaliadorId` | Simples | Verificar se usuário já avaliou determinada viagem |

---

## 6. Coleção `denuncias`

**Módulo:** `modules/seguranca`
**Descrição:** Denúncias realizadas por usuários contra outros. Mantém histórico completo para moderação.

```json
{
  "_id": "ObjectId (String)",
  "denuncianteId": "String (ref: usuarios._id)",
  "denunciadoId": "String (ref: usuarios._id)",
  "caronaId": "String | null (ref: caronas._id)",
  "motivo": "ATRASO_FALTA | CONDUTA_INADEQUADA | COBRANCA_INDEVIDA | OUTRO",
  "descricao": "String | null",
  "status": "PENDENTE | EM_ANALISE | RESOLVIDA | ARQUIVADA",
  "resolucao": "String | null",
  "analisadoPor": "String | null",
  "criadoEm": "ISODate",
  "atualizadoEm": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `denunciadoId` | Simples | Verificar histórico de denúncias de um usuário |
| `denuncianteId` | Simples | Listar denúncias feitas pelo usuário |
| `status` | Simples | Fila de moderação (PENDENTE, EM_ANALISE) |
| `caronaId` | Simples | Denúncias relacionadas a uma viagem específica |

---

## 7. Coleção `bloqueios`

**Módulo:** `modules/seguranca`
**Descrição:** Registros de bloqueio entre usuários. Usada para filtrar resultados de matching, aceites de carona e chat (RF47).

```json
{
  "_id": "ObjectId (String)",
  "usuarioId": "String (ref: usuarios._id)",
  "bloqueadoId": "String (ref: usuarios._id)",
  "criadoEm": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `usuarioId` + `bloqueadoId` | Composto Único | Impede bloqueio duplicado |
| `usuarioId` | Simples | Listar quem o usuário bloqueou |
| `bloqueadoId` | Simples | Verificar se um usuário está bloqueado (usado no matching) |

---

## 8. Coleção `matchings`

**Módulo:** `modules/matching`
**Descrição:** Armazena os resultados calculados pelo algoritmo de compatibilidade. Serve de cache dos resultados e base para auditoria do matching. Resultados expiram ou são invalidados quando trajetos são alterados.

```json
{
  "_id": "ObjectId (String)",
  "trajetoPassageiroId": "String (ref: trajetos._id)",
  "trajetoMotoristaId": "String (ref: trajetos._id)",
  "passageiroId": "String (ref: usuarios._id)",
  "motoristaId": "String (ref: usuarios._id)",
  "indiceCompatibilidade": "Double (0.0 a 1.0)",
  "desvioCalculadoKm": "Double",
  "diferencaHorarioMin": "Integer",
  "diasCompativeis": ["SEGUNDA | TERCA | ..."],
  "custoEstimadoPorPassageiro": "Double",
  "valido": "Boolean",
  "expiradoEm": "ISODate | null",
  "criadoEm": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `trajetoPassageiroId` | Simples | Buscar combinações para o passageiro |
| `trajetoMotoristaId` | Simples | Buscar passageiros compatíveis com motorista |
| `passageiroId` + `motoristaId` | Composto | Verificar se combinação específica existe |
| `valido` + `indiceCompatibilidade` | Composto | Listar combinações válidas ordenadas por score |
| `expiradoEm` | Simples (TTL) | Limpeza automática de resultados obsoletos |

---

## 9. Coleção `indicadores`

**Módulo:** `modules/indicador`
**Descrição:** Indicadores de mobilidade e sustentabilidade agregados por usuário e período. Atualizados ao concluir uma viagem. Períodos pré-calculados para resposta rápida na tela de Dados.

```json
{
  "_id": "ObjectId (String)",
  "usuarioId": "String (ref: usuarios._id)",
  "periodo": "SEMANAL | MENSAL | SEMESTRAL | TOTAL",
  "referencia": "String (ex: '2025-05' para mensal, '2025-S1' para semestral, 'TOTAL' para acumulado)",
  "mobilidade": {
    "totalCaronas": "Integer",
    "totalViagens": "Integer",
    "kmCompartilhados": "Double",
    "carrosEvitadosEstimado": "Integer"
  },
  "sustentabilidade": {
    "combustivelEconomizadoL": "Double | null",
    "energiaEconomizadaKwh": "Double | null",
    "co2EvitadoKg": "Double",
    "arvoresEquivalentes": "Double"
  },
  "financeiro": {
    "economiaTotalPassageiro": "Double",
    "receitaTotalMotorista": "Double"
  },
  "serie": [
    {
      "data": "ISODate",
      "caronas": "Integer",
      "kmCompartilhados": "Double",
      "co2EvitadoKg": "Double"
    }
  ],
  "atualizadoEm": "ISODate"
}
```

### Índices

| Campo(s) | Tipo | Justificativa |
|---|---|---|
| `usuarioId` + `periodo` + `referencia` | Composto Único | Garantir um documento por usuário/período |
| `usuarioId` | Simples | Buscar todos os períodos de um usuário |

---

## Diagrama de Relacionamentos

```
usuarios (1) ──────────────────── (N) veiculos
    │                                     │
    │ (1)                                 │ (N)
    │                                     │
    ├──── (N) trajetos ◄──────────────────┘
    │          │
    │          │ trajetoMotoristaId / trajetoPassageiroId
    │          │
    │          ▼
    │      matchings (N) ────────► trajetos
    │
    ├──── (N) caronas (como motorista)
    │          │
    │          │ participantes[] (subdocumento embutido)
    │          │   └── usuarioId, papel, status, custos individuais
    │          │
    │          ▼
    ├──── (N) avaliacoes ──────► (avaliador → avaliado)
    │
    ├──── (N) denuncias ───────► (denunciante → denunciado, ref. caronaId)
    │
    ├──── (N) bloqueios ───────► (usuário → bloqueado)
    │
    └──── (N) indicadores (por período)
```

---

## Estratégia de Embedding vs. Referência

| Situação | Decisão | Justificativa |
|---|---|---|
| Participantes dentro de `caronas` | **Embedded** (subdocumento) | Atomicidade na atualização de vagas e valores. Sempre consultados juntos com a carona. |
| Coordenadas dentro de `trajetos` e `caronas` | **Embedded** | Dados derivados do endereço, sempre consultados juntos. Necessários para matching. |
| Custos dentro de `caronas` | **Embedded** | Snapshot imutável dos custos da viagem — não deve mudar se o veículo for editado depois. |
| Veículo referenciado em `trajetos` e `caronas` | **Referência por ID** | Veículo pode ser editado; `caronas` guarda snapshot dos dados relevantes (tipo, placa) no momento da viagem. |
| Avaliações, denúncias, bloqueios | **Coleção própria** | Volume variável, consultados de forma independente, base de moderação. |
| Série temporal em `indicadores` | **Embedded (array `serie`)** | Número limitado de pontos por documento; sempre lidos em conjunto com os totais. |

---

## Enums Consolidados

### `TipoParticipacao` (usuarios.tiposParticipacao[])
| Valor | Descrição |
|---|---|
| `PASSAGEIRO` | Procura vagas em caronas |
| `MOTORISTA` | Oferece carona com veículo próprio |

### `StatusVinculo` (usuarios.vinculoAcademico.status)
| Valor | Descrição |
|---|---|
| `PENDENTE` | Recém cadastrado, aguarda envio de documentação |
| `EM_ANALISE` | Documentação enviada, em revisão |
| `VERIFICADO` | Vínculo confirmado |
| `REJEITADO` | Documentação inválida ou vínculo não confirmado |

### `TipoVeiculo` (veiculos.tipo)
| Valor | Descrição |
|---|---|
| `HATCH` | Carro hatch |
| `SEDAN` | Carro sedan |
| `SUV` | SUV |
| `PICKUP` | Camionete/Pickup |
| `VAN` | Van |
| `MOTO` | Moto |
| `OUTRO` | Outro tipo |

### `FonteEnergia` (veiculos.consumos[].fonteEnergia)
| Valor | Descrição |
|---|---|
| `GASOLINA` | Gasolina |
| `ETANOL` | Etanol |
| `DIESEL` | Diesel |
| `ELETRICO` | Energia elétrica (km/kWh) |

### `TipoTrajeto` (trajetos.tipo)
| Valor | Descrição |
|---|---|
| `OFERECER` | Motorista oferecendo vagas |
| `PROCURAR` | Passageiro procurando carona |

### `StatusTrajeto` (trajetos.status)
| Valor | Descrição |
|---|---|
| `ATIVO` | Elegível para matching |
| `PAUSADO` | Temporariamente fora do matching |
| `INATIVO` | Desativado pelo usuário |

### `DiaSemana` (trajetos.diasSemana[], caronas.diasSemana[])
| Valor | Descrição |
|---|---|
| `SEGUNDA` | Segunda-feira |
| `TERCA` | Terça-feira |
| `QUARTA` | Quarta-feira |
| `QUINTA` | Quinta-feira |
| `SEXTA` | Sexta-feira |
| `SABADO` | Sábado |
| `DOMINGO` | Domingo |

### `StatusCarona` (caronas.status)
| Valor | Descrição |
|---|---|
| `AGUARDANDO_CONFIRMACAO` | Solicitação enviada, aguardando aceite do motorista |
| `CONFIRMADA` | Motorista aceitou; vagas sendo preenchidas |
| `EM_ANDAMENTO` | Viagem iniciada |
| `CONCLUIDA` | Viagem finalizada com sucesso |
| `CANCELADA` | Cancelada por motorista ou passageiro |

### `PapelParticipante` (caronas.participantes[].papel)
| Valor | Descrição |
|---|---|
| `MOTORISTA` | Condutor da carona |
| `PASSAGEIRO` | Passageiro na carona |

### `StatusParticipante` (caronas.participantes[].status)
| Valor | Descrição |
|---|---|
| `CONFIRMADO` | Participação ativa confirmada |
| `CANCELADO` | Cancelou antes da viagem |
| `AUSENTE` | Não compareceu (registrado pelo motorista) |
| `CONCLUIDO` | Participou até o fim |

### `StatusPagamento` (caronas.participantes[].statusPagamento)
| Valor | Descrição |
|---|---|
| `PENDENTE` | Valor calculado, pagamento não realizado |
| `AGUARDANDO_CONFIRMACAO` | Passageiro marcou como pago; aguarda motorista confirmar |
| `PAGO` | Motorista confirmou recebimento |
| `NAO_APLICAVEL` | Participante cancelou ou foi ausente sem cobrança |

### `FrequenciaCobranca` (usuarios / caronas)
| Valor | Descrição |
|---|---|
| `POR_VIAGEM` | Cobrança ao fim de cada viagem |
| `MENSAL` | Cobrança consolidada mensal |

### `MotivoDenuncia` (denuncias.motivo)
| Valor | Descrição |
|---|---|
| `ATRASO_FALTA` | Atraso ou ausência injustificada |
| `CONDUTA_INADEQUADA` | Comportamento inadequado |
| `COBRANCA_INDEVIDA` | Cobrança incorreta ou abusiva |
| `OUTRO` | Outro motivo descrito na descrição |

### `StatusDenuncia` (denuncias.status)
| Valor | Descrição |
|---|---|
| `PENDENTE` | Recebida, aguarda análise |
| `EM_ANALISE` | Em revisão pela moderação |
| `RESOLVIDA` | Analisada com resolução registrada |
| `ARQUIVADA` | Arquivada sem ação (improcedente) |

### `PeriodoIndicador` (indicadores.periodo)
| Valor | Descrição |
|---|---|
| `SEMANAL` | Dados da semana atual |
| `MENSAL` | Dados do mês referência |
| `SEMESTRAL` | Dados do semestre referência |
| `TOTAL` | Acumulado histórico total |

### `NivelReputacao` (usuarios.reputacao.*.nivel)
| Valor | Descrição |
|---|---|
| `ALTA` | Boa avaliação e baixa taxa de problemas |
| `MEDIA` | Desempenho intermediário |
| `BAIXA` | Avaliação baixa ou alta taxa de problemas |

### `TipoSelo` (usuarios.selos[].tipo)
| Valor | Descrição |
|---|---|
| `CONFIANCA` | Selo exibido no perfil (apenas visual) |
| `SUSTENTABILIDADE` | Selo exibido no perfil (apenas visual) |
