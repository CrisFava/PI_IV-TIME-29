# Arquitetura do Servidor Puro de Sockets (Java)

Especificação da arquitetura modular do servidor de sockets TCP em Java 21, localizado em `app/server/`.

---

## 🎯 Objetivos de Design

- **Pure Java Sockets**: Comunicação orientada a conexão via TCP com serialização nativa de objetos Java (`ObjectInputStream` / `ObjectOutputStream`), com alta performance e sem dependência de frameworks externos de rede.
- **Transparência de Conexão e Desconexão**: Tratamento robusto para conexões novas, desconexões graciosas solicitadas pelo cliente, quedas de conexão abruptas (EOF/SocketException) e desligamento administrativo ordenado (*graceful shutdown*).
- **Desacoplamento Completo da Lógica de Negócio**: A infraestrutura de rede e concorrência é fixa e estável. Os desenvolvedores implementam apenas classes de *handlers* independentes através da interface funcional `RequestHandler`.
- **Tratamento Padronizado de Erros**: Códigos padronizados em String através do enum `ServerErrorCode` (`DISCONNECTION_ERROR`, `CONNECTION_ERROR`, etc.) encapsulados em mensagens de erro (`ErrorResponse`).

---

## 🏗️ Estrutura de Pacotes

```text
com.converge
├── Main.java                          # Ponto de entrada, bootstrap e loop de comandos administrativos
├── core/
│   ├── SocketServer.java              # Coordenador de ciclo de vida (start, stop, graceful shutdown)
│   ├── client/
│   │   ├── ClientConnection.java      # Wrapper thread-safe de Socket, I/O e Semáforo
│   │   └── ClientRegistry.java        # Registro e gerenciamento sincronizado de conexões ativas
│   ├── connection/
│   │   ├── ConnectionAcceptor.java    # Thread de escuta de conexões no ServerSocket
│   │   └── ConnectionSupervisor.java  # Thread supervisora de ciclo de vida e despacho por cliente
│   ├── exception/
│   │   ├── ServerErrorCode.java       # Enum de códigos de erro padronizados (DISCONNECTION_ERROR, etc.)
│   │   └── ServerException.java       # Exceção customizada da camada de sockets
│   ├── handler/
│   │   ├── RequestHandler.java        # Interface funcional base para os handlers de negócio
│   │   └── HandlerRegistry.java       # Despachante central que mapeia Request -> Handler e intercepta erros
│   └── protocol/
│       ├── Message.java               # Contrato base serializável (Serializable, Cloneable)
│       ├── DisconnectRequest.java     # Mensagem enviada pelo cliente ao solicitar saída limpa
│       ├── ServerShutdownNotification.java # Mensagem enviada pelo servidor antes de fechar conexões
│       └── ErrorResponse.java         # Resposta estruturada de erro contendo errorCode e errorMessage
└── handler/                           # Pacote de domínio para os handlers desenvolvidos pela equipe
    └── {modulo}/                      # Ex: math, auth, caronas, etc.
        ├── {Nome}Request.java         # Objeto de requisição (estende com.converge.core.protocol.Message)
        ├── {Nome}Response.java        # Objeto de resposta (estende com.converge.core.protocol.Message)
        └── {Nome}Handler.java         # Implementação de RequestHandler<{Nome}Request, {Nome}Response>
```

---

## 🔄 Fluxo de Comunicação e Ciclo de Vida

```text
+-------------------+        Socket.connect()         +----------------------+
|   Cliente Java    | ------------------------------> |  ConnectionAcceptor  |
+-------------------+                                 +----------------------+
          |                                                      |
          | Stream Header Handshake                              | spawns
          v                                                      v
+-------------------+      receives / sends           +----------------------+
| ClientConnection  | <=============================> | ConnectionSupervisor |
+-------------------+                                 +----------------------+
          |                                                      |
          | Request Message                                      | handlerRegistry.dispatch()
          |                                                      v
          |                                           +----------------------+
          |                                           |    RequestHandler    |
          |                                           |   execute(request)   |
          |                                           +----------------------+
          |                                                      |
          | <------------------ Response Message ----------------+
```

### 1. Conexão
1. O cliente estabelece a conexão TCP.
2. `ConnectionAcceptor` aceita o socket e dispara uma nova thread `ConnectionSupervisor`.
3. O supervisor inicializa os streams na ordem canônica (`ObjectOutputStream.flush()` seguido por `ObjectInputStream`).
4. Cria o `ClientConnection` e o adiciona ao `ClientRegistry`.

### 2. Processamento e Interceptação de Erros
1. O supervisor aguarda `clientConnection.receive()`.
2. Se a mensagem for `DisconnectRequest`, o cliente é removido do registro e os recursos são fechados.
3. Se for uma mensagem de negócio, é despachada via `HandlerRegistry.dispatch(request)`.
4. O `RequestHandler.execute(request)` processa a lógica de forma isolada.
5. Se o handler retornar um objeto derivado de `Message`, o supervisor o transmite de volta ao cliente.
6. **Tratamento de Exceções**: Se o handler lançar uma exceção:
   - `ServerException`: capturada em `HandlerRegistry.dispatch()` e convertida para `ErrorResponse(se.getErrorCode(), se.getMessage())`.
   - `Exception` geral: capturada e convertida para `ErrorResponse(ServerErrorCode.INTERNAL_SERVER_ERROR, e.getMessage())`.
   - Mensagem sem handler registrado: devolve `ErrorResponse(ServerErrorCode.INVALID_MESSAGE, ...)`.

### 3. Desconexão
- **Iniciada pelo Cliente**: Ao enviar `DisconnectRequest` ou ao fechar o socket, o bloco `finally` do supervisor garante a remoção do registro e liberação de descritores de arquivo.
- **Iniciada pelo Servidor**: O comando administrativo `stop` ou `desativar` no `Main` aciona `SocketServer.stop()`, que notifica todos os clientes via `ServerShutdownNotification` e fecha os sockets de forma coordenada.

---

## 🛠️ Guia para Desenvolvedores: Como Adicionar Novas Funcionalidades

1. Crie a classe do Request em `com.converge.handler.{modulo}` estendendo `com.converge.core.protocol.Message`.
2. Crie a classe do Response correspondente estendendo `com.converge.core.protocol.Message`.
3. Crie a classe do Handler implementando `com.converge.core.handler.RequestHandler<SeuRequest, SeuResponse>` e sobrescreva:
   ```java
   public SeuResponse execute(SeuRequest request) throws Exception {
       // Lógica de negócio pura
       return new SeuResponse(...);
   }
   ```
4. No `Main.java` (ou inicializador do servidor), registre o handler:
   ```java
   server.registerHandler(SeuRequest.class, new SeuHandler());
   ```
