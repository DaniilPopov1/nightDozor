class SubmitTaskKeyResult {
  const SubmitTaskKeyResult({
    required this.sessionId,
    required this.teamId,
    required this.completedTaskId,
    required this.completedTaskTitle,
    required this.completedOrderIndex,
    required this.totalPenaltyMinutes,
    required this.gameSessionFinished,
    required this.sessionStatus,
    required this.nextTaskId,
    required this.nextTaskTitle,
    required this.nextOrderIndex,
    required this.submittedAt,
  });

  final int sessionId;
  final int teamId;
  final int? completedTaskId;
  final String? completedTaskTitle;
  final int? completedOrderIndex;
  final int? totalPenaltyMinutes;
  final bool gameSessionFinished;
  final String sessionStatus;
  final int? nextTaskId;
  final String? nextTaskTitle;
  final int? nextOrderIndex;
  final DateTime? submittedAt;
}
