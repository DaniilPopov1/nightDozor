import '../models/current_game_task.dart';
import '../models/game_overview.dart';
import '../models/game_chat_message.dart';
import '../models/submit_task_key_result.dart';

abstract class GameRepository {
  Future<GameOverview?> getCurrentGameOverview();

  Future<CurrentGameTask?> getCurrentTask();

  Future<SubmitTaskKeyResult> submitTaskKey(String answerKey);

  Future<List<GameChatMessage>> getTeamChatMessages(int gameId);

  Future<GameChatMessage> sendTeamChatMessage(int gameId, String text);

  Future<List<GameChatMessage>> getCaptainOrganizerChatMessages(int gameId);

  Future<GameChatMessage> sendCaptainOrganizerChatMessage(int gameId, String text);
}
