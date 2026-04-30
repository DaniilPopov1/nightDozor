import 'package:flutter_bloc/flutter_bloc.dart';
import 'dart:async';

import '../../../team/domain/models/team.dart';
import '../../../team/domain/repository/team_repository.dart';
import '../../data/datasource/game_chat_socket.dart';
import '../../domain/models/current_game_task.dart';
import '../../domain/models/game_chat_message.dart';
import '../../domain/models/game_overview.dart';
import '../../domain/repository/game_repository.dart';

class GameState {
  const GameState({
    this.isLoading = false,
    this.isSubmittingKey = false,
    this.isSendingTeamMessage = false,
    this.isSendingCaptainMessage = false,
    this.team,
    this.overview,
    this.currentTask,
    this.teamMessages = const [],
    this.captainMessages = const [],
    this.errorMessage,
    this.isCaptain = false,
  });

  final bool isLoading;
  final bool isSubmittingKey;
  final bool isSendingTeamMessage;
  final bool isSendingCaptainMessage;
  final Team? team;
  final GameOverview? overview;
  final CurrentGameTask? currentTask;
  final List<GameChatMessage> teamMessages;
  final List<GameChatMessage> captainMessages;
  final String? errorMessage;
  final bool isCaptain;

  bool get hasTeam => team != null;

  bool get hasOverview => overview != null;

  bool get hasActiveGame => overview?.hasActiveGame ?? false;

  int? get currentGameId {
    if (overview == null) {
      return null;
    }

    if (overview!.hasActiveGame) {
      return overview!.progress!.gameId;
    }

    return overview!.registration!.gameId;
  }

  bool get canUseChats {
    if (overview == null) {
      return false;
    }

    if (overview!.hasActiveGame) {
      return true;
    }

    return overview!.registration!.registrationStatus.toUpperCase() == 'APPROVED';
  }

  bool get canUseCaptainChat => canUseChats && isCaptain;

  bool get canSubmitKey {
    return isCaptain &&
        currentTask != null &&
        currentTask!.sessionStatus.toUpperCase() == 'IN_PROGRESS';
  }

  GameState copyWith({
    bool? isLoading,
    bool? isSubmittingKey,
    bool? isSendingTeamMessage,
    bool? isSendingCaptainMessage,
    Team? team,
    bool clearTeam = false,
    GameOverview? overview,
    bool clearOverview = false,
    CurrentGameTask? currentTask,
    bool clearCurrentTask = false,
    List<GameChatMessage>? teamMessages,
    List<GameChatMessage>? captainMessages,
    String? errorMessage,
    bool clearErrorMessage = false,
    bool? isCaptain,
  }) {
    return GameState(
      isLoading: isLoading ?? this.isLoading,
      isSubmittingKey: isSubmittingKey ?? this.isSubmittingKey,
      isSendingTeamMessage: isSendingTeamMessage ?? this.isSendingTeamMessage,
      isSendingCaptainMessage:
          isSendingCaptainMessage ?? this.isSendingCaptainMessage,
      team: clearTeam ? null : (team ?? this.team),
      overview: clearOverview ? null : (overview ?? this.overview),
      currentTask:
          clearCurrentTask ? null : (currentTask ?? this.currentTask),
      teamMessages: teamMessages ?? this.teamMessages,
      captainMessages: captainMessages ?? this.captainMessages,
      errorMessage:
          clearErrorMessage ? null : (errorMessage ?? this.errorMessage),
      isCaptain: isCaptain ?? this.isCaptain,
    );
  }
}

class GameCubit extends Cubit<GameState> {
  GameCubit(
    this._gameRepository,
    this._teamRepository,
    this._gameChatSocket,
  ) : super(const GameState()) {
    _chatSubscription = _gameChatSocket.events.listen(_handleSocketEvent);
  }

  final GameRepository _gameRepository;
  final TeamRepository _teamRepository;
  final GameChatSocket _gameChatSocket;
  int? _currentUserId;
  StreamSubscription<GameChatSocketEvent>? _chatSubscription;

  Future<void> loadGame({
    required int currentUserId,
    bool silent = false,
  }) async {
    _currentUserId = currentUserId;

    if (!silent) {
      emit(state.copyWith(isLoading: true, clearErrorMessage: true));
    }

    try {
      final team = await _teamRepository.getCurrentTeam();
      if (team == null) {
        emit(
          state.copyWith(
            isLoading: false,
            clearTeam: true,
            clearOverview: true,
            clearCurrentTask: true,
            teamMessages: const [],
            captainMessages: const [],
            isCaptain: false,
            clearErrorMessage: true,
          ),
        );
        return;
      }

      final isCaptain = team.captainId == currentUserId;
      final overview = await _gameRepository.getCurrentGameOverview();
      final currentTask = overview?.hasActiveGame == true
          ? await _gameRepository.getCurrentTask()
          : null;

      final gameId = overview == null
          ? null
          : overview.hasActiveGame
              ? overview.progress!.gameId
              : overview.registration!.gameId;

      final canUseChats = overview != null &&
          (overview.hasActiveGame ||
              overview.registration!.registrationStatus.toUpperCase() ==
                  'APPROVED');

      final teamMessages = canUseChats && gameId != null
          ? await _gameRepository.getTeamChatMessages(gameId)
          : const <GameChatMessage>[];
      final captainMessages = canUseChats && gameId != null && isCaptain
          ? await _gameRepository.getCaptainOrganizerChatMessages(gameId)
          : const <GameChatMessage>[];

      await _gameChatSocket.disconnect();
      if (canUseChats && gameId != null) {
        await _gameChatSocket.subscribe(
          gameId: gameId,
          teamId: team.id,
          channel: 'TEAM',
        );

        if (isCaptain) {
          await _gameChatSocket.subscribe(
            gameId: gameId,
            teamId: team.id,
            channel: 'CAPTAIN_ORGANIZER',
          );
        }
      }

      emit(
        state.copyWith(
          isLoading: false,
          team: team,
          overview: overview,
          currentTask: currentTask,
          clearCurrentTask: currentTask == null,
          teamMessages: teamMessages,
          captainMessages: captainMessages,
          isCaptain: isCaptain,
          clearErrorMessage: true,
        ),
      );
    } catch (error) {
      await _gameChatSocket.disconnect();
      emit(
        state.copyWith(
          isLoading: false,
          errorMessage: error.toString().replaceFirst('Exception: ', ''),
        ),
      );
    }
  }

  Future<void> submitTaskKey(String answerKey) async {
    if (_currentUserId == null || !state.canSubmitKey) {
      throw Exception('Ввод ключа доступен только капитану во время активной игры');
    }

    emit(state.copyWith(isSubmittingKey: true, clearErrorMessage: true));

    try {
      await _gameRepository.submitTaskKey(answerKey);
      await loadGame(currentUserId: _currentUserId!, silent: true);
      emit(state.copyWith(isSubmittingKey: false));
    } catch (error) {
      emit(state.copyWith(isSubmittingKey: false));
      rethrow;
    }
  }

  Future<void> sendTeamMessage(String text) async {
    final gameId = state.currentGameId;
    final teamId = state.team?.id;
    if (gameId == null || teamId == null || !state.canUseChats) {
      throw Exception('Командный чат сейчас недоступен');
    }

    emit(state.copyWith(isSendingTeamMessage: true, clearErrorMessage: true));

    try {
      await _gameChatSocket.sendMessage(
        gameId: gameId,
        teamId: teamId,
        channel: 'TEAM',
        text: text,
      );
      emit(state.copyWith(isSendingTeamMessage: false));
    } catch (_) {
      emit(state.copyWith(isSendingTeamMessage: false));
      rethrow;
    }
  }

  Future<void> sendCaptainMessage(String text) async {
    final gameId = state.currentGameId;
    final teamId = state.team?.id;
    if (gameId == null || teamId == null || !state.canUseCaptainChat) {
      throw Exception('Чат капитана с организатором сейчас недоступен');
    }

    emit(
      state.copyWith(
        isSendingCaptainMessage: true,
        clearErrorMessage: true,
      ),
    );

    try {
      await _gameChatSocket.sendMessage(
        gameId: gameId,
        teamId: teamId,
        channel: 'CAPTAIN_ORGANIZER',
        text: text,
      );
      emit(state.copyWith(isSendingCaptainMessage: false));
    } catch (_) {
      emit(state.copyWith(isSendingCaptainMessage: false));
      rethrow;
    }
  }

  void _handleSocketEvent(GameChatSocketEvent event) {
    if (event is GameChatSocketMessage) {
      if (event.channel == 'TEAM') {
        final exists = state.teamMessages.any((item) => item.id == event.message.id);
        if (exists) {
          return;
        }

        emit(
          state.copyWith(
            teamMessages: [...state.teamMessages, event.message],
          ),
        );
        return;
      }

      if (event.channel == 'CAPTAIN_ORGANIZER') {
        final exists =
            state.captainMessages.any((item) => item.id == event.message.id);
        if (exists) {
          return;
        }

        emit(
          state.copyWith(
            captainMessages: [...state.captainMessages, event.message],
          ),
        );
      }
      return;
    }

    if (event is GameChatSocketError) {
      emit(state.copyWith(errorMessage: event.message));
    }
  }

  @override
  Future<void> close() async {
    await _chatSubscription?.cancel();
    await _gameChatSocket.disconnect();
    return super.close();
  }
}
