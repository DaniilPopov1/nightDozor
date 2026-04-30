import '../../domain/models/game_team_progress.dart';

class GameTeamProgressModel extends GameTeamProgress {
  const GameTeamProgressModel({
    required super.gameId,
    required super.gameTitle,
    required super.teamId,
    required super.teamName,
    required super.currentPlace,
    required super.completedTasksCount,
    required super.totalTasksCount,
    required super.totalPenaltyMinutes,
    required super.elapsedSeconds,
    required super.totalScoreSeconds,
    required super.sessionStatus,
    required super.startedAt,
    required super.finishedAt,
  });

  factory GameTeamProgressModel.fromJson(Map<String, dynamic> json) {
    return GameTeamProgressModel(
      gameId: json['gameId'] as int? ?? 0,
      gameTitle: json['gameTitle'] as String? ?? '',
      teamId: json['teamId'] as int? ?? 0,
      teamName: json['teamName'] as String? ?? '',
      currentPlace: json['currentPlace'] as int? ?? 0,
      completedTasksCount: json['completedTasksCount'] as int? ?? 0,
      totalTasksCount: json['totalTasksCount'] as int? ?? 0,
      totalPenaltyMinutes: json['totalPenaltyMinutes'] as int?,
      elapsedSeconds: json['elapsedSeconds'] as int? ?? 0,
      totalScoreSeconds: json['totalScoreSeconds'] as int? ?? 0,
      sessionStatus: json['sessionStatus'] as String? ?? '',
      startedAt: DateTime.tryParse(json['startedAt'] as String? ?? ''),
      finishedAt: DateTime.tryParse(json['finishedAt'] as String? ?? ''),
    );
  }
}
