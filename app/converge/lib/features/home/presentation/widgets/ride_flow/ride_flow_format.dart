// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

/// Distância no formato brasileiro: `12,4`.
String formatRideKm(double value) =>
    value.toStringAsFixed(1).replaceAll('.', ',');

/// Tempo de viagem: `25 min`, `1 h` ou `1 h 10 min`.
String formatTripDuration(int minutes) {
  if (minutes < 60) return '$minutes min';
  final hours = minutes ~/ 60;
  final rest = minutes % 60;
  return rest == 0 ? '$hours h' : '$hours h $rest min';
}
