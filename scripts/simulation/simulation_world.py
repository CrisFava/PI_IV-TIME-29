from __future__ import annotations

import random
import re
import unicodedata
from datetime import date, datetime, timezone
from decimal import Decimal
from typing import Optional

import verificar_usuario
from simulation.api_client import ApiClient
from simulation.api_response import ApiResponse
from simulation.simulated_user import SimulatedUser
from simulation.simulation_failure import SimulationFailure

FIREBASE_AUTH_URL = "https://identitytoolkit.googleapis.com/v1"
PASSWORD = "Simulacao#2026"
WEEK_DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]
DEPARTURE_TIME = "07:30"
ARRIVAL_TIME = "08:10"

CAMPUS = ("PUC-Campinas Campus I", "Parque das Universidades", -22.8338, -47.0519)
DRIVER_ORIGIN = ("Av. Francisco Glicério, 1000", "Centro", -22.9064, -47.0616)
PASSENGER_ORIGINS = [
    ("Rua Coronel Quirino, 900", "Cambuí", -22.8955, -47.0525),
    ("Av. Heitor Penteado, 1200", "Taquaral", -22.8732, -47.0558),
    ("Rua Dr. Sampaio Ferraz, 500", "Guanabara", -22.8890, -47.0700),
]


class SimulationWorld:
    """
    Monta o "mundo" de um cenário pela API de verdade: contas no Firebase, cadastro, aprovação
    da CNH e do vínculo acadêmico no Mongo, carro e trajetos. Também guarda os atalhos que os
    cenários usam para agir como cada pessoa e conferir o resultado.
    """

    def __init__(self, api: ApiClient, firebase_api_key: str, users_collection, run_id: str,
                 ride_date: date):
        self.api = api
        self.firebase_api_key = firebase_api_key
        self.users_collection = users_collection
        self.run_id = run_id
        self.ride_date = ride_date
        self.week_day = WEEK_DAYS[ride_date.weekday()]
        self.created_users: list[SimulatedUser] = []
        self._sequence = 0

    # ------------------------------------------------------------------ narração e conferência

    @staticmethod
    def step(description: str) -> None:
        print(f"    · {description}")

    @staticmethod
    def check(condition: bool, message: str) -> None:
        if not condition:
            raise SimulationFailure(message)

    @staticmethod
    def expect_ok(response: ApiResponse, what: str):
        if not response.ok:
            raise SimulationFailure(f"{what}: esperava sucesso, veio {response.describe()}")
        return response.data

    @staticmethod
    def expect_error(response: ApiResponse, what: str, code: Optional[str] = None) -> None:
        if response.ok:
            raise SimulationFailure(f"{what}: esperava erro{f' {code}' if code else ''}, mas deu certo")
        if code and response.error_code != code:
            raise SimulationFailure(f"{what}: esperava {code}, veio {response.describe()}")
        print(f"      recusado como esperado ({response.describe()})")

    # ------------------------------------------------------------------ pessoas

    def create_driver(self, nickname: str) -> SimulatedUser:
        driver = self._create_user(nickname, is_driver=True)
        vehicle = self.expect_ok(self.api.post("/vehicles", driver.token, {
            "nickname": "Carro da simulação",
            "model": "Volkswagen Gol",
            "licensePlate": f"SIM{random.randint(0, 9)}{random.choice('ABCDEFGHJ')}{random.randint(10, 99)}",
            "color": "Prata",
            "type": "HATCH",
            "totalCapacity": 4,
            "fuelSources": [{"energySource": "GASOLINE", "averageConsumption": 12.0, "currentEnergyPrice": 6.29}],
        }), f"cadastrar o carro de {nickname}")
        driver.vehicle_id = vehicle["id"]
        return driver

    def create_passenger(self, nickname: str) -> SimulatedUser:
        return self._create_user(nickname, is_driver=False)

    def _create_user(self, nickname: str, is_driver: bool) -> SimulatedUser:
        self._sequence += 1
        email = f"sim.{self.run_id}.{self._sequence}.{_slug(nickname)}@converge-sim.test"

        # O Firebase responde o JSON direto, sem o envelope {success, data} do backend.
        response = self.api.call(
            "POST", f"{FIREBASE_AUTH_URL}/accounts:signUp?key={self.firebase_api_key}",
            body={"email": email, "password": PASSWORD, "returnSecureToken": True},
        )
        account = response.body or {}
        if not response.ok or "idToken" not in account:
            raise SimulationFailure(f"criar a conta de {nickname} no Firebase: {response.describe()}")
        user = SimulatedUser(nickname, email, PASSWORD, account["localId"], account["idToken"])
        self.created_users.append(user)

        body = {
            "firebaseUid": user.firebase_uid,
            "name": f"[SIM] {nickname}",
            "email": email,
            "phone": "(19) 99999-0000",
            "participationRoles": ["PASSENGER", "DRIVER"] if is_driver else ["PASSENGER"],
            "academicRecord": {
                "university": "PUC-Campinas",
                "course": "Engenharia de Software",
                "enrollmentNumber": f"SIM{self.run_id}{self._sequence:02d}",
                "institutionalEmail": email,
            },
        }
        if is_driver:
            body["driverLicense"] = {"number": f"{random.randint(10**10, 10**11 - 1)}", "category": "B",
                                     "expiresAt": "2031-01-01T00:00:00"}
            body["pixKey"] = email
            body["pixKeyType"] = "EMAIL"

        user.id = self.expect_ok(self.api.post("/users", user.token, body), f"cadastrar {nickname}")["id"]
        self._approve_documents(user, is_driver)
        return user

    def _approve_documents(self, user: SimulatedUser, is_driver: bool) -> None:
        document = verificar_usuario.find_user(self.users_collection, user.email)
        self.check(document is not None,
                   f"{user.nickname} não apareceu no Mongo: o script e o backend estão no mesmo banco?")

        changes, warnings = verificar_usuario.build_update(
            document, None if is_driver else "estudante", datetime.now(timezone.utc))
        self.check(not warnings, f"não deu para aprovar {user.nickname}: {' '.join(warnings)}")
        self.users_collection.update_one({"_id": document["_id"]}, {"$set": changes})

    def delete_firebase_accounts(self) -> list:
        """Apaga as contas criadas nesta execução e devolve os e-mails que não deu para apagar."""
        leftovers = []
        for user in self.created_users:
            try:
                response = self.api.call("POST", f"{FIREBASE_AUTH_URL}/accounts:delete?key={self.firebase_api_key}",
                                         body={"idToken": user.token})
                if not response.ok:
                    leftovers.append(user.email)
            except SimulationFailure:
                leftovers.append(user.email)
        return leftovers

    def delete_accounts_of_run(self, run_id: str) -> tuple:
        """Apaga as contas de uma execução anterior (que caiu antes de limpar), achadas pelo Mongo."""
        prefix = re.compile(f"^sim\\.{re.escape(run_id)}\\.")
        deleted, leftovers = [], []

        for document in self.users_collection.find({"email": prefix}, {"email": 1}):
            email = document["email"]
            try:
                login = self.api.call("POST", f"{FIREBASE_AUTH_URL}/accounts:signInWithPassword"
                                              f"?key={self.firebase_api_key}",
                                      body={"email": email, "password": PASSWORD, "returnSecureToken": True})
                token = (login.body or {}).get("idToken")
                if not token:
                    # EMAIL_NOT_FOUND: a conta já tinha sido apagada no fim daquela execução.
                    continue
                response = self.api.call("POST", f"{FIREBASE_AUTH_URL}/accounts:delete?key={self.firebase_api_key}",
                                         body={"idToken": token})
                (deleted if response.ok else leftovers).append(email)
            except SimulationFailure:
                leftovers.append(email)

        return deleted, leftovers

    # ------------------------------------------------------------------ trajetos e pedidos

    def offer_route(self, driver: SimulatedUser, seats: int) -> str:
        return self._create_route(driver, {
            "type": "OFFER",
            "vehicleId": driver.vehicle_id,
            "origin": _place(DRIVER_ORIGIN),
            "maxDetourKm": 5.0,
            "totalSeats": seats,
            "toll": 0,
            "parking": 0,
        })

    def search_route(self, passenger: SimulatedUser) -> str:
        origin = PASSENGER_ORIGINS[self._sequence % len(PASSENGER_ORIGINS)]
        return self._create_route(passenger, {"type": "SEARCH", "origin": _place(origin)})

    def _create_route(self, user: SimulatedUser, fields: dict) -> str:
        body = {
            "destination": _place(CAMPUS),
            "departureTime": DEPARTURE_TIME,
            "estimatedArrivalTime": ARRIVAL_TIME,
            "weekDays": [self.week_day],
            **fields,
        }
        route = self.expect_ok(self.api.post("/routes", user.token, body), f"criar o trajeto de {user.nickname}")
        return route["id"]

    def request_seat(self, passenger: SimulatedUser, driver_route_id: str, passenger_route_id: str) -> ApiResponse:
        return self.api.post("/rides/request", passenger.token, {
            "driverRouteId": driver_route_id,
            "passengerRouteId": passenger_route_id,
            "date": self.ride_date.isoformat(),
        })

    # ------------------------------------------------------------------ viagem

    def ride(self, user: SimulatedUser, ride_id: str) -> dict:
        return self.expect_ok(self.api.get(f"/rides/{ride_id}", user.token),
                              f"{user.nickname} consultar a viagem")

    def ride_action(self, user: SimulatedUser, ride_id: str, action: str,
                    body: Optional[dict] = None) -> ApiResponse:
        return self.api.post(f"/rides/{ride_id}/{action}", user.token, body)

    def passenger_action(self, driver: SimulatedUser, ride_id: str, passenger: SimulatedUser,
                         action: str, body: Optional[dict] = None) -> ApiResponse:
        return self.api.post(f"/rides/{ride_id}/passengers/{passenger.id}/{action}", driver.token, body)

    def payment(self, user: SimulatedUser, ride_id: str, passenger: SimulatedUser,
                action: str = "", body: Optional[dict] = None) -> ApiResponse:
        path = f"/rides/{ride_id}/passengers/{passenger.id}/payment" + (f"/{action}" if action else "")
        if action:
            return self.api.post(path, user.token, body)
        return self.api.get(path, user.token)

    @staticmethod
    def participant(ride: dict, user: SimulatedUser) -> dict:
        for participant in ride.get("participants") or []:
            if participant.get("userId") == user.id:
                return participant
        raise SimulationFailure(f"{user.nickname} não aparece entre os participantes da viagem")

    def expect_ride(self, ride: dict, status: str, available_seats: Optional[int] = None) -> None:
        self.check(ride.get("status") == status,
                   f"a viagem deveria estar {status}, está {ride.get('status')}")
        if available_seats is not None:
            self.check(ride.get("availableSeats") == available_seats,
                       f"deveria ter {available_seats} vaga(s) livre(s), tem {ride.get('availableSeats')}")

    def expect_participant(self, ride: dict, user: SimulatedUser, status: str) -> dict:
        participant = self.participant(ride, user)
        self.check(participant.get("status") == status,
                   f"{user.nickname} deveria estar {status}, está {participant.get('status')}")
        return participant

    @staticmethod
    def charge_for(participant: dict) -> Decimal:
        """Valor a cobrar: o final calculado, ou um valor fixo dentro do teto se ele não vier."""
        final = Decimal(str(participant.get("finalAmount") or 0))
        if final > 0:
            return final
        ceiling = Decimal(str(participant.get("maxAmount") or 0))
        return min(Decimal("10.00"), ceiling) if ceiling > 0 else Decimal("10.00")


def _place(spot: tuple) -> dict:
    address, neighborhood, latitude, longitude = spot
    return {"address": address, "neighborhood": neighborhood, "city": "Campinas",
            "latitude": latitude, "longitude": longitude}


def _slug(text: str) -> str:
    ascii_text = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]+", "-", ascii_text.lower()).strip("-")
