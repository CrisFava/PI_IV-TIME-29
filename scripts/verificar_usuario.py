#!/usr/bin/env python3
"""
Marca a CNH e o vínculo acadêmico (documento de estudante) de um usuário como VERIFIED.

O backend ainda não tem um fluxo de aprovação: todo cadastro nasce com o vínculo acadêmico
PENDING e a CNH UNDER_REVIEW, e sem os dois VERIFIED ninguém oferece nem pede carona.
Este script faz a aprovação direto no MongoDB, para desenvolvimento e testes.

Uso:
    python3 scripts/verificar_usuario.py aluno@puccampinas.edu.br
    python3 scripts/verificar_usuario.py aluno@puccampinas.edu.br --somente cnh
    python3 scripts/verificar_usuario.py aluno@puccampinas.edu.br --banco converge-testes

A conexão vem de MONGODB_CONNECTION_STRING e o banco de MONGODB_DATABASE_NAME (padrão
converge-dev, o mesmo do perfil dev do backend). Se as variáveis não estiverem no ambiente,
o script lê app/converge-backend/.env.
"""

import argparse
import os
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

try:
    from pymongo import MongoClient
    from pymongo.errors import PyMongoError
except ImportError:
    sys.exit("Falta o pymongo. Instale com: python3 -m pip install -r scripts/requirements.txt")

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_ENV_FILE = REPO_ROOT / "app" / "converge-backend" / ".env"
DEFAULT_DATABASE = "converge-dev"
USERS_COLLECTION = "users"
VERIFIED = "VERIFIED"


def read_env_file(path: Path) -> dict:
    """Lê um .env simples (CHAVE=valor), ignorando comentários e aspas."""
    values = {}
    if not path.is_file():
        return values

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue

        key, value = line.split("=", 1)
        value = value.strip().rstrip("\\").strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        values[key.strip()] = value

    return values


def setting(name: str, env_file: dict, default=None):
    return os.environ.get(name) or env_file.get(name) or default


def find_user(users, email: str):
    # Aceita tanto o e-mail da conta quanto o institucional, sem diferenciar maiúsculas.
    exact = re.compile(f"^{re.escape(email.strip())}$", re.IGNORECASE)
    return users.find_one({"$or": [
        {"email": exact},
        {"academicRecord.institutionalEmail": exact},
    ]})


def build_update(user: dict, only: str, now: datetime):
    """Monta o $set e devolve também os avisos sobre o que não pôde ser verificado."""
    changes = {}
    warnings = []

    if only in (None, "estudante"):
        if user.get("academicRecord"):
            changes["academicRecord.status"] = VERIFIED
            changes["academicRecord.verifiedAt"] = now
            changes["academicRecord.reviewedAt"] = now
            changes["academicRecord.rejectionReason"] = None
        else:
            warnings.append("O usuário não tem vínculo acadêmico cadastrado.")

    if only in (None, "cnh"):
        license_doc = user.get("driverLicense")
        if license_doc:
            changes["driverLicense.status"] = VERIFIED
            changes["driverLicense.verifiedAt"] = now
            changes["driverLicense.rejectionReason"] = None

            expires_at = license_doc.get("expiresAt")
            if isinstance(expires_at, datetime):
                expires = expires_at if expires_at.tzinfo else expires_at.replace(tzinfo=timezone.utc)
                if expires.date() < now.date():
                    warnings.append(
                        f"A CNH venceu em {expires.date()}. Ela fica VERIFIED no banco, "
                        "mas o backend continua tratando como EXPIRED."
                    )
        else:
            warnings.append("O usuário ainda não enviou a CNH (POST /api/v1/users/me/driver-license).")

    return changes, warnings


def describe(user: dict) -> str:
    academic = (user.get("academicRecord") or {}).get("status", "-")
    license_status = (user.get("driverLicense") or {}).get("status", "-")
    return f"vínculo acadêmico: {academic} | CNH: {license_status}"


def parse_args():
    parser = argparse.ArgumentParser(
        description="Marca a CNH e o vínculo acadêmico de um usuário como VERIFIED no MongoDB.")
    parser.add_argument("email", help="E-mail da conta ou e-mail institucional do usuário")
    parser.add_argument("--somente", choices=["cnh", "estudante"],
                        help="Verifica só a CNH ou só o vínculo acadêmico (padrão: os dois)")
    parser.add_argument("--banco", help=f"Nome do banco (padrão: MONGODB_DATABASE_NAME ou {DEFAULT_DATABASE})")
    parser.add_argument("--env", type=Path, default=DEFAULT_ENV_FILE,
                        help="Arquivo .env de onde ler a conexão (padrão: app/converge-backend/.env)")
    parser.add_argument("--permitir-producao", action="store_true",
                        help="Necessário para alterar um banco cujo nome contém 'prod'")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    env_file = read_env_file(args.env)

    connection = setting("MONGODB_CONNECTION_STRING", env_file)
    if not connection:
        print(f"MONGODB_CONNECTION_STRING não encontrada no ambiente nem em {args.env}.", file=sys.stderr)
        return 2

    database = args.banco or setting("MONGODB_DATABASE_NAME", env_file, DEFAULT_DATABASE)
    if "prod" in database.lower() and not args.permitir_producao:
        print(f"Recusado: '{database}' parece ser o banco de produção. "
              "Use --permitir-producao se for isso mesmo.", file=sys.stderr)
        return 2

    try:
        with MongoClient(connection, serverSelectionTimeoutMS=10_000) as client:
            users = client[database][USERS_COLLECTION]
            user = find_user(users, args.email)
            if user is None:
                print(f"Nenhum usuário com o e-mail {args.email} no banco {database}.", file=sys.stderr)
                return 1

            print(f"Usuário: {user.get('name')} <{user.get('email')}> (id {user['_id']}, banco {database})")
            print(f"Antes:   {describe(user)}")

            changes, warnings = build_update(user, args.somente, datetime.now(timezone.utc))
            for warning in warnings:
                print(f"Aviso:   {warning}")

            if not changes:
                print("Nada para alterar.")
                return 1

            users.update_one({"_id": user["_id"]}, {"$set": changes})
            print(f"Depois:  {describe(users.find_one({'_id': user['_id']}))}")
    except PyMongoError as error:
        print(f"Erro no MongoDB: {error}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main())
