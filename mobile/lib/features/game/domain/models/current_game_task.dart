import 'current_game_task_hint.dart';

class CurrentGameTask {
  const CurrentGameTask({
    required this.sessionId,
    required this.gameId,
    required this.gameTitle,
    required this.teamId,
    required this.taskId,
    required this.taskTitle,
    required this.riddleText,
    required this.currentOrderIndex,
    required this.totalTasks,
    required this.timeLimitMinutes,
    required this.failurePenaltyMinutes,
    required this.totalPenaltyMinutes,
    required this.taskStartedAt,
    required this.taskDeadlineAt,
    required this.remainingSeconds,
    required this.sessionStatus,
    required this.taskProgressStatus,
    required this.availableHints,
  });

  final int sessionId;
  final int gameId;
  final String gameTitle;
  final int teamId;
  final int taskId;
  final String taskTitle;
  final String riddleText;
  final int? currentOrderIndex;
  final int totalTasks;
  final int? timeLimitMinutes;
  final int? failurePenaltyMinutes;
  final int? totalPenaltyMinutes;
  final DateTime? taskStartedAt;
  final DateTime? taskDeadlineAt;
  final int remainingSeconds;
  final String sessionStatus;
  final String taskProgressStatus;
  final List<CurrentGameTaskHint> availableHints;
}
