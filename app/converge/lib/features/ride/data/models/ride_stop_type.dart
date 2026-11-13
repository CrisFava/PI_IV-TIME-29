// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

/// O que acontece numa parada do itinerário: buscar ou deixar um passageiro.
enum RideStopType {
  pickup,
  dropOff;

  static RideStopType fromJson(Object? value) =>
      value == 'DROPOFF' ? RideStopType.dropOff : RideStopType.pickup;
}
