class TeamGameRegistration {
  const TeamGameRegistration({
    required this.registrationId,
    required this.gameId,
    required this.gameTitle,
    required this.gameCity,
    required this.gameStatus,
    required this.minTeamSize,
    required this.maxTeamSize,
    required this.startsAt,
    required this.registrationStatus,
    required this.createdAt,
    required this.updatedAt,
  });

  final int registrationId;
  final int gameId;
  final String gameTitle;
  final String gameCity;
  final String gameStatus;
  final int? minTeamSize;
  final int? maxTeamSize;
  final DateTime? startsAt;
  final String registrationStatus;
  final DateTime? createdAt;
  final DateTime? updatedAt;
}
