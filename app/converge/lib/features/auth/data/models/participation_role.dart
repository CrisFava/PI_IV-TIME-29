enum ParticipationRole {
  passenger,
  driver;

  String toJson() => switch (this) {
    passenger => 'PASSENGER',
    driver => 'DRIVER',
  };

  static ParticipationRole? fromJson(String? value) {
    for (final role in values) {
      if (role.toJson() == value) return role;
    }
    return null;
  }
}
