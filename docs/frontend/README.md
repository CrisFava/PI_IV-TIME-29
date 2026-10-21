# Documentação de Arquitetura do Frontend (Flutter)

Esta pasta centraliza a documentação de arquitetura e diretrizes de desenvolvimento para o aplicativo mobile **Converge** (Flutter).

## Documento Principal de Arquitetura

Consulte a especificação arquitetural completa em:
- [Feature-Driven & Modular Architecture (Flutter)](../arquitetura/frontend/feature_modular_architecture.md)

---

## Regras Fundamentais para Agentes e Desenvolvedores

1. **Uso Obrigatório do Arquivo Central de Cores**:
   - **NUNCA** utilize valores hexadecimais ou instanciações arbitrárias de `Color(...)` em páginas, widgets, estados ou temas.
   - **SEMPRE** importe e utilize a classe `AppColors` de `lib/core/config/styles.dart`.
   - Todas as superfícies, fundos, bordas, textos e estados de feedback (erro/sucesso) estão padronizados na paleta monocromática com tokens semânticos.

2. **Tratamento de Erros da Aplicação (Atual)**:
   - A comunicação com o backend Spring Boot utiliza o envelope `ApplicationResponse<T>` (`{ success: bool, data: T?, error: { code, message, fields } }`).
   - Falhas de rede ou respostas com `success: false` são convertidas em `ApiException` pelo `ErrorInterceptor` do Dio.
   - Os erros são categorizados pelo enum `ApiErrorCode` (`lib/core/network/api_error_code.dart`).
   - Erros do provedor de identidade/Firebase Auth utilizam `AuthException`.
   - A função `authErrorMessage(Object error)` faz o pattern matching e resolve mensagens amigáveis para a interface.
   - **Atenção:** O padrão antigo RFC 7807 (`ProblemDetailsException`) foi descontinuado e não deve ser utilizado.

3. **Diretrizes Visuais e Ícones**:
   - Utilize estritamente a família nativa `Icons.*_rounded`.
   - Proibido gerar ou adicionar SVGs, Lottie, Rive ou arquivos de assets locais não homologados.
   - O arquivo `assets/icon.png` é protegido e **NUNCA** deve ser alterado ou removido.

4. **Organização Modular Feature-First**:
   - Todo módulo de negócio em `lib/features/{feature}/` deve ser dividido estritamente em:
     - `data/` (models, DTOs e repository)
     - `state/` (controllers e gerenciamento de estado)
     - `presentation/` (`pages/` e `widgets/` locais)
   - Recursos compartilhados residem exclusivamente em `lib/core/`.
