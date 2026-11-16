# Relatório de Análise dos Testes do Backend (`converge-backend` e `server`)

> **Objetivo:** Avaliar a suíte de testes automatizados do backend para identificar testes obsoletos, redundantes, fragmentados ou que possam ser removidos/mesclados — **caso seja necessário enxugar**.

---

## 1. Resumo Executivo

**Não há necessidade de enxugar a suíte por motivos de performance.**  
Toda a suíte Java (API Spring Boot + Servidor de Sockets) é composta por testes unitários e de componente sem inicialização de contexto pesado (`@SpringBootTest`, `@WebMvcTest` ou `@DataMongoTest` não são utilizados), executando em **menos de 12 segundos** no total.

| Módulo | Arquivos em `src/test` | Suítes Surefire | Execuções (`@Test` / `@ParameterizedTest`) | Falhas / Erros / Ignorados | Tempo Total (Surefire) |
| :--- | :---: | :---: | :---: | :---: | :---: |
| `app/converge-backend` | 86 *(83 suítes + 3 auxiliares)* | 83 | 499 *(462 métodos)* | 0 / 0 / 0 | 11,39 s |
| `app/server` | 7 | 7 | 61 | 0 / 0 / 0 | 0,34 s |
| **Total** | **93** | **90** | **560** | **0 / 0 / 0** | **11,73 s** |

*(Medição obtida via `./mvnw test` em `app/`.)*

Apesar de o tempo de execução ser excelente, existem **oportunidades pontuais de limpeza e consolidação** para reduzir ruído, eliminar um teste placeholder morto, corrigir testes colocados no pacote errado e remover acoplamento acidental à porta `3000`.

---

## 2. O que Pode Ser Removido Diretamente

### 2.1. Teste placeholder sem asserção real (`app/server`)
- **Arquivo:** [`SocketServerTest.java`](../app/server/src/test/java/com/converge/SocketServerTest.java) (17 linhas, 1 teste)
- **Problema:** Contém apenas:
  ```java
  @Test
  public void teste() {
      assertTrue(true);
  }
  ```
- **Ação recomendada:** **Remover** o arquivo (ou substituir por testes reais de ciclo de vida do `SocketServer`, se desejado).

### 2.2. Método de teste redundante em `UserDriverLicenseTest`
- **Arquivo:** [`UserDriverLicenseTest.java`](../app/converge-backend/src/test/java/com/converge/api/modules/user/application/service/UserDriverLicenseTest.java)
- **Problema:** O método `shouldEditProfileWithoutResendingLicense()` (linhas 90–98) apenas executa `driver.updateProfile(...)` e verifica que não lança exceção. O teste imediatamente anterior, `shouldNotChangeVerifiedLicenseWhenUpdatingProfile()` (linhas 76–88), já executa a mesma chamada `driver.updateProfile(...)` e valida nome, preservação da CNH e elegibilidade.
- **Ação recomendada:** **Remover** `shouldEditProfileWithoutResendingLicense()`.

---

## 3. O que Pode Ser Mesclado / Reorganizado

### 3.1. Testes de domínio de `User` alocados em `application/service/`
Cinco arquivos estão no pacote `com.converge.api.modules.user.application.service`, mas **não testam nenhum Service ou UseCase** — apenas instanciam `new User(...)` diretamente e exercitam métodos da entidade de domínio:

| Arquivo atual | Testes | Linhas | O que realmente testa |
| :--- | :---: | :---: | :--- |
| [`UserDeactivateTest.java`](../app/converge-backend/src/test/java/com/converge/api/modules/user/application/service/UserDeactivateTest.java) | 1 | 35 | `user.deactivate()` |
| [`UserBecomeDriverTest.java`](../app/converge-backend/src/test/java/com/converge/api/modules/user/application/service/UserBecomeDriverTest.java) | 2 | 61 | `user.addRole(DRIVER)` |
| [`UserUpdateProfileTest.java`](../app/converge-backend/src/test/java/com/converge/api/modules/user/application/service/UserUpdateProfileTest.java) | 2 | 65 | `user.updateProfile(...)` |
| [`UserPixKeyTest.java`](../app/converge-backend/src/test/java/com/converge/api/modules/user/application/service/UserPixKeyTest.java) | 9 | 142 | Normalização/validação de PIX em `User` |
| [`UserDriverLicenseTest.java`](../app/converge-backend/src/test/java/com/converge/api/modules/user/application/service/UserDriverLicenseTest.java) | 7 *(6 úteis)* | 132 | `submitDriverLicense`, `updateProfile`, `hasValidDriverLicense` em `User` |

- **Sobreposição adicional:** `UserDriverLicenseTest` (regras de vencimento no dia vs. dia seguinte) sobrepõe parcialmente [`DriverLicenseTest.java`](../app/converge-backend/src/test/java/com/converge/api/modules/user/domain/model/user/subtypes/driver/DriverLicenseTest.java) (4 testes, 75 linhas).
- **Ação recomendada:**
  1. Mover os testes de comportamento da entidade `User` para `modules/user/domain/model/user/` (mesmo pacote de `UserValidationTest`).
  2. Mesclar `UserDeactivateTest` (1 teste), `UserBecomeDriverTest` (2 testes) e `UserUpdateProfileTest` (2 testes) em uma única classe (ex.: `UserLifecycleTest` ou `UserProfileTest`, com 5 testes), reduzindo 3 arquivos pequenos para 1.

### 3.2. Testes do `UserMapper` divididos em dois arquivos
- **Arquivos:**
  - [`UserMapperDriverLicenseTest.java`](../app/converge-backend/src/test/java/com/converge/api/modules/user/application/mapper/UserMapperDriverLicenseTest.java) (3 testes, 90 linhas)
  - [`UserMapperTest.java`](../app/converge-backend/src/test/java/com/converge/api/modules/user/presentation/mapper/UserMapperTest.java) (2 testes, 101 linhas)
- **Problema:** Ambos testam a mesma classe (`UserMapper`), mas ficaram separados em pacotes diferentes (`application/mapper` vs. `presentation/mapper`).
- **Ação recomendada:** **Mesclar** os 3 testes de CNH no `UserMapperTest.java` (totalizando 5 testes em um único arquivo).

### 3.3. Acoplamento acidental à porta `3000` em testes unitários de validação
- **Arquivos:**
  - [`ValidatePlateUseCaseTest.java`](../app/converge-backend/src/test/java/com/converge/api/modules/user/application/service/ValidatePlateUseCaseTest.java) (3 testes, 44 linhas)
  - [`ValidateRenavanUseCaseTest.java`](../app/converge-backend/src/test/java/com/converge/api/modules/user/application/service/ValidateRenavanUseCaseTest.java) (3 testes, 44 linhas)
- **Problema observado na execução:** Durante o `mvn test`, ambos instanciam o UseCase real sem mockar o `SocketClient`, tentam conectar via TCP em `localhost:3000`, falham com `CONNECTION_ERROR` e imprimem `WARN` no log antes de cair no fallback local. Além de poluir o log, se o `app/server` estiver rodando localmente durante o `mvn test`, o comportamento do teste muda (passa a depender da rede).
- **Sobreposição:** A lógica de validação de placa e RENAVAM já é amplamente testada em `app/server` por [`ValidatePlateHandlerTest.java`](../app/server/src/test/java/com/converge/socket/handler/vehicle/ValidatePlateHandlerTest.java) (4 testes), [`ValidateRenavanHandlerTest.java`](../app/server/src/test/java/com/converge/socket/handler/vehicle/ValidateRenavanHandlerTest.java) (6 testes) e [`ValidateVehicleDataHandlerTest.java`](../app/server/src/test/java/com/converge/socket/handler/vehicle/ValidateVehicleDataHandlerTest.java) (4 testes).
- **Ação recomendada:** Manter os testes dos UseCases apenas se o objetivo for garantir o **fallback local** — nesse caso, isolar o `SocketClient` com mock/stub para evitar tentativa real de conexão na porta `3000`.

---

## 4. Sobreposições Analisadas que Devem Ser Mantidas

Alguns conjuntos parecem sobrepostos à primeira vista, mas cumprem papéis distintos e não devem ser removidos:

| Conjunto | Quantidade | Por que manter separados |
| :--- | :---: | :--- |
| `RideTest` (45t) vs. `RideUseCasesTest` (24t) vs. `RideJourneyTest` (12t) | 81 testes | `RideTest` valida invariantes puras da entidade; `RideUseCasesTest` valida autorização, isolamento de falhas e rollback `@Transactional` por caso de uso; `RideJourneyTest` valida fluxos completos ponta-a-ponta com mocks (contraparte rápida do `scripts/simular_corrida.py`). Há apenas leve sobreposição em regras simples (ex.: não cancelar viagem em andamento). |
| `UserQueryUseCasesTest` (10t) vs. `RideEligibilityTest` (8t) | 18 testes | O primeiro testa a montagem de `UserEligibilityDTO` a partir de `User`; o segundo testa a tradução desse DTO em `ApplicationException` no módulo `ride`. |
| `MatchingRouteFactoryTest` (3t), `MatchingSearchRadiusTest` (4t), `NextRideDateTest` (4t), `MatchViewAssemblerTest` (2t) | 13 testes | Embora pequenos, cada arquivo testa uma classe utilitária/componente diferente e coesa do módulo `matching`. |
| `MongoCollectionFieldsMappingTest` (5t) e `UserImplicitPassengerRoleTest` (1t) | 6 testes | Protegem regressões reais de deserialização do `MappingMongoConverter` sem custo de subir MongoDB. |

---

## 5. Distribuição Atual por Módulo (`converge-backend`)

| Módulo | Arquivos de Teste | Métodos `@Test` / `@ParameterizedTest` | Destaques |
| :--- | :---: | :---: | :--- |
| `ride` | 22 | 162 | Maior suíte; foco forte em máquina de estados, rateio, pagamento e concorrência atômica |
| `user` | 19 | 96 | Maior concentração de arquivos pequenos/fragmentados e testes de domínio fora de `domain/` |
| `route` | 11 *(10 suítes + `FakeMapsGateway`)* | 67 | Boa separação entre domínio (`RouteTest`, `HaversineCalculatorTest`), `GoogleMapsGatewayTest` e `RouteRepositoryImplTest` |
| `indicator` | 11 *(10 suítes + `FakeIndicatorStore`)* | 57 | Único módulo com teste de controller (`IndicatorControllerTest`) |
| `matching` | 10 *(9 suítes + `MatchingTestRoutes`)* | 28 | Bem modularizado; `SocketMatchingGatewayTest` testa fallback/integração local com `MatchRoutesHandler` |
| `review` | 4 | 17 | Enxuto e direto ao ponto |
| `shared` | 5 | 16 | Cobre `IdempotencyFilter`, mapeamento Mongo, `SafeActionHandler` e `SocketClient` |
| `security` | 2 | 5 | Cobre `Block` e `BlockAccess` |

---

## 6. Lacunas Fora do Escopo de "Enxugamento" (Observação)

Para visão completa da cobertura:
- **Controllers REST:** Apenas [`IndicatorControllerTest.java`](../app/converge-backend/src/test/java/com/converge/api/modules/indicator/presentation/controller/IndicatorControllerTest.java) possui testes automatizados na camada `presentation/controller/`. Os demais módulos (`user`, `ride`, `route`, `matching`, `review`) dependem de testes manuais ou do `scripts/simular_corrida.py` para validar rotas HTTP, serialização JSON e segurança.

---

## 7. Plano de Ação Sugerido (Opcional)

Se a equipe quiser apenas **enxugar o que realmente é ruído** sem perder cobertura útil:

1. **Remover** [`SocketServerTest.java`](../app/server/src/test/java/com/converge/SocketServerTest.java) (teste vazio `assertTrue(true)`).
2. **Remover** `shouldEditProfileWithoutResendingLicense()` de [`UserDriverLicenseTest.java`](../app/converge-backend/src/test/java/com/converge/api/modules/user/application/service/UserDriverLicenseTest.java).
3. **Mesclar** `UserMapperDriverLicenseTest` dentro de `UserMapperTest`.
4. **Consolidar** `UserDeactivateTest`, `UserBecomeDriverTest` e `UserUpdateProfileTest` em uma única suíte dentro de `modules/user/domain/model/user/`, e mover `UserPixKeyTest` e `UserDriverLicenseTest` para `domain/model/user/`.
5. **Mockar `SocketClient`** em `ValidatePlateUseCaseTest` e `ValidateRenavanUseCaseTest` para eliminar chamadas reais à porta `3000`.

*Impacto estimado:* redução de **90 para ~85 suítes**, eliminação de **2 testes sem valor/redundantes** e **zero perda de cobertura funcional**.
