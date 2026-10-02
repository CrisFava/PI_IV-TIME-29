# Arquitetura do Sistema

Este diretório centraliza a documentação arquitetural do projeto **Converge**.

## Especificações Detalhadas

- [Arquitetura Frontend - Feature-Driven Modular Architecture](./frontend/feature_modular_architecture.md)
- [Arquitetura Backend - Monolito Modular Hexagonal](./backend/modular_hexagonal_monolith.md)
- [Arquitetura Servidor Socket Java](./java/server_architecture.md)

---

## Estrutura Arquitetural

### Frontend (Flutter) — `app/converge/lib/`

```text
lib/
├── core/                                       # Recursos transversais compartilhados
│   ├── config/                                 # Configurações de ambiente (URLs, chaves de API)
│   ├── network/                                # Cliente HTTP e interceptors (Auth, Idempotency, RFC 7807)
│   ├── services/                               # Integrações externas e SDKs (Auth, Maps, Storage)
│   ├── theme/                                  # Cores, tipografia e temas globais
│   ├── utils/                                  # Formatadores e validadores genéricos
│   └── widgets/                                # Componentes visuais globais reutilizáveis
│
├── features/                                   # Módulos de negócio independentes
│   └── {nomeFeature}/                          # Funcionalidade encapsulada
│       ├── data/                               # Camada de comunicação e dados
│       │   ├── models/                         # DTOs e modelos serializáveis
│       │   └── repository/                     # Contratos e implementações de requisições
│       │
│       ├── state/                              # Gerenciamento de estado e regras de tela
│       │   └── {feature}_controller            # Controller e estados da tela
│       │
│       └── presentation/                       # Interface de usuário (UI)
│           ├── pages/                          # Telas e rotas da funcionalidade
│           └── widgets/                        # Componentes visuais exclusivos da feature
│
├── app_widget.dart                             # Configuração do MaterialApp, temas e rotas
└── main.dart                                   # Ponto de entrada (Bootstrap e injeção de dependências)
```

---

### Backend (Spring Boot) — `app/converge-backend/src/main/java/com/converge/api/`

```text
com/converge/api/
├── {Application}.java                          # Inicializador da aplicação
│
├── modules/                                    # Domínios de negócio (Bounded Contexts)
│   └── {nomeModulo}/                           # Funcionalidade encapsulada
│       ├── application/                        # Casos de uso e orquestração de regras
│       │   └── {modulo}UseCase                 # Classes de caso de uso
│       │
│       ├── domain/                             # Núcleo de domínio
│       │   ├── enums/                          # Enums de negócio
│       │   ├── model/                          # Entidades persistidas
│       │   └── repository/                     # Interfaces de repositório
│       │
│       ├── infrastructure/                     # Adaptadores e integrações do módulo
│       │   ├── client/                         # Clientes externos específicos do módulo
│       │   └── listener/                       # Ouvintes de eventos do módulo
│       │
│       └── presentation/                       # Porta de entrada HTTP
│           ├── controller/                     # Controladores REST
│           └── dtos/                           # DTOs de entrada e saída
│
└── shared/                                     # Recursos transversais compartilhados
    ├── config/                                 # Configurações globais
    ├── exception/                              # Tratamento global de exceções (RFC 7807)
    └── infrastructure/                         # Provedores de serviços externos
```

---

### Servidor de Sockets (Java Puro) — `app/server/src/main/java/com/converge/`

```text
com/converge/
├── Main.java                                   # Ponto de entrada e comando de shutdown do servidor
├── core/
│   ├── SocketServer.java                       # Gerenciador de ciclo de vida do servidor
│   ├── client/                                 # Conexão e registro de clientes
│   │   ├── ClientConnection.java               # Abstração de socket, I/O e Semáforo
│   │   └── ClientRegistry.java                 # Gerenciamento thread-safe dos clientes ativos
│   ├── connection/                             # Escuta e supervisão de conexões
│   │   ├── ConnectionAcceptor.java             # Thread que aceita sockets TCP
│   │   └── ConnectionSupervisor.java           # Thread que supervisiona cada cliente
│   ├── exception/
│   │   ├── ServerErrorCode.java                # Enum de códigos de erro padronizados (DISCONNECTION_ERROR, etc.)
│   │   └── ServerException.java                # Exceção customizada da camada de sockets
│   ├── handler/                                # Contrato e despacho de lógica de negócio
│   │   ├── RequestHandler.java                 # Interface funcional: execute(request)
│   │   └── HandlerRegistry.java                # Roteador de Request para Handler
│   └── protocol/                               # Mensagens de protocolo base (Message, ErrorResponse, etc.)
│       ├── Message.java
│       ├── DisconnectRequest.java
│       ├── ServerShutdownNotification.java
│       └── ErrorResponse.java
|                              # Tratamento e enum de erros (ServerErrorCode)
└── handler/                                    # Módulos e regras de negócio da aplicação
    └── {modulo}/                               # Handlers independentes por domínio
```
