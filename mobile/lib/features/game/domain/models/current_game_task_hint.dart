class CurrentGameTaskHint {
  const CurrentGameTaskHint({
    required this.id,
    required this.orderIndex,
    required this.text,
    required this.unlockedAt,
  });

  final int id;
  final int? orderIndex;
  final String text;
  final DateTime? unlockedAt;
}
