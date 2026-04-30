import '../../domain/models/current_game_task_hint.dart';

class CurrentGameTaskHintModel extends CurrentGameTaskHint {
  const CurrentGameTaskHintModel({
    required super.id,
    required super.orderIndex,
    required super.text,
    required super.unlockedAt,
  });

  factory CurrentGameTaskHintModel.fromJson(Map<String, dynamic> json) {
    return CurrentGameTaskHintModel(
      id: json['id'] as int? ?? 0,
      orderIndex: json['orderIndex'] as int?,
      text: json['text'] as String? ?? '',
      unlockedAt: DateTime.tryParse(json['unlockedAt'] as String? ?? ''),
    );
  }
}
