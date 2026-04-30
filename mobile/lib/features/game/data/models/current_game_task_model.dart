import '../../domain/models/current_game_task.dart';
import 'current_game_task_hint_model.dart';

class CurrentGameTaskModel extends CurrentGameTask {
  const CurrentGameTaskModel({
    required super.sessionId,
    required super.gameId,
    required super.gameTitle,
    required super.teamId,
    required super.taskId,
    required super.taskTitle,
    required super.riddleText,
    required super.currentOrderIndex,
    required super.totalTasks,
    required super.timeLimitMinutes,
    required super.failurePenaltyMinutes,
    required super.totalPenaltyMinutes,
    required super.taskStartedAt,
    required super.taskDeadlineAt,
    required super.remainingSeconds,
    required super.sessionStatus,
    required super.taskProgressStatus,
    required super.availableHints,
  });

  factory CurrentGameTaskModel.fromJson(Map<String, dynamic> json) {
    final hintsJson = json['availableHints'] as List<dynamic>? ?? const [];

    return CurrentGameTaskModel(
      sessionId: json['sessionId'] as int? ?? 0,
      gameId: json['gameId'] as int? ?? 0,
      gameTitle: json['gameTitle'] as String? ?? '',
      teamId: json['teamId'] as int? ?? 0,
      taskId: json['taskId'] as int? ?? 0,
      taskTitle: json['taskTitle'] as String? ?? '',
      riddleText: json['riddleText'] as String? ?? '',
      currentOrderIndex: json['currentOrderIndex'] as int?,
      totalTasks: json['totalTasks'] as int? ?? 0,
      timeLimitMinutes: json['timeLimitMinutes'] as int?,
      failurePenaltyMinutes: json['failurePenaltyMinutes'] as int?,
      totalPenaltyMinutes: json['totalPenaltyMinutes'] as int?,
      taskStartedAt: DateTime.tryParse(json['taskStartedAt'] as String? ?? ''),
      taskDeadlineAt: DateTime.tryParse(json['taskDeadlineAt'] as String? ?? ''),
      remainingSeconds: json['remainingSeconds'] as int? ?? 0,
      sessionStatus: json['sessionStatus'] as String? ?? '',
      taskProgressStatus: json['taskProgressStatus'] as String? ?? '',
      availableHints: hintsJson
          .whereType<Map<String, dynamic>>()
          .map(CurrentGameTaskHintModel.fromJson)
          .toList(growable: false),
    );
  }
}
