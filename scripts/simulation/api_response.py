# Autor: Cristian Eduardo Fava - RA 25000636

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Optional


@dataclass(frozen=True)
class ApiResponse:
    """Resposta HTTP já com o envelope ApplicationResponse<T> do backend aberto."""

    status: int
    body: Optional[dict]

    @property
    def ok(self) -> bool:
        """Indica se a resposta HTTP teve status de sucesso e o envelope success não é false."""
        return 200 <= self.status < 300 and (self.body or {}).get("success", True) is not False

    @property
    def data(self) -> Any:
        """Extrai o payload principal contido no campo 'data' do envelope ApplicationResponse."""
        return (self.body or {}).get("data")

    @property
    def error_code(self) -> Optional[str]:
        """Obtém o código do erro (code) contido no envelope de falha, se presente."""
        return ((self.body or {}).get("error") or {}).get("code")

    @property
    def error_message(self) -> str:
        """Obtém a mensagem descritiva de erro retornada pelo backend."""
        return ((self.body or {}).get("error") or {}).get("message") or ""

    def describe(self) -> str:
        """Retorna uma representação textual resumida do status e do erro (se houver)."""
        if self.ok:
            return f"HTTP {self.status}"
        return f"HTTP {self.status} {self.error_code or ''} {self.error_message}".strip()
