// Autor: Cristian Eduardo Fava - RA 25000636

/// Identifica um passageiro dentro de uma viagem. É a chave do pagamento
/// (`/rides/{rideId}/passengers/{passengerId}/payment`).
typedef RidePassengerRef = ({String rideId, String passengerId});
