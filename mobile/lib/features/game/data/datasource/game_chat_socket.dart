import 'dart:async';
import 'dart:convert';

import 'package:web_socket_channel/web_socket_channel.dart';

import '../../../../core/network/web_socket_url.dart';
import '../../../auth/data/datasource/auth_local_storage.dart';
import '../models/game_chat_message_model.dart';

class GameChatSocket {
  GameChatSocket(this._localStorage);

  final AuthLocalStorage _localStorage;
  WebSocketChannel? _channel;
  StreamSubscription<dynamic>? _subscription;
  final StreamController<GameChatSocketEvent> _events =
      StreamController<GameChatSocketEvent>.broadcast();
  final Set<String> _subscriptions = <String>{};

  Stream<GameChatSocketEvent> get events => _events.stream;

  Future<void> connect() async {
    if (_channel != null) {
      return;
    }

    final token = _localStorage.getToken();
    if (token == null || token.isEmpty) {
      throw Exception('Не найден токен авторизации для подключения чата');
    }

    final url = buildChatWebSocketUrl(token);
    _channel = WebSocketChannel.connect(Uri.parse(url));
    _subscription = _channel!.stream.listen(
      _handleRawEvent,
      onError: (error) {
        _events.add(GameChatSocketError('Ошибка соединения чата'));
      },
      onDone: () {
        _events.add(GameChatSocketClosed());
        _channel = null;
      },
    );
  }

  Future<void> disconnect() async {
    for (final key in _subscriptions.toList()) {
      final parts = key.split(':');
      if (parts.length != 3) {
        continue;
      }

      _send({
        'type': 'UNSUBSCRIBE',
        'gameId': int.tryParse(parts[0]) ?? 0,
        'teamId': int.tryParse(parts[1]) ?? 0,
        'channel': parts[2],
      });
    }

    _subscriptions.clear();
    await _subscription?.cancel();
    await _channel?.sink.close();
    _subscription = null;
    _channel = null;
  }

  Future<void> subscribe({
    required int gameId,
    required int teamId,
    required String channel,
  }) async {
    await connect();

    final key = _buildSubscriptionKey(gameId, teamId, channel);
    if (_subscriptions.contains(key)) {
      return;
    }

    _subscriptions.add(key);
    _send({
      'type': 'SUBSCRIBE',
      'gameId': gameId,
      'teamId': teamId,
      'channel': channel,
    });
  }

  Future<void> sendMessage({
    required int gameId,
    required int teamId,
    required String channel,
    required String text,
  }) async {
    await connect();
    _send({
      'type': 'SEND',
      'gameId': gameId,
      'teamId': teamId,
      'channel': channel,
      'text': text,
    });
  }

  void _send(Map<String, dynamic> payload) {
    _channel?.sink.add(jsonEncode(payload));
  }

  void _handleRawEvent(dynamic raw) {
    try {
      final decoded = jsonDecode(raw as String) as Map<String, dynamic>;
      final type = decoded['type'] as String? ?? '';
      final payload = decoded['payload'] as Map<String, dynamic>? ?? {};

      switch (type) {
        case 'MESSAGE':
          final messageJson = payload['message'] as Map<String, dynamic>?;
          if (messageJson == null) {
            return;
          }

          _events.add(
            GameChatSocketMessage(
              channel: payload['channel'] as String? ?? '',
              message: GameChatMessageModel.fromJson(messageJson),
            ),
          );
          return;
        case 'ERROR':
          _events.add(
            GameChatSocketError(
              payload['error'] as String? ?? 'Ошибка websocket-чата',
            ),
          );
          return;
        default:
          return;
      }
    } catch (_) {
      _events.add(GameChatSocketError('Не удалось обработать сообщение чата'));
    }
  }

  String _buildSubscriptionKey(int gameId, int teamId, String channel) {
    return '$gameId:$teamId:$channel';
  }

  Future<void> dispose() async {
    await disconnect();
    await _events.close();
  }
}

sealed class GameChatSocketEvent {}

class GameChatSocketMessage extends GameChatSocketEvent {
  GameChatSocketMessage({
    required this.channel,
    required this.message,
  });

  final String channel;
  final GameChatMessageModel message;
}

class GameChatSocketError extends GameChatSocketEvent {
  GameChatSocketError(this.message);

  final String message;
}

class GameChatSocketClosed extends GameChatSocketEvent {}
