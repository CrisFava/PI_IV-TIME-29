# Atividade de FTTVS

## Teste do Servidor JAVA PI IV

- **RA:** 25008211 **NOME:** Allan Giovanni Matias Paes
- **RA:** 25000636 **NOME:** Cristian Eduardo Fava
- **RA:** 25003068 **NOME:** Daniela Mikie Kikuchi Gonçalves
- **RA:** 25001650 **NOME:** Gustavo Alves de Siqueira Costa
- **RA:** 25024107 **NOME:** Sara Fernandes Monteiro

---

## Parte I - Explicar o propósito, arquitetura e estrutura das classes do servidor de sua aplicação

### a) Nome e descrição sucinta do que faz o projeto da Equipe

- **Nome do Projeto:** **Converge** (Módulo: **Servidor Java de Sockets TCP** — `app/server`)
- **Descrição Sucinta:**
  O **Converge** é uma plataforma que conecta estudantes universitários da PUC-Campinas com rotas e horários compatíveis para compartilhamento diário de caronas, substituindo grupos informais por um ecossistema com verificação acadêmica institucional, chat em tempo real, avaliações mútuas, controle de inadimplência e indicadores de sustentabilidade (CO₂ evitado).

  O submódulo **`app/server`** consiste em um **servidor TCP de alta performance desenvolvido em Java 21 puro**, sem dependência de frameworks externos de rede. Ele implementa comunicação orientada a conexão via sockets com serialização nativa de objetos Java (`ObjectInputStream` / `ObjectOutputStream`), concorrência multithreaded (thread-per-client com supervisores dedicados e semáforos) e arquitetura modular orientada a despachantes de requisições (`HandlerRegistry` e `RequestHandler`).

  **Propósito no ecossistema:** O servidor atua como o motor de processamento desacoplado do backend Spring Boot, sendo responsável por cálculos determinísticos e intensivos da aplicação:
  1. **Matching de Trajetos (`MatchRoutesHandler`):** Cálculo geoespacial multidimensional de compatibilidade entre ofertas e procuras, proximidade de polylines codificadas, tolerâncias de horários e sobreposição de rotas.
  2. **Cálculo de Custos e Rateio (`RideCostHandler`):** Estimativa do custo total da viagem (energia, consumo médio, pedágios, estacionamento), cálculo de desvios gerados por passageiros, fixação de tetos protetivos (`maxAmount`) e divisão justa de despesas exclusivamente entre passageiros efetivos com compensação de centavos ao motorista.
  3. **Indicadores de Sustentabilidade (`SustainabilityHandler`):** Cálculo de CO₂ evitado, carros individuais retirados do trânsito e árvores equivalentes.
  4. **Emissão de Cobrança PIX (`PixPayloadHandler`):** Montagem do payload EMVCo (BR Code) e cálculo de redundância cíclica CRC16 para QR Codes estáticos.
  5. **Validações de Documentos e Geodésia:** Validação algorítmica de CPF (`ValidateCpfHandler`), validação de dígitos verificadores de RENAVAM via módulo 11 (`ValidateRenavamHandler`), cálculo de distâncias esféricas de Haversine em lote (`DistanceHandler`) e referências de períodos ISO (`IndicatorReferenceHandler`).

---

### b) Diagrama que ilustre a arquitetura e a estrutura do servidor JAVA

> **Arquivo do Diagrama Mermaid:** [arquitetura_servidor.mermaid](file:///E:/PIs/PI_IV_ES_TIME_29/docs/testes/diagramas/arquitetura_servidor.mermaid)

#### Estrutura Arquitetural do Servidor TCP (`app/server`)

O diagrama de arquitetura modela a separação em camadas e o fluxo de dados entre clientes e o subsistema de sockets:

| Camada | Componentes Principais | Papel e Responsabilidades |
| :----- | :--------------------- | :------------------------ |
| **1. Clientes Externos** | `SocketClient`, Backend Spring Boot (`app/backend`) | Abrem conexões TCP via Sockets, estabelecem handshake de `ObjectInputStream`/`ObjectOutputStream` e transmitem instâncias serializadas de subclasses de `Message`. |
| **2. Escuta & Conexões** | `SocketServer`, `ConnectionAcceptor`, `ConnectionSupervisor`, `ClientConnection`, `ClientRegistry` | `SocketServer` gerencia o ciclo de vida (`start()`, `stop()`). O `ConnectionAcceptor` escuta novos clientes em loop assíncrono. Cada conexão aceita dispara um `ConnectionSupervisor` (thread dedicada). `ClientConnection` encapsula streams de I/O com mutex de concorrência e buffer `peek()`. `ClientRegistry` mantém controle thread-safe de clientes ativos. |
| **3. Roteamento & Protocolo** | `HandlerRegistry`, `RequestHandler<T, R>`, `Message` | `HandlerRegistry` mapeia dinamicamente tipos de mensagem (`Class<? extends Message>`) ao respectivo handler. Retorna `ErrorResponse` em falhas ou mensagens não cadastradas sem derrubar o socket. |
| **4. Handlers de Negócio** | `RideCostHandler`, `MatchRoutesHandler`, `PixPayloadHandler`, `SustainabilityHandler`, etc. | Unidades puras de computação de domínio que implementam `RequestHandler<T, R>`, isoladas de sockets, responsáveis pelas regras financeiras, geodésicas e operacionais de caronas. |

---

### c) Modelo UML de cada uma das classes e interfaces definidas - Nome, Atributos e Métodos

> **Arquivo do Diagrama Mermaid:** [classes_detalhadas.mermaid](file:///E:/PIs/PI_IV_ES_TIME_29/docs/testes/diagramas/classes_detalhadas.mermaid)

#### Especificação Detalhada das Classes, Atributos e Métodos

A tabela abaixo consolida as classes e interfaces do servidor socket TCP, especificando visibilidade, atributos e métodos conforme a modelagem UML:

| Pacote / Módulo | Classe / Interface | Atributos Principais | Métodos Principais |
| :-------------- | :----------------- | :------------------- | :----------------- |
| `protocol` | **`Message`** | `- long serialVersionUID$` | `+ clone(): Object` |
| `protocol` | **`DisconnectRequest`** | `- long serialVersionUID$`<br/>`- String reason` | `+ DisconnectRequest()`<br/>`+ DisconnectRequest(String reason)`<br/>`+ getReason(): String`<br/>`+ toString(): String` |
| `protocol` | **`ServerShutdownNotification`** | `- long serialVersionUID$`<br/>`- String reason` | `+ ServerShutdownNotification()`<br/>`+ ServerShutdownNotification(String reason)`<br/>`+ getReason(): String`<br/>`+ toString(): String` |
| `protocol` | **`ErrorResponse`** | `- long serialVersionUID$`<br/>`- String errorCode`<br/>`- String errorMessage` | `+ ErrorResponse(ServerErrorCode errorCode)`<br/>`+ ErrorResponse(ServerErrorCode errorCode, String errorMessage)`<br/>`+ ErrorResponse(String errorCode, String errorMessage)`<br/>`+ getErrorCode(): String`<br/>`+ getErrorMessage(): String`<br/>`+ toString(): String` |
| `exception` | **`ServerErrorCode`** *(enum)* | `- String code`<br/>`- String defaultMessage` | Constantes: `PORT_IN_USE`, `CONNECTION_ERROR`, `DISCONNECTION_ERROR`, `TRANSMISSION_ERROR`, `RECEPTION_ERROR`, `CLIENT_DISCONNECTED`, `INVALID_MESSAGE`, `STREAM_INITIALIZATION_ERROR`, `INTERNAL_SERVER_ERROR`, `SERVER_SHUTDOWN`<br/>`+ getCode(): String`<br/>`+ getDefaultMessage(): String`<br/>`+ toString(): String` |
| `exception` | **`ServerException`** | `- ServerErrorCode errorCode` | `+ ServerException(ServerErrorCode errorCode)`<br/>`+ ServerException(ServerErrorCode errorCode, String message)`<br/>`+ ServerException(ServerErrorCode errorCode, String message, Throwable cause)`<br/>`+ ServerException(ServerErrorCode errorCode, Throwable cause)`<br/>`+ getErrorCode(): ServerErrorCode`<br/>`+ getErrorCodeString(): String`<br/>`+ getMessage(): String` |
| `core` | **`RequestHandler<T, R>`** *(interface)* | *(nenhum)* | `+ execute(T request)*: R` |
| `core.handler` | **`HandlerRegistry`** | `- Map<Class<? extends Message>, RequestHandler<?, ?>> handlers` | `+ register(Class<T> requestClass, RequestHandler<T, ?> handler): void`<br/>`+ dispatch(Message request): Message` |
| `core.client` | **`ClientConnection`** | `- Socket socket`<br/>`- ObjectInputStream receiver`<br/>`- ObjectOutputStream transmitter`<br/>`- Message nextMessage`<br/>`- Semaphore mutex`<br/>`- volatile boolean closed` | `+ ClientConnection(Socket socket, ObjectInputStream in, ObjectOutputStream out)`<br/>`+ send(Message message): void`<br/>`+ peek(): Message`<br/>`+ receive(): Message`<br/>`+ close(): void`<br/>`+ isClosed(): boolean` |
| `core.client` | **`ClientRegistry`** | `- List<ClientConnection> clients` | `+ add(ClientConnection client): void`<br/>`+ remove(ClientConnection client): void`<br/>`+ getAll(): List<ClientConnection>`<br/>`+ getActiveCount(): int`<br/>`+ broadcast(Message message): void`<br/>`+ closeAll(Message notification): void` |
| `core.connection` | **`ConnectionAcceptor`** | `- ServerSocket serverSocket`<br/>`- ClientRegistry clientRegistry`<br/>`- HandlerRegistry handlerRegistry` | `+ ConnectionAcceptor(ServerSocket ss, ClientRegistry cr, HandlerRegistry hr)`<br/>`+ run(): void` |
| `core.connection` | **`ConnectionSupervisor`** | `- Socket socket`<br/>`- ClientRegistry clientRegistry`<br/>`- HandlerRegistry handlerRegistry`<br/>`- ClientConnection clientConnection` | `+ ConnectionSupervisor(Socket s, ClientRegistry cr, HandlerRegistry hr)`<br/>`+ run(): void` |
| `core` | **`SocketServer`** | `+ int DEFAULT_PORT$`<br/>`- int port`<br/>`- ClientRegistry clientRegistry`<br/>`- HandlerRegistry handlerRegistry`<br/>`- ServerSocket serverSocket`<br/>`- ConnectionAcceptor connectionAcceptor`<br/>`- boolean running` | `+ SocketServer()`<br/>`+ SocketServer(int port)`<br/>`+ registerHandler(Class<T> clazz, RequestHandler<T, ?> handler): SocketServer`<br/>`+ start(): void`<br/>`+ stop(): void`<br/>`+ isRunning(): boolean`<br/>`+ getPort(): int`<br/>`+ getClientRegistry(): ClientRegistry`<br/>`+ getHandlerRegistry(): HandlerRegistry` |
| `handler.ride` | **`RideCostHandler`** | *(nenhum atributo de estado mutável)* | `+ execute(RideCostRequest request): RideCostResponse`<br/>`- confirmationQuote(RideCostRequest req): RideCostResponse`<br/>`- costSharing(RideCostRequest req): RideCostResponse`<br/>`- detourKm(RideCostRequest req): double`<br/>`- routeCost(double dist, RideCostRequest req): BigDecimal`<br/>`- energyCost(double dist, RideCostRequest req): BigDecimal`<br/>`- validateRequest(RideCostRequest req): void` |
| `handler.ride` | **`RideCostRequest`** | `- long serialVersionUID$`<br/>`- Operation operation`<br/>`- double baseDistanceKm`<br/>`- Double legOriginToBoardingKm`<br/>`- Double legBoardingToDropOffKm`<br/>`- Double legDropOffToDestinationKm`<br/>`- double averageConsumption`<br/>`- BigDecimal energyPrice`<br/>`- BigDecimal toll`<br/>`- BigDecimal parking`<br/>`- List<PassengerCostInput> passengers` | `+ forConfirmationQuote(...)$: RideCostRequest`<br/>`+ forCostSharing(...)$: RideCostRequest`<br/>`+ getOperation(): Operation`<br/>`+ getBaseDistanceKm(): double`<br/>`+ getPassengers(): List<PassengerCostInput>` |
| `handler.ride` | **`RideCostResponse`** | `- long serialVersionUID$`<br/>`- double detourGeneratedKm`<br/>`- BigDecimal maxAmount`<br/>`- double baseDistanceKm`<br/>`- double totalDistanceKm`<br/>`- BigDecimal energyCost`<br/>`- BigDecimal totalCost`<br/>`- BigDecimal totalShared`<br/>`- Map<String, BigDecimal> sharesByPassenger` | `+ forConfirmationQuote(...)$: RideCostResponse`<br/>`+ forCostSharing(...)$: RideCostResponse`<br/>`+ getDetourGeneratedKm(): double`<br/>`+ getMaxAmount(): BigDecimal`<br/>`+ getTotalCost(): BigDecimal`<br/>`+ getSharesByPassenger(): Map<String, BigDecimal>` |

---

### d) Modelo de Classes UML, contendo as classes de (c), associações e heranças

> **Arquivo do Diagrama Mermaid:** [modelo_classes.mermaid](file:///E:/PIs/PI_IV_ES_TIME_29/docs/testes/diagramas/modelo_classes.mermaid)

#### Mapeamento de Relacionamentos (Herança, Realização, Agregação e Associação)

O diagrama de modelo de classes sintetiza os relacionamentos estruturais e comportamentais do sistema:

| Tipo de Relacionamento | Elementos Envolvidos | Descrição e Multiplicidade |
| :--------------------- | :------------------- | :------------------------- |
| **Herança (`extends`)** | `Message` $\leftarrow$ `DisconnectRequest`, `ServerShutdownNotification`, `ErrorResponse`, `RideCostRequest`, `RideCostResponse` | Hierarquia de protocolo polimórfico baseada em serialização Java (`java.io.Serializable`). |
| **Herança (`extends`)** | `Exception` $\leftarrow$ `ServerException` | Exceção verificada de domínio tipada com enum `ServerErrorCode`. |
| **Herança (`extends`)** | `Thread` $\leftarrow$ `ConnectionAcceptor`, `ConnectionSupervisor` | Modelo de concorrência multithreaded (1 thread de accept contínua + 1 thread dedicada de supervisor por socket cliente). |
| **Realização (`implements`)** | `RequestHandler<T, R>` $\leftarrow$ `RideCostHandler` | Contrato genérico de execução assíncrona de requisições sem acoplamento a rede. |
| **Composição Forte (`*--`)** | `SocketServer` $\rightarrow$ `ClientRegistry`, `HandlerRegistry`, `ConnectionAcceptor` | O ciclo de vida dos registros e da thread de accept é gerenciado e estritamente subordinado à instância do servidor. |
| **Composição Forte (`*--`)** | `ConnectionSupervisor` $\rightarrow$ `ClientConnection` | Cada supervisor instancia e encapsula exclusivamente a conexão de um cliente durante sua vida ativa. |
| **Agregação (`o--`)** | `ClientRegistry` $\rightarrow$ `ClientConnection` | Gerenciamento de coleção thread-safe de conexões ativas (`CopyOnWriteArrayList`). |
| **Agregação (`o--`)** | `HandlerRegistry` $\rightarrow$ `RequestHandler` | Tabela hash de despacho (`ConcurrentHashMap`) associando classe da mensagem ao handler de execução. |
| **Dependência / Uso (`..>`)** | `RideCostHandler` $\rightarrow$ `RideCostRequest`, `RideCostResponse`, `ServerException` | O handler consome requisições de carona, calcula desvios/rateios, produz respostas e dispara exceções de validação. |
| **Dependência / Uso (`..>`)** | `SocketServer` $\rightarrow$ `ServerShutdownNotification` | Notificação em broadcast emitida a todos os clientes conectados ao invocar `server.stop()`. |

---

## Parte II - Aplicar as técnicas de Teste OO e automatizar os testes:

### e) INTRACLASSE em JUNIT de pelo menos duas classes do Domínio da Aplicação do Servidor desenvolvido

Classes selecionadas para o teste intraclasse e justificativa das técnicas segundo as diretrizes de Teste OO:

1. **`RideCostHandler`**: Responsável pelas regras matemáticas de desvio, cálculo de consumo de combustível, proteção de teto (`maxAmount`) e rateio de despesas entre múltiplos passageiros.
   - **Técnica de Particionamento:** **CATEGORIAS DE MÉTODOS (Cálculos de Negócio)**. Conforme a diretriz da disciplina, como a classe não possui estado relevante, mas concentra métodos que realizam cálculos complexos e críticos (`confirmationQuote`, `costSharing`, `detourKm`, `routeCost`, `energyCost` e `validateRequest`), aplica-se o particionamento por categoria de métodos de cálculo, associando classes de equivalência e análise de valores limite aos parâmetros de cada categoria.
2. **`ClientConnection`**: Responsável pelo encapsulamento do canal de comunicação TCP, controle de concorrência com semáforo, buffering de leitura antecipada (`peek`) e ciclo de vida de fechamento.
   - **Técnica de Particionamento:** **ESTADOS (Ciclo de Vida da Conexão)**. Conforme a diretriz da disciplina, como a classe possui máquina de estados relevante (transições entre estado Aberto, Em Espera, Bufferizado/Peek, Consumido e Fechado/Idempotente), aplica-se o particionamento por transições de estado.

| Classe | Particionamento adotado | Sequências de ativação INTRACLASSE (1 sequência por linha) |
| :----- | :---------------------- | :--------------------------------------------------------- |
| **1) `RideCostHandler`** | **CATEGORIAS DE MÉTODOS (Cálculos de Desvio, Rateio e Validação)** | *Categoria 1 (Orçamento de Confirmação e Desvio):* `execute(confirmationQuote)` com trechos válidos $\rightarrow$ validação de `detourGeneratedKm` e `maxAmount` |
| | | *Categoria 1 (Valores Limite):* `execute(confirmationQuote)` sem trechos (`null`) $\rightarrow$ desvio zero e teto igual à rota original |
| | | *Categoria 1 (Compensação):* `execute(confirmationQuote)` com desvio negativo compensado $\rightarrow$ clamp em 0.0 |
| | | *Categoria 2 (Rateio Proporcional Simples):* `execute(costSharing)` com 1 passageiro $\rightarrow$ passageiro arca com 100% dos custos |
| | | *Categoria 2 (Rateio Múltiplo e Divisão):* `execute(costSharing)` com múltiplos passageiros e despesas adicionais $\rightarrow$ divisão igualitária truncada em centavos |
| | | *Categoria 2 (Travas de Teto Contratado):* `execute(costSharing)` onde a cota excede o teto contratado (`maxAmount`) $\rightarrow$ trava no teto e redistribuição do excedente para o motorista |
| | | *Categoria 2 (Caso Limite sem Passageiros):* `execute(costSharing)` sem passageiros $\rightarrow$ motorista arca com o total sem divisão por zero |
| | | *Categoria 3 (Validação Operacional):* `execute(confirmationQuote)` com consumo/preço zerados ou negativos $\rightarrow$ lançamento de `ServerException(INVALID_MESSAGE)` |
| **2) `ClientConnection`** | **ESTADOS (Ciclo de Vida e I/O)** | *Estado Inicial Aberto:* `ClientConnection(socket, in, out)` $\rightarrow$ `send(msg)` no estado Aberto $\rightarrow$ mensagem transmitida com sucesso |
| | | *Transição Aberto $\rightarrow$ Bufferizado:* `ClientConnection()` $\rightarrow$ `peek()` $\rightarrow$ cacheamento de mensagem em `nextMessage` sem consumo |
| | | *Estado Bufferizado Idempotente:* `ClientConnection()` $\rightarrow$ `peek()` sucessivo $\rightarrow$ retorno idêntico da mesma instância em cache |
| | | *Transição Bufferizado $\rightarrow$ Consumido:* `ClientConnection()` $\rightarrow$ `receive()` após `peek()` $\rightarrow$ entrega do objeto cacheado e invalidação do cache |
| | | *Transição para Final de Fluxo (EOF):* `ClientConnection()` $\rightarrow$ `receive()` no final de fluxo (EOF) $\rightarrow$ transição para desconexão com `CLIENT_DISCONNECTED` |
| | | *Transição Aberto $\rightarrow$ Fechado:* `ClientConnection()` $\rightarrow$ `close()` $\rightarrow$ fechamento atômico de socket e streams e `isClosed() == true` |
| | | *Estado Fechado Idempotente:* `ClientConnection()` $\rightarrow$ `close()` repetido $\rightarrow$ idempotência (nenhuma operação redundante executada) |
| | | *Operação Proibida no Estado Fechado:* `ClientConnection()` $\rightarrow$ `send(msg)` no estado Fechado $\rightarrow$ bloqueio com `ServerException(CLIENT_DISCONNECTED)` |

---

#### Tabela de Falhas, Anomalias e Melhorias (Intraclasse)

| Falha/Anomalias                      | Descrição e Localização do Defeito                                                                                                                                                          | Possível Causa / Correção Adotada                                                                                                                                                                               |
| :----------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Divisão por Zero sem passageiros** | Em `RideCostHandler.costSharing()`, se a lista de passageiros viesse vazia, ocorria risco de `ArithmeticException` ao dividir pelo total de passageiros.                                    | **Correção:** Implementado `BigDecimal.valueOf(Math.max(1, request.getPassengers().size()))`, atribuindo todo o custo à cota do motorista e evitando divisão por zero.                                          |
| **Fechamento redundante de Streams** | Em `ClientConnection.close()`, se o método fosse chamado mais de uma vez em condições de corrida, os streams de I/O eram fechados repetidamente gerando ruído de log e exceptions espúrias. | **Correção:** Introduzida flag atômica `volatile boolean closed`. Ao ser acionado uma vez, marca `closed = true` e retorna imediatamente nas chamadas subsequentes, tornando o método estritamente idempotente. |
| **Vazamento de Lock no `peek()`**    | Em `ClientConnection.peek()`, se a desserialização de stream lançasse exceção de runtime, o semáforo `mutex` corria risco de não ser liberado.                                              | **Correção:** Envolvido todo o processamento em bloco `try { mutex.acquireUninterruptibly(); ... } finally { mutex.release(); }`.                                                                               |

---

#### Código da Automação de Testes (Intraclasse)

As suítes reais de testes unitários intraclasse estão implementadas em:

- [RideCostHandlerTest.java](file:///E:/PIs/PI_IV_ES_TIME_29/app/server/src/test/java/com/converge/socket/handler/ride/RideCostHandlerTest.java)
- [ClientConnectionTest.java](file:///E:/PIs/PI_IV_ES_TIME_29/app/server/src/test/java/com/converge/socket/core/client/ClientConnectionTest.java)

Abaixo estão trechos representativos extraídos diretamente dessas classes:

```java
// Autor: Sara Fernandes Monteiro - RA 25024107
// Trecho de RideCostHandlerTest.java (Particionamento por Categorias de Métodos de Cálculo)

package com.converge.socket.handler.ride;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RideCostHandlerTest {

    private final RideCostHandler handler = new RideCostHandler();

    @Test
    @DisplayName("Deve calcular o desvio e o valor máximo ao confirmar o passageiro")
    void shouldComputeDetourAndMaxAmount() throws ServerException {
        RideCostRequest request = RideCostRequest.forConfirmationQuote(
                40.0, 8.0, 30.0, 6.0, 10.0,
                new BigDecimal("5.50"), new BigDecimal("7.00"), new BigDecimal("3.00")
        );

        RideCostResponse response = handler.execute(request);

        // 44 km ÷ 10 × 5,50 = 24,20 de energia; + 7,00 + 3,00 = 34,20, o custo inteiro com o desvio
        assertEquals(4.0, response.getDetourGeneratedKm(), 0.0001);
        assertEquals(new BigDecimal("34.20"), response.getMaxAmount());
    }

    @Test
    @DisplayName("O teto contratado pelo passageiro prevalece se a cota calculada for maior")
    void shouldCapPassengerShareAtMaxAmount() throws ServerException {
        RideCostRequest request = RideCostRequest.forCostSharing(
                100.0, 10.0, new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(
                        new RideCostRequest.PassengerCostInput("p1", 0.0, new BigDecimal("30.00")),
                        new RideCostRequest.PassengerCostInput("p2", 0.0, new BigDecimal("60.00"))
                )
        );

        RideCostResponse response = handler.execute(request);

        // Cada um pagaria 50,00, mas p1 trava em 30,00
        assertEquals(new BigDecimal("30.00"), response.getShares().get("p1"));
        assertEquals(new BigDecimal("50.00"), response.getShares().get("p2"));
    }

    @Test
    @DisplayName("Consumo médio zerado ou negativo deve ser rejeitado com INVALID_MESSAGE")
    void shouldRejectInvalidConsumption() {
        RideCostRequest request = RideCostRequest.forConfirmationQuote(
                40.0, 8.0, 30.0, 6.0, 0.0,
                new BigDecimal("5.50"), BigDecimal.ZERO, BigDecimal.ZERO
        );

        ServerException ex = assertThrows(ServerException.class, () -> handler.execute(request));
        assertEquals(ServerErrorCode.INVALID_MESSAGE, ex.getErrorCode());
    }
}
```

```java
// Autor: Allan Giovanni Matias Paes - RA 25008211
// Trecho de ClientConnectionTest.java (Transições de Estados: Aberto, Peek, Receive e Close)

package com.converge.socket.core.client;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.protocol.DisconnectRequest;
import com.converge.socket.core.protocol.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

class ClientConnectionTest {

    @Test
    @DisplayName("peek deve cachear a mensagem sem consumi-la e receive subsequente deve consumi-la")
    void shouldCacheMessageOnPeekAndConsumeOnReceive() throws Exception {
        DisconnectRequest first = new DisconnectRequest("msg-1");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(first);
            oos.flush();
        }

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        ObjectOutputStream out = new ObjectOutputStream(new ByteArrayOutputStream());

        ClientConnection conn = new ClientConnection(new Socket(), in, out);

        Message peeked1 = conn.peek();
        Message peeked2 = conn.peek();
        assertSame(first, peeked1);
        assertSame(peeked1, peeked2);

        Message received = conn.receive();
        assertSame(first, received);
    }

    @Test
    @DisplayName("close deve ser idempotente e fechar streams e socket")
    void shouldCloseResourcesAndBeIdempotent() throws Exception {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(new byte[0]));
        ObjectOutputStream out = new ObjectOutputStream(new ByteArrayOutputStream());

        ClientConnection conn = new ClientConnection(new Socket(), in, out);
        assertFalse(conn.isClosed());

        conn.close();
        assertTrue(conn.isClosed());

        // Segunda chamada de close não deve lançar erro nem reexecutar operações
        conn.close();
        assertTrue(conn.isClosed());
    }

    @Test
    @DisplayName("send em conexão fechada deve lançar ServerException com CLIENT_DISCONNECTED")
    void shouldThrowClientDisconnectedWhenSendingOnClosedConnection() throws Exception {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(new byte[0]));
        ObjectOutputStream out = new ObjectOutputStream(new ByteArrayOutputStream());

        ClientConnection conn = new ClientConnection(new Socket(), in, out);
        conn.close();

        ServerException ex = assertThrows(ServerException.class, () -> conn.send(new DisconnectRequest()));
        assertEquals(ServerErrorCode.CLIENT_DISCONNECTED, ex.getErrorCode());
    }
}
```

---

### f) INTERCLASSE em JUNIT

Simulação ponta a ponta da camada de serviço do servidor TCP Socket, exercitando a colaboração entre `SocketServer`, `ConnectionAcceptor`, `ConnectionSupervisor`, `ClientRegistry`, `HandlerRegistry` e o `RequestHandler`.

#### Cenário NORMAL

**Descrição do que faz:**
Um cliente TCP conecta ao `SocketServer` escutando em porta efêmera. O `ConnectionAcceptor` aceita o socket e dispara um `ConnectionSupervisor`, que realiza o handshake dos Object Streams, instancia a `ClientConnection` e a adiciona ao `ClientRegistry`. O cliente envia um `RideCostRequest` serializado para cálculo de custos. O supervisor lê a mensagem, aciona o `HandlerRegistry.dispatch()`, que roteia para o `RideCostHandler.execute()`. A resposta `RideCostResponse` é calculada e devolvida via stream de volta ao cliente. Em seguida, o cliente envia `DisconnectRequest` e o supervisor finaliza e limpa a conexão do registro graciosamente.

| Nome do Serviço Principal                                                                                              | Sequências INTERCLASSES para testar Cenário Solicitado                                                                                   |
| :--------------------------------------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------- |
| **Ciclo Completo de Atendimento de Requisição de Carona via TCP Sockets (`SocketServer.start()` & `RideCostHandler`)** | 1. `SocketServer(0).registerHandler(RideCostRequest.class, rideCostHandler).start()`                                                     |
|                                                                                                                        | 2. `Socket.connect(localhost, server.getPort())` $\rightarrow$ `ConnectionAcceptor.accept()`                                             |
|                                                                                                                        | 3. `ConnectionSupervisor.start()` $\rightarrow$ instanciação de `ObjectOutputStream` e `ObjectInputStream`                               |
|                                                                                                                        | 4. `ClientRegistry.add(clientConnection)`                                                                                                |
|                                                                                                                        | 5. Cliente envia `RideCostRequest.forCostSharing(...)`                                                                                   |
|                                                                                                                        | 6. `ConnectionSupervisor` $\rightarrow$ `ClientConnection.receive()` $\rightarrow$ `HandlerRegistry.dispatch(request)`                   |
|                                                                                                                        | 7. `HandlerRegistry` $\rightarrow$ `RideCostHandler.execute(request)` $\rightarrow$ calcula e retorna `RideCostResponse`                 |
|                                                                                                                        | 8. `ConnectionSupervisor` $\rightarrow$ `ClientConnection.send(response)` $\rightarrow$ escrita no socket                                |
|                                                                                                                        | 9. Cliente faz `ObjectInputStream.readObject()` e valida cotas calculadas                                                                |
|                                                                                                                        | 10. Cliente envia `DisconnectRequest()` $\rightarrow$ `ConnectionSupervisor` encerra e executa `ClientRegistry.remove(clientConnection)` |

---

#### Cenário Alternativo ou de Erro

**Descrição do que faz:**
O cliente TCP conecta com sucesso, mas envia uma mensagem não cadastrada no catálogo do servidor (ou uma requisição com parâmetros ilegais que dispara `ServerException`). O `ConnectionSupervisor` recebe o objeto e despacha para `HandlerRegistry.dispatch()`. O registro detecta a ausência de handler associado (ou captura a exceção de negócio) e constrói dinamicamente uma resposta `ErrorResponse` com o código `INVALID_MESSAGE` e a mensagem explicativa, transmitindo-a ao cliente sem derrubar a thread do servidor e mantendo o socket íntegro para operações seguintes.

| Nome do Serviço Principal                                                                            | Sequências INTERCLASSES para testar Cenário Solicitado                                                                           |
| :--------------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------- |
| **Interceptação e Tratamento de Mensagem Inválida ou Não Registrada (`HandlerRegistry.dispatch()`)** | 1. `SocketServer(0).start()` com handlers padrão registrados                                                                     |
|                                                                                                      | 2. `Socket.connect(localhost, server.getPort())` $\rightarrow$ Handshake de streams concluído                                    |
|                                                                                                      | 3. Cliente envia objeto `Message` não mapeado em `HandlerRegistry` (ou `null`)                                                   |
|                                                                                                      | 4. `ConnectionSupervisor` $\rightarrow$ `ClientConnection.receive()` $\rightarrow$ `HandlerRegistry.dispatch(unregisteredMsg)`   |
|                                                                                                      | 5. `HandlerRegistry` identifica ausência de handler $\rightarrow$ constrói `ErrorResponse(ServerErrorCode.INVALID_MESSAGE, ...)` |
|                                                                                                      | 6. `ConnectionSupervisor` $\rightarrow$ `ClientConnection.send(errorResponse)`                                                   |
|                                                                                                      | 7. Cliente lê `ErrorResponse`, valida o código `"INVALID_MESSAGE"`                                                               |
|                                                                                                      | 8. Servidor permanece ativo e responsivo para requisições seguintes                                                              |

---

#### Tabela de Falhas, Anomalias e Melhorias (Interclasse)

| Falha/Anomalias                          | Descrição e Localização do Defeito                                                                                                                                                                                   | Possível Causa / Correção Adotada                                                                                                                                                                                                                        |
| :--------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Condição de Corrida na Porta Efêmera** | Nos testes de integração do servidor, o método `findFreePort()` alocava e fechava a porta antes de passar ao `SocketServer`, gerando falhas intermitentes de `PORT_IN_USE` em ambientes com concorrência de sockets. | **Correção:** Alterado o `SocketServer` para aceitar porta `0` e expor `getPort()` dinamicamente com `serverSocket.getLocalPort()`. Os testes agora conectam na porta real vinculada atomicamente pelo sistema operacional, eliminando o race condition. |
| **Deadlock na Desconexão Abrupta**       | No teste de queda inesperada do cliente, se o socket fechasse antes de o supervisor concluir o registro, o teste podia congelar ou ler contagem inconsistente.                                                       | **Correção:** Implementado polling com timeout delimitado (`waitUntil`) aguardando a confirmação da presença do cliente no `ClientRegistry` antes de simular o desligamento forçado do socket cliente.                                                   |
| **Timeout de Leitura no Socket Cliente** | Em caso de falha silenciosa na inicialização dos streams do supervisor, os testes clientes podiam travar indefinidamente aguardando dados.                                                                           | **Correção:** Configurado `clientSocket.setSoTimeout(3000)` em todos os clientes de teste de integração antes de abrir os streams de I/O.                                                                                                                |

---

#### Código da Automação de Testes (Interclasse)

A suíte real de testes de integração interclasse está implementada em:

- [ServerInterClassIntegrationTest.java](file:///E:/PIs/PI_IV_ES_TIME_29/app/server/src/test/java/com/converge/socket/core/ServerInterClassIntegrationTest.java)

Abaixo está a implementação completa da classe:

```java
// Autor: Allan Giovanni Matias Paes - RA 25008211
// Teste de Integração Interclasse Cliente-Servidor TCP

package com.converge.socket.core;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.protocol.DisconnectRequest;
import com.converge.socket.core.protocol.ErrorResponse;
import com.converge.socket.core.protocol.Message;
import com.converge.socket.handler.ride.RideCostHandler;
import com.converge.socket.handler.ride.RideCostRequest;
import com.converge.socket.handler.ride.RideCostResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServerInterClassIntegrationTest {

    @Test
    @DisplayName("Cenário NORMAL: Fluxo completo TCP Socket cliente -> supervisor -> handler -> resposta")
    void testNormalExecutionFlow() throws Exception {
        SocketServer server = new SocketServer(0);
        server.registerHandler(RideCostRequest.class, new RideCostHandler());
        server.start();

        int port = server.getPort();

        try (Socket client = new Socket(InetAddress.getLoopbackAddress(), port)) {
            client.setSoTimeout(3000);
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());

            // 1. Envio de requisição de negócio válida
            RideCostRequest request = RideCostRequest.forCostSharing(
                    50.0, 10.0, new BigDecimal("6.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                    List.of(new RideCostRequest.PassengerCostInput("aluno_puc", 0.0, new BigDecimal("50.00")))
            );
            out.writeObject(request);
            out.flush();

            // 2. Recebimento da resposta processada pelo RideCostHandler
            Object responseObj = in.readObject();
            RideCostResponse response = assertInstanceOf(RideCostResponse.class, responseObj);
            assertEquals(new BigDecimal("30.00"), response.getTotalCost());
            assertEquals(new BigDecimal("30.00"), response.getSharesByPassenger().get("aluno_puc"));

            // 3. Desconexão graciosa
            out.writeObject(new DisconnectRequest("Operação concluída com sucesso"));
            out.flush();
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("Cenário ALTERNATIVO/ERRO: Envio de mensagem sem handler gera ErrorResponse sem derrubar o servidor")
    void testAlternativeErrorFlow() throws Exception {
        SocketServer server = new SocketServer(0);
        server.start();

        int port = server.getPort();

        try (Socket client = new Socket(InetAddress.getLoopbackAddress(), port)) {
            client.setSoTimeout(3000);
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());

            // Envia mensagem base Message (sem handler registrado em HandlerRegistry)
            out.writeObject(new Message());
            out.flush();

            Object responseObj = in.readObject();
            ErrorResponse error = assertInstanceOf(ErrorResponse.class, responseObj);

            assertEquals(ServerErrorCode.INVALID_MESSAGE.getCode(), error.getErrorCode());
            assertTrue(error.getErrorMessage().contains("No handler registered"));

            // Servidor permanece ativo
            assertTrue(server.isRunning());
        } finally {
            server.stop();
        }
    }
}
```

---

### g) Diagrama de sequência equivalente ao item (f), cenário NORMAL

> **Arquivo do Diagrama Mermaid:** [sequencia_cenario_normal.mermaid](file:///E:/PIs/PI_IV_ES_TIME_29/docs/testes/diagramas/sequencia_cenario_normal.mermaid)

#### Rastreamento Passo a Passo do Cenário Interclasse Normal

O fluxo do cenário de integração normal executado no teste [ServerInterClassIntegrationTest.java](file:///E:/PIs/PI_IV_ES_TIME_29/app/server/src/test/java/com/converge/socket/core/ServerInterClassIntegrationTest.java) organiza-se em 4 fases sequenciais:

| Passo | Origem $\rightarrow$ Destino | Ação / Mensagem Trocada | Descrição Técnica |
| :---- | :--------------------------- | :---------------------- | :---------------- |
| **Fase 1** | **Inicialização do Servidor TCP** | | |
| 1 | `SocketServer` $\rightarrow$ `ConnectionAcceptor` | `start()` | Inicializa `ServerSocket` vinculando à porta configurada (porta 0 efêmera nos testes para evitar condições de corrida) e inicia a thread `ConnectionAcceptor`. |
| **Fase 2** | **Conexão e Handshake** | | |
| 2 | `Cliente` $\rightarrow$ `ConnectionAcceptor` | `TCP Connect (SYN / ACK)` | Cliente abre socket TCP em `localhost:port`. `serverSocket.accept()` desbloqueia retornando o socket cliente conectado. |
| 3 | `ConnectionAcceptor` $\rightarrow$ `ConnectionSupervisor` | `new ConnectionSupervisor(...)` & `start()` | Instancia supervisor passando socket, `ClientRegistry` e `HandlerRegistry`, disparando a thread dedicada de atendimento. |
| 4 | `ConnectionSupervisor` $\rightarrow$ `ClientConnection` | `new ClientConnection(socket, in, out)` | Cria wrapper encapsulando os Object Streams com controle de concorrência e buffer `peek()`. |
| 5 | `ConnectionSupervisor` $\rightarrow$ `ClientRegistry` | `add(clientConnection)` | Adiciona a conexão à lista thread-safe de clientes ativos do servidor. |
| **Fase 3** | **Transmissão e Execução da Regra de Negócio** | | |
| 6 | `Cliente` $\rightarrow$ `ClientConnection` | `out.writeObject(RideCostRequest)` | Cliente serializa `RideCostRequest.forCostSharing(...)` com os dados da rota e passageiros. |
| 7 | `ClientConnection` $\rightarrow$ `ConnectionSupervisor` | `receive() -> RideCostRequest` | Supervisor consome o objeto desserializado do stream de entrada. |
| 8 | `ConnectionSupervisor` $\rightarrow$ `HandlerRegistry` | `dispatch(RideCostRequest)` | Supervisor despacha a requisição para o registro de handlers. |
| 9 | `HandlerRegistry` $\rightarrow$ `RideCostHandler` | `execute(RideCostRequest)` | Localiza o handler cadastrado para a classe e invoca a computação das regras de rateio. |
| 10 | `RideCostHandler` $\rightarrow$ `HandlerRegistry` | `return RideCostResponse` | Retorna resposta com `totalCost`, `detourGeneratedKm` e `sharesByPassenger` calculados. |
| 11 | `HandlerRegistry` $\rightarrow$ `ConnectionSupervisor` | `return RideCostResponse` | Devolve a resposta ao supervisor. |
| 12 | `ConnectionSupervisor` $\rightarrow$ `ClientConnection` | `send(RideCostResponse)` | Supervisor aciona envio na conexão do cliente. |
| 13 | `ClientConnection` $\rightarrow$ `Cliente` | `out.writeObject(RideCostResponse)` | Escreve e descarrega (`flush()`) o objeto de resposta no socket TCP. O cliente lê via `in.readObject()` e valida as cotas. |
| **Fase 4** | **Desconexão Graciosa** | | |
| 14 | `Cliente` $\rightarrow$ `ClientConnection` | `out.writeObject(DisconnectRequest)` | Cliente sinaliza término voluntário de sessão enviando `DisconnectRequest`. |
| 15 | `ClientConnection` $\rightarrow$ `ConnectionSupervisor` | `receive() -> DisconnectRequest` | Supervisor detecta mensagem de encerramento. |
| 16 | `ConnectionSupervisor` $\rightarrow$ `ClientRegistry` & `ClientConnection` | `remove(clientConnection)` & `close()` | Remove o cliente do registro de ativos, fecha streams/socket e encerra a thread sem erros. |
