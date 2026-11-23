#!/usr/bin/env python3
# Autor: Cristian Eduardo Fava - RA 25000636

"""
Teste manual no app com 3 robôs (Ana, Bruno e Carla) fazendo a outra ponta da corrida, em duas fases.

Fase 1 (você é o MOTORISTA): os 3 robôs esperam você publicar no app uma carona que sai nos
próximos 6 minutos e pedem vaga nela assim que ela aparece. O resto é com você no app: aceitar,
iniciar, marcar "Entrou", confirmar a chegada de cada um e encerrar. Quando a cobrança chega, cada
robô informa o pagamento ("Já paguei").

Enter no terminal: começa a fase 2 (também pula a fase 1 se ela ainda não acabou).

Fase 2 (a Ana é a MOTORISTA): ela publica uma carona Campus I → Campus II que sai daqui a 6 min,
Bruno e Carla pedem vaga e ela aceita todos os pedidos (o seu também, se você pedir pelo app).
A Ana só inicia a viagem quando você der Enter. Outro Enter confirma a chegada de todos (o que
emite as cobranças) e encerra a viagem.

Todos os pontos de embarque dos robôs ficam a até 3 km do Campus I e todos os de descida a até
3 km do Campus II (o script confere antes de começar).

    python3 scripts/teste_no_app.py                                       # fase 1: vale qualquer carona nova
    python3 scripts/teste_no_app.py --motorista voce@puccampinas.edu.br   # fase 1: só a sua carona
    python3 scripts/teste_no_app.py --limpar 1123134501                   # apaga contas e dados da execução

Regras de horário do backend: o pedido de vaga só vale até 5 min antes da saída e expira em 3 min
sem resposta; o início vale de 5 min antes a 5 min depois da saída. As contas dos robôs ficam no
Firebase para logar no app (senha da simulação).
"""

import argparse
import math
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import date, datetime, timedelta, timezone

import verificar_usuario
from pymongo import MongoClient
from pymongo.errors import PyMongoError
from simular_corrida import DEFAULT_API, firebase_api_key
from simulation.simulated_user import SimulatedUser
from simulation.api_client import ApiClient
from simulation.simulation_failure import SimulationFailure
from simulation.simulation_world import FIREBASE_AUTH_URL, PASSWORD, WEEK_DAYS, SimulationWorld

# Trajeto padrão do app
PUC_I = {"address": "PUC-Campinas - Portaria Principal", "neighborhood": "Parque das Universidades",
         "city": "Campinas", "latitude": -22.8335, "longitude": -47.0512}
PUC_II = {"address": "PUC-Campinas - Campus II (Saúde)", "neighborhood": "Jardim Ipaussurama",
          "city": "Campinas", "latitude": -22.9288, "longitude": -47.0789}
MAX_DISTANCE_KM = 3.0


def _spot(address: str, latitude: float, longitude: float) -> dict:
    """Ponto no formato da API."""
    return {"address": address, "neighborhood": "Campinas", "city": "Campinas",
            "latitude": latitude, "longitude": longitude}


# Embarque perto do Campus I e descida perto do Campus II. A Ana é a primeira porque dirige na fase 2.
BOTS = {
    "Ana": (_spot("Embarque 1 - perto do Campus I", -22.8420, -47.0560),
            _spot("Descida 1 - a caminho do Campus II (norte)", -22.9035, -47.0735)),
    "Bruno": (_spot("Embarque 2 - perto do Campus I", -22.8250, -47.0430),
              _spot("Descida 2 - a caminho do Campus II (leste)", -22.9100, -47.0590)),
    "Carla": (_spot("Embarque 3 - perto do Campus I", -22.8460, -47.0650),
              _spot("Descida 3 - a caminho do Campus II (oeste)", -22.9080, -47.0930)),
}

REQUEST_CUTOFF = timedelta(minutes=5)
START_WINDOW = timedelta(minutes=5)
POLL_SECONDS = 2
# O idToken do Firebase vale 1 h; os robôs entram de novo antes disso
TOKEN_LIFETIME = timedelta(minutes=45)
ENDED = ("REJECTED", "EXPIRED", "CANCELLED", "NO_SHOW")

_last_login: dict = {}


def log(message: str) -> None:
    """Imprime a mensagem com o horário atual."""
    print(f"[{datetime.now():%H:%M:%S}] {message}", flush=True)


def distance_km(a: dict, b: dict) -> float:
    """Distância em linha reta (haversine) entre dois pontos."""
    lat1, lon1, lat2, lon2 = map(math.radians, (a["latitude"], a["longitude"], b["latitude"], b["longitude"]))
    h = math.sin((lat2 - lat1) / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin((lon2 - lon1) / 2) ** 2
    return 2 * 6371 * math.asin(math.sqrt(h))


def points_out_of_range() -> list:
    """Pontos dos robôs que passam de 3 km do campus deles (embarque: Campus I; descida: Campus II)."""
    problems = []
    for nickname, (pickup, drop_off) in BOTS.items():
        for kind, spot, campus, name in (("embarque", pickup, PUC_I, "Campus I"),
                                         ("descida", drop_off, PUC_II, "Campus II")):
            km = distance_km(spot, campus)
            if km > MAX_DISTANCE_KM:
                problems.append(f"{kind} de {nickname} a {km:.2f} km do {name}")
    return problems


def place_from_mongo(stored: dict) -> dict:
    """Converte o Place gravado no Mongo (GeoJSON [lon, lat]) para o formato da API."""
    longitude, latitude = stored["location"]["coordinates"]
    return {"address": stored.get("address"), "neighborhood": stored.get("neighborhood"),
            "city": stored.get("city"), "latitude": latitude, "longitude": longitude}


def ceil_minute(moment: datetime) -> datetime:
    """Arredonda para o próximo minuto cheio (o app só guarda HH:MM)."""
    rounded = moment.replace(second=0, microsecond=0)
    return rounded if rounded == moment else rounded + timedelta(minutes=1)


def departure_of(offer: dict, day: date) -> datetime:
    """Horário de saída da carona no dia informado."""
    hour, minute = map(int, offer["departureTime"].split(":"))
    return datetime.combine(day, datetime.min.time()).replace(hour=hour, minute=minute)


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


def route_body(kind: str, origin: dict, destination: dict, departure: datetime, extra: dict) -> dict:
    """Trajeto de hoje (ONCE) no formato do CreateRouteCommand do backend."""
    return {
        "type": kind,
        "origin": origin,
        "destination": destination,
        "departureTime": departure.strftime("%H:%M"),
        "estimatedArrivalTime": (departure + timedelta(minutes=30)).strftime("%H:%M"),
        "weekDays": [WEEK_DAYS[departure.weekday()]],
        "recurrence": "ONCE",
        "date": departure.date().isoformat(),
        **extra,
    }


def enter_event() -> threading.Event:
    """Evento que fica pronto quando a pessoa der Enter no terminal (sem travar o script)."""
    pressed = threading.Event()

    def wait_for_enter() -> None:
        try:
            input()
        except EOFError:
            pass
        pressed.set()

    threading.Thread(target=wait_for_enter, daemon=True).start()
    return pressed


# ---------------------------------------------------------------------------------- robôs

def create_bots(world: SimulationWorld) -> list:
    """Cria Ana (com carro e PIX, porque dirige na fase 2), Bruno e Carla, com documentos aprovados."""
    log("Criando os robôs Ana, Bruno e Carla...")
    bots = []
    for index, nickname in enumerate(BOTS):
        bot = world.create_driver(nickname) if index == 0 else world.create_passenger(nickname)
        _last_login[bot.email] = datetime.now()
        bots.append(bot)
    log("Robôs prontos.")
    return bots


def keep_tokens_fresh(world: SimulationWorld) -> None:
    """Entra de novo com os robôs cujo idToken está perto de vencer."""
    for user in world.created_users:
        if datetime.now() - _last_login.get(user.email, datetime.min) < TOKEN_LIFETIME:
            continue
        response = world.api.call("POST", f"{FIREBASE_AUTH_URL}/accounts:signInWithPassword"
                                          f"?key={world.firebase_api_key}",
                                  body={"email": user.email, "password": PASSWORD, "returnSecureToken": True})
        token = (response.body or {}).get("idToken")
        if not token:
            raise SimulationFailure(f"entrar de novo com {user.nickname}: {response.describe()}")
        user.token = token
        _last_login[user.email] = datetime.now()


def request_seat(world: SimulationWorld, bot: SimulatedUser, offer_id: str, departure: datetime) -> str:
    """Cria a procura do robô (do embarque dele até a descida dele) e pede a vaga. Devolve a viagem."""
    pickup, drop_off = BOTS[bot.nickname]
    search = world.expect_ok(world.api.post("/routes", bot.token,
                                            route_body("SEARCH", pickup, drop_off, departure, {})),
                             f"criar a procura de {bot.nickname}")
    ride = world.expect_ok(world.api.post("/rides/request", bot.token, {
        "driverRouteId": offer_id, "passengerRouteId": search["id"], "date": departure.date().isoformat(),
    }), f"{bot.nickname} pedir vaga")
    log(f"{bot.nickname} pediu vaga: embarca a {distance_km(pickup, PUC_I):.1f} km do Campus I e desce a "
        f"{distance_km(drop_off, PUC_II):.1f} km do Campus II.")
    return ride["id"]


def request_seats(world: SimulationWorld, bots: list, offer_id: str, departure: datetime) -> tuple:
    """Os robôs pedem vaga ao mesmo tempo (a janela de pedido pode ser curta). Devolve a viagem e quem entrou."""
    def attempt(bot: SimulatedUser):
        try:
            return bot, request_seat(world, bot, offer_id, departure)
        except SimulationFailure as failure:
            log(f"{bot.nickname} não conseguiu pedir vaga: {failure}")
            return bot, ""

    with ThreadPoolExecutor(len(bots)) as pool:
        results = list(pool.map(attempt, bots))
    requested = [(bot, ride_id) for bot, ride_id in results if ride_id]
    if not requested:
        raise SimulationFailure("nenhum robô conseguiu pedir vaga")
    return requested[0][1], [bot for bot, _ in requested]


def update_passenger_bots(world: SimulationWorld, ride_id: str, riders: list, seen: dict, pay_at: dict,
                          paid: set, pay_after: int) -> tuple:
    """
    Mostra cada mudança de status dos robôs passageiros e informa o pagamento ("Já paguei")
    pay_after segundos depois de a cobrança chegar. Devolve o status da viagem e se todos já acabaram.
    """
    ride_status = ""
    finished = True
    for bot in riders:
        ride = world.ride(bot, ride_id)
        ride_status = ride.get("status", "")
        me = world.participant(ride, bot)
        current = (me.get("status"), me.get("paymentStatus"), me.get("finalAmount"))
        if seen.get(bot.nickname) != current:
            log(f"{bot.nickname}: {current[0]}, pagamento {current[1]}, valor {current[2]}")
            seen[bot.nickname] = current

        if current[0] == "COMPLETED" and current[1] == "PENDING" and bot.nickname not in paid:
            pay_at.setdefault(bot.nickname, datetime.now() + timedelta(seconds=pay_after))
            if datetime.now() >= pay_at[bot.nickname]:
                world.expect_ok(world.payment(bot, ride_id, bot, "mark-paid"), f"{bot.nickname} informar o pagamento")
                log(f"{bot.nickname} informou o pagamento (\"Já paguei\").")
                paid.add(bot.nickname)

        done = current[0] in ENDED or (current[0] == "COMPLETED" and (current[1] != "PENDING" or bot.nickname in paid))
        finished = finished and done

    if seen.get("_ride") != ride_status:
        log(f"Viagem: {ride_status}")
        seen["_ride"] = ride_status
    return ride_status, finished


# ---------------------------------------------------------------------------------- fase 1

def wait_for_offer(db, driver_id: str, since: datetime, window: int, skip: threading.Event):
    """
    Espera uma carona de hoje que sai nos próximos `window` minutos e ainda aceita pedidos. Com
    --motorista, só as dessa conta; sem, só as publicadas depois que o script começou. Devolve
    None se a pessoa der Enter antes.
    """
    query = {"type": "OFFER", "status": "ACTIVE"}
    if driver_id:
        query["userId"] = driver_id
    else:
        query["createdAt"] = {"$gte": since.astimezone(timezone.utc).replace(tzinfo=None)}

    ignored: set = set()
    while not skip.is_set():
        now = datetime.now()
        for offer in db["routes"].find(query, sort=[("createdAt", -1)]):
            if not runs_on(offer, now.date()):
                continue
            departure = departure_of(offer, now.date())
            if now < departure - REQUEST_CUTOFF and departure <= ceil_minute(now + timedelta(minutes=window)):
                return offer
            if offer["_id"] not in ignored and now < departure:
                ignored.add(offer["_id"])
                reason = ("já não aceita pedidos (só até 5 min antes)" if now >= departure - REQUEST_CUTOFF
                          else f"sai depois de {window} min")
                log(f"Carona das {departure:%H:%M} ignorada: {reason}.")
        skip.wait(POLL_SECONDS)
    return None


def phase_one(world: SimulationWorld, db, bots: list, driver_id: str, since: datetime, window: int,
              pay_after: int) -> None:
    """Os 3 robôs pedem vaga na carona que você publicar e pagam quando a cobrança chega."""
    log("FASE 1: você é o motorista.")
    log(f"Publique no app uma carona Campus I → Campus II que sai nos próximos {window} min. O pedido de "
        "vaga só vale até 5 min antes da saída, então publique rápido.")
    log("(Enter a qualquer momento pula para a fase 2.)")
    skip = enter_event()

    offer = wait_for_offer(db, driver_id, since, window, skip)
    if offer is None:
        return
    departure = departure_of(offer, date.today())
    origin, destination = place_from_mongo(offer["origin"]), place_from_mongo(offer["destination"])
    log(f"Carona encontrada: {origin['address']} → {destination['address']}, saída {departure:%H:%M}, "
        f"{offer.get('availableSeats', offer.get('totalSeats'))} vaga(s). Pedidos até {departure - REQUEST_CUTOFF:%H:%M}.")
    if distance_km(origin, PUC_I) > MAX_DISTANCE_KM:
        log(f"AVISO: a saída da carona está a {distance_km(origin, PUC_I):.1f} km do Campus I.")
    if distance_km(destination, PUC_II) > MAX_DISTANCE_KM:
        log(f"AVISO: o destino da carona está a {distance_km(destination, PUC_II):.1f} km do Campus II.")

    ride_id, riders = request_seats(world, bots, str(offer["_id"]), departure)
    log(f"Agora é com você no app: aceite os pedidos (expiram em 3 min), inicie a partir de "
        f"{departure - START_WINDOW:%H:%M}, marque \"Entrou\", confirme a chegada de cada um e encerre.")

    seen: dict = {}
    pay_at: dict = {}
    paid: set = set()
    while not skip.is_set():
        keep_tokens_fresh(world)
        ride_status, finished = update_passenger_bots(world, ride_id, riders, seen, pay_at, paid, pay_after)
        if finished and ride_status != "IN_PROGRESS":
            log("Fase 1 terminou. Dê Enter para começar a fase 2.")
            skip.wait()
            return
        skip.wait(POLL_SECONDS)


# ---------------------------------------------------------------------------------- fase 2

def phase_two(world: SimulationWorld, bots: list, minutes: int, pay_after: int) -> None:
    """A Ana publica a carona, aceita os pedidos e inicia/encerra a viagem quando você der Enter."""
    keep_tokens_fresh(world)
    driver, passengers = bots[0], bots[1:]
    departure = ceil_minute(datetime.now() + timedelta(minutes=minutes))
    log("FASE 2: a Ana é a motorista.")
    offer = world.expect_ok(world.api.post("/routes", driver.token, route_body("OFFER", PUC_I, PUC_II, departure, {
        "vehicleId": driver.vehicle_id, "maxDetourKm": 5.0, "totalSeats": 3, "toll": 0, "parking": 0,
    })), "publicar a carona da Ana")
    log(f"Ana publicou a carona {PUC_I['address']} → {PUC_II['address']}, saída {departure:%H:%M}, 3 vagas.")

    ride_id, riders = request_seats(world, passengers, offer["id"], departure)
    log(f"Se quiser ir junto, peça vaga no app até {departure - REQUEST_CUTOFF:%H:%M}: a Ana aceita na hora.")
    log(f"Dê Enter para a Ana iniciar a viagem (o início vale de {departure - START_WINDOW:%H:%M} "
        f"a {departure + START_WINDOW:%H:%M}).")

    bot_ids = {bot.id for bot in bots}
    seen: dict = {}
    pay_at: dict = {}
    paid: set = set()
    stage = "start"
    pressed = enter_event()
    waiting_logged = False

    while True:
        keep_tokens_fresh(world)
        ride = world.ride(driver, ride_id)
        others = [p for p in ride.get("participants") or []
                  if p.get("role") == "PASSENGER" and p.get("userId") not in bot_ids]
        for p in others:
            current = (p.get("status"), p.get("paymentStatus"))
            if seen.get(p["userId"]) != current:
                log(f"{p.get('name')} (você): {current[0]}, pagamento {current[1]}")
                seen[p["userId"]] = current

        for p in ride.get("participants") or []:
            if p.get("role") == "PASSENGER" and p.get("status") == "REQUESTED":
                world.expect_ok(world.api.post(f"/rides/{ride_id}/passengers/{p['userId']}/accept", driver.token),
                                f"aceitar o pedido de {p.get('name')}")
                log(f"Ana aceitou o pedido de {p.get('name')}.")

        if stage == "start" and pressed.is_set():
            opens_at = departure - START_WINDOW + timedelta(seconds=3)
            if datetime.now() < opens_at:
                if not waiting_logged:
                    log(f"Enter recebido. A janela de início abre às {opens_at:%H:%M:%S}; a Ana inicia nessa hora.")
                    waiting_logged = True
            else:
                start_and_board(world, driver, ride_id)
                log("Dê Enter para a Ana confirmar a chegada de todos (emite as cobranças) e encerrar a viagem.")
                stage, pressed = "arrive", enter_event()
                continue
        elif stage == "arrive" and pressed.is_set():
            arrive_and_complete(world, driver, ride_id)
            stage = "done"

        ride_status, finished = update_passenger_bots(world, ride_id, riders, seen, pay_at, paid, pay_after)
        if ride_status == "CANCELLED" or (ride_status == "COMPLETED" and finished):
            log("Fase 2 terminou.")
            return
        time.sleep(POLL_SECONDS)


def start_and_board(world: SimulationWorld, driver: SimulatedUser, ride_id: str) -> None:
    """Inicia a viagem e marca "Entrou" para todos os passageiros confirmados."""
    world.expect_ok(world.api.post(f"/rides/{ride_id}/start", driver.token), "iniciar a viagem")
    log("Ana iniciou a viagem.")
    for p in world.ride(driver, ride_id).get("participants") or []:
        if p.get("role") == "PASSENGER" and p.get("status") == "CONFIRMED":
            world.expect_ok(world.api.post(f"/rides/{ride_id}/passengers/{p['userId']}/board", driver.token),
                            f"marcar que {p.get('name')} entrou")
            log(f"Ana marcou que {p.get('name')} entrou.")


def arrive_and_complete(world: SimulationWorld, driver: SimulatedUser, ride_id: str) -> None:
    """Confirma a chegada de quem está a bordo (emite a cobrança de cada um) e encerra a viagem."""
    ride = world.ride(driver, ride_id)
    for p in ride.get("participants") or []:
        if p.get("role") == "PASSENGER" and p.get("status") == "CONFIRMED":
            world.expect_ok(world.api.post(f"/rides/{ride_id}/passengers/{p['userId']}/arrive", driver.token),
                            f"confirmar a chegada de {p.get('name')}")
            log(f"Ana confirmou a chegada de {p.get('name')}: a cobrança foi emitida.")
    world.expect_ok(world.api.post(f"/rides/{ride_id}/complete", driver.token), "encerrar a viagem")
    log("Ana encerrou a viagem.")


# ---------------------------------------------------------------------------------- limpar

def clean_run(api: ApiClient, key: str, db, run_id: str) -> int:
    """Apaga as contas do Firebase e tudo o que a execução gravou no Mongo (viagens só com gente dela)."""
    users = db[verificar_usuario.USERS_COLLECTION]
    world = SimulationWorld(api, key, users, run_id, date.today())
    deleted, leftovers = world.delete_accounts_of_run(run_id)
    log(f"Firebase: {len(deleted)} conta(s) apagada(s).")
    for email in leftovers:
        log(f"  não deu para apagar: {email}")

    found = list(users.find({"email": {"$regex": f"^sim\\.{run_id}\\."}}, {"_id": 1, "firebaseUid": 1}))
    ids = [str(user["_id"]) for user in found]
    who = ids + [user["_id"] for user in found] + [user["firebaseUid"] for user in found if user.get("firebaseUid")]
    rides = [ride for ride in db["rides"].find({"participants.userId": {"$in": ids}})
             if all(p.get("userId") in ids for p in ride.get("participants") or [])]
    # Viagem com gente de fora (a sua) não é mexida, e quem aparece nela continua em users para ela não ficar órfã
    shared = list(db["rides"].find({"participants.userId": {"$in": ids}, "_id": {"$nin": [r["_id"] for r in rides]}}))
    in_shared = {p.get("userId") for ride in shared for p in ride.get("participants") or []}
    ride_ids = [str(ride["_id"]) for ride in rides] + [ride["_id"] for ride in rides]
    plan = {
        "rides": {"_id": {"$in": [ride["_id"] for ride in rides]}},
        "routes": {"userId": {"$in": ids}},
        "vehicles": {"userId": {"$in": ids}},
        "notifications": {"userId": {"$in": who}},
        "matchings": {"$or": [{"driverId": {"$in": who}}, {"passengerId": {"$in": who}}]},
        "audit_logs": {"$or": [{"actorId": {"$in": who}}, {"targetUserId": {"$in": who}}]},
        "idempotency_keys": {"userId": {"$in": who}},
        "indicators": {"userId": {"$in": who}},
        "indicator_ride_registrations": {"_id": {"$in": ride_ids}},
        "reviews": {"$or": [{"reviewerId": {"$in": who}}, {"reviewedId": {"$in": who}}]},
        "users": {"_id": {"$in": [user["_id"] for user in found if str(user["_id"]) not in in_shared]}},
    }
    for collection, query in plan.items():
        log(f"Mongo {collection}: {db[collection].delete_many(query).deleted_count} apagado(s)")
    if shared:
        log(f"{len(shared)} viagem(ns) com gente de fora ficaram como estavam, e os "
            f"{len(set(ids) & in_shared)} robô(s) dela continuam em users.")
    return 1 if leftovers else 0


# ---------------------------------------------------------------------------------- main

def parse_args():
    """Opções de linha de comando."""
    parser = argparse.ArgumentParser(description="Teste manual no app com 3 robôs: na fase 1 eles pedem vaga "
                                                 "na sua carona; na fase 2 a Ana dirige e os outros dois vão junto.")
    parser.add_argument("--motorista", help="Fase 1: e-mail da sua conta. Sem ele, vale a primeira carona "
                                            "publicada por qualquer pessoa depois que o script começar.")
    parser.add_argument("--janela", type=int, default=6,
                        help="Fase 1: só vale carona que sai nos próximos N min (padrão: 6)")
    parser.add_argument("--minutos", type=int, default=6,
                        help="Fase 2: em quantos minutos a carona da Ana sai (padrão: 6)")
    parser.add_argument("--pagar-depois", type=int, default=10,
                        help="Segundos entre a cobrança chegar e o robô informar o pagamento (padrão: 10)")
    parser.add_argument("--api", default=DEFAULT_API, help=f"URL da API (padrão: {DEFAULT_API})")
    parser.add_argument("--limpar", metavar="EXECUCAO",
                        help="Só apaga as contas e os dados de uma execução anterior e sai")
    return parser.parse_args()


def main() -> int:
    """Cria os robôs e roda as duas fases."""
    args = parse_args()
    started = datetime.now().astimezone()
    problems = points_out_of_range()
    if problems:
        log(f"Pontos a mais de {MAX_DISTANCE_KM:.0f} km do campus: {'; '.join(problems)}.")
        return 2

    env_file = verificar_usuario.read_env_file(verificar_usuario.DEFAULT_ENV_FILE)
    connection = verificar_usuario.setting("MONGODB_CONNECTION_STRING", env_file)
    database = verificar_usuario.setting("MONGODB_DATABASE_NAME", env_file, verificar_usuario.DEFAULT_DATABASE)
    if not connection:
        log("MONGODB_CONNECTION_STRING não encontrada (ambiente ou app/converge-backend/.env).")
        return 2
    if "prod" in database.lower():
        log(f"Recusado: o teste não roda no banco '{database}'.")
        return 2

    api = ApiClient(args.api)
    key = firebase_api_key(env_file)
    if args.limpar:
        with MongoClient(connection, serverSelectionTimeoutMS=10_000) as client:
            return clean_run(api, key, client[database], args.limpar)

    run_id = time.strftime("%m%d%H%M%S")
    log(f"Execução {run_id} | API {args.api} | banco {database}")

    world = None
    try:
        with MongoClient(connection, serverSelectionTimeoutMS=10_000) as client:
            db = client[database]
            users = db[verificar_usuario.USERS_COLLECTION]
            driver_id = ""
            if args.motorista:
                driver = verificar_usuario.find_user(users, args.motorista)
                if driver is None:
                    log(f"Não achei a conta {args.motorista} no banco {database}.")
                    return 2
                driver_id = str(driver["_id"])

            world = SimulationWorld(api, key, users, run_id, date.today())
            bots = create_bots(world)
            phase_one(world, db, bots, driver_id, started, args.janela, args.pagar_depois)
            phase_two(world, bots, args.minutos, args.pagar_depois)
            log("Teste concluído.")
            return 0
    except SimulationFailure as failure:
        log(f"FALHOU: {failure}")
        return 1
    except PyMongoError as error:
        log(f"Erro no MongoDB: {error}")
        return 2
    except KeyboardInterrupt:
        log("Interrompido.")
        return 130
    finally:
        if world and world.created_users:
            print(f"\nContas dos robôs (senha: {PASSWORD}):")
            for user in world.created_users:
                print(f"  {user.nickname:6} {user.email}")
            print(f"Para apagar: python3 scripts/teste_no_app.py --limpar {run_id}")


if __name__ == "__main__":
    sys.exit(main())
