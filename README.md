# Converge

## Equipe do Projeto

| Integrante                      | RA       |
| :------------------------------ | :------- |
| Allan Giovanni Matias Paes      | 25008211 |
| Cristian Eduardo Fava           | 25000636 |
| Daniela Mikie Kikuchi Gonçalves | 25003068 |
| Gustavo Alves de Siqueira Costa | 25001650 |
| Sara Fernandes Monteiro         | 25024107 |

O **Converge** é uma plataforma que conecta estudantes universitários com rotas e horários compatíveis para compartilhamento de caronas diárias. O projeto substitui grupos informais e desorganizados por um ecossistema integrado com verificação de vínculo acadêmico, rateio transparente de custos via PIX, chat em tempo real, avaliações mútuas e indicadores de impacto ambiental (CO₂ evitado, consumo de combustível, etc).

Desenvolvido para o componente curricular **Ideação e Validação em Engenharia de Software** da Escola Politécnica da **PUC-Campinas**.

📅 **Cronograma do Projeto:** [Planilha no Google Sheets](https://docs.google.com/spreadsheets/d/1arm-50iZBGYSQvaEW27iy0OP_JpuNIx8qC4BAJbUTD0/edit?usp=sharing)

---

## Visão Geral da Arquitetura

O sistema é estruturado em três módulos complementares dentro do diretório [`app/`](app):

```
       ┌────────────────────────┐   Login / ID Token   ┌────────────────────────┐
       │   App Móvel Flutter    │ ◄──────────────────► │     Firebase Auth      │
       │    (app/converge)      │                      │ (projeto es-pi-iv-2026)│
       └───────────┬────────────┘                      └────────────────────────┘
                   │ HTTP / REST (/api/v1)
                   │ Authorization: Bearer <Firebase ID Token>
                   ▼
       ┌────────────────────────┐         TCP Sockets         ┌────────────────────────┐
       │   API Spring Boot 3    │ ◄─────────────────────────► │  Servidor Socket TCP   │
       │ (app/converge-backend) │                             │      (app/server)      │
       └───────────┬────────────┘                             └────────────────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │     MongoDB Atlas      │
       │ (Banco de Dados Cloud) │
       └────────────────────────┘
```

| Módulo          | Diretório                                      | Tecnologia                              | Papel no Sistema                                                                                                                |
| :-------------- | :--------------------------------------------- | :-------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------ |
| **Server**      | [`app/server`](app/server)                     | Java 21, Sockets TCP, Maven             | Servidor de sockets de alto desempenho com serialização de objetos e handlers desacoplados para comunicação em tempo real.      |
| **Backend API** | [`app/converge-backend`](app/converge-backend) | Java 21, Spring Boot 3, MongoDB Atlas   | API RESTful com arquitetura modular hexagonal, autenticação via token do Firebase, persistência e documentação Swagger/OpenAPI. |
| **Mobile App**  | [`app/converge`](app/converge)                 | Flutter & Dart, Riverpod, Dio, Firebase | Aplicativo móvel para estudantes (motoristas e passageiros) gerenciarem caronas, perfis e viagens.                              |

---

## Guia Rápido de Execução (Quick Start)

Se você já possui o ambiente configurado, execute estes passos rápidos a partir da raiz do repositório:

### Pré-requisitos

- **Java JDK 21** (`java -version`)
- **Maven 3.9+** ou uso dos wrappers incluídos (`mvnw.cmd` / `./mvnw`)
- **Flutter SDK** com **Dart 3.13.3+** (`flutter doctor`)
- Cluster ativo no **MongoDB Atlas**
- Acesso ao projeto Firebase **`es-pi-iv-2026`** (para obter os arquivos de configuração do app)

---

### Configurar o `.env` do Backend

Crie o arquivo de configuração no módulo do backend a partir do exemplo:

```bash
# No Windows (PowerShell):
Copy-Item app/converge-backend/.env-example app/converge-backend/.env

# No Linux / macOS:
cp app/converge-backend/.env-example app/converge-backend/.env
```

Edite o arquivo `app/converge-backend/.env` com as suas credenciais do MongoDB Atlas:

```properties
MONGODB_USERNAME=seu_usuario
MONGODB_PASSWORD=sua_senha
MONGODB_CONNECTION_STRING=mongodb+srv://seu_usuario:sua_senha@seu-cluster.mongodb.net/?appName=converge-cluster

MONGODB_CLUSTER_NAME=converge-cluster
MONGODB_DATABASE_NAME_DEV=converge-dev
MONGODB_DATABASE_NAME_TESTS=converge-tests
MONGODB_DATABASE_NAME_PROD=converge-prod

ENV_CHECK=true
```

> [!NOTE]
> O backend se conecta ao servidor de sockets em `localhost:3000` (valores padrão definidos em `SocketClient`).

---

### Configurar o Firebase no App Flutter

Os arquivos de configuração nativos do Firebase **não são versionados** (estão no `.gitignore` de `app/converge`). Antes de rodar o app, coloque-os nos caminhos abaixo:

- `app/converge/android/app/google-services.json`
- `app/converge/ios/Runner/GoogleService-Info.plist`

Eles podem ser baixados no console do Firebase (projeto `es-pi-iv-2026`) ou gerados com o FlutterFire CLI:

```bash
cd app/converge
flutterfire configure --project=es-pi-iv-2026
```

---

### Compilar e Instalar o Módulo `server`

> [!IMPORTANT]
> O módulo `converge-backend` depende da biblioteca gerada pelo `server`. Por isso, instale o artefato do servidor no repositório Maven local antes de iniciar a API.

Na pasta raiz do repositório:

```bash
cd app/server
mvn clean install
cd ../..
```

---

### Iniciar os Serviços (3 Terminais)

Abra três janelas de terminal a partir da raiz do projeto:

#### Terminal 1: Servidor de Sockets (`app/server`)

```bash
cd app/server
mvn compile
mvn exec:java -Dexec.mainClass="com.converge.socket.Main"
```

_O servidor iniciará escutando na porta **3000**._  
_(Para encerrar com segurança quando necessário, digite `stop` ou `exit` no console)._

#### Terminal 2: API REST Spring Boot (`app/converge-backend`)

```bash
cd app/converge-backend

# No Windows:
.\mvnw.cmd spring-boot:run

# No Linux / macOS:
./mvnw spring-boot:run
```

_A API estará disponível em:_

- **Endpoints REST:** `http://localhost:8080/api/v1`
- **Swagger UI (Documentação Interativa):** `http://localhost:8080/swagger-ui.html`

_Rotas protegidas exigem o cabeçalho `Authorization: Bearer <Firebase ID Token>`._

#### Terminal 3: Aplicativo Móvel Flutter (`app/converge`)

Certifique-se de que há um emulador ativo ou dispositivo físico conectado (`flutter devices`):

```bash
cd app/converge
flutter pub get
cp .env.example .env   # preencha GOOGLE_MAPS_API_KEY
flutter run --dart-define-from-file=.env
```

O `--dart-define-from-file=.env` é obrigatório: é por ele que o código Dart recebe a `GOOGLE_MAPS_API_KEY` usada nas chamadas à Routes, Places e Geocoding API. Sem ele, o mapa nativo carrega (a chave chega por `build.gradle.kts` / `Info.plist`), mas o cálculo de rotas falha.

O app aponta para `http://localhost:8080/api/v1` por padrão. No **emulador Android**, `localhost` é o próprio emulador; use o endereço do host:

```bash
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

---

# Instruções Detalhadas de Execução

### Opção via IDE (IntelliJ IDEA / VS Code / Eclipse)

Caso prefira rodar os serviços pela sua IDE:

1. **Abrir o projeto**:
   - Abra a pasta raiz ou a pasta `app` na sua IDE como um projeto Maven.
2. **Executar o Servidor de Sockets**:
   - Navegue até [`app/server/src/main/java/com/converge/socket/Main.java`](app/server/src/main/java/com/converge/socket/Main.java).
   - Clique em **Run** no método `main`.
3. **Executar a API Backend**:
   - Certifique-se de que o arquivo `.env` existe em `app/converge-backend/.env` e será levado em consideração na hora da execução.
   - Navegue até [`app/converge-backend/src/main/java/com/converge/api/ConvergeBackendApplication.java`](app/converge-backend/src/main/java/com/converge/api/ConvergeBackendApplication.java).
   - Clique em **Run**.
4. **Executar o App Flutter**:
   - Abra o arquivo [`app/converge/lib/main.dart`](app/converge/lib/main.dart).
   - Selecione o dispositivo de destino e pressione **F5** (ou clique em **Run Without Debugging**).

---

## 🧪 Como Rodar os Testes

### Testes dos Módulos Java (JUnit 5 & Spring Boot Test)

Para rodar os testes automatizados de todos os módulos Java a partir da pasta `app/`:

```bash
cd app

# No Windows:
.\mvnw.cmd test

# No Linux / macOS:
./mvnw test
```

Para rodar os testes apenas de um módulo específico:

```bash
# Testes do Socket Server:
cd app/server
mvn test

# Testes da API Backend (Windows: .\mvnw.cmd test):
cd app/converge-backend
./mvnw test
```

> [!NOTE]
> Os testes do backend usam o perfil `test` e precisam das mesmas variáveis do `.env` (conexão com o MongoDB Atlas). Instale o módulo `server` antes (`mvn clean install` em `app/server`).

### Testes do Aplicativo Flutter

```bash
cd app/converge
flutter test
```

---

## Estrutura de Pastas do Repositório

```text
.
├── README.md                          # Documentação principal e guia de inicialização
├── app/                               # Monorepo dos códigos da aplicação
│   ├── pom.xml                        # Agregador multi-módulo Maven (server + converge-backend)
│   ├── mvnw / mvnw.cmd                # Maven Wrapper
│   ├── README.md                      # Documentação técnica dos módulos em app/
│   │
│   ├── server/                        # Módulo Servidor TCP puro (Java 21)
│   │   ├── pom.xml
│   │   ├── README.md                  # Documentação dos handlers e protocolo do server
│   │   └── src/
│   │       ├── main/java/com/converge/socket/
│   │       │   ├── Main.java          # Ponto de entrada (porta padrão 3000)
│   │       │   ├── core/              # client, connection, exception, handler, protocol
│   │       │   └── handler/           # Handlers de negócio (ex.: math)
│   │       └── test/
│   │
│   ├── converge-backend/              # Módulo API REST Spring Boot (Java 21)
│   │   ├── pom.xml
│   │   ├── .env-example               # Modelo de variáveis de ambiente
│   │   ├── README.md                  # Documentação da API REST
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/com/converge/api/
│   │       │   │   ├── ConvergeBackendApplication.java
│   │       │   │   ├── modules/user/  # application, domain, presentation (hexagonal)
│   │       │   │   └── shared/        # application (UserUtils), infrastructure
│   │       │   │                      # (auth, mongo, socket, startup), presentation
│   │       │   └── resources/         # application.yaml + perfis dev / test / prod
│   │       └── test/
│   │
│   └── converge/                      # Módulo Aplicativo Móvel (Flutter)
│       ├── pubspec.yaml               # Dependências do app Flutter
│       ├── analysis_options.yaml
│       ├── firebase.json              # Configuração do FlutterFire
│       ├── android/ & ios/
│       ├── lib/
│       │   ├── main.dart              # Ponto de entrada do app Flutter
│       │   ├── app_widget.dart
│       │   ├── app_router.dart / app_routes.dart
│       │   ├── firebase_options.dart
│       │   ├── core/                  # config, network (Dio + interceptors),
│       │   │                          # services/auth, utils, widgets
│       │   └── features/
│       │       └── auth/              # data, presentation, state
│       └── test/
│
└── docs/                              # Documentos de especificação e ideação
    ├── arquitetura/                   # Documentações detalhadas de arquitetura
    │   ├── OVERVIEW.md
    │   ├── backend/                   # modular_hexagonal_monolith.md
    │   ├── frontend/                  # feature_modular_architecture.md
    │   └── java/                      # server_architecture.md
    ├── BD/                            # Modelagem e dicionário de dados
    ├── requisitos/                    # Requisitos Funcionais e Não-Funcionais
    ├── Telas/                         # Wireframes e protótipos de interface
    ├── assets/                        # Diagramas usados na documentação
    ├── MapaMental.pdf
    └── Relatório_PI_IV.pdf
```

---

## Resolução de Problemas Comuns (Troubleshooting)

### 1. `Cannot resolve symbol 'socket'` / Pacote do Server não encontrado no Backend

**Causa:** O backend precisa do artefato `com.converge:server:1.0-SNAPSHOT` instalado no repositório Maven local (`~/.m2/repository`).  
**Solução:**

```bash
cd app/server
mvn clean install
```

Em seguida, recarregue os projetos Maven na sua IDE (**Reload All Maven Projects**).

### 2. Erro de Conexão ou SSL no MongoDB Atlas (`Received fatal alert: internal_error` ou Timeout)

**Causa:** O endereço IP da sua máquina não está liberado no firewall do MongoDB Atlas.  
**Solução:**

1. Acesse o painel do [MongoDB Atlas](https://cloud.mongodb.com/).
2. Vá em **Security** ➔ **Network Access**.
3. Clique em **Add IP Address** ➔ selecione **Add Current IP Address** (ou adicione `0.0.0.0/0` temporariamente para ambiente de testes).
4. Verifique no arquivo `.env` se o usuário e senha foram preenchidos corretamente e sem caracteres corrompidos.

### 3. Porta Já em Uso (`Port 3000 / 8080 already in use`)

**Causa:** Outro processo está ocupando a porta do servidor de sockets (3000) ou da API (8080).  
**Solução:**

- Finalize o processo conflitante, ou:
- Altere a porta do servidor passando o argumento:
  ```bash
  mvn exec:java -Dexec.mainClass="com.converge.socket.Main" -Dexec.args="3005"
  ```
  E atualize a variável `SOCKET_SERVER_PORT=3005` no arquivo `.env` da API.

---

## 🎯 Funcionalidades Previstas para o MVP

- **Cadastro e Validação:** Cadastro com verificação de vínculo universitário institucional.
- **Oferta e Busca de Caronas:** Cadastro de trajetos com filtros por horários, dias da semana e rotas.
- **Matching Inteligente:** Algoritmo para compatibilização de motoristas e passageiros dentro do raio aceitável.
- **Comunicação Segura:** Chat integrado entre participantes da carona.
- **Rateio Justo:** Estimativa automática de custos com orientações para transferência via PIX.
- **Confiança e Segurança:** Avaliações mútuas, histórico de viagens e sistema de denúncia/bloqueio.
- **Sustentabilidade:** Painel com métricas de quilômetros compartilhados e emissões de CO₂ evitadas.
