from __future__ import annotations

import json
import uuid
from typing import Optional
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

from simulation.api_response import ApiResponse
from simulation.simulation_failure import SimulationFailure

_MUTATING_METHODS = {"POST", "PUT", "PATCH"}


class ApiClient:
    """Cliente HTTP mínimo (só biblioteca padrão) que imita o Dio do app: Bearer e Idempotency-Key."""

    def __init__(self, base_url: str, timeout_seconds: float = 30):
        self.base_url = base_url.rstrip("/")
        self.timeout_seconds = timeout_seconds

    def get(self, path: str, token: Optional[str] = None, params: Optional[dict] = None) -> ApiResponse:
        return self.call("GET", self._url(path, params), token)

    def post(self, path: str, token: Optional[str] = None, body: Optional[dict] = None) -> ApiResponse:
        return self.call("POST", self._url(path), token, body)

    def call(self, method: str, url: str, token: Optional[str] = None, body: Optional[dict] = None) -> ApiResponse:
        headers = {"Accept": "application/json"}
        payload = None

        if body is not None or method in _MUTATING_METHODS:
            headers["Content-Type"] = "application/json"
            payload = json.dumps(body if body is not None else {}).encode("utf-8")
        if token:
            headers["Authorization"] = f"Bearer {token}"
        if method in _MUTATING_METHODS:
            headers["Idempotency-Key"] = str(uuid.uuid4())

        request = Request(url, data=payload, headers=headers, method=method)
        try:
            with urlopen(request, timeout=self.timeout_seconds) as response:
                return ApiResponse(response.status, _json_or_none(response.read()))
        except HTTPError as error:
            return ApiResponse(error.code, _json_or_none(error.read()))
        except URLError as error:
            raise SimulationFailure(f"Sem resposta de {url}: {error.reason}") from error
        except OSError as error:  # conexão derrubada no meio, timeout de leitura etc.
            raise SimulationFailure(f"A conexão com {url} caiu: {error!r}") from error

    def _url(self, path: str, params: Optional[dict] = None) -> str:
        url = f"{self.base_url}/{path.lstrip('/')}"
        return f"{url}?{urlencode(params)}" if params else url


def _json_or_none(raw: bytes) -> Optional[dict]:
    if not raw:
        return None
    try:
        parsed = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError):
        return None
    return parsed if isinstance(parsed, dict) else {"data": parsed}
