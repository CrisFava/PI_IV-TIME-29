# Converge

## Equipe do Projeto

| Integrante                      | RA       |
| :------------------------------ | :------- |
| Allan Giovanni Matias Paes      | 25008211 |
| Cristian Eduardo Fava           | 25000636 |
| Daniela Mikie Kikuchi Gonçalves | 25003068 |
| Gustavo Alves de Siqueira Costa | 25001650 |
| Sara Fernandes Monteiro         | 25024107 |

O **Converge** é uma plataforma que conecta estudantes universitários com rotas e horários compatíveis para compartilhamento de caronas diárias. O projeto substitui grupos informais e desorganizados por um ecossistema integrado com verificação de vínculo acadêmico, rateio transparente de custos via PIX, chat em tempo real, avaliações mútuas e indicadores de impacto ambiental (CO₂ evitado, consumo de combustível etc.).

Desenvolvido para o componente curricular **Ideação e Validação em Engenharia de Software** da Escola Politécnica da **PUC-Campinas**.

📅 **Cronograma do Projeto:** [Planilha no Google Sheets](https://docs.google.com/spreadsheets/d/1arm-50iZBGYSQvaEW27iy0OP_JpuNIx8qC4BAJbUTD0/edit?usp=sharing)

---

## Visão Geral da Arquitetura

O sistema é formado por três módulos dentro do diretório [`app/`](app), apoiados por serviços do Firebase e do Google Maps:

```
 ┌─────────────────────────┐  Login / ID Token   ┌──────────────────────────────┐
 │    App Móvel Flutter    │ ◄─────────────────► │  Firebase (es-pi-iv-2026)    │
 │     (app/converge)      │  Chat em tempo real │  Auth · Realtime DB · Storage│
 │                         │ ◄─────────────────► │                              │
 └────────────┬────────────┘                     └──────────────────────────────┘
              │ HTTP / REST (/api/v1)
              │ Authorization: Bearer <Firebase ID Token>
              ▼
 ┌─────────────────────────┐   TCP (porta 3000)   ┌──────────────────────────────┐
 │   API Spring Boot 4     │ ◄──────────────────► │   Servidor de Sockets Java   │
 │ (app/converge-backend)  │                      │        (app/server)          │
 └──────┬───────────┬──────┘                      │ matching, custos, PIX, etc.  │
        │           │                             └──────────────────────────────┘
        ▼           ▼
 ┌────────────┐ ┌──────────────────────────┐
 │  MongoDB   │ │ Google Maps Platform     │
 │   Atlas    │ │ (Places e Routes API)    │
 └────────────┘ └──────────────────────────┘
```

| Módulo          | Diretório                                      | Tecnologia                                                 | Papel no Sistema                                                                                                                                                                       |
| :-------------- | :--------------------------------------------- | :--------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Server**      | [`app/server`](app/server)                     | Java 21 puro, Sockets TCP, Maven                           | Faz os cálculos pesados: matching, custo e rateio da viagem, distâncias, código PIX (BR Code), indicadores de sustentabilidade e validação de CPF e RENAVAM. Cada cálculo é um *handler*. |
| **Backend API** | [`app/converge-backend`](app/converge-backend) | Java 21, Spring Boot 4, Spring Security, MongoDB, SpringDoc | API REST em monólito modular hexagonal. Valida o token do Firebase, aplica as regras de negócio, persiste no MongoDB e delega os cálculos ao servidor de sockets.                      |
| **Mobile App**  | [`app/converge`](app/converge)                 | Flutter/Dart, Riverpod, go_router, Dio, Firebase           | App para estudantes (motoristas e passageiros): cadastro, trajetos, matching, viagens, pagamento PIX, chat, avaliações e indicadores.                                                  |

A documentação detalhada de cada camada está em [`docs/arquitetura/`](docs/arquitetura/OVERVIEW.md).

---

## Guia Rápido de Execução (Quick Start)

### Pré-requisitos

- **Java JDK 21** (`java -version`)
- **Maven 3.9+** ou os wrappers incluídos (`./mvnw` / `mvnw.cmd`)
- **Flutter SDK** com **Dart 3.13.3+** (`flutter doctor`)
- Cluster ativo no **MongoDB Atlas**
- Chave da **Google Maps Platform** (Places API, Routes API e Maps SDK)
- Acesso ao projeto Firebase **`es-pi-iv-2026`** (para obter os arquivos de configuração do app)
- **Python 3.9+** _(opcional, para os [scripts de desenvolvimento](scripts/README.md))_

---

### 1. Configurar o `.env` do Backend

Crie o arquivo a partir do exemplo:

```bash
# Linux / macOS:
cp app/converge-backend/.env-example app/converge-backend/.env

# Windows (PowerShell):
Copy-Item app/converge-backend/.env-example app/converge-backend/.env
```

Preencha `app/converge-backend/.env`:

```properties
MONGODB_USERNAME=seu_usuario
MONGODB_PASSWORD=sua_senha
MONGODB_CONNECTION_STRING=mongodb+srv://seu_usuario:sua_senha@seu-cluster.mongodb.net/?appName=converge-cluster

MONGODB_CLUSTER_NAME=converge-cluster
MONGODB_DATABASE_NAME_DEV=converge-dev
MONGODB_DATABASE_NAME_TESTS=converge-tests
MONGODB_DATABASE_NAME_PROD=converge-prod

ENV_CHECK=true

# Busca de endereços e cálculo de rotas (Places e Routes API)
GOOGLE_MAPS_API_KEY=sua_chave_aqui

# Job de cobranças vencidas (RF67). Deixe desligado no banco compartilhado converge-dev
# CONVERGE_PAYMENT_OVERDUE_ENABLED=false
# CONVERGE_PAYMENT_OVERDUE_SINCE=2026-11-20T00:00:00
```

> [!NOTE]
> O perfil `dev` (padrão) usa o banco `converge-dev`. Para usar outro banco sem mexer no `.env`, defina `MONGODB_DATABASE_NAME` no terminal antes de subir a API.

---

### 2. Configurar o App Flutter

Os arquivos nativos do Firebase **não são versionados** (estão no `.gitignore` de `app/converge`). Coloque-os nos caminhos abaixo:

- `app/converge/android/app/google-services.json`
- `app/converge/ios/Runner/GoogleService-Info.plist`

Eles podem ser baixados no console do Firebase (projeto `es-pi-iv-2026`) ou gerados com o FlutterFire CLI:

```bash
cd app/converge
flutterfire configure --project=es-pi-iv-2026
```

Crie também o `.env` do app:

```bash
cd app/converge
cp .env.example .env   # preencha GOOGLE_MAPS_API_KEY e API_BASE_URL
```

O `.env` é empacotado como asset e lido em tempo de execução (`flutter_dotenv` no Dart e `AppDelegate` no iOS). Sem a `GOOGLE_MAPS_API_KEY`, o mapa e o cálculo de rotas falham com `MISSING_API_KEY`.

A URL da API vem de `API_BASE_URL` (não há endereço fixo no código; sem ela o app avisa ao iniciar):

| Onde o app roda   | `API_BASE_URL`                           |
| :---------------- | :--------------------------------------- |
| Simulador iOS     | `http://localhost:8080/api/v1`           |
| Emulador Android  | `http://10.0.2.2:8080/api/v1`            |
| Celular físico    | `http://<IP-da-sua-máquina>:8080/api/v1` |

---

### 3. Subir os Serviços

> [!IMPORTANT]
> A ordem importa: o backend compila contra o artefato `com.converge:server` e, ao iniciar, **se recusa a subir se o servidor de sockets não estiver respondendo em `localhost:3000`** (erro `SERVER_NOT_RUNNING`).

#### Opção A: script automático (recomendado)

O [`scripts/run_servers.py`](scripts/README.md#run_serverspy) instala o `server` no Maven local, abre um terminal com o servidor de sockets, espera a porta 3000 responder e abre outro terminal com o backend já com o `.env` carregado. Funciona no Windows, macOS e Linux.

```bash
python scripts/run_servers.py
```

#### Opção B: manualmente (2 terminais)

**Terminal 1: Servidor de Sockets (`app/server`)**

```bash
cd app/server
./mvnw clean install          # instala o artefato usado pelo backend (Windows: .\mvnw.cmd)
./mvnw exec:java -Dexec.mainClass="com.converge.socket.Main"
```

_O servidor escuta na porta **3000**. Para encerrar com segurança, digite `stop`, `exit` ou `desativar` no console._

**Terminal 2: API REST Spring Boot (`app/converge-backend`)**

```bash
cd app/converge-backend
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

_A API fica disponível em:_

- **Endpoints REST:** `http://localhost:8080/api/v1`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`

_Rotas protegidas exigem o cabeçalho `Authorization: Bearer <Firebase ID Token>`._

#### Aplicativo Flutter (`app/converge`)

Com um emulador ou dispositivo conectado (`flutter devices`):

```bash
cd app/converge
flutter pub get
flutter run
```

---

### Execução pela IDE (IntelliJ IDEA / VS Code / Eclipse)

1. Abra a pasta `app` como projeto Maven (o [`app/pom.xml`](app/pom.xml) agrega `server` e `converge-backend`).
2. Rode o método `main` de [`app/server/src/main/java/com/converge/socket/Main.java`](app/server/src/main/java/com/converge/socket/Main.java).
3. Garanta que `app/converge-backend/.env` existe e é carregado na configuração de execução, e rode [`ConvergeBackendApplication.java`](app/converge-backend/src/main/java/com/converge/api/ConvergeBackendApplication.java).
4. Abra [`app/converge/lib/main.dart`](app/converge/lib/main.dart), escolha o dispositivo e pressione **F5**.

---

### Primeiros Passos no App

Todo cadastro nasce com o vínculo acadêmico `PENDING` e a CNH `UNDER_REVIEW`, e ainda não há tela de aprovação de documentos. Sem os dois `VERIFIED`, ninguém oferece nem pede carona. Para aprovar uma conta de teste:

```bash
python scripts/verificar_usuario.py aluno@puccampinas.edu.br
```

Veja o [`scripts/README.md`](scripts/README.md) para os demais scripts, como a simulação de corridas completas contra a API (`simular_corrida.py`) e o teste manual no app com robôs fazendo a outra ponta da corrida (`teste_no_app.py`).

---

## 🧪 Testes

### Módulos Java (JUnit 5)

```bash
# Servidor de sockets
cd app/server
./mvnw test

# API backend (instale o server antes: ./mvnw install em app/server)
cd app/converge-backend
./mvnw test
```

> [!NOTE]
> Os testes do backend rodam com o perfil `test` e precisam de `MONGODB_CONNECTION_STRING` (do `.env` ou do ambiente). O banco usado é o `converge-testes`.

O `RideJourneyTest` cobre a jornada inteira da viagem com a infraestrutura mockada e roda em menos de um segundo:

```bash
cd app/converge-backend
./mvnw test -Dtest=RideJourneyTest
```

### Aplicativo Flutter

```bash
cd app/converge
flutter analyze   # deve terminar com 0 erros e 0 warnings
flutter test
```

### Integração Contínua

O workflow [`.github/workflows/tests.yml`](.github/workflows/tests.yml) roda em todo pull request e em pushes para `develop` e `main`, com três jobs em paralelo: testes do `server`, testes do backend (com um MongoDB 7 local) e `flutter test`. O job **Testes aprovados** só passa se os três passarem.

---

## Estrutura de Pastas do Repositório

```text
.
├── README.md                           # Este documento
├── AGENTS.md                           # Regras para agentes de IA e padrões do projeto
├── .github/workflows/tests.yml         # CI: server, backend e Flutter
│
├── app/
│   ├── pom.xml                         # Agregador Maven (server + converge-backend)
│   ├── mvnw / mvnw.cmd                 # Maven Wrapper
│   │
│   ├── server/                         # Servidor de sockets TCP (Java 21 puro)
│   │   └── src/main/java/com/converge/socket/
│   │       ├── Main.java               # Ponto de entrada e registro dos handlers (porta 3000)
│   │       ├── core/                   # client, connection, exception, handler, protocol
│   │       └── handler/                # Cálculos: matching, ride (custo e rateio), distance,
│   │                                   # pix, sustainability, indicator, cpf, renavam, math
│   │
│   ├── converge-backend/               # API REST Spring Boot 4 (Java 21)
│   │   ├── .env-example
│   │   └── src/main/
│   │       ├── java/com/converge/api/
│   │       │   ├── modules/            # Bounded contexts, cada um com application,
│   │       │   │                       # domain, infrastructure e presentation:
│   │       │   │                       # user (usuário, veículo, CNH, PIX), route,
│   │       │   │                       # matching, ride (viagem e pagamento), review,
│   │       │   │                       # security (denúncias e bloqueios), indicator
│   │       │   └── shared/             # application, domain (notificações, auditoria),
│   │       │                           # infrastructure (auth Firebase, idempotência,
│   │       │                           # mongo, scheduling, socket, startup), presentation
│   │       └── resources/              # application.yaml + perfis dev / test / prod
│   │
│   └── converge/                       # App Flutter
│       ├── .env.example                # GOOGLE_MAPS_API_KEY e API_BASE_URL
│       ├── database.rules.json         # Regras do Realtime Database (chat)
│       └── lib/
│           ├── main.dart / app_widget.dart / app_router.dart / app_routes.dart
│           ├── core/                   # config (AppColors), network (Dio + interceptors),
│           │                           # services (auth, maps, storage), utils, widgets
│           └── features/               # auth, home, route, matching, ride, chat,
│                                       # review, vehicle, security, indicator
│
├── scripts/                            # Scripts Python de desenvolvimento e simulação
│
└── docs/
    ├── arquitetura/                    # OVERVIEW + backend, frontend e servidor de sockets
    ├── frontend/                       # Regras do app Flutter
    ├── BD/                             # Modelagem MongoDB e dicionário de dados
    ├── requisitos/                     # Requisitos funcionais e não funcionais
    ├── Telas/                          # Protótipos de interface
    ├── assets/                         # Diagramas usados na documentação
    ├── MapaMental.pdf
    └── Relatório_PI_IV.pdf
```

---

## 🎯 Funcionalidades

### Implementadas

- **Conta e perfil:** cadastro e login com Firebase Auth, recuperação de senha, dados acadêmicos, foto de perfil e validação de CPF.
- **Motorista:** cadastro de veículos (com validação de RENAVAM, consumo e fonte de energia), CNH e chave PIX, exigidos para oferecer carona.
- **Trajetos:** oferta e procura com busca de endereços (Google Places), horários, dias da semana e desvio máximo aceitável.
- **Matching:** índice de compatibilidade calculado no servidor de sockets e resultados ordenados.
- **Viagem:** pedido de vaga com aceite ou recusa (e expiração automática), embarque, registro de ausência, confirmação de chegada por passageiro, conclusão, cancelamento e saída da carona.
- **Custos e pagamento:** estimativa de custo da rota com desvios, rateio entre passageiros com valor máximo garantido, cobrança automática com PIX Copia e Cola e QR Code, "Já paguei" e controle de inadimplência.
- **Chat em tempo real** entre os participantes da carona (Firebase Realtime Database).
- **Confiança:** avaliações mútuas, reputação, selos recalculados semanalmente, denúncias e bloqueios.
- **Indicadores:** caronas, quilômetros compartilhados, economia, CO₂ evitado e equivalências, por usuário e da plataforma, com série histórica e metodologia.
- **Histórico de viagens** e área de pagamentos pendentes.

### Pendentes

- Vínculo recorrente de vaga reservada (RF64–RF66).
- Tela ou endpoint para aprovar documentos (hoje feito com `scripts/verificar_usuario.py`).
- Moderação das denúncias (fila de análise e resolução).
- Execução via Docker (`compose.yaml` ainda não configurado).

A lista completa de requisitos está em [`docs/requisitos/`](docs/requisitos/reqFuncionais.md).

---

## Resolução de Problemas Comuns (Troubleshooting)

### 1. `Cannot resolve symbol 'socket'` / pacote do server não encontrado no backend

**Causa:** o backend precisa do artefato `com.converge:server:1.0-SNAPSHOT` no repositório Maven local (`~/.m2/repository`). Um jar antigo também causa erros de compilação ou cálculos desatualizados.
**Solução:**

```bash
cd app/server
./mvnw clean install
```

Depois, recarregue os projetos Maven na IDE (**Reload All Maven Projects**).

### 2. Backend encerra ao iniciar com `Servidor não está no ar.`

**Causa:** o servidor de sockets não está respondendo em `localhost:3000`.
**Solução:** suba o `app/server` primeiro (ou use `python scripts/run_servers.py`, que espera a porta abrir antes de iniciar o backend).

### 3. Erro de conexão ou SSL no MongoDB Atlas (`Received fatal alert: internal_error` ou timeout)

**Causa:** o IP da sua máquina não está liberado no firewall do Atlas.
**Solução:**

1. Acesse o painel do [MongoDB Atlas](https://cloud.mongodb.com/).
2. Vá em **Security** ➔ **Network Access**.
3. Clique em **Add IP Address** ➔ **Add Current IP Address** (ou `0.0.0.0/0` temporariamente, só em ambiente de testes).
4. Confira no `.env` se usuário e senha estão corretos.

### 4. Porta já em uso (`3000` ou `8080`)

**Causa:** outra instância do servidor de sockets (3000) ou da API (8080) já está rodando.
**Solução:** encerre o processo antigo. A porta 3000 não deve ser trocada: o backend sempre se conecta a `localhost:3000` (`SocketClient`).

### 5. App não alcança a API

**Causa:** `API_BASE_URL` aponta para `localhost` num emulador Android ou num celular físico.
**Solução:** use `http://10.0.2.2:8080/api/v1` no emulador Android ou o IP da sua máquina na rede local no celular. Depois de editar o `.env` do app, rode o `flutter run` de novo (o arquivo é empacotado como asset).
