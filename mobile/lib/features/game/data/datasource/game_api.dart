import 'package:dio/dio.dart';

import '../models/current_game_task_model.dart';
import '../models/game_chat_message_model.dart';
import '../models/game_team_progress_model.dart';
import '../models/submit_task_key_result_model.dart';
import '../models/team_game_registration_model.dart';

class GameApi {
  GameApi(this._dio);

  final Dio _dio;

  Future<GameTeamProgressModel> getMyTeamProgress() async {
    final response = await _dio.get<Map<String, dynamic>>('/games/progress/my-team');
    final data = response.data ?? <String, dynamic>{};
    return GameTeamProgressModel.fromJson(data);
  }

  Future<List<TeamGameRegistrationModel>> getMyTeamRegistrations() async {
    final response = await _dio.get<List<dynamic>>('/games/registrations/my-team');
    final data = response.data ?? const [];

    return data
        .whereType<Map<String, dynamic>>()
        .map(TeamGameRegistrationModel.fromJson)
        .toList(growable: false);
  }

  Future<CurrentGameTaskModel> getCurrentTask() async {
    final response = await _dio.get<Map<String, dynamic>>('/games/current-task');
    final data = response.data ?? <String, dynamic>{};
    return CurrentGameTaskModel.fromJson(data);
  }

  Future<SubmitTaskKeyResultModel> submitTaskKey(String answerKey) async {
    final response = await _dio.post<Map<String, dynamic>>(
      '/games/current-task/submit-key',
      data: {'key': answerKey},
    );
    final data = response.data ?? <String, dynamic>{};
    return SubmitTaskKeyResultModel.fromJson(data);
  }

  Future<List<GameChatMessageModel>> getTeamChatMessages(int gameId) async {
    final response = await _dio.get<List<dynamic>>('/games/$gameId/chats/team/messages');
    final data = response.data ?? const [];

    return data
        .whereType<Map<String, dynamic>>()
        .map(GameChatMessageModel.fromJson)
        .toList(growable: false);
  }

  Future<GameChatMessageModel> sendTeamChatMessage(int gameId, String text) async {
    final response = await _dio.post<Map<String, dynamic>>(
      '/games/$gameId/chats/team/messages',
      data: {'text': text},
    );
    final data = response.data ?? <String, dynamic>{};
    return GameChatMessageModel.fromJson(data);
  }

  Future<List<GameChatMessageModel>> getCaptainOrganizerChatMessages(int gameId) async {
    final response = await _dio.get<List<dynamic>>(
      '/games/$gameId/chats/captain-organizer/messages',
    );
    final data = response.data ?? const [];

    return data
        .whereType<Map<String, dynamic>>()
        .map(GameChatMessageModel.fromJson)
        .toList(growable: false);
  }

  Future<GameChatMessageModel> sendCaptainOrganizerChatMessage(
    int gameId,
    String text,
  ) async {
    final response = await _dio.post<Map<String, dynamic>>(
      '/games/$gameId/chats/captain-organizer/messages',
      data: {'text': text},
    );
    final data = response.data ?? <String, dynamic>{};
    return GameChatMessageModel.fromJson(data);
  }
}
