# Autor: Allan Giovanni Matias Paes - RA 25008211

"""
Sobe o ambiente do backend em dois terminais novos, na ordem certa: primeiro o servidor de
sockets (app/server), depois o Spring (app/converge-backend) com as variáveis do .env.

Uso:
    python scripts/run_servers.py

Cada terminal novo roda este mesmo arquivo em modo interno (--executar socket|backend). É ele
que carrega o .env e chama o Maven dentro do terminal, então as variáveis chegam ao Spring em
qualquer sistema. Passar o ambiente pelo processo que abre o terminal não funciona no macOS
(o Terminal.app ignora o ambiente do osascript) nem sempre no Linux (gnome-terminal-server).
"""

import argparse
import os
import shlex
import shutil
import socket
import subprocess
import sys
import time
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
APP_DIR = REPO_ROOT / "app"
SERVER_DIR = APP_DIR / "server"
BACKEND_DIR = APP_DIR / "converge-backend"
SOCKET_PORT = 3000
SOCKET_WAIT_SECONDS = 180
IS_WINDOWS = sys.platform == "win32"


def load_env(env_path: Path) -> dict:
    """Carrega as variáveis de um arquivo .env em um dicionário."""
    env_vars = {}
    if not env_path.is_file():
        return env_vars

    with open(env_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if line.startswith("export "):
                line = line[len("export "):].strip()
            if "=" in line:
                key, val = line.split("=", 1)
                key = key.strip()
                val = val.strip()
                if len(val) >= 2 and (
                    (val.startswith('"') and val.endswith('"'))
                    or (val.startswith("'") and val.endswith("'"))
                ):
                    val = val[1:-1]
                env_vars[key] = val
    return env_vars


def backend_env_file() -> Path:
    env_file = BACKEND_DIR / ".env"
    return env_file if env_file.is_file() else APP_DIR / ".env"


def maven_command(module_dir: Path, goals: list) -> list:
    """Usa o wrapper do módulo (mvnw ou mvnw.cmd) e cai para o mvn instalado se não houver."""
    if IS_WINDOWS and (module_dir / "mvnw.cmd").exists():
        return [str(module_dir / "mvnw.cmd"), *goals]
    if not IS_WINDOWS and (module_dir / "mvnw").exists():
        return [str(module_dir / "mvnw"), *goals]
    return ["mvn", *goals]


SERVERS = {
    "socket": {
        "title": "Converge - Socket Server",
        "dir": SERVER_DIR,
        "goals": ["compile", "exec:java", "-Dexec.mainClass=com.converge.socket.Main"],
    },
    "backend": {
        "title": "Converge - Spring Backend",
        "dir": BACKEND_DIR,
        "goals": ["spring-boot:run"],
    },
}


def run_server(name: str) -> int:
    """Modo interno: roda dentro do terminal novo, carrega o .env (backend) e chama o Maven."""
    server = SERVERS[name]
    env = os.environ.copy()

    if name == "backend":
        env_file = backend_env_file()
        loaded = load_env(env_file)
        env.update(loaded)
        print(f"Arquivo .env: {env_file} ({len(loaded)} variáveis carregadas)")

    command = maven_command(server["dir"], server["goals"])
    print(f"Diretório: {server['dir']}")
    print(f"Comando:   {' '.join(command)}\n")

    try:
        # O mvnw.cmd é um script .cmd e só roda pelo shell no Windows.
        return subprocess.call(command, cwd=str(server["dir"]), env=env, shell=IS_WINDOWS)
    except KeyboardInterrupt:
        return 130


def open_terminal(name: str):
    """Abre um terminal independente que roda este arquivo com --executar <name>."""
    title = SERVERS[name]["title"]
    cwd = SERVERS[name]["dir"]
    args = [sys.executable, str(Path(__file__).resolve()), "--executar", name]

    if IS_WINDOWS:
        # Com /s, o cmd tira só as aspas das pontas e preserva as dos caminhos.
        inner = subprocess.list2cmdline(args)
        return subprocess.Popen(
            f'cmd.exe /s /k "title {title} && {inner}"',
            cwd=str(cwd),
            creationflags=subprocess.CREATE_NEW_CONSOLE,
        )

    shell_cmd = f"cd {shlex.quote(str(cwd))} && {shlex.join(args)}"

    if sys.platform == "darwin":
        applescript_cmd = shell_cmd.replace("\\", "\\\\").replace('"', '\\"')
        return subprocess.Popen([
            "osascript",
            "-e", f'tell application "Terminal" to do script "{applescript_cmd}"',
            "-e", 'tell application "Terminal" to activate',
        ])

    keep_open = f"{shell_cmd}; exec bash"
    if shutil.which("gnome-terminal"):
        return subprocess.Popen(["gnome-terminal", "--title", title, "--", "bash", "-c", keep_open])
    if shutil.which("konsole"):
        return subprocess.Popen(["konsole", "-p", f"tabtitle={title}", "-e", "bash", "-c", keep_open])
    if shutil.which("xterm"):
        return subprocess.Popen(["xterm", "-T", title, "-e", "bash", "-c", keep_open])

    # Sem emulador de terminal conhecido: roda em segundo plano neste mesmo terminal.
    print("      Nenhum terminal gráfico encontrado; rodando neste terminal.")
    return subprocess.Popen(args, cwd=str(cwd))


def port_open(port: int) -> bool:
    try:
        with socket.create_connection(("127.0.0.1", port), timeout=1):
            return True
    except OSError:
        return False


def wait_for_socket_server() -> bool:
    deadline = time.monotonic() + SOCKET_WAIT_SECONDS
    while time.monotonic() < deadline:
        if port_open(SOCKET_PORT):
            return True
        time.sleep(1)
    return False


def parse_args():
    parser = argparse.ArgumentParser(
        description="Abre o servidor de sockets e o backend em dois terminais, na ordem certa.")
    parser.add_argument("--executar", choices=sorted(SERVERS), help=argparse.SUPPRESS)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.executar:
        return run_server(args.executar)

    print("=" * 60)
    print("Iniciando ecossistema Backend do Converge...")
    print("=" * 60)

    # 1. Socket Server
    if port_open(SOCKET_PORT):
        print(f"\n[1/2] Já tem algo ouvindo na porta {SOCKET_PORT}; usando o Socket Server que está rodando.")
    else:
        print("\n[1/2] Abrindo terminal do Socket Server...")
        print(f"      Diretório: {SERVER_DIR}")
        open_terminal("socket")

        print(f"      Esperando a porta {SOCKET_PORT} (até {SOCKET_WAIT_SECONDS} s)...")
        if wait_for_socket_server():
            print("      Socket Server no ar.")
        else:
            print("      A porta não abriu a tempo. Confira o terminal do Socket Server; "
                  "o backend será aberto mesmo assim.")

    # 2. Spring Backend (o .env é carregado dentro do terminal novo)
    env_file = backend_env_file()
    print("\n[2/2] Abrindo terminal do Spring Backend...")
    print(f"      Diretório: {BACKEND_DIR}")
    print(f"      Arquivo .env: {env_file}" + ("" if env_file.is_file() else " (não encontrado!)"))
    open_terminal("backend")

    print("\n" + "=" * 60)
    print("Os 2 terminais foram abertos na ordem correta!")
    print("=" * 60)
    return 0


if __name__ == "__main__":
    sys.exit(main())
