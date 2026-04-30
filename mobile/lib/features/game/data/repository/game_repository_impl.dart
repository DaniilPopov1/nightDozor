import 'package:dio/dio.dart';

import '../../domain/models/current_game_task.dart';
import '../../domain/models/game_chat_message.dart';
import '../../domain/models/game_overview.dart';
import '../../domain/models/submit_task_key_result.dart';
import '../../domain/repository/game_repository.dart';
import '../datasource/game_api.dart';

class GameRepositoryImpl implements GameRepository {
  GameRepositoryImpl(this._gameApi);

  final GameApi _gameApi;

  @override
  Future<GameOverview?> getCurrentGameOverview() async {
    try {
      final progress = await _gameApi.getMyTeamProgress();
      return GameOverview.active(progress);
    } on DioException catch (error) {
      final statusCode = error.response?.statusCode;

      if (statusCode != 404) {
        throw Exception(_extractMessage(error) ?? 'Не удалось загрузить текущую игру');
      }
    } catch (_) {
      throw Exception('Не удалось загрузить текущую игру');
    }

    try {
      final registrations = await _gameApi.getMyTeamRegistrations();
      if (registrations.isEmpty) {
        return null;
      }

      return GameOverview.registration(registrations.first);
    } on DioException catch (error) {
      final statusCode = error.response?.statusCode;

      if (statusCode == 404) {
        return null;
      }

      throw Exception(_extractMessage(error) ?? 'Не удалось загрузить текущую игру');
    } catch (_) {
      throw Exception('Не удалось загрузить текущую игру');
    }
  }

  @override
  Future<CurrentGameTask?> getCurrentTask() async {
    try {
      return await _gameApi.getCurrentTask();
    } on DioException catch (error) {
      if (error.response?.statusCode == 404) {
        return null;
      }

      throw Exception(_extractMessage(error) ?? 'Не удалось загрузить текущее задание');
    } catch (_) {
      throw Exception('Не удалось загрузить текущее задание');
    }
  }

  @override
  Future<SubmitTaskKeyResult> submitTaskKey(String answerKey) async {
    try {
      return await _gameApi.submitTaskKey(answerKey);
    } on DioException catch (error) {
      throw Exception(_extractMessage(error) ?? 'Не удалось отправить ключ');
    } catch (_) {
      throw Exception('Не удалось отправить ключ');
    }
  }

  @override
  Future<List<GameChatMessage>> getTeamChatMessages(int gameId) async {
    try {
      return await _gameApi.getTeamChatMessages(gameId);
    } on DioException catch (error) {
      if (error.response?.statusCode == 404) {
        return const [];
      }

      throw Exception(_extractMessage(error) ?? 'Не удалось загрузить командный чат');
    } catch (_) {
      throw Exception('Не удалось загрузить командный чат');
    }
  }

  @override
  Future<GameChatMessage> sendTeamChatMessage(int gameId, String text) async {
    try {
      return await _gameApi.sendTeamChatMessage(gameId, text);
    } on DioException catch (error) {
      throw Exception(_extractMessage(error) ?? 'Не удалось отправить сообщение');
    } catch (_) {
      throw Exception('Не удалось отправить сообщение');
    }
  }

  @override
  Future<List<GameChatMessage>> getCaptainOrganizerChatMessages(int gameId) async {
    try {
      return await _gameApi.getCaptainOrganizerChatMessages(gameId);
    } on DioException catch (error) {
      if (error.response?.statusCode == 404) {
        return const [];
      }

      throw Exception(
        _extractMessage(error) ?? 'Не удалось загрузить чат капитана с организатором',
      );
    } catch (_) {
      throw Exception('Не удалось загрузить чат капитана с организатором');
    }
  }

  @override
  Future<GameChatMessage> sendCaptainOrganizerChatMessage(int gameId, String text) async {
    try {
      return await _gameApi.sendCaptainOrganizerChatMessage(gameId, text);
    } on DioException catch (error) {
      throw Exception(_extractMessage(error) ?? 'Не удалось отправить сообщение');
    } catch (_) {
      throw Exception('Не удалось отправить сообщение');
    }
  }

  String? _extractMessage(DioException error) {
    final data = error.response?.data;
    if (data is Map<String, dynamic>) {
      return data['error'] as String?;
    }

    return null;
  }
}
