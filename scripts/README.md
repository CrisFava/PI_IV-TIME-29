# Scripts de desenvolvimento

Ferramentas para testar o Converge sem precisar sair de casa nem fazer cadastros na mão.

| Script | Para que serve |
| :-- | :-- |
| `verificar_usuario.py` | Marca a CNH e o vínculo acadêmico de um usuário como `VERIFIED`, buscando pelo e-mail |
| `simular_corrida.py` | Percorre corridas inteiras contra o backend rodando e confere cada resposta |

Os testes **mockados** (sem banco e sem API) ficam no backend, em
`app/converge-backend/src/test/java/com/converge/api/modules/ride/application/RideJourneyTest.java`.
Veja a seção [Testes mockados](#testes-mockados) no fim.

---

## Preparação (uma vez)

Precisa de **Python 3.9+**.

```bash
# Na raiz do repositório
python3 -m venv scripts/.venv

# macOS / Linux
source scripts/.venv/bin/activate
# Windows (PowerShell)
scripts\.venv\Scripts\Activate.ps1

python -m pip install -r scripts/requirements.txt
```

Os dois scripts leem a conexão do Mongo do `app/converge-backend/.env`, o mesmo arquivo do backend.
Nada mais precisa ser configurado.

| Variável | Para quê | Padrão |
| :-- | :-- | :-- |
| `MONGODB_CONNECTION_STRING` | Conexão com o Atlas | lida do `.env` |
| `MONGODB_DATABASE_NAME` | Banco usado | `converge-dev`, igual ao perfil `dev` do backend |
| `FIREBASE_API_KEY` | Chave para criar as contas de teste | lida de `app/converge/lib/firebase_options.dart` |

Variável definida no terminal tem prioridade sobre o `.env`. Os dois scripts **se recusam a mexer
em banco com `prod` no nome**.

---

## `verificar_usuario.py`

O backend ainda não tem tela nem endpoint para aprovar documentos: todo cadastro nasce com o
vínculo acadêmico `PENDING` e a CNH `UNDER_REVIEW`. Sem os dois `VERIFIED`, ninguém oferece nem
pede carona. Este script faz essa aprovação direto no banco.

```bash
python scripts/verificar_usuario.py aluno@puccampinas.edu.br                  # CNH e vínculo
python scripts/verificar_usuario.py aluno@puccampinas.edu.br --somente cnh
python scripts/verificar_usuario.py aluno@puccampinas.edu.br --somente estudante
python scripts/verificar_usuario.py aluno@puccampinas.edu.br --banco converge-testes
```

- Aceita tanto o e-mail da conta quanto o institucional, sem diferenciar maiúsculas.
- Mostra o status de antes e de depois.
- Avisa quando a pessoa ainda não mandou a CNH, e quando a CNH já venceu. Nesse caso ela fica
  `VERIFIED` no banco, mas o backend continua tratando como `EXPIRED`.
- Retorna `0` se alterou, `1` se não achou o usuário ou não havia o que alterar, `2` se faltou
  configuração.

---

## `simular_corrida.py`

### O que ele faz

Para cada cenário, cria pessoas novas e percorre a viagem **pela API de verdade**, do mesmo jeito
que o app faria:

1. cria as contas no Firebase (`sim.<execução>.<n>.<nome>@converge-sim.test`);
2. cadastra cada pessoa em `POST /users` e aprova os documentos no Mongo (usa o `verificar_usuario.py`);
3. cadastra o carro do motorista e os trajetos de oferta e de procura para **amanhã**;
4. executa os passos do cenário e confere status, vagas, valores, PIX, avaliações e indicadores;
5. no fim, apaga as contas do Firebase (a não ser que você use `--manter-contas`).

Cada cenário cria as próprias pessoas, então eles não dependem uns dos outros.

| Cenário | O que confere |
| :-- | :-- |
| `caminho-feliz` | matching acha o motorista, 2 pedidos na mesma viagem, aceite, início, conclusão (`COMPLETED`, `completedAt`, valor final ≤ teto), cobrança com PIX, confirmação (`PAID`), avaliação (e a segunda é recusada) e indicadores |
| `ausencia-parcial` | quem falta fica `NO_SHOW` e `NOT_APPLICABLE`, a vaga volta, os presentes concluem e cobrar o ausente é recusado |
| `todos-ausentes` | sem ninguém presente, concluir vira `CANCELLED` |
| `transicoes-invalidas` | concluir antes de iniciar, avaliar antes de concluir, passageira iniciar ou concluir, cancelar ou sair com a viagem andando, concluir duas vezes, iniciar ou marcar ausência depois de concluída |
| `desistencia-cancelamento` | passageira sai antes da saída (vaga volta, não dá pra iniciar) e o motorista cancela com passageiro confirmado |
| `lotacao-recusa` | pedido recusado não pode ser refeito e carro lotado não aceita mais ninguém |

### Como rodar

Suba o ambiente como no README principal: **servidor de sockets** e depois o **backend**.

```bash
python scripts/simular_corrida.py --listar                       # lista os cenários
python scripts/simular_corrida.py                                # roda todos
python scripts/simular_corrida.py --cenario caminho-feliz        # só um
python scripts/simular_corrida.py --cenario todos-ausentes --cenario lotacao-recusa
python scripts/simular_corrida.py --api http://10.0.2.2:8080/api/v1   # outra URL
```

A saída mostra cada passo. Quando um erro era esperado, ela mostra também a resposta da API:

```text
▶ transicoes-invalidas: Ações fora de hora ou feitas pela pessoa errada são recusadas
    · Ana pede vaga
    · Motorista aceita Ana
    · Concluir antes de iniciar
      recusado como esperado (HTTP 400 BAD_REQUEST Esta ação só vale para uma viagem em andamento.)
    ...
  ✔ passou

Resultado: 6/6 cenário(s) passaram.
```

Se algum cenário falhar, ele mostra o passo e o motivo e o comando termina com código `1`.

Linhas com `AVISO` não reprovam o cenário, mas indicam algo incompleto. Hoje aparecem duas:

- **custo total R$ 0**: o `RideCostHandler` do servidor de sockets ainda devolve tudo zerado,
  porque o rateio foi retirado em 12/11 e ainda não voltou. Enquanto isso, a cobrança usa R$ 10,00.
- **indicadores sem a viagem**: o `CompletedRidesIndicatorReconciler` reprocessa em até 15 min.

### Se a simulação cair no meio

As contas do Firebase são apagadas no fim de cada execução. Se o script for interrompido (Ctrl+C,
queda de rede), use o número que aparece no topo da saída (`Simulação 1113102523 | ...`) para
apagar as contas que sobraram:

```bash
python scripts/simular_corrida.py --limpar 1113102523
```

### Ver a corrida no app

Use `--manter-contas` para não apagar as contas no fim. O script imprime os e-mails, e a senha de
todas é `Simulacao#2026`. Faça login no app com o motorista ou com um passageiro para ver a viagem,
o chat e o pagamento na interface.

### Qual banco usar

A simulação **grava de verdade** no banco que o backend está usando: pessoas `[SIM] ...`, carros,
trajetos, viagens, avaliações e indicadores. O ideal é usar um banco só para isso, passando o mesmo
nome para o backend e para o script:

```bash
# Terminal do backend
MONGODB_DATABASE_NAME=converge-sim ./mvnw spring-boot:run

# Terminal do script
MONGODB_DATABASE_NAME=converge-sim python scripts/simular_corrida.py
```

No Windows (PowerShell), defina a variável antes de cada comando com `$env:MONGODB_DATABASE_NAME="converge-sim"`.

Se o script e o backend estiverem em bancos diferentes, a simulação para logo no começo com
"não apareceu no Mongo: o script e o backend estão no mesmo banco?".

---

## Testes mockados

`RideJourneyTest` monta os casos de uso reais da viagem e troca só a infraestrutura por mocks:
banco, cálculo de custos, PIX e indicadores. Ele cobre os mesmos fluxos da simulação, mas roda em
menos de um segundo, sem backend, sem Mongo e sem Firebase. Ele também entra no `mvn test`
normal.

```bash
cd app/converge-backend
./mvnw test -Dtest=RideJourneyTest        # Windows: .\mvnw.cmd test -Dtest=RideJourneyTest
```

Qual usar:

- **Mudou regra de negócio da viagem?** Rode o `RideJourneyTest` (e o `mvn test` completo). É rápido e não depende de nada.
- **Quer saber se o sistema inteiro funciona junto?** Use o `simular_corrida.py`. Ele cobre o que o
  teste mockado não alcança: controllers, segurança, serialização JSON, Mongo, servidor de sockets e
  Firebase.
