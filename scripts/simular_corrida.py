#!/usr/bin/env python3
"""
Simula corridas inteiras contra o backend rodando, sem ninguém sair de casa.

Para cada cenário o script cria contas de teste no Firebase, cadastra as pessoas na API,
aprova a CNH e o vínculo acadêmico direto no MongoDB (o backend ainda não tem esse fluxo),
cadastra carro e trajetos e então percorre a viagem pela API, conferindo cada resposta.

Uso:
    python3 scripts/simular_corrida.py                      # todos os cenários
    python3 scripts/simular_corrida.py --cenario caminho-feliz
    python3 scripts/simular_corrida.py --listar
    python3 scripts/simular_corrida.py --manter-contas      # para abrir o app com as contas criadas

Precisa do servidor de sockets e do backend no ar, e do .env do backend preenchido.
"""

import argparse
import re
import sys
import time
from datetime import date, timedelta
from pathlib import Path

import verificar_usuario
from pymongo import MongoClient
from pymongo.errors import PyMongoError
from simulation.api_client import ApiClient
from simulation.scenarios import SCENARIOS
from simulation.simulation_failure import SimulationFailure
from simulation.simulation_world import PASSWORD, SimulationWorld

REPO_ROOT = Path(__file__).resolve().parent.parent
FIREBASE_OPTIONS = REPO_ROOT / "app" / "converge" / "lib" / "firebase_options.dart"
DEFAULT_API = "http://localhost:8080/api/v1"


def firebase_api_key(env_file: dict) -> str:
    """A mesma chave pública que o app usa, lida do firebase_options.dart (ou de FIREBASE_API_KEY)."""
    configured = verificar_usuario.setting("FIREBASE_API_KEY", env_file)
    if configured:
        return configured
    if FIREBASE_OPTIONS.is_file():
        match = re.search(r"apiKey:\s*'([^']+)'", FIREBASE_OPTIONS.read_text(encoding="utf-8"))
        if match:
            return match.group(1)
    sys.exit("Não achei a API key do Firebase: defina FIREBASE_API_KEY ou gere o firebase_options.dart.")


def clean_previous_run(api: ApiClient, connection: str, database: str, env_file: dict, run_id: str) -> int:
    try:
        with MongoClient(connection, serverSelectionTimeoutMS=10_000) as client:
            users = client[database][verificar_usuario.USERS_COLLECTION]
            world = SimulationWorld(api, firebase_api_key(env_file), users, run_id, date.today())
            deleted, leftovers = world.delete_accounts_of_run(run_id)
    except PyMongoError as error:
        print(f"Erro no MongoDB: {error}", file=sys.stderr)
        return 2

    print(f"Execução {run_id}: {len(deleted)} conta(s) apagada(s) do Firebase.")
    for email in leftovers:
        print(f"  não deu para apagar: {email}")
    return 1 if leftovers else 0


def parse_args():
    parser = argparse.ArgumentParser(description="Simula corridas completas contra o backend do Converge.")
    parser.add_argument("--cenario", action="append", choices=sorted(SCENARIOS),
                        help="Roda só este cenário (pode repetir). Padrão: todos.")
    parser.add_argument("--listar", action="store_true", help="Lista os cenários e sai")
    parser.add_argument("--api", default=DEFAULT_API, help=f"URL base da API (padrão: {DEFAULT_API})")
    parser.add_argument("--banco", help="Banco do Mongo (padrão: MONGODB_DATABASE_NAME ou converge-dev). "
                                        "Tem que ser o mesmo que o backend está usando.")
    parser.add_argument("--env", type=Path, default=verificar_usuario.DEFAULT_ENV_FILE,
                        help="Arquivo .env com a conexão do Mongo (padrão: app/converge-backend/.env)")
    parser.add_argument("--manter-contas", action="store_true",
                        help="Não apaga as contas do Firebase no fim (para logar no app com elas)")
    parser.add_argument("--limpar", metavar="EXECUCAO",
                        help="Só apaga do Firebase as contas de uma execução anterior (o número que aparece "
                             "em 'Simulação <número>') e sai")
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    if args.listar:
        for name, (description, _) in SCENARIOS.items():
            print(f"{name:26} {description}")
        return 0

    env_file = verificar_usuario.read_env_file(args.env)
    connection = verificar_usuario.setting("MONGODB_CONNECTION_STRING", env_file)
    if not connection:
        print(f"MONGODB_CONNECTION_STRING não encontrada no ambiente nem em {args.env}.", file=sys.stderr)
        return 2

    database = args.banco or verificar_usuario.setting(
        "MONGODB_DATABASE_NAME", env_file, verificar_usuario.DEFAULT_DATABASE)
    if "prod" in database.lower():
        print(f"Recusado: a simulação não roda no banco '{database}'.", file=sys.stderr)
        return 2

    api = ApiClient(args.api)

    if args.limpar:
        return clean_previous_run(api, connection, database, env_file, args.limpar)

    try:
        health = api.get("/users/me")
    except SimulationFailure as error:
        print(f"A API não respondeu em {args.api}. O backend está no ar?\n  {error}", file=sys.stderr)
        return 2
    if health.status not in (401, 403):
        print(f"Aviso: GET /users/me sem token deveria dar 401/403 e deu {health.status}.")

    selected = args.cenario or list(SCENARIOS)
    run_id = time.strftime("%m%d%H%M%S")
    ride_date = date.today() + timedelta(days=1)
    print(f"Simulação {run_id} | API {args.api} | banco {database} | viagens em {ride_date.isoformat()}\n")

    results = []
    try:
        with MongoClient(connection, serverSelectionTimeoutMS=10_000) as client:
            users = client[database][verificar_usuario.USERS_COLLECTION]
            world = SimulationWorld(api, firebase_api_key(env_file), users, run_id, ride_date)

            try:
                for name in selected:
                    description, scenario = SCENARIOS[name]
                    print(f"▶ {name}: {description}")
                    try:
                        scenario(world)
                        print("  ✔ passou\n")
                        results.append((name, None))
                    except SimulationFailure as failure:
                        print(f"  ✘ FALHOU: {failure}\n")
                        results.append((name, str(failure)))
                    except Exception as error:  # noqa: BLE001 - um cenário quebrado não para os outros
                        print(f"  ✘ ERRO INESPERADO: {error!r}\n")
                        results.append((name, repr(error)))
            finally:
                if args.manter_contas:
                    print(f"Contas mantidas no Firebase (senha de todas: {PASSWORD}):")
                    for user in world.created_users:
                        print(f"  {user.nickname:10} {user.email}")
                    print()
                else:
                    leftovers = world.delete_firebase_accounts()
                    if leftovers:
                        print(f"Aviso: {len(leftovers)} conta(s) do Firebase não foram apagadas. "
                              f"Rode depois: python scripts/simular_corrida.py --limpar {run_id}\n")
    except PyMongoError as error:
        print(f"Erro no MongoDB: {error}", file=sys.stderr)
        return 2

    failed = [name for name, failure in results if failure]
    print(f"Resultado: {len(results) - len(failed)}/{len(results)} cenário(s) passaram.")
    for name in failed:
        print(f"  ✘ {name}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
