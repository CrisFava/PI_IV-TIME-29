#!/usr/bin/env python3
# Autor: Cristian Eduardo Fava - RA 25000636

"""
Marca a CNH e o vínculo acadêmico de TODOS os usuários do banco como VERIFIED.

É a versão em lote do verificar_usuario.py, para desenvolvimento e testes: em vez de aprovar uma
pessoa pelo e-mail, aprova todo mundo de uma vez. Documentos que já estão VERIFIED não são
tocados, então o verifiedAt original é mantido.

Uso:
    python3 scripts/verificar_todos_usuarios.py
    python3 scripts/verificar_todos_usuarios.py --somente estudante
    python3 scripts/verificar_todos_usuarios.py --banco converge-testes
    python3 scripts/verificar_todos_usuarios.py --simular        # só mostra, não grava
    python3 scripts/verificar_todos_usuarios.py --sem-confirmar  # não pergunta antes de gravar

A conexão e o banco vêm das mesmas variáveis do verificar_usuario.py (MONGODB_CONNECTION_STRING e
MONGODB_DATABASE_NAME), com o app/converge-backend/.env como alternativa.
"""

import argparse
import sys
from datetime import datetime, timezone
from pathlib import Path

import verificar_usuario
from verificar_usuario import VERIFIED

# O import do verificar_usuario acima já reabre o script no scripts/.venv quando falta o pymongo.
from pymongo import MongoClient, UpdateOne
from pymongo.errors import PyMongoError

# Parte do documento do usuário -> valor de --somente que a inclui.
PARTS = {"academicRecord": "estudante", "driverLicense": "cnh"}


def pending_parts(user: dict, only: str) -> list:
    """Devolve as partes (vínculo e/ou CNH) que existem e ainda não estão VERIFIED."""
    parts = []
    for field, option in PARTS.items():
        if only not in (None, option):
            continue
        document = user.get(field)
        if document and document.get("status") != VERIFIED:
            parts.append(field)
    return parts


def build_user_update(user: dict, only: str, now: datetime):
    """Reaproveita o build_update do script individual, mas só para as partes pendentes."""
    parts = pending_parts(user, only)
    if not parts:
        return {}, []

    # Com uma parte só, restringe o build_update a ela para não sobrescrever a outra.
    restrict = PARTS[parts[0]] if len(parts) == 1 else None
    return verificar_usuario.build_update(user, restrict, now)


def label(user: dict) -> str:
    """Retorna o nome e e-mail formatados do usuário para listagem em terminal."""
    return f"{user.get('name') or '(sem nome)'} <{user.get('email') or '-'}>"


def parse_args():
    """Analisa os argumentos de linha de comando para a verificação em lote."""
    parser = argparse.ArgumentParser(
        description="Marca a CNH e o vínculo acadêmico de todos os usuários como VERIFIED no MongoDB.")
    parser.add_argument("--somente", choices=["cnh", "estudante"],
                        help="Verifica só a CNH ou só o vínculo acadêmico (padrão: os dois)")
    parser.add_argument("--banco", help="Nome do banco (padrão: MONGODB_DATABASE_NAME ou "
                                        f"{verificar_usuario.DEFAULT_DATABASE})")
    parser.add_argument("--env", type=Path, default=verificar_usuario.DEFAULT_ENV_FILE,
                        help="Arquivo .env de onde ler a conexão (padrão: app/converge-backend/.env)")
    parser.add_argument("--simular", action="store_true",
                        help="Só mostra o que seria alterado, sem gravar nada")
    parser.add_argument("--sem-confirmar", action="store_true",
                        help="Grava sem pedir confirmação no terminal")
    parser.add_argument("--permitir-producao", action="store_true",
                        help="Necessário para alterar um banco cujo nome contém 'prod'")
    return parser.parse_args()


def confirm(total: int, database: str) -> bool:
    """Solicita confirmação interativa do usuário antes de realizar a atualização em lote."""
    try:
        answer = input(f"\nVerificar {total} usuário(s) no banco {database}? [s/N] ")
    except EOFError:
        return False
    return answer.strip().lower() in ("s", "sim", "y", "yes")


def main() -> int:
    """Ponto de entrada: localiza e atualiza em lote todos os usuários com documentos pendentes."""
    args = parse_args()
    env_file = verificar_usuario.read_env_file(args.env)

    connection = verificar_usuario.setting("MONGODB_CONNECTION_STRING", env_file)
    if not connection:
        print(f"MONGODB_CONNECTION_STRING não encontrada no ambiente nem em {args.env}.", file=sys.stderr)
        return 2

    database = args.banco or verificar_usuario.setting(
        "MONGODB_DATABASE_NAME", env_file, verificar_usuario.DEFAULT_DATABASE)
    if "prod" in database.lower() and not args.permitir_producao:
        print(f"Recusado: '{database}' parece ser o banco de produção. "
              "Use --permitir-producao se for isso mesmo.", file=sys.stderr)
        return 2

    try:
        with MongoClient(connection, serverSelectionTimeoutMS=10_000) as client:
            users = client[database][verificar_usuario.USERS_COLLECTION]
            now = datetime.now(timezone.utc)

            operations = []
            already_verified = 0
            without_documents = 0
            total = 0

            for user in users.find({}, {"name": 1, "email": 1, "academicRecord": 1, "driverLicense": 1}):
                total += 1
                changes, warnings = build_user_update(user, args.somente, now)
                if not changes:
                    if any(user.get(field) for field in PARTS):
                        already_verified += 1
                    else:
                        without_documents += 1
                    continue

                print(f"- {label(user)}")
                print(f"    antes: {verificar_usuario.describe(user)}")
                for warning in warnings:
                    print(f"    aviso: {warning}")
                operations.append(UpdateOne({"_id": user["_id"]}, {"$set": changes}))

            print(f"\nBanco {database}: {total} usuário(s), {len(operations)} para verificar, "
                  f"{already_verified} já verificado(s), {without_documents} sem documento para verificar.")

            if not operations:
                print("Nada para alterar.")
                return 1

            if args.simular:
                print("Simulação: nada foi gravado.")
                return 0

            if not args.sem_confirmar and not confirm(len(operations), database):
                print("Cancelado. Nada foi gravado.")
                return 1

            result = users.bulk_write(operations, ordered=False)
            print(f"Pronto: {result.modified_count} usuário(s) alterado(s).")
    except PyMongoError as error:
        print(f"Erro no MongoDB: {error}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main())
