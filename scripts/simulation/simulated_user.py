from dataclasses import dataclass


@dataclass
class SimulatedUser:
    """Uma pessoa da simulação: conta no Firebase, cadastro no backend e, se dirige, o carro."""

    nickname: str
    email: str
    password: str
    firebase_uid: str
    token: str
    id: str = ""
    vehicle_id: str = ""
