# Pure Java Socket Server

Arquitetura modular, legível e desacoplada para servidor puro de sockets TCP em Java 21, inspirada no modelo de sockets com objetos serializados (`continhas`), porém organizada em classes de handlers independentes e pronta para conexões e encerramentos graciosos.

---

## 🏗️ Estrutura da Arquitetura

```text
com.converge
├── Main.java                               # Ponto de entrada, inicialização e comando do console ("stop", "desativar")
├── core/
│   ├── SocketServer.java                   # Coordena o ciclo de vida do servidor (start, stop, graceful shutdown)
│   ├── client/
│   │   ├── ClientConnection.java           # Wrapper thread-safe de Socket, I/O e Semáforo (equivalente a Parceiro)
│   │   └── ClientRegistry.java             # Gerenciador thread-safe de clientes ativos (equivalente a lista 'usuarios')
│   ├── connection/
│   │   ├── ConnectionAcceptor.java         # Thread que escuta o ServerSocket (equivalente a AceitadoraDeConexao)
│   │   └── ConnectionSupervisor.java       # Thread por cliente que supervisiona o socket e despacha para handlers
│   ├── exception/
│   │   ├── ServerErrorCode.java            # Enum padronizado de erros com códigos em String ("DISCONNECTION_ERROR", etc.)
│   │   └── ServerException.java            # Exceção customizada contendo código e mensagem
│   ├── handler/
│   │   ├── RequestHandler.java             # Interface padrão: R execute(T request)
│   │   └── HandlerRegistry.java            # Roteador central que mapeia Request.class -> RequestHandler e intercepta erros
│   └── protocol/
│       ├── Message.java                    # Classe base serializável (equivalente a Comunicado)
│       ├── DisconnectRequest.java          # Requisição de desconexão enviada pelo cliente (equivalente a PedidoParaSair)
│       ├── ServerShutdownNotification.java # Notificação de encerramento enviada pelo servidor (equivalente a ComunicadoDeDesligamento)
│       └── ErrorResponse.java              # Resposta de erro padrão com errorCode (String) e errorMessage
└── handler/                                # Pacote com as implementações de negócio dos desenvolvedores
    └── math/
        ├── MathOperationRequest.java       # Exemplo de request de negócio (+, -, *, /)
        ├── MathOperationResponse.java      # Exemplo de response com o resultado
        └── MathOperationHandler.java       # Handler com método execute(MathOperationRequest)
```

---

## 🚀 Como Implementar uma Nova Lógica de Negócio (Handler)

Para adicionar uma nova funcionalidade, o desenvolvedor não precisa alterar o código de infraestrutura de sockets ou gerenciamento de conexões. Basta:

### 1. Criar o Request e Response (estendendo `Message`)
```java
package com.converge.handler.ride;

import com.converge.core.protocol.Message;

public class CreateRideRequest extends Message {
    private final String driver;
    private final String destination;
    // Construtor e getters...
}

public class CreateRideResponse extends Message {
    private final long rideId;
    // Construtor e getters...
}
```

### 2. Criar a classe do Handler implementando `RequestHandler` com `execute(request)`
```java
package com.converge.handler.ride;

import com.converge.core.handler.RequestHandler;

public class CreateRideHandler implements RequestHandler<CreateRideRequest, CreateRideResponse> {

    @Override
    public CreateRideResponse execute(CreateRideRequest request) throws Exception {
        // Lógica de negócio isolada
        long id = database.saveRide(request.getDriver(), request.getDestination());
        return new CreateRideResponse(id);
    }
}
```

### 3. Registrar o Handler no Servidor
```java
SocketServer server = new SocketServer(3000);

// Registra os handlers de negócio
server.registerHandler(CreateRideRequest.class, new CreateRideHandler());
server.registerHandler(MathOperationRequest.class, new MathOperationHandler());

server.start();
```

Se o handler retornar um objeto derivado de `Message`, o supervisor envia a resposta automaticamente de volta ao cliente. Se disparar uma exceção, o servidor intercepta no `HandlerRegistry` e devolve um [`ErrorResponse`](file:///E:/PIs/PI_IV_ES_TIME_29/app/server/src/main/java/com/converge/core/protocol/ErrorResponse.java) padronizado.

---

## ⚠️ Padrão de Erros (`ServerErrorCode`)

O enum [`ServerErrorCode`](file:///E:/PIs/PI_IV_ES_TIME_29/app/server/src/main/java/com/converge/core/exception/ServerErrorCode.java) padroniza os erros tanto interna quanto externamente:
- `DISCONNECTION_ERROR`
- `CONNECTION_ERROR`
- `TRANSMISSION_ERROR`
- `RECEPTION_ERROR`
- `CLIENT_DISCONNECTED`
- `INVALID_MESSAGE`
- `STREAM_INITIALIZATION_ERROR`
- `INTERNAL_SERVER_ERROR`
- `PORT_IN_USE`
- `SERVER_SHUTDOWN`

As respostas de erro enviadas ao cliente são instâncias de [`ErrorResponse`](file:///E:/PIs/PI_IV_ES_TIME_29/app/server/src/main/java/com/converge/core/protocol/ErrorResponse.java), permitindo obter o código em string via `getErrorCode()` (ex: `"DISCONNECTION_ERROR"`).

---

## 🔌 Conexão e Encerramento de Conexões

1. **Conexão**: O cliente conecta ao socket. O [`ConnectionAcceptor`](file:///E:/PIs/PI_IV_ES_TIME_29/app/server/src/main/java/com/converge/core/connection/ConnectionAcceptor.java) aceita e instancia a thread [`ConnectionSupervisor`](file:///E:/PIs/PI_IV_ES_TIME_29/app/server/src/main/java/com/converge/core/connection/ConnectionSupervisor.java), que registra o cliente no [`ClientRegistry`](file:///E:/PIs/PI_IV_ES_TIME_29/app/server/src/main/java/com/converge/core/client/ClientRegistry.java).
2. **Desconexão Graciosa pelo Cliente**: O cliente envia [`DisconnectRequest`](file:///E:/PIs/PI_IV_ES_TIME_29/app/server/src/main/java/com/converge/core/protocol/DisconnectRequest.java). O supervisor remove o cliente do registro e fecha o socket sem lançar erro de queda.
3. **Desconexão por Queda / Fechamento de Socket**: Capturado pelo [`ClientConnection`](file:///E:/PIs/PI_IV_ES_TIME_29/app/server/src/main/java/com/converge/core/client/ClientConnection.java) e limpo pelo bloco `finally` do supervisor.
4. **Desativação pelo Servidor**: O administrador digita `stop`, `exit` ou `desativar` no console (ou chama `server.stop()`). O servidor envia [`ServerShutdownNotification`](file:///E:/PIs/PI_IV_ES_TIME_29/app/server/src/main/java/com/converge/core/protocol/ServerShutdownNotification.java) para todos os clientes ativos e encerra os sockets.

---

## 🧪 Testes

Para executar os testes com JUnit 5:
```bash
mvn test
```
