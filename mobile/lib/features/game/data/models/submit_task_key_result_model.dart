import '../../domain/models/submit_task_key_result.dart';

class SubmitTaskKeyResultModel extends SubmitTaskKeyResult {
  const SubmitTaskKeyResultModel({
    required super.sessionId,
    required super.teamId,
    required super.completedTaskId,
    required super.completedTaskTitle,
    required super.completedOrderIndex,
    required super.totalPenaltyMinutes,
    required super.gameSessionFinished,
    required super.sessionStatus,
    required super.nextTaskId,
    required super.nextTaskTitle,
    required super.nextOrderIndex,
    required super.submittedAt,
  });

  factory SubmitTaskKeyResultModel.fromJson(Map<String, dynamic> json) {
    return SubmitTaskKeyResultModel(
      sessionId: json['sessionId'] as int? ?? 0,
      teamId: json['teamId'] as int? ?? 0,
      completedTaskId: json['completedTaskId'] as int?,
      completedTaskTitle: json['completedTaskTitle'] as String?,
      completedOrderIndex: json['completedOrderIndex'] as int?,
      totalPenaltyMinutes: json['totalPenaltyMinutes'] as int?,
      gameSessionFinished: json['gameSessionFinished'] as bool? ?? false,
      sessionStatus: json['sessionStatus'] as String? ?? '',
      nextTaskId: json['nextTaskId'] as int?,
      nextTaskTitle: json['nextTaskTitle'] as String?,
      nextOrderIndex: json['nextOrderIndex'] as int?,
      submittedAt: DateTime.tryParse(json['submittedAt'] as String? ?? ''),
    );
  }
}
