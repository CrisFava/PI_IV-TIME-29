"""
Cenários da simulação. Cada um cria as próprias pessoas e trajetos, então eles não dependem
uns dos outros e podem rodar sozinhos (--cenario).
"""

from __future__ import annotations

from datetime import datetime, timezone
from decimal import Decimal

from simulation.simulation_world import SimulationWorld

# Origem a uns 90 km do campus (São Paulo): fora do raio de candidatos do matching
FAR_ORIGIN = ("Av. Paulista, 1000", "Bela Vista", -23.5614, -46.6559)
# Segundo motorista perto, saindo de outro ponto: o desvio muda e a compatibilidade fica diferente
SECOND_ORIGIN = ("Rua Sacramento, 300", "Centro", -22.9120, -47.0560)


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
    matches = world.expect_ok(world.api.get("/matchings", ana.token, {"routeId": ana_route}),
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


def matching_filters(world: SimulationWorld) -> None:
    near = world.create_driver("Motorista perto")
    near_route = world.offer_route(near, seats=3)
    second = world.create_driver("Segundo motorista perto")
    second_route = world.offer_route(second, seats=3, origin=SECOND_ORIGIN)
    far = world.create_driver("Motorista longe")
    far_route = world.offer_route(far, seats=3, origin=FAR_ORIGIN)
    blocked = world.create_driver("Motorista bloqueado")
    blocked_route = world.offer_route(blocked, seats=3)
    no_pix = world.create_driver("Motorista sem PIX")
    no_pix_route = world.offer_route(no_pix, seats=3)
    ana = world.create_passenger("Ana")
    ana_route = world.search_route(ana)

    database = world.users_collection.database
    world.step("Ana bloqueia um motorista e outro motorista fica sem chave PIX (direto no Mongo)")
    database["blocks"].insert_one({"userId": ana.id, "blockedUserId": blocked.id,
                                   "createdAt": datetime.now(timezone.utc)})
    database["users"].update_one({"email": no_pix.email}, {"$unset": {"pixKey": "", "pixKeyType": ""}})

    try:
        world.step("Ana procura carona: só o motorista perto, sem bloqueio e com PIX aparece")
        matches = world.expect_ok(world.api.get("/matchings", ana.token, {"routeId": ana_route}),
                                  "matching da Ana") or []
        found = {match.get("driverRouteId"): match for match in matches}
        world.check(near_route in found, "o motorista perto não apareceu para a Ana")
        world.check(second_route in found, "o segundo motorista perto não apareceu para a Ana")
        world.check(far_route not in found, "o motorista a 90 km apareceu para a Ana")
        world.check(blocked_route not in found, "o motorista bloqueado apareceu para a Ana")
        world.check(no_pix_route not in found, "o motorista sem chave PIX apareceu para a Ana")
        near_score = found[near_route].get("compatibilityScore") or 0
        second_score = found[second_route].get("compatibilityScore") or 0
        world.check(near_score != second_score,
                    f"os dois motoristas tiveram a mesma compatibilidade ({near_score}); a ordem não seria testada")
        print(f"      compatibilidade: motorista perto {near_score}, segundo motorista {second_score}")
        scores = [match.get("compatibilityScore") or 0 for match in matches]
        world.check(scores == sorted(scores, reverse=True), "o matching não veio do mais para o menos compatível")
        print(f"      {len(matches)} motorista(s) compatível(is) para a Ana")

        world.step("O motorista perto vê a Ana entre os passageiros compatíveis (N passageiros x 1 motorista)")
        passengers = world.expect_ok(world.api.get("/matchings", near.token, {"routeId": near_route}),
                                     "matching do motorista perto") or []
        world.check(any(match.get("passengerRouteId") == ana_route for match in passengers),
                    "a Ana não apareceu para o motorista perto")

        world.step("O bloqueio vale nos dois sentidos: o motorista bloqueado também não vê a Ana")
        passengers = world.expect_ok(world.api.get("/matchings", blocked.token, {"routeId": blocked_route}),
                                     "matching do motorista bloqueado") or []
        world.check(all(match.get("passengerRouteId") != ana_route for match in passengers),
                    "a Ana apareceu para o motorista que ela bloqueou")

        world.step("O motorista longe não vê a Ana")
        passengers = world.expect_ok(world.api.get("/matchings", far.token, {"routeId": far_route}),
                                     "matching do motorista longe") or []
        world.check(all(match.get("passengerRouteId") != ana_route for match in passengers),
                    "a Ana apareceu para o motorista a 90 km")
    finally:
        database["blocks"].delete_many({"userId": ana.id})


def itinerary_with_stops(world: SimulationWorld) -> None:
    driver = world.create_driver("Motorista")
    driver_route = world.offer_route(driver, seats=3)
    ana = world.create_passenger("Ana")
    bruno = world.create_passenger("Bruno")

    world.step("Ana e Bruno pedem vaga e o motorista aceita os dois")
    ride_id = world.expect_ok(world.request_seat(ana, driver_route, world.search_route(ana)), "Ana pedir vaga")["id"]
    world.expect_ok(world.request_seat(bruno, driver_route, world.search_route(bruno)), "Bruno pedir vaga")
    world.expect_ok(world.passenger_action(driver, ride_id, ana, "accept"), "aceitar Ana")
    ride = world.expect_ok(world.passenger_action(driver, ride_id, bruno, "accept"), "aceitar Bruno")

    world.step("O motorista vê o itinerário: busca os dois e depois deixa os dois")
    stops = ride.get("itinerary") or []
    types = [stop.get("type") for stop in stops]
    world.check(types == ["PICKUP", "PICKUP", "DROPOFF", "DROPOFF"],
                f"o itinerário deveria buscar os dois e depois deixar, veio {types}")
    world.check({stop.get("passengerId") for stop in stops} == {ana.id, bruno.id},
                "o itinerário não tem as paradas dos dois passageiros")
    distance = ride.get("itineraryDistanceKm")
    world.check(distance is not None and distance > 0, "o itinerário veio sem distância")
    print(f"      {len(stops)} paradas, {distance:.1f} km, "
          f"rota do Google: {'sim' if ride.get('itineraryPolyline') else 'não (calculada em linha reta)'}")

    world.step("A Ana vê só as próprias paradas, sem a rota que passa pela casa do Bruno")
    seen_by_ana = world.ride(ana, ride_id)
    own = seen_by_ana.get("itinerary") or []
    world.check(len(own) == 2 and all(stop.get("passengerId") == ana.id for stop in own),
                "a Ana viu paradas de outro passageiro")
    world.check(seen_by_ana.get("itineraryPolyline") is None, "a Ana recebeu a rota completa do motorista")

    world.step("O Bruno sai da carona e a rota deixa de passar por ele")
    world.expect_ok(world.ride_action(bruno, ride_id, "leave", {"reason": "Simulação"}), "Bruno sair")
    ride = world.ride(driver, ride_id)
    remaining = {stop.get("passengerId") for stop in ride.get("itinerary") or []}
    world.check(remaining == {ana.id}, f"depois da saída do Bruno, o itinerário deveria ter só a Ana: {remaining}")


SCENARIOS = {
    "itinerario": ("Rota com paradas: buscar e deixar cada passageiro aceito, e privacidade", itinerary_with_stops),
    "matching-filtros": ("Matching: proximidade, bloqueio nos dois sentidos, PIX e ordem por compatibilidade",
                         matching_filters),
    "caminho-feliz": ("Pedido, aceite, início, conclusão, PIX, avaliação e indicadores", happy_path),
    "ausencia-parcial": ("Um passageiro falta: os presentes concluem e o ausente não paga", partial_no_show),
    "todos-ausentes": ("Ninguém aparece: concluir vira CANCELLED", everyone_absent),
    "transicoes-invalidas": ("Ações fora de hora ou feitas pela pessoa errada são recusadas", invalid_transitions),
    "desistencia-cancelamento": ("Passageiro desiste antes da saída e o motorista cancela", leave_and_cancel),
    "lotacao-recusa": ("Pedido recusado e carro lotado", full_and_rejected),
}
