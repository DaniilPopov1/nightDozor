import '../../domain/models/team_game_registration.dart';

class TeamGameRegistrationModel extends TeamGameRegistration {
  const TeamGameRegistrationModel({
    required super.registrationId,
    required super.gameId,
    required super.gameTitle,
    required super.gameCity,
    required super.gameStatus,
    required super.minTeamSize,
    required super.maxTeamSize,
    required super.startsAt,
    required super.registrationStatus,
    required super.createdAt,
    required super.updatedAt,
  });

  factory TeamGameRegistrationModel.fromJson(Map<String, dynamic> json) {
    return TeamGameRegistrationModel(
      registrationId: json['registrationId'] as int? ?? 0,
      gameId: json['gameId'] as int? ?? 0,
      gameTitle: json['gameTitle'] as String? ?? '',
      gameCity: json['gameCity'] as String? ?? '',
      gameStatus: json['gameStatus'] as String? ?? '',
      minTeamSize: json['minTeamSize'] as int?,
      maxTeamSize: json['maxTeamSize'] as int?,
      startsAt: DateTime.tryParse(json['startsAt'] as String? ?? ''),
      registrationStatus: json['registrationStatus'] as String? ?? '',
      createdAt: DateTime.tryParse(json['createdAt'] as String? ?? ''),
      updatedAt: DateTime.tryParse(json['updatedAt'] as String? ?? ''),
    );
  }
}
