class GameTeamProgress {
  const GameTeamProgress({
    required this.gameId,
    required this.gameTitle,
    required this.teamId,
    required this.teamName,
    required this.currentPlace,
    required this.completedTasksCount,
    required this.totalTasksCount,
    required this.totalPenaltyMinutes,
    required this.elapsedSeconds,
    required this.totalScoreSeconds,
    required this.sessionStatus,
    required this.startedAt,
    required this.finishedAt,
  });

  final int gameId;
  final String gameTitle;
  final int teamId;
  final String teamName;
  final int currentPlace;
  final int completedTasksCount;
  final int totalTasksCount;
  final int? totalPenaltyMinutes;
  final int elapsedSeconds;
  final int totalScoreSeconds;
  final String sessionStatus;
  final DateTime? startedAt;
  final DateTime? finishedAt;
}
