import '../../domain/models/game_chat_message.dart';

class GameChatMessageModel extends GameChatMessage {
  const GameChatMessageModel({
    required super.id,
    required super.gameId,
    required super.teamId,
    required super.channel,
    required super.senderId,
    required super.senderEmail,
    required super.text,
    required super.createdAt,
  });

  factory GameChatMessageModel.fromJson(Map<String, dynamic> json) {
    return GameChatMessageModel(
      id: json['id'] as int? ?? 0,
      gameId: json['gameId'] as int? ?? 0,
      teamId: json['teamId'] as int? ?? 0,
      channel: json['channel'] as String? ?? '',
      senderId: json['senderId'] as int? ?? 0,
      senderEmail: json['senderEmail'] as String? ?? '',
      text: json['text'] as String? ?? '',
      createdAt: DateTime.tryParse(json['createdAt'] as String? ?? ''),
    );
  }
}
