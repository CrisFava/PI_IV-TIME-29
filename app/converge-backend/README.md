# 🌐 Converge Backend (API)

API REST construída em **Spring Boot (Java 21)** conectada ao MongoDB Atlas e ao servidor de sockets.

---

## ⚙️ Configuração (.env)

Crie ou edite o arquivo `.env` na pasta `converge-backend`:

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

## ▶️ Como Rodar a API

### 1. Via IDE (IntelliJ IDEA / VS Code / Eclipse)
* Abra a classe [`com.converge.api.ConvergeBackendApplication`](src/main/java/com/converge/api/ConvergeBackendApplication.java) e clique em **Run**.

### 2. Via Linha de Comando (Maven Wrapper)
```powershell
# No Windows:
.\mvnw.cmd spring-boot:run

# No Linux / macOS:
./mvnw spring-boot:run
```

### 3. Acessos
* **API Endpoints:** `http://localhost:8080`
* **Swagger UI / Documentação OpenAPI:** `http://localhost:8080/swagger-ui.html`

---

## 🧪 Como Rodar os Testes
```powershell
.\mvnw.cmd test
```
