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
        return 200 <= self.status < 300 and (self.body or {}).get("success", True) is not False

    @property
    def data(self) -> Any:
        return (self.body or {}).get("data")

    @property
    def error_code(self) -> Optional[str]:
        return ((self.body or {}).get("error") or {}).get("code")

    @property
    def error_message(self) -> str:
        return ((self.body or {}).get("error") or {}).get("message") or ""

    def describe(self) -> str:
        if self.ok:
            return f"HTTP {self.status}"
        return f"HTTP {self.status} {self.error_code or ''} {self.error_message}".strip()
