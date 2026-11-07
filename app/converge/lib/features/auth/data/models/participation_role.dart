// Autor: Cristian Eduardo Fava - RA 25000636

enum ParticipationRole {
  passenger,
  driver;

  String toJson() => switch (this) {
    passenger => 'PASSENGER',
    driver => 'DRIVER',
  };

  static ParticipationRole? fromJson(String? value) {
    if (value == null) return null;
    final normalized = value.trim().toUpperCase();
    for (final role in values) {
      if (role.toJson() == normalized) return role;
    }
    return null;
  }
}
