# 🚀 Converge - Sistema Integrado

Repositório contendo o ecossistema do **Converge**, composto por:

- **`server`**: Servidor de Sockets TCP puro em Java 21 com serialização de objetos e handlers desacoplados.
- **`converge-backend`**: API REST em Spring Boot (Java 21) conectada ao MongoDB Atlas e ao servidor de sockets.
- **`converge`**: Aplicativo móvel construído em Flutter.

---

## 📋 Pré-requisitos

- **Java JDK 21** instalado e configurado no `PATH` (ou `JAVA_HOME`).
- **Maven 3.9+** (ou utilize o wrapper incluído `mvnw` / `mvnw.cmd`).
- **Flutter SDK 3.13+** e **Dart** (para executar o app móvel).
- **Docker & Docker Compose** _(opcional, recomendado para subir tudo junto)_.

---

## ⚙️ Configuração de Ambiente (.env)

Antes de rodar a API, certifique-se de que o arquivo `converge-backend/.env` existe e contém as variáveis necessárias (você pode se basear no `converge-backend/.env-example`):

```properties
MONGODB_USERNAME=seu_usuario
MONGODB_PASSWORD=sua_senha
MONGODB_CONNECTION_STRING=mongodb+srv://...

MONGODB_CLUSTER_NAME=converge-cluster
MONGODB_DATABASE_NAME_DEV=converge-dev
MONGODB_DATABASE_NAME_TESTS=converge-tests
MONGODB_DATABASE_NAME_PROD=converge-prod

ENV_CHECK=true

SOCKET_SERVER_HOST=localhost
SOCKET_SERVER_PORT=3000
```

---

## Como Rodar

Como o projeto é um **multi-módulo Maven**, você pode compilar tudo de uma vez a partir da raiz:

### 1. Compilar os módulos a partir da raiz

No terminal na pasta `/app`:

```powershell
mvn clean compile
# Ou usando o wrapper:
.\mvnw.cmd clean compile
```

---

### 2. Como Rodar o Servidor de Sockets (`server`)

O servidor de sockets deve ser iniciado primeiro para que possa receber requisições dos clientes.

#### Via IDE (IntelliJ IDEA / VS Code / Eclipse):

- Abra o arquivo [`server/src/main/java/com/converge/socket/Main.java`](server/src/main/java/com/converge/socket/Main.java) e clique em **Run** (Executar).

#### Via Linha de Comando:

```powershell
cd server
mvn compile
mvn exec:java -Dexec.mainClass="com.converge.socket.Main"
```

_(Opcional: você pode passar a porta desejada como argumento, ex: `-Dexec.args="3000"`)_

> **Comandos úteis no console do server:**
> Digite `stop`, `exit` ou `desativar` no terminal do servidor para fazer o encerramento gracioso (_graceful shutdown_).

---

### 3. Como Rodar a API Backend (`converge-backend`)

Com o servidor de sockets rodando (ou com a porta configurada no `.env`):

#### Via IDE:

- Abra o arquivo [`converge-backend/src/main/java/com/converge/api/ConvergeBackendApplication.java`](converge-backend/src/main/java/com/converge/api/ConvergeBackendApplication.java) e clique em **Run**.

#### Via Linha de Comando:

```powershell
cd converge-backend
.\mvnw.cmd spring-boot:run
# No Linux/Mac: ./mvnw spring-boot:run
```

A API estará disponível em:

- **Endpoints HTTP:** `http://localhost:8080`
- **Documentação Swagger / OpenAPI:** `http://localhost:8080/swagger-ui.html`

## 🧪 Como Rodar os Testes

### Testes dos Módulos Java (Backend e Server)

Para executar os testes de todos os módulos de uma vez:

```powershell
# Na pasta /app:
mvn test
# Ou usando o wrapper:
.\mvnw.cmd test
```

---

## ❓ Resolução de Problemas Comuns

### 1. "Cannot resolve symbol 'socket' / Pacote com.converge.socket não encontrado no backend"

Se você acabou de clonar o projeto em uma nova máquina e a IDE não reconhecer o pacote do server:

1. Abra um terminal na pasta `server`:
   ```powershell
   cd server
   mvn clean install
   ```
2. Na sua IDE, clique no botão de **Reload / Sync Maven Projects**.
