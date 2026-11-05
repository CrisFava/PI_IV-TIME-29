# Instruções e Diretrizes para Agentes de IA — Converge Mobile

> **Este documento define as regras inegociáveis, padrões de engenharia de software e boas práticas de código para o desenvolvimento do aplicativo mobile Converge (Flutter/Dart). Qualquer agente atuando neste repositório DEVE ler e seguir estas diretrizes.**

---

## 1. Documentação de Arquitetura Obrigatória

Antes de planejar, criar ou modificar qualquer funcionalidade de frontend, o agente **DEVE consultar e seguir estritamente a documentação de arquitetura localizada em**:
📁 **`docs/frontend`** (ou `docs/arquitetura/frontend/feature_modular_architecture.md`)

Esta especificação define o padrão arquitetural **Feature-Driven (Feature-First) Modular**, desenhado em simetria direta com o backend Spring Boot do projeto.

---

## 2. Regra Mandatória de Cores: Uso Exclusivo de `AppColors`

> ⚠️ **REGRA INEGOCIÁVEL**: É expressamente proibido declarar cores hardcoded (como `Color(0xFF...)` ou cores genéricas de `Colors.*`) em arquivos de telas, widgets, componentes ou controllers.

### Onde estão as cores?
Todas as cores da aplicação residem em um único ponto central:
📄 **`app/converge/lib/core/config/styles.dart`** na classe abstrata **`AppColors`**.

### Diretrizes de Uso:
- **Sempre importe `styles.dart`**: `import '../../../../core/config/styles.dart';` (ou o caminho relativo apropriado).
- **Consuma exclusivamente tokens de `AppColors`**:
  - **Superfícies & Primárias**: `AppColors.primary`, `AppColors.primaryDark`, `AppColors.dockBackground`, `AppColors.carButtonResting`, `AppColors.carButtonDepressed`.
  - **Fundos & Cartões**: `AppColors.scaffoldBackground`, `AppColors.cardBackground`, `AppColors.cardSecondary`, `AppColors.neutralLight`, `AppColors.inputBackground`, `AppColors.buttonSecondary`, `AppColors.chipBackground`.
  - **Bordas & Linhas**: `AppColors.borderSubtle`, `AppColors.borderCardPressed`, `AppColors.borderMedium`, `AppColors.borderLight`, `AppColors.footerBorder`, `AppColors.chipBorder`, `AppColors.routeLine`, `AppColors.progressTrack`.
  - **Tipografia & Ícones**: `AppColors.textPrimary`, `AppColors.textDark`, `AppColors.textMedium`, `AppColors.textSecondary`, `AppColors.textBodySecondary`, `AppColors.textMuted`, `AppColors.textPlaceholder`, `AppColors.textLight`, `AppColors.iconMuted`.
  - **Feedback & Estados**:
    - **Erro**: `AppColors.error`, `AppColors.errorBackground`, `AppColors.errorBorder`, `AppColors.errorText`.
    - **Sucesso**: `AppColors.success`, `AppColors.successBackground`, `AppColors.successBorder`.
  - **Dock de Navegação**: `AppColors.dockItemActive`, `AppColors.dockItemInactive`, `AppColors.dockIndicatorDot`.
  - **Barreira**: `AppColors.barrier`.
- **Caso precise de uma nova cor semântica**: Adicione a constante correspondente dentro de `AppColors` em `lib/core/config/styles.dart` e então a utilize no componente. Nunca a instancie diretamente no widget.

#### Exemplo:
```dart
// ❌ INCORRETO (Proibido)
Container(
  color: const Color(0xFFFEF2F2),
  decoration: BoxDecoration(border: Border.all(color: const Color(0xFFFECACA))),
  child: const Text('Erro', style: TextStyle(color: Color(0xFFB91C1C))),
)

// ✅ CORRETO (Padrão Converge)
Container(
  color: AppColors.errorBackground,
  decoration: BoxDecoration(border: Border.all(color: AppColors.errorBorder)),
  child: const Text('Erro', style: TextStyle(color: AppColors.errorText)),
)
```

---

## 3. Diretrizes de Ícones e Assets

1. **Biblioteca de Ícones**:
   - Utilize estritamente a família nativa **`Icons.*_rounded`** do Flutter (ex.: `Icons.home_rounded`, `Icons.directions_car_rounded`, `Icons.person_rounded`, `Icons.search_rounded`, `Icons.notifications_rounded`).
2. **Proibição de Assets Não Autorizados**:
   - Não adicione SVGs, ilustrações, Lottie, Rive ou arquivos de imagem locais sem aprovação explícita.
3. **Preservação de `assets/icon.png`**:
   - O arquivo `assets/icon.png` é protegido e **JAMAIS** deve ser alterado, substituído ou removido.

---

## 4. Tratamento de Erros e Envelope de Rede

> ⚠️ **Atenção**: O tratamento de erros antigo baseado em **RFC 7807 (`ProblemDetailsException`) NÃO É MAIS UTILIZADO**. Não reintroduza referências a RFC 7807 ou `ProblemDetailsException`.

### Como o backend responde:
O backend responde todas as requisições no envelope **`ApplicationResponse<T>`**:
```json
{
  "success": true, // ou false em caso de erro
  "data": { ... }, // null em caso de erro
  "error": {
    "code": "EMAIL_ALREADY_EXISTS",
    "message": "Mensagem descritiva do erro",
    "fields": ["email"]
  },
  "timestamp": "2026-10-20T18:00:00Z"
}
```

### Classes Oficiais de Erro (`lib/core/network/`):
- **`ApiException`** (`lib/core/network/api_exception.dart`): Lançada automaticamente pelo `ErrorInterceptor` ou pela função `unwrapApiResponse`. Contém `code` (`ApiErrorCode`), `message`, `status` e `fields`.
- **`ApiErrorCode`** (`lib/core/network/api_error_code.dart`): Mapeia os códigos do backend (`badRequest`, `userNotFound`, `emailAlreadyExists`, `invalidCredentials`, `notAllowed`, `network`, `unknown`).
- **`AuthException`** (`lib/core/services/auth/auth_exception.dart`): Exceção originada pelo Firebase Auth / Provedor de Identidade.
- **Função `authErrorMessage(Object error)`**: Centralizada em `auth_action_controller.dart`, deve ser usada para extrair a mensagem amigável a ser exibida em banners ou feedbacks da interface.

---

## 5. Estrutura Modular Feature-First

Toda funcionalidade de negócio reside em `lib/features/{nomeFeature}/` e deve manter estritamente a separação em 3 camadas:

```text
lib/features/{feature}/
├── data/                       # Comunicação com a API e Modelagem
│   ├── models/                 # DTOs com fromJson e toJson
│   └── repository/             # Interfaces abstratas e implementações com Dio
│
├── state/                      # Orquestração de regras de negócio de tela
│   └── {feature}_controller    # Notifiers / Cubits / Controllers (Riverpod)
│
└── presentation/               # Interface pura (UI)
    ├── pages/                  # Telas completas roteáveis
    └── widgets/                # Widgets específicos e atômicos da feature
```

### Regras das Camadas:
- **`presentation/`**: Nunca executa chamadas diretas de HTTP ou lógica assíncrona pesada. Apenas consome o estado de um controller e despacha eventos de interação.
- **`state/`**: Não importa widgets ou elementos de UI do Flutter (exceto tipos imutáveis como `AsyncValue`).
- **`core/`**: Fornece infraestrutura global e nunca deve importar código de `features/`.

---

## 6. Boas Práticas Gerais de Código

1. **Null Safety e Tipagem Estrita**:
   - Não use `dynamic` a menos que estritamente indispensável para parsing bruto de JSON.
   - Prefira pattern matching com `switch` expressions para exaustividade de tipos.
2. **Imutabilidade e Construtores `const`**:
   - Sempre utilize `const` em construtores e widgets que não mudam dinamicamente.
   - Classes de modelo e estado devem ser imutáveis (`@immutable`).
3. **Resiliência e Micro-Interações**:
   - Forneça feedback tátil com `HapticFeedback.lightImpact()` em seleções leves e `HapticFeedback.mediumImpact()` em ações centrais/críticas.
   - Todos os botões e formulários devem desabilitar interação e exibir feedback de progresso durante operações assíncronas.
4. **Verificação de Qualidade**:
   - Antes de concluir qualquer tarefa ou alteração de código, execute sempre `flutter analyze` e garanta **0 erros e 0 warnings**.

---

## 7. Grafo de Conhecimento (graphify)

O repositório pode ser mapeado com o [graphify](https://github.com/safishamsi/graphify), que gera um grafo de conhecimento do código e da documentação em `graphify-out/`. Essa pasta é local e está no `.gitignore`. O escopo do grafo é definido em `.graphifyignore`.

### Se o graphify estiver disponível
Verifique com `which graphify` ou procure o arquivo `graphify-out/graph.json`.
1. **Consulte o grafo antes de explorar o código**: para entender a arquitetura, as dependências entre módulos ou o impacto de uma mudança, comece por `graphify-out/GRAPH_REPORT.md` e pelos comandos:
   - `graphify query "<pergunta>"`: visão geral de um tema.
   - `graphify path "A" "B"`: caminho entre dois conceitos ou classes.
   - `graphify explain "<Nó>"`: explicação de um nó e dos seus vizinhos.
2. **Atualize o grafo após toda alteração**: ao concluir qualquer mudança de código, rode `graphify update .`. O comando reextrai só os arquivos alterados e não usa LLM. Se a mudança removeu código e o comando recusar sobrescrever o grafo, use `graphify update . --force`.
3. **Mudanças em documentação** (`docs/`, `*.md`, PDFs) não são cobertas pelo `graphify update`. Nesses casos, rode `/graphify . --update` no Claude Code.
4. Opcional: `graphify hook install` instala um hook de post-commit que atualiza o grafo automaticamente a cada commit.

### Se o graphify não estiver instalado
O graphify não é obrigatório. Na primeira interação, recomende a instalação ao usuário, mas não bloqueie a tarefa por isso:
```bash
uv tool install graphifyy   # ou: pipx install graphifyy
graphify install            # instala a skill /graphify no Claude Code
```
Depois, gere o grafo inicial com `/graphify .`.

---

## 8. Autoria dos Arquivos de Código (Java e Dart)

Todo arquivo `.java` e `.dart` versionado deve ter, **na primeira linha**, um comentário de autoria seguido de uma linha em branco:

```dart
// Autor: Nome Completo - RA 00000000

package com.converge...;   // ou o primeiro import/declaração em Dart
```

### Integrantes e identidades de commit
Os nomes completos e RAs oficiais estão na tabela **Equipe do Projeto** do `README.md`, que é a fonte da verdade. Os nomes de commit abaixo pertencem a cada integrante:

| Integrante (nome no cabeçalho)  | RA       | Nomes / usuários usados nos commits                              |
| :------------------------------ | :------- | :--------------------------------------------------------------- |
| Allan Giovanni Matias Paes      | 25008211 | `Allan Matias`, `Matias`, `Allan Giovanni Matias Paes`, `Matiaszz` |
| Cristian Eduardo Fava           | 25000636 | `duckcoder1101`, `Cristian Eduardo Fava`, `CrisFava`             |
| Daniela Mikie Kikuchi Gonçalves | 25003068 | `Dani Mikie`                                                     |
| Gustavo Alves de Siqueira Costa | 25001650 | `gustavoADSC`, `GustavoADSC`, `Gustavo Alves`                    |
| Sara Fernandes Monteiro         | 25024107 | `Sara Monteiro`                                                  |

Se aparecer uma identidade de commit que não está na tabela, pergunte ao usuário a quem ela pertence antes de definir a autoria.

### Regra para definir o autor
1. **Base**: o autor é quem **criou o arquivo** no histórico da branch `develop`, ignorando commits de merge:
   `git log origin/develop --no-merges --follow --diff-filter=A --format='%an' -- <arquivo> | tail -1`
2. **Exceção (arquivo muito alterado)**: se o criador responde por **menos de 50%** das linhas atuais (`git blame -w HEAD -- <arquivo>`), o autor passa a ser quem tem mais linhas. Em caso de empate em 50%, o criador continua como autor.
3. **Arquivo novo** que ainda não existe na `develop`: o autor é o integrante dono do `git config user.name`/`user.email` local, ou seja, quem está fazendo o commit.
4. **Não altere uma autoria existente** só porque outra pessoa editou o arquivo. Reavalie apenas se o arquivo foi reescrito e passou a se encaixar na exceção da regra 2.
5. **Arquivos gerados por ferramenta** não recebem cabeçalho, porque a próxima geração apagaria o comentário. Exemplo: `app/converge/lib/firebase_options.dart`, gerado pelo FlutterFire CLI.

### Quando aplicar
- **Antes de cada commit**: confira os arquivos `.java` e `.dart` que estão sendo adicionados no commit e garanta que todos têm o cabeçalho de autoria correto.
- **Periodicamente** (por exemplo, ao iniciar uma tarefa ou antes de abrir um PR): procure arquivos sem autoria no repositório inteiro e defina a autoria pelas regras acima:
  ```bash
  git ls-files '*.java' '*.dart' | grep -v 'firebase_options.dart' \
    | while read -r f; do head -1 "$f" | grep -q '^// Autor:' || echo "$f"; done
  ```
- Depois de adicionar cabeçalhos em arquivos Dart, rode `flutter analyze` (0 erros e 0 warnings) e, se o graphify estiver disponível, `graphify update .`.
