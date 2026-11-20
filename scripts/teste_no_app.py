#!/usr/bin/env python3
# Autor: Cristian Eduardo Fava - RA 25000636

"""
Prepara um teste manual no app e faz o papel da "outra pessoa" da corrida.

Quem testa no simulador usa uma conta só (a "conta do app"). A outra ponta da corrida é um robô
que age pela API, no tempo certo das regras de horário (pedido até 5 min antes da saída, início
de 5 min antes a 5 min depois, pedido expira em 3 min sem resposta):

    python3 scripts/teste_no_app.py preparar      # cria a conta do app e os dois robôs
    python3 scripts/teste_no_app.py motorista     # robô MOTORISTA: oferece a carona, aceita, inicia,
                                                  #   confirma a chegada (cobra) e encerra
    python3 scripts/teste_no_app.py passageiro    # robô PASSAGEIRO: pede vaga na carona que a conta
                                                  #   do app publicou e informa o pagamento ("Já paguei")
    python3 scripts/teste_no_app.py limpar        # apaga as contas do Firebase criadas pelo preparar

As contas ficam em scripts/.teste_no_app.json (fora do Git). A senha de todas é a da simulação.
A carona usa os trajetos padrão do app: PUC-Campinas - Portaria Principal → Campus II (Saúde).
"""

import argparse
import json
import sys
import time
import uuid
from datetime import date, datetime, timedelta, timezone
from pathlib import Path

import verificar_usuario
from pymongo import MongoClient
from simular_corrida import DEFAULT_API, firebase_api_key
from simulation.api_client import ApiClient
from simulation.simulation_failure import SimulationFailure
from simulation.simulation_world import FIREBASE_AUTH_URL, PASSWORD, WEEK_DAYS, SimulationWorld

STATE_FILE = Path(__file__).resolve().parent / ".teste_no_app.json"
ORIGIN = {"address": "PUC-Campinas - Portaria Principal", "neighborhood": "Parque das Universidades",
          "city": "Campinas", "latitude": -22.8335, "longitude": -47.0512}
DESTINATION = {"address": "PUC-Campinas - Campus II (Saúde)", "neighborhood": "Jardim Ipaussurama",
               "city": "Campinas", "latitude": -22.9288, "longitude": -47.0789}
REQUEST_CUTOFF = timedelta(minutes=5)
START_WINDOW = timedelta(minutes=5)
POLL_SECONDS = 5


def log(message: str) -> None:
    print(f"[{datetime.now():%H:%M:%S}] {message}", flush=True)


def load_state() -> dict:
    if not STATE_FILE.is_file():
        sys.exit("Rode antes: python3 scripts/teste_no_app.py preparar")
    return json.loads(STATE_FILE.read_text(encoding="utf-8"))


def sign_in(api: ApiClient, key: str, email: str) -> str:
    response = api.call("POST", f"{FIREBASE_AUTH_URL}/accounts:signInWithPassword?key={key}",
                        body={"email": email, "password": PASSWORD, "returnSecureToken": True})
    token = (response.body or {}).get("idToken")
    if not token:
        sys.exit(f"Não deu para entrar com {email}: {response.describe()}")
    return token


def expect(response, what: str):
    if not response.ok:
        raise SimulationFailure(f"{what}: {response.describe()}")
    return response.data


def route_body(kind: str, departure: datetime, extra: dict) -> dict:
    return {
        "type": kind,
        "origin": ORIGIN,
        "destination": DESTINATION,
        "departureTime": departure.strftime("%H:%M"),
        "estimatedArrivalTime": (departure + timedelta(minutes=30)).strftime("%H:%M"),
        "weekDays": [WEEK_DAYS[departure.weekday()]],
        "recurrence": "ONCE",
        "date": departure.date().isoformat(),
        **extra,
    }


# ---------------------------------------------------------------------------------- preparar

def prepare(api: ApiClient, key: str, connection: str, database: str) -> None:
    run_id = uuid.uuid4().hex
    emails = [
        f"sim.{run_id}.1.conta-do-app@converge-sim.test",
        f"sim.{run_id}.2.motorista-robo@converge-sim.test",
        f"sim.{run_id}.3.passageira-robo@converge-sim.test",
    ]
    try:
        with STATE_FILE.open("x", encoding="utf-8") as state_file:
            json.dump({"runId": run_id, "emails": emails}, state_file)
    except FileExistsError:
        raise SimulationFailure("Já existe uma preparação. Rode limpar antes de preparar novamente.")

    with MongoClient(connection, serverSelectionTimeoutMS=10_000) as client:
        users = client[database][verificar_usuario.USERS_COLLECTION]
        world = SimulationWorld(api, key, users, run_id, date.today())
        try:
            app_user = world.create_driver("Conta do App")
            driver_bot = world.create_driver("Motorista Robo")
            passenger_bot = world.create_passenger("Passageira Robo")
        except BaseException:
            # Falhou no meio: apaga do Firebase as contas já criadas, que ficariam sem registro
            for email in world.delete_firebase_accounts():
                log(f"Não deu para apagar a conta {email} do Firebase.")
            raise

    state = {
        "runId": run_id,
        "emails": emails,
        "app": {"email": app_user.email, "id": app_user.id},
        "motorista": {"email": driver_bot.email, "id": driver_bot.id},
        "passageiro": {"email": passenger_bot.email, "id": passenger_bot.id},
    }
    prepared_file = STATE_FILE.with_suffix(".tmp")
    prepared_file.write_text(json.dumps(state, indent=2, ensure_ascii=False), encoding="utf-8")
    prepared_file.replace(STATE_FILE)

    print("\nContas criadas (documentos aprovados; a conta do app também tem carro e PIX):\n")
    print(f"  CONTA DO APP (use no simulador): {app_user.email}")
    print(f"  Senha: {PASSWORD}\n")
    print(f"  robô motorista:  [SIM] Motorista Robo  ({driver_bot.email})")
    print(f"  robô passageira: [SIM] Passageira Robo ({passenger_bot.email})")
    print(f"\nSalvo em {STATE_FILE}. Para apagar as contas depois: python3 scripts/teste_no_app.py limpar")


# ---------------------------------------------------------------------------------- robô motorista

def run_driver_bot(api: ApiClient, key: str, minutes: int, ride_minutes: int) -> None:
    """A conta do app é a PASSAGEIRA: o robô oferece, aceita, inicia, cobra na chegada e encerra."""
    state = load_state()
    token = sign_in(api, key, state["motorista"]["email"])
    vehicles = expect(api.get("/vehicles", token), "listar o carro do robô") or []
    if not vehicles:
        sys.exit("O robô motorista está sem carro. Rode o preparar de novo.")

    departure = (datetime.now() + timedelta(minutes=minutes)).replace(second=0, microsecond=0)
    route = expect(api.post("/routes", token, route_body("OFFER", departure, {
        "vehicleId": vehicles[0]["id"], "maxDetourKm": 5.0, "totalSeats": 3, "toll": 0, "parking": 0,
    })), "publicar a carona do robô")
    log(f"Carona publicada pelo [SIM] Motorista Robo: Portaria Principal → Campus II, saída {departure:%H:%M}.")
    log(f"Peça a vaga no app até {departure - REQUEST_CUTOFF:%H:%M} (depois disso a carona não aceita pedidos).")

    accepted: set = set()
    arrived: set = set()
    started = completed = waiting_logged = False
    started_at = None
    deadline = departure + timedelta(minutes=ride_minutes + 15)

    while datetime.now() < deadline and not completed:
        rides = [r for r in (expect(api.get("/rides", token), "listar as viagens do robô") or [])
                 if r.get("driverRouteId") == route["id"]]
        for ride in rides:
            passengers = [p for p in ride.get("participants") or [] if p.get("role") == "PASSENGER"]

            for p in passengers:
                if p.get("status") == "REQUESTED" and p["userId"] not in accepted:
                    expect(api.post(f"/rides/{ride['id']}/passengers/{p['userId']}/accept", token),
                           "aceitar o pedido")
                    accepted.add(p["userId"])
                    log(f"Pedido de vaga de {p.get('name')} aceito.")

            if ride.get("status") == "CONFIRMED" and not started:
                if datetime.now() >= departure - START_WINDOW + timedelta(seconds=5):
                    expect(api.post(f"/rides/{ride['id']}/start", token), "iniciar a viagem")
                    started, started_at = True, datetime.now()
                    log(f"Viagem iniciada. A chegada vai ser confirmada em {ride_minutes} min.")
                elif accepted and not waiting_logged:
                    log(f"Esperando a janela de início ({departure - START_WINDOW:%H:%M}) para iniciar.")
                    waiting_logged = True

            if ride.get("status") == "IN_PROGRESS" and started_at \
                    and datetime.now() >= started_at + timedelta(minutes=ride_minutes):
                for p in passengers:
                    if p.get("status") == "CONFIRMED" and p["userId"] not in arrived:
                        expect(api.post(f"/rides/{ride['id']}/passengers/{p['userId']}/arrive", token),
                               "confirmar a chegada")
                        arrived.add(p["userId"])
                        log(f"Chegada de {p.get('name')} confirmada: a cobrança dela foi emitida.")
                time.sleep(20)
                expect(api.post(f"/rides/{ride['id']}/complete", token), "encerrar a viagem")
                completed = True
                log("Viagem encerrada pelo robô. Fim do papel de motorista.")
        if not completed:
            time.sleep(POLL_SECONDS if accepted else 10)

    if not completed:
        log("Tempo esgotado sem concluir a viagem (ninguém pediu vaga a tempo?).")


# ---------------------------------------------------------------------------------- robô passageira

def runs_on(route: dict, day: date) -> bool:
    """Igual ao Route.runsOn do backend: ONCE só na data dele; WEEKLY nos dias da semana dele."""
    if route.get("recurrence", "WEEKLY") != "ONCE":
        return WEEK_DAYS[day.weekday()] in (route.get("weekDays") or [])
    stored = route.get("date")
    if isinstance(stored, datetime):
        # O Mongo devolve o LocalDate como meia-noite do fuso do backend, em UTC
        stored = stored.replace(tzinfo=timezone.utc).astimezone().date()
    elif isinstance(stored, str):
        stored = date.fromisoformat(stored[:10])
    return stored == day


def run_passenger_bot(api: ApiClient, key: str, connection: str, database: str, pay_after: int) -> None:
    """A conta do app é a MOTORISTA: o robô pede vaga na carona que ela publicou e informa o pagamento."""
    state = load_state()
    token = sign_in(api, key, state["passageiro"]["email"])
    my_id = state["passageiro"]["id"]

    log("Procurando a carona publicada pela conta do app (de hoje, ainda ativa)...")
    today = date.today()
    with MongoClient(connection, serverSelectionTimeoutMS=10_000) as client:
        routes = client[database]["routes"]
        offer = None
        for _ in range(120):
            offers = routes.find({"userId": state["app"]["id"], "type": "OFFER", "status": "ACTIVE"},
                                 sort=[("createdAt", -1)])
            offer = next((route for route in offers if runs_on(route, today)), None)
            if offer:
                break
            time.sleep(POLL_SECONDS)
    if not offer:
        sys.exit("A conta do app não publicou carona para hoje nos últimos 10 min.")

    hour, minute = map(int, offer["departureTime"].split(":"))
    departure = datetime.combine(today, datetime.min.time()).replace(hour=hour, minute=minute)
    if datetime.now() >= departure - REQUEST_CUTOFF:
        sys.exit(f"A carona sai às {departure:%H:%M}: não dá mais para pedir vaga (só até 5 min antes).")
    log(f"Carona encontrada: saída {departure:%H:%M}.")

    search = expect(api.post("/routes", token, route_body("SEARCH", departure, {})),
                    "criar a procura do robô")
    ride = expect(api.post("/rides/request", token, {
        "driverRouteId": str(offer["_id"]), "passengerRouteId": search["id"], "date": today.isoformat(),
    }), "pedir vaga")
    ride_id = ride["id"]
    log("[SIM] Passageira Robo pediu vaga. ACEITE no app em até 3 min (o pedido expira).")

    paid = False
    while not paid:
        ride = expect(api.get(f"/rides/{ride_id}", token), "consultar a viagem")
        me = next((p for p in ride.get("participants") or [] if p.get("userId") == my_id), {})
        status, payment = me.get("status"), me.get("paymentStatus")

        if status in ("REJECTED", "EXPIRED", "CANCELLED", "NO_SHOW") or ride.get("status") == "CANCELLED":
            log(f"Fim: o pedido da robô ficou {status} (viagem {ride.get('status')}).")
            return
        if status == "COMPLETED" and payment == "PENDING":
            log(f"Chegada confirmada e cobrança recebida. A robô informa o pagamento em {pay_after} s.")
            time.sleep(pay_after)
            expect(api.post(f"/rides/{ride_id}/passengers/{my_id}/payment/mark-paid", token), "Já paguei")
            log("A robô informou o pagamento (\"Já paguei\"). No app, ela deve aparecer como \"Pagou\".")
            paid = True
        elif status == "COMPLETED":
            log(f"Viagem da robô terminou com pagamento {payment}. Fim.")
            return
        else:
            time.sleep(POLL_SECONDS)


# ---------------------------------------------------------------------------------- limpar

def clean(api: ApiClient, key: str, connection: str, database: str) -> None:
    state = load_state()
    with MongoClient(connection, serverSelectionTimeoutMS=10_000) as client:
        world = SimulationWorld(api, key, client[database][verificar_usuario.USERS_COLLECTION],
                                state["runId"], date.today())
        deleted, leftovers = world.delete_accounts_of_run(state["runId"], state.get("emails", []))
    print(f"{len(deleted)} conta(s) apagada(s) do Firebase.")
    for email in leftovers:
        print(f"  não deu para apagar: {email}")
    if leftovers:
        raise SimulationFailure("Limpeza incompleta; o estado foi mantido para tentar novamente.")
    STATE_FILE.unlink(missing_ok=True)


def main() -> int:
    parser = argparse.ArgumentParser(description="Teste manual no app com um robô fazendo a outra ponta da corrida.")
    parser.add_argument("acao", choices=["preparar", "motorista", "passageiro", "limpar"])
    parser.add_argument("--api", default=DEFAULT_API, help=f"URL da API (padrão: {DEFAULT_API})")
    parser.add_argument("--minutos", type=int, default=15,
                        help="motorista: em quantos minutos a carona do robô sai (padrão: 15)")
    parser.add_argument("--duracao", type=int, default=3,
                        help="motorista: minutos de viagem antes de confirmar a chegada (padrão: 3)")
    parser.add_argument("--pagar-depois", type=int, default=30,
                        help="passageiro: segundos até a robô informar o pagamento (padrão: 30)")
    args = parser.parse_args()

    env_file = verificar_usuario.read_env_file(verificar_usuario.DEFAULT_ENV_FILE)
    connection = verificar_usuario.setting("MONGODB_CONNECTION_STRING", env_file)
    database = verificar_usuario.setting("MONGODB_DATABASE_NAME", env_file, verificar_usuario.DEFAULT_DATABASE)
    if not connection:
        sys.exit("MONGODB_CONNECTION_STRING não encontrada (ambiente ou app/converge-backend/.env).")
    if "prod" in database.lower():
        sys.exit(f"Recusado: o teste não roda no banco '{database}'.")

    api = ApiClient(args.api)
    key = firebase_api_key(env_file)
    try:
        if args.acao == "preparar":
            prepare(api, key, connection, database)
        elif args.acao == "motorista":
            run_driver_bot(api, key, args.minutos, args.duracao)
        elif args.acao == "passageiro":
            run_passenger_bot(api, key, connection, database, args.pagar_depois)
        else:
            clean(api, key, connection, database)
    except SimulationFailure as failure:
        log(f"ERRO: {failure}")
        return 1
    except KeyboardInterrupt:
        log("Interrompido.")
        return 130
    return 0


if __name__ == "__main__":
    sys.exit(main())
