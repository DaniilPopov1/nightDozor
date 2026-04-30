class GameChatMessage {
  const GameChatMessage({
    required this.id,
    required this.gameId,
    required this.teamId,
    required this.channel,
    required this.senderId,
    required this.senderEmail,
    required this.text,
    required this.createdAt,
  });

  final int id;
  final int gameId;
  final int teamId;
  final String channel;
  final int senderId;
  final String senderEmail;
  final String text;
  final DateTime? createdAt;
}
