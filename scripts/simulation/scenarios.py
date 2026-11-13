"""
Cenários da simulação. Cada um cria as próprias pessoas e trajetos, então eles não dependem
uns dos outros e podem rodar sozinhos (--cenario).
"""

from __future__ import annotations

from decimal import Decimal

from simulation.simulation_world import SimulationWorld


def _ride_with_passengers(world: SimulationWorld, seats: int, *nicknames: str):
    """Motorista oferece o trajeto, cada passageiro pede vaga e é aceito. Devolve tudo pronto."""
    driver = world.create_driver("Motorista")
    driver_route = world.offer_route(driver, seats)
    passengers = []
    ride_id = ""

    for nickname in nicknames:
        passenger = world.create_passenger(nickname)
        route = world.search_route(passenger)
        world.step(f"{nickname} pede vaga")
        ride_id = world.expect_ok(world.request_seat(passenger, driver_route, route),
                                  f"{nickname} pedir vaga")["id"]
        world.step(f"Motorista aceita {nickname}")
        world.expect_ok(world.passenger_action(driver, ride_id, passenger, "accept"), f"aceitar {nickname}")
        passengers.append(passenger)

    return driver, passengers, ride_id


def happy_path(world: SimulationWorld) -> None:
    driver = world.create_driver("Motorista")
    driver_route = world.offer_route(driver, seats=3)
    ana = world.create_passenger("Ana")
    ana_route = world.search_route(ana)
    bruno = world.create_passenger("Bruno")
    bruno_route = world.search_route(bruno)

    world.step("Ana procura carona e o matching encontra o motorista")
    matches = world.expect_ok(world.api.get("/matching/passenger", ana.token, {"routeId": ana_route}),
                              "matching da Ana")
    world.check(any(match.get("driverRouteId") == driver_route for match in matches or []),
                "o matching não trouxe o trajeto do motorista para a Ana")

    world.step("Ana e Bruno pedem vaga na mesma viagem")
    ride_id = world.expect_ok(world.request_seat(ana, driver_route, ana_route), "Ana pedir vaga")["id"]
    same_ride = world.expect_ok(world.request_seat(bruno, driver_route, bruno_route), "Bruno pedir vaga")["id"]
    world.check(same_ride == ride_id, "Ana e Bruno caíram em viagens diferentes")

    world.step("Motorista aceita os dois")
    world.expect_ok(world.passenger_action(driver, ride_id, ana, "accept"), "aceitar Ana")
    world.expect_ok(world.passenger_action(driver, ride_id, bruno, "accept"), "aceitar Bruno")
    world.expect_ride(world.ride(driver, ride_id), "CONFIRMED", available_seats=1)

    world.step("Motorista inicia a viagem")
    world.expect_ok(world.ride_action(driver, ride_id, "start"), "iniciar")
    world.expect_ride(world.ride(ana, ride_id), "IN_PROGRESS")

    world.step("Motorista conclui a viagem")
    ride = world.expect_ok(world.ride_action(driver, ride_id, "complete"), "concluir")
    world.expect_ride(ride, "COMPLETED")
    world.check(ride.get("completedAt") is not None, "a viagem concluída veio sem completedAt")
    world.expect_participant(ride, driver, "COMPLETED")
    total_cost = Decimal(str((ride.get("costs") or {}).get("totalCost") or 0))
    if total_cost <= 0:
        print("      AVISO: custo total da viagem R$ 0. O RideCostHandler do servidor de sockets ainda "
              "devolve valores zerados (rateio não implementado); a cobrança abaixo usa R$ 10,00.")
    for passenger in (ana, bruno):
        participant = world.expect_participant(ride, passenger, "COMPLETED")
        final = Decimal(str(participant.get("finalAmount") or 0))
        ceiling = participant.get("maxAmount")
        world.check(ceiling is None or final <= Decimal(str(ceiling)),
                    f"o valor final de {passenger.nickname} ({final}) passou do teto ({ceiling})")
        print(f"      {passenger.nickname}: valor final R$ {final} (teto {ceiling})")

    world.step("Motorista cobra a Ana pelo PIX e confirma o recebimento")
    amount = world.charge_for(world.participant(ride, ana))
    charge = world.expect_ok(world.payment(driver, ride_id, ana, "request", {"amount": str(amount)}),
                             "cobrar Ana")
    world.check((charge.get("pixCopyPaste") or "").startswith("000201"),
                "a cobrança não trouxe um PIX Copia e Cola válido")
    seen_by_ana = world.expect_ok(world.payment(ana, ride_id, ana), "Ana ver a própria cobrança")
    world.check(seen_by_ana.get("status") == "PENDING", f"a cobrança da Ana deveria estar PENDING, está {seen_by_ana.get('status')}")
    paid = world.expect_ok(world.payment(driver, ride_id, ana, "confirm"), "confirmar pagamento da Ana")
    world.check(paid.get("status") == "PAID", f"o pagamento da Ana deveria estar PAID, está {paid.get('status')}")

    world.step("Ana avalia o motorista (e não consegue avaliar duas vezes)")
    review = {"rideId": ride_id, "reviewedId": driver.id, "rating": 5, "comment": "Simulação: tudo certo."}
    world.expect_ok(world.api.post("/reviews", ana.token, review), "Ana avaliar o motorista")
    world.expect_error(world.api.post("/reviews", ana.token, review), "avaliar de novo", "REVIEW_ALREADY_EXISTS")

    world.step("Indicadores do motorista contam a viagem")
    indicators = world.api.get("/indicators/me", driver.token)
    total = (indicators.data or {}).get("totalRides", 0) if indicators.ok else 0
    if total >= 1:
        print(f"      totalRides = {total}, sharedKm = {(indicators.data or {}).get('sharedKm')}")
    else:
        print(f"      AVISO: indicadores ainda sem a viagem ({indicators.describe()}). O "
              "CompletedRidesIndicatorReconciler reprocessa em até 15 min.")


def partial_no_show(world: SimulationWorld) -> None:
    driver, (ana, bruno), ride_id = _ride_with_passengers(world, 3, "Ana", "Bruno")

    world.step("Motorista inicia e marca o Bruno como ausente")
    world.expect_ok(world.ride_action(driver, ride_id, "start"), "iniciar")
    ride = world.expect_ok(world.passenger_action(driver, ride_id, bruno, "no-show"), "marcar ausência do Bruno")
    world.expect_participant(ride, bruno, "NO_SHOW")
    world.expect_ride(ride, "IN_PROGRESS", available_seats=2)

    world.step("Motorista conclui: Ana conclui, Bruno continua ausente e não paga")
    ride = world.expect_ok(world.ride_action(driver, ride_id, "complete"), "concluir")
    world.expect_ride(ride, "COMPLETED")
    world.expect_participant(ride, ana, "COMPLETED")
    absent = world.expect_participant(ride, bruno, "NO_SHOW")
    world.check(absent.get("paymentStatus") == "NOT_APPLICABLE",
                f"o ausente deveria ter pagamento NOT_APPLICABLE, tem {absent.get('paymentStatus')}")

    world.step("Cobrar o Bruno é recusado")
    world.expect_error(world.payment(driver, ride_id, bruno, "request", {"amount": "5.00"}),
                       "cobrar ausente", "BAD_REQUEST")


def everyone_absent(world: SimulationWorld) -> None:
    driver, (ana,), ride_id = _ride_with_passengers(world, 3, "Ana")

    world.step("Motorista inicia e a única passageira não aparece")
    world.expect_ok(world.ride_action(driver, ride_id, "start"), "iniciar")
    world.expect_ok(world.passenger_action(driver, ride_id, ana, "no-show"), "marcar ausência da Ana")

    world.step("Concluir sem ninguém presente encerra a viagem como CANCELLED")
    ride = world.expect_ok(world.ride_action(driver, ride_id, "complete"), "concluir")
    world.expect_ride(ride, "CANCELLED")
    world.check(ride.get("completedAt") is None, "viagem cancelada não deveria ter completedAt")


def invalid_transitions(world: SimulationWorld) -> None:
    driver, (ana,), ride_id = _ride_with_passengers(world, 3, "Ana")

    world.step("Concluir antes de iniciar")
    world.expect_error(world.ride_action(driver, ride_id, "complete"), "concluir antes de iniciar", "BAD_REQUEST")

    world.step("Avaliar antes de a viagem terminar")
    world.expect_error(world.api.post("/reviews", ana.token,
                                      {"rideId": ride_id, "reviewedId": driver.id, "rating": 5}),
                       "avaliar antes de concluir", "RIDE_NOT_COMPLETED")

    world.step("A passageira tenta iniciar a viagem")
    world.expect_error(world.ride_action(ana, ride_id, "start"), "passageira iniciar", "RIDE_NOT_FOUND")

    world.expect_ok(world.ride_action(driver, ride_id, "start"), "iniciar")

    world.step("Com a viagem em andamento: passageira tenta concluir, motorista tenta cancelar, passageira tenta sair")
    world.expect_error(world.ride_action(ana, ride_id, "complete"), "passageira concluir", "RIDE_NOT_FOUND")
    world.expect_error(world.ride_action(driver, ride_id, "cancel", {"reason": "Simulação"}),
                       "cancelar em andamento", "BAD_REQUEST")
    world.expect_error(world.ride_action(ana, ride_id, "leave", {"reason": "Simulação"}),
                       "sair em andamento", "BAD_REQUEST")

    world.expect_ok(world.ride_action(driver, ride_id, "complete"), "concluir")

    world.step("Depois de concluída: concluir de novo, iniciar de novo, marcar ausência")
    world.expect_error(world.ride_action(driver, ride_id, "complete"), "concluir duas vezes", "BAD_REQUEST")
    world.expect_error(world.ride_action(driver, ride_id, "start"), "iniciar concluída", "BAD_REQUEST")
    world.expect_error(world.passenger_action(driver, ride_id, ana, "no-show"), "ausência após concluir", "BAD_REQUEST")
    world.expect_ride(world.ride(driver, ride_id), "COMPLETED")


def leave_and_cancel(world: SimulationWorld) -> None:
    driver, (ana,), ride_id = _ride_with_passengers(world, 3, "Ana")

    world.step("Ana desiste antes da saída: a vaga volta e a viagem não pode iniciar")
    ride = world.expect_ok(world.ride_action(ana, ride_id, "leave", {"reason": "Simulação"}), "Ana sair")
    world.expect_participant(ride, ana, "CANCELLED")
    ride = world.ride(driver, ride_id)
    world.expect_ride(ride, "AWAITING_CONFIRMATION", available_seats=3)
    world.expect_error(world.ride_action(driver, ride_id, "start"), "iniciar sem passageiro", "BAD_REQUEST")

    world.step("Bruno entra na mesma viagem e o motorista cancela tudo")
    bruno = world.create_passenger("Bruno")
    world.expect_ok(world.request_seat(bruno, ride["driverRouteId"], world.search_route(bruno)), "Bruno pedir vaga")
    world.expect_ok(world.passenger_action(driver, ride_id, bruno, "accept"), "aceitar Bruno")
    ride = world.expect_ok(world.ride_action(driver, ride_id, "cancel", {"reason": "Simulação"}), "cancelar")
    world.expect_ride(ride, "CANCELLED")
    world.expect_participant(ride, bruno, "CANCELLED")
    world.expect_error(world.ride_action(driver, ride_id, "start"), "iniciar cancelada", "BAD_REQUEST")


def full_and_rejected(world: SimulationWorld) -> None:
    driver = world.create_driver("Motorista")
    driver_route = world.offer_route(driver, seats=1)
    ana = world.create_passenger("Ana")
    bruno = world.create_passenger("Bruno")
    carla = world.create_passenger("Carla")

    world.step("Bruno pede vaga e é recusado; pedir de novo na mesma viagem não vale")
    bruno_route = world.search_route(bruno)
    ride_id = world.expect_ok(world.request_seat(bruno, driver_route, bruno_route), "Bruno pedir vaga")["id"]
    ride = world.expect_ok(world.passenger_action(driver, ride_id, bruno, "reject", {"reason": "Simulação"}),
                           "recusar Bruno")
    world.expect_participant(ride, bruno, "REJECTED")
    world.expect_error(world.request_seat(bruno, driver_route, bruno_route), "Bruno pedir de novo")

    world.step("Ana ocupa a única vaga e Carla não consegue mais entrar")
    world.expect_ok(world.request_seat(ana, driver_route, world.search_route(ana)), "Ana pedir vaga")
    ride = world.expect_ok(world.passenger_action(driver, ride_id, ana, "accept"), "aceitar Ana")
    world.expect_ride(ride, "CONFIRMED", available_seats=0)
    world.expect_error(world.request_seat(carla, driver_route, world.search_route(carla)), "Carla pedir vaga lotada")


SCENARIOS = {
    "caminho-feliz": ("Pedido, aceite, início, conclusão, PIX, avaliação e indicadores", happy_path),
    "ausencia-parcial": ("Um passageiro falta: os presentes concluem e o ausente não paga", partial_no_show),
    "todos-ausentes": ("Ninguém aparece: concluir vira CANCELLED", everyone_absent),
    "transicoes-invalidas": ("Ações fora de hora ou feitas pela pessoa errada são recusadas", invalid_transitions),
    "desistencia-cancelamento": ("Passageiro desiste antes da saída e o motorista cancela", leave_and_cancel),
    "lotacao-recusa": ("Pedido recusado e carro lotado", full_and_rejected),
}
