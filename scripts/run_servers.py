import os
import sys
import time
import subprocess
from pathlib import Path


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


def open_terminal(cmd: str, cwd: Path, title: str = "", env: dict = None):
    """Abre um novo terminal independente executando o comando informado."""
    if sys.platform == "win32":
        full_cmd = f"title {title} && {cmd}" if title else cmd
        return subprocess.Popen(
            ["cmd.exe", "/k", full_cmd],
            cwd=str(cwd),
            env=env,
            creationflags=subprocess.CREATE_NEW_CONSOLE,
        )
    elif sys.platform == "darwin":
        script = f'tell application "Terminal" to do script "cd {cwd} && {cmd}"'
        return subprocess.Popen(["osascript", "-e", script], env=env)
    else:
        for term in ["gnome-terminal", "xterm", "konsole"]:
            try:
                if term == "gnome-terminal":
                    return subprocess.Popen(
                        ["gnome-terminal", "--", "bash", "-c", f"cd '{cwd}' && {cmd}; exec bash"],
                        env=env,
                    )
                else:
                    return subprocess.Popen(
                        [term, "-e", f"bash -c \"cd '{cwd}' && {cmd}; exec bash\""],
                        env=env,
                    )
            except FileNotFoundError:
                continue
        return subprocess.Popen(cmd, cwd=str(cwd), env=env, shell=True)


def main():
    # O script fica em scripts/; os módulos ficam em app/.
    base_dir = Path(__file__).resolve().parent.parent / "app"
    server_dir = base_dir / "server"
    backend_dir = base_dir / "converge-backend"

    # Comando do Socket Server
    if sys.platform == "win32" and (server_dir / "mvnw.cmd").exists():
        server_cmd = r".\mvnw.cmd compile exec:java -Dexec.mainClass=com.converge.socket.Main"
    elif (server_dir / "mvnw").exists():
        server_cmd = "./mvnw compile exec:java -Dexec.mainClass=com.converge.socket.Main"
    else:
        server_cmd = "mvn compile exec:java -Dexec.mainClass=com.converge.socket.Main"

    print("=" * 60)
    print("Iniciando ecossistema Backend do Converge...")
    print("=" * 60)

    # 1. Abrir terminal do Socket Server
    print("\n[1/2] Abrindo terminal do Socket Server...")
    print(f"      Diretório: {server_dir}")
    print(f"      Comando:   {server_cmd}")
    open_terminal(
        cmd=server_cmd,
        cwd=server_dir,
        title="Converge - Socket Server",
    )

    # Aguarda o socket server antes de disparar o Spring
    time.sleep(2)

    # 2. Carregar .env e abrir terminal do Spring Backend
    env_file = backend_dir / ".env"
    if not env_file.is_file():
        env_file = base_dir / ".env"

    loaded_env = load_env(env_file)
    backend_env = os.environ.copy()
    backend_env.update(loaded_env)

    if sys.platform == "win32" and (backend_dir / "mvnw.cmd").exists():
        backend_cmd = r".\mvnw.cmd spring-boot:run"
    elif (backend_dir / "mvnw").exists():
        backend_cmd = "./mvnw spring-boot:run"
    else:
        backend_cmd = "mvn spring-boot:run"

    print("\n[2/2] Abrindo terminal do Spring Backend...")
    print(f"      Diretório: {backend_dir}")
    print(f"      Arquivo .env: {env_file} ({len(loaded_env)} variáveis carregadas)")
    print(f"      Comando:   {backend_cmd}")

    open_terminal(
        cmd=backend_cmd,
        cwd=backend_dir,
        title="Converge - Spring Backend",
        env=backend_env,
    )

    print("\n" + "=" * 60)
    print("Os 2 terminais foram abertos na ordem correta!")
    print("=" * 60)


if __name__ == "__main__":
    main()
