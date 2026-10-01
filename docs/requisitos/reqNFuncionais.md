# Requisitos Não Funcionais e Restrições Tecnológicas — Converge

## Requisitos Não Funcionais (RNF)

Os requisitos não funcionais descrevem como o sistema deve se comportar — qualidade, segurança, desempenho e organização.

| ID | Requisito |
|---|---|
| **RNF01** | O sistema deve proteger informações pessoais e acadêmicas dos usuários contra acesso não autorizado, garantindo que cada usuário acesse apenas seus próprios dados privados. |
| **RNF02** | O sistema deve impedir que um usuário consulte ou altere informações privadas de outro usuário sem a devida autorização. |
| **RNF03** | As credenciais dos usuários não devem ser armazenadas como texto puro; tokens e dados sensíveis devem ser gerenciados exclusivamente pelo provedor de autenticação (Firebase Auth). |
| **RNF04** | O sistema deve possuir mecanismos de autenticação (Firebase Auth como provedor de identidade, verificando tokens via `FirebaseAuthService`) e autorização (Spring Security) para restringir funcionalidades e dados conforme o perfil do usuário. |
| **RNF05** | O sistema deve validar os dados recebidos via API antes de processá-los ou armazená-los, utilizando Bean Validation (`jakarta.validation`) nos DTOs de entrada do backend. |
| **RNF06** | O sistema deve preservar a integridade dos dados armazenados no MongoDB. Referências entre módulos distintos devem ser feitas exclusivamente por ID simples (`String`), sem uso de `@DBRef`. |
| **RNF07** | O backend deve disponibilizar uma API REST documentada via OpenAPI (Swagger), gerada automaticamente pelo SpringDoc OpenAPI. |
| **RNF08** | A comunicação entre o aplicativo e o backend deve utilizar HTTP/HTTPS com troca de dados em formato JSON. Respostas de erro devem seguir o padrão RFC 7807 (Problem Details), tratadas no `GlobalExceptionHandler` do backend e no `ErrorInterceptor` do cliente Flutter. |
| **RNF09** | O sistema deve apresentar mensagens de erro compreensíveis ao usuário quando uma operação não puder ser concluída, utilizando as propriedades `title` e `detail` do padrão RFC 7807. |
| **RNF10** | O backend deve registrar erros e eventos relevantes em logs estruturados para permitir diagnóstico e manutenção da aplicação. |
| **RNF11** | As principais regras de negócio do backend (casos de uso) devem possuir testes unitários automatizados com JUnit. |
| **RNF12** | O módulo de matching deve possuir testes unitários que cubram cenários de compatibilidade e incompatibilidade de trajetos. |
| **RNF13** | O módulo de cálculo e rateio de custos deve possuir testes unitários que validem os valores calculados e o comportamento esperado. |
| **RNF14** | O projeto Flutter deve possuir testes automatizados das funcionalidades relevantes utilizando Flutter Test. |
| **RNF15** | O projeto deve permitir a execução dos testes automatizados de forma isolada durante o desenvolvimento e no processo de validação. |
| **RNF16** | O sistema deve ser capaz de processar múltiplas requisições simultâneas de usuários sem degradar o funcionamento das funcionalidades principais. |
| **RNF17** | As operações de busca de trajetos e execução do matching devem apresentar tempo de resposta adequado para uso cotidiano pelos estudantes. |
| **RNF18** | O mecanismo de matching deve ser desenvolvido de forma a comportar crescimento na quantidade de usuários e trajetos cadastrados sem exigir reescrita. |
| **RNF19** | O sistema deve manter de forma consistente os dados necessários para geração dos indicadores de mobilidade e sustentabilidade. |
| **RNF20** | A confirmação de carona e a atualização de vagas devem ser atômicas, impedindo que duas solicitações simultâneas ocupem a última vaga. |
| **RNF21** | O backend deve tratar o cabeçalho `Idempotency-Key` enviado pelo cliente (RT11), evitando confirmações, cancelamentos ou registros de ausência duplicados em caso de repetição da requisição. |
| **RNF22** | Valores monetários devem ser calculados e armazenados com precisão decimal, em duas casas, com regra de arredondamento definida. A soma das partes do rateio deve ser igual ao custo total, com a diferença de centavos atribuída ao motorista. |
| **RNF23** | O sistema deve utilizar mecanismos que garantam a consistência dos valores de rateio durante entradas, cancelamentos e ausências de participantes. |
| **RNF24** | O sistema deve manter de forma consistente os registros de viagens, participantes, pagamentos, cancelamentos e ausências para permitir a geração correta dos históricos e indicadores. |
| **RNF25** | O backend deve ser organizado conforme a arquitetura de Monólito Modular Hexagonal definida no projeto, separando responsabilidades entre as camadas `presentation`, `application`, `domain` e `infrastructure` em cada módulo de negócio. |
| **RNF26** | O código-fonte deve seguir convenções de nomenclatura e estrutura definidas nos documentos de arquitetura do projeto (`modular_hexagonal_monolith.md` para o backend e `feature_modular_architecture.md` para o frontend), facilitando manutenção e onboarding. |
| **RNF27** | O sistema deve ser desenvolvido de forma modular, de modo que alterações em um módulo de negócio não exijam modificações nos demais módulos. |
| **RNF28** | Os provedores de serviços externos do backend (autenticação, armazenamento e geolocalização) devem ser desacoplados por interfaces e alternáveis via `application.yaml` usando `@ConditionalOnProperty`, sem impacto nas camadas de negócio. |
| **RNF29** | O backend deve suportar o disparo de notificações assíncronas (como e-mails de confirmação e cancelamento de carona) via `ApplicationEventPublisher` do Spring, com listeners no pacote `infrastructure/listener` do módulo destinatário. |
| **RNF30** | A interface do aplicativo deve ser consistente e compreensível, seguindo o Design System definido na arquitetura (Material 3), permitindo que os estudantes utilizem as funcionalidades do MVP sem treinamento. |
| **RNF31** | O aplicativo Flutter deve suportar internacionalização via `flutter_localizations`, permitindo adaptação do conteúdo textual a diferentes configurações de idioma. |
| **RNF32** | O sistema deve utilizar Git para controle de versão e gerenciamento do código-fonte, com repositório centralizado. |

---

## Restrições Tecnológicas (RT)

As restrições tecnológicas são decisões de projeto fixadas pela equipe — não são negociáveis no contexto do MVP.

| ID | Restrição |
|---|---|
| **RT01** | O backend deve ser desenvolvido em **Java 21**. |
| **RT02** | O backend deve utilizar **Spring Boot 4** (`spring-boot-starter-parent:4.1.1` como parent Maven). |
| **RT03** | O banco de dados principal da aplicação deve ser **MongoDB** (Atlas em produção; URI configurada via variável de ambiente `MONGODB_CONNECTION_STRING`). |
| **RT04** | O módulo de matching deve ser implementado em Java, integrado ao backend como um bounded context dentro do Monólito Modular, sem servidor separado. |
| **RT05** | O aplicativo cliente deve ser desenvolvido em **Flutter/Dart** (Dart SDK `^3.13.3`). |
| **RT06** | O projeto deve possuir testes unitários automatizados em todas as camadas críticas (casos de uso, matching e cálculo de custos no backend; funcionalidades relevantes no frontend). |
| **RT07** | O projeto deve utilizar **JUnit** no backend e **Flutter Test** no aplicativo como frameworks de teste. |
| **RT08** | A aplicação deve integrar **Google Maps API** para geocoding, cálculo de distância e visualização de rotas, via `GoogleMapsService` na camada `shared/infrastructure/maps`, ativado por `app.maps.provider=google`. Em ambiente de desenvolvimento, deve ser utilizada a implementação `MockMapsService` (`app.maps.provider=mock`). |
| **RT09** | A aplicação deve utilizar **Firebase Auth** como provedor de autenticação (`app.auth.provider=firebase`), **Firebase Storage** para arquivos (`app.storage.provider=firebase`) e **Firebase Realtime Database** para comunicação em tempo real. Implementações mock devem estar disponíveis para desenvolvimento local. |
| **RT10** | O backend deve suportar execução via **Docker**, com `compose.yaml` definindo os serviços necessários para o ambiente de desenvolvimento. |
| **RT11** | O cliente HTTP do Flutter deve utilizar **Dio**, com três interceptors obrigatórios: `AuthInterceptor` (injeta `Authorization: Bearer <token>`), `IdempotencyInterceptor` (injeta `Idempotency-Key` via UUID v4 em `POST`, `PUT` e `PATCH`) e `ErrorInterceptor` (converte respostas RFC 7807 em `ProblemDetailsException`). |
| **RT12** | O gerenciamento de dependências do backend deve utilizar **Maven** via Maven Wrapper (`mvnw`/`mvnw.cmd`), sem exigir instalação local do Maven. |
| **RT13** | O backend deve utilizar **Lombok** para redução de boilerplate em entidades, DTOs e casos de uso. |
| **RT14** | A documentação da API REST deve ser gerada automaticamente via **SpringDoc OpenAPI** (`springdoc-openapi-starter-webmvc-ui:3.1.0`). |
| **RT15** | Variáveis de ambiente sensíveis (credenciais MongoDB, Firebase, Google Maps) devem ser gerenciadas via arquivo `.env`, com `.env-example` versionado no repositório e `.env` listado no `.gitignore`. |
| **RT16** | O sistema deve utilizar **MongoDB** como banco de dados principal, com coleções organizadas por módulo de negócio, sendo cada módulo responsável exclusivamente por suas próprias coleções. |
| **RT17** | O sistema deve utilizar **Spring Data MongoDB** para armazenamento, consulta e recuperação dos dados de viagens, usuários, trajetos, avaliações e demais entidades. |
| **RT18** | O sistema deve utilizar **Firebase Storage** para armazenamento de arquivos (fotos de perfil e documentos de verificação universitária), ativado via `app.storage.provider=firebase` no `application.yaml`. |
| **RT19** | O sistema deve utilizar **Firebase Realtime Database** para as funcionalidades de chat entre participantes de carona, garantindo comunicação em tempo real. |
| **RT20** | O mecanismo de matching deve ser implementado como módulo integrado ao backend Spring Boot, respeitando a arquitetura de Monólito Modular definida no projeto. |