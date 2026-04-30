import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../app/theme/app_theme.dart';
import '../../../../shared/presentation/widgets/night_dozor_backdrop.dart';
import '../../../auth/presentation/bloc/auth_bloc.dart';
import '../../domain/models/current_game_task.dart';
import '../../domain/models/current_game_task_hint.dart';
import '../../domain/models/game_chat_message.dart';
import '../../domain/models/game_team_progress.dart';
import '../../domain/models/team_game_registration.dart';
import '../cubit/game_cubit.dart';

class GamePage extends StatefulWidget {
  const GamePage({super.key});

  @override
  State<GamePage> createState() => _GamePageState();
}

class _GamePageState extends State<GamePage> {
  final _keyController = TextEditingController();
  final _teamChatController = TextEditingController();
  final _captainChatController = TextEditingController();
  Timer? _refreshTimer;

  @override
  void initState() {
    super.initState();
    _refreshTimer = Timer.periodic(const Duration(seconds: 10), (_) {
      final authState = context.read<AuthBloc>().state;
      if (authState is Authenticated) {
        context.read<GameCubit>().loadGame(
              currentUserId: authState.user.id,
              silent: true,
            );
      }
    });
  }

  @override
  void dispose() {
    _refreshTimer?.cancel();
    _keyController.dispose();
    _teamChatController.dispose();
    _captainChatController.dispose();
    super.dispose();
  }

  Future<void> _refresh() async {
    final authState = context.read<AuthBloc>().state;
    if (authState is Authenticated) {
      await context.read<GameCubit>().loadGame(currentUserId: authState.user.id);
    }
  }

  Future<void> _submitKey() async {
    final text = _keyController.text.trim();
    if (text.isEmpty) {
      _showSnackBar('Введите ключ задания');
      return;
    }

    try {
      await context.read<GameCubit>().submitTaskKey(text);
      _keyController.clear();
      _showSnackBar('Ключ отправлен');
    } catch (error) {
      _showSnackBar(error.toString().replaceFirst('Exception: ', ''));
    }
  }

  Future<void> _sendTeamMessage() async {
    final text = _teamChatController.text.trim();
    if (text.isEmpty) {
      return;
    }

    try {
      await context.read<GameCubit>().sendTeamMessage(text);
      _teamChatController.clear();
    } catch (error) {
      _showSnackBar(error.toString().replaceFirst('Exception: ', ''));
    }
  }

  Future<void> _sendCaptainMessage() async {
    final text = _captainChatController.text.trim();
    if (text.isEmpty) {
      return;
    }

    try {
      await context.read<GameCubit>().sendCaptainMessage(text);
      _captainChatController.clear();
    } catch (error) {
      _showSnackBar(error.toString().replaceFirst('Exception: ', ''));
    }
  }

  void _showSnackBar(String message) {
    if (!mounted) {
      return;
    }

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final authState = context.watch<AuthBloc>().state;
    final currentUserId = authState is Authenticated ? authState.user.id : 0;

    return SafeArea(
      child: BlocConsumer<GameCubit, GameState>(
        listenWhen: (previous, current) =>
            previous.errorMessage != current.errorMessage &&
            current.errorMessage != null &&
            (current.hasTeam || current.hasOverview),
        listener: (context, state) {
          final message = state.errorMessage;
          if (message == null) {
            return;
          }

          _showSnackBar(message);
        },
        builder: (context, state) {
          return RefreshIndicator(
            color: AppTheme.accent,
            onRefresh: _refresh,
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 18, 16, 120),
              children: [
                GlassCard(
                  padding: const EdgeInsets.all(28),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const EyebrowText('Night Dozor'),
                      const SizedBox(height: 16),
                      Text('Игра', style: theme.textTheme.headlineMedium),
                    ],
                  ),
                ),
                const SizedBox(height: 18),
                if (state.isLoading)
                  const _LoadingCard(text: 'Загружаем игровое состояние...')
                else if (!state.hasTeam)
                  const _EmptyStateCard(
                    title: 'Команда не найдена',
                    text:
                        'Сейчас этот аккаунт не состоит в команде, поэтому игровой раздел недоступен.',
                  )
                else if (state.errorMessage != null && !state.hasOverview)
                  _ErrorCard(
                    title: 'Не удалось загрузить игру',
                    message: state.errorMessage!,
                    onRetry: _refresh,
                  )
                else ...[
                  _OverviewSection(state: state),
                  const SizedBox(height: 14),
                  _TaskSection(
                    state: state,
                    keyController: _keyController,
                    onSubmitKey: _submitKey,
                    onTimerExpired: _refresh,
                  ),
                  const SizedBox(height: 14),
                  _ChatSection(
                    title: 'Командный чат',
                    description: state.canUseChats
                        ? 'Все участники команды могут общаться здесь во время игры.'
                        : 'Чат станет доступен после подтверждения участия команды в игре.',
                    isVisible: true,
                    isEnabled: state.canUseChats,
                    currentUserId: currentUserId,
                    messages: state.teamMessages,
                    controller: _teamChatController,
                    isSending: state.isSendingTeamMessage,
                    onSend: _sendTeamMessage,
                    emptyText: 'В командном чате пока нет сообщений.',
                  ),
                  if (state.isCaptain) ...[
                    const SizedBox(height: 14),
                    _ChatSection(
                      title: 'Чат капитан ↔ организатор',
                      description: state.canUseCaptainChat
                          ? 'Канал связи капитана с организатором игры.'
                          : 'Чат капитана станет доступен после подтверждения участия команды.',
                      isVisible: true,
                      isEnabled: state.canUseCaptainChat,
                      currentUserId: currentUserId,
                      messages: state.captainMessages,
                      controller: _captainChatController,
                      isSending: state.isSendingCaptainMessage,
                      onSend: _sendCaptainMessage,
                      emptyText: 'В этом чате пока нет сообщений.',
                    ),
                  ],
                ],
              ],
            ),
          );
        },
      ),
    );
  }
}

class _OverviewSection extends StatelessWidget {
  const _OverviewSection({required this.state});

  final GameState state;

  @override
  Widget build(BuildContext context) {
    final overview = state.overview;
    if (overview == null) {
      return const _EmptyStateCard(
        title: 'Активной игры нет',
        text:
            'У команды пока нет активной игровой сессии и нет зарегистрированной игры, которую можно показать в мобильном клиенте.',
      );
    }

    if (overview.hasActiveGame) {
      return _ActiveGameView(progress: overview.progress!);
    }

    return _RegistrationGameView(registration: overview.registration!);
  }
}

class _TaskSection extends StatelessWidget {
  const _TaskSection({
    required this.state,
    required this.keyController,
    required this.onSubmitKey,
    required this.onTimerExpired,
  });

  final GameState state;
  final TextEditingController keyController;
  final Future<void> Function() onSubmitKey;
  final Future<void> Function() onTimerExpired;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final task = state.currentTask;

    if (!state.hasActiveGame) {
      final registration = state.overview?.registration;
      final status = registration == null
          ? null
          : _registrationStatusLabel(registration.registrationStatus);

      return GlassCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Текущее задание', style: theme.textTheme.titleLarge),
            const SizedBox(height: 12),
            Text(
              status == null
                  ? 'Задание появится здесь, когда команда будет допущена к игре и сессия начнётся.'
                  : 'Сейчас игровая сессия ещё не началась. Текущая заявка: $status.',
              style: theme.textTheme.bodyLarge,
            ),
          ],
        ),
      );
    }

    if (task == null) {
      final sessionStatus = state.overview?.progress?.sessionStatus ?? '';

      return GlassCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Текущее задание', style: theme.textTheme.titleLarge),
            const SizedBox(height: 12),
            Text(
              sessionStatus.toUpperCase() == 'FINISHED'
                  ? 'Игра завершена, активного задания больше нет.'
                  : 'Сейчас активное задание не найдено. Возможно, уровень уже завершился или сервер обновляет состояние игры.',
              style: theme.textTheme.bodyLarge,
            ),
          ],
        ),
      );
    }

    return Column(
      children: [
        GlassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Текущее задание', style: theme.textTheme.titleLarge),
              const SizedBox(height: 18),
              _InfoRow(
                label: 'Название',
                value: task.taskTitle,
              ),
              const SizedBox(height: 12),
              _InfoRow(
                label: 'Этап',
                value:
                    '${task.currentOrderIndex ?? 0}/${task.totalTasks}',
              ),
              const SizedBox(height: 12),
              _InfoRow(
                label: 'Статус',
                value: _taskStatusLabel(task.taskProgressStatus),
              ),
              const SizedBox(height: 12),
              _InfoRow(
                label: 'Лимит времени',
                value: task.timeLimitMinutes == null
                    ? 'Нет данных'
                    : '${task.timeLimitMinutes} мин',
              ),
              const SizedBox(height: 12),
              _InfoRow(
                label: 'Дедлайн',
                value: _formatDate(task.taskDeadlineAt),
              ),
              const SizedBox(height: 22),
              Text(
                task.riddleText,
                style: theme.textTheme.bodyLarge?.copyWith(
                  color: AppTheme.textPrimary,
                  height: 1.7,
                ),
              ),
              const SizedBox(height: 22),
              _CountdownCard(
                task: task,
                onExpired: onTimerExpired,
              ),
            ],
          ),
        ),
        const SizedBox(height: 14),
        _HintsCard(hints: task.availableHints),
        if (state.isCaptain) ...[
          const SizedBox(height: 14),
          _KeySubmitCard(
            controller: keyController,
            onSubmit: onSubmitKey,
            isSubmitting: state.isSubmittingKey,
            isEnabled: state.canSubmitKey,
          ),
        ],
      ],
    );
  }
}

class _ChatSection extends StatelessWidget {
  const _ChatSection({
    required this.title,
    required this.description,
    required this.isVisible,
    required this.isEnabled,
    required this.currentUserId,
    required this.messages,
    required this.controller,
    required this.isSending,
    required this.onSend,
    required this.emptyText,
  });

  final String title;
  final String description;
  final bool isVisible;
  final bool isEnabled;
  final int currentUserId;
  final List<GameChatMessage> messages;
  final TextEditingController controller;
  final bool isSending;
  final Future<void> Function() onSend;
  final String emptyText;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    if (!isVisible) {
      return const SizedBox.shrink();
    }

    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: theme.textTheme.titleLarge),
          const SizedBox(height: 10),
          Text(description, style: theme.textTheme.bodyMedium),
          const SizedBox(height: 18),
          if (!isEnabled)
            Text(
              'Этот чат пока недоступен.',
              style: theme.textTheme.bodyLarge,
            )
          else ...[
            if (messages.isEmpty)
              Padding(
                padding: const EdgeInsets.only(bottom: 16),
                child: Text(emptyText, style: theme.textTheme.bodyLarge),
              )
            else
              SizedBox(
                height: 280,
                child: ListView.separated(
                  itemCount: messages.length,
                  itemBuilder: (context, index) {
                    final message = messages[index];
                    return _ChatBubble(
                      message: message,
                      isMine: message.senderId == currentUserId,
                    );
                  },
                  separatorBuilder: (context, index) =>
                      const SizedBox(height: 10),
                ),
              ),
            const SizedBox(height: 16),
            Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Expanded(
                  child: TextField(
                    controller: controller,
                    minLines: 1,
                    maxLines: 4,
                    decoration: const InputDecoration(
                      labelText: 'Сообщение',
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                FilledButton(
                  onPressed: isSending ? null : onSend,
                  style: FilledButton.styleFrom(
                    minimumSize: const Size(64, 56),
                    padding: const EdgeInsets.symmetric(horizontal: 18),
                  ),
                  child: Icon(
                    isSending ? Icons.hourglass_top_rounded : Icons.send_rounded,
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

class _ActiveGameView extends StatelessWidget {
  const _ActiveGameView({required this.progress});

  final GameTeamProgress progress;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _HeroCard(
          title: progress.gameTitle,
          subtitle: 'Сейчас команда участвует в игре',
          chips: [
            _ChipData(
              label: _sessionStatusLabel(progress.sessionStatus),
              color: const Color(0x2234D399),
              textColor: const Color(0xFFBBF7D0),
            ),
            _ChipData(
              label:
                  '${progress.completedTasksCount}/${progress.totalTasksCount} этапов',
              color: const Color(0x22F59E0B),
              textColor: const Color(0xFFFDE68A),
            ),
          ],
        ),
        const SizedBox(height: 14),
        GlassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Текущее состояние',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 18),
              _InfoRow(label: 'Команда', value: progress.teamName),
              const SizedBox(height: 12),
              _InfoRow(
                label: 'Статус участия',
                value: _sessionStatusLabel(progress.sessionStatus),
              ),
              const SizedBox(height: 12),
              _InfoRow(label: 'Текущее место', value: '${progress.currentPlace}'),
              const SizedBox(height: 12),
              _InfoRow(
                label: 'Штрафные минуты',
                value: '${progress.totalPenaltyMinutes ?? 0}',
              ),
              const SizedBox(height: 12),
              _InfoRow(
                label: 'Старт игры',
                value: _formatDate(progress.startedAt),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _RegistrationGameView extends StatelessWidget {
  const _RegistrationGameView({required this.registration});

  final TeamGameRegistration registration;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _HeroCard(
          title: registration.gameTitle,
          subtitle: 'У команды есть заявка на участие в игре',
          chips: [
            _ChipData(
              label: _registrationStatusLabel(registration.registrationStatus),
              color: _registrationChipColor(registration.registrationStatus),
              textColor: _registrationChipTextColor(registration.registrationStatus),
            ),
            _ChipData(
              label: _gameStatusLabel(registration.gameStatus),
              color: const Color(0x221F9CF0),
              textColor: const Color(0xFFBFDBFE),
            ),
          ],
        ),
        const SizedBox(height: 14),
        GlassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Статус участия',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 18),
              _InfoRow(label: 'Название игры', value: registration.gameTitle),
              const SizedBox(height: 12),
              _InfoRow(
                label: 'Город',
                value: registration.gameCity.isEmpty
                    ? 'Не указан'
                    : registration.gameCity,
              ),
              const SizedBox(height: 12),
              _InfoRow(
                label: 'Статус заявки',
                value: _registrationStatusLabel(registration.registrationStatus),
              ),
              const SizedBox(height: 12),
              _InfoRow(
                label: 'Статус игры',
                value: _gameStatusLabel(registration.gameStatus),
              ),
              const SizedBox(height: 12),
              _InfoRow(
                label: 'Старт игры',
                value: _formatDate(registration.startsAt),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _HeroCard extends StatelessWidget {
  const _HeroCard({
    required this.title,
    required this.subtitle,
    required this.chips,
  });

  final String title;
  final String subtitle;
  final List<_ChipData> chips;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: theme.textTheme.headlineMedium),
          const SizedBox(height: 12),
          Text(subtitle, style: theme.textTheme.bodyLarge),
          const SizedBox(height: 18),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final chip in chips)
                _InfoChip(
                  label: chip.label,
                  color: chip.color,
                  textColor: chip.textColor,
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _CountdownCard extends StatefulWidget {
  const _CountdownCard({
    required this.task,
    required this.onExpired,
  });

  final CurrentGameTask task;
  final Future<void> Function() onExpired;

  @override
  State<_CountdownCard> createState() => _CountdownCardState();
}

class _CountdownCardState extends State<_CountdownCard> {
  Timer? _timer;
  late int _remainingSeconds;
  bool _expiredHandled = false;

  @override
  void initState() {
    super.initState();
    _remainingSeconds = widget.task.remainingSeconds;
    _startTimer();
  }

  @override
  void didUpdateWidget(covariant _CountdownCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.task.taskId != widget.task.taskId ||
        oldWidget.task.taskDeadlineAt != widget.task.taskDeadlineAt ||
        oldWidget.task.remainingSeconds != widget.task.remainingSeconds) {
      _expiredHandled = false;
      _remainingSeconds = widget.task.remainingSeconds;
      _timer?.cancel();
      _startTimer();
    }
  }

  void _startTimer() {
    _timer = Timer.periodic(const Duration(seconds: 1), (_) async {
      if (!mounted) {
        return;
      }

      if (_remainingSeconds <= 0) {
        _timer?.cancel();
        if (!_expiredHandled) {
          _expiredHandled = true;
          await widget.onExpired();
        }
        return;
      }

      setState(() {
        _remainingSeconds -= 1;
      });
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: const LinearGradient(
          colors: [Color(0x22F59E0B), Color(0x221F9CF0)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        border: Border.all(color: AppTheme.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Оставшееся время', style: theme.textTheme.bodyMedium),
          const SizedBox(height: 8),
          Text(
            _formatDuration(_remainingSeconds),
            style: theme.textTheme.headlineLarge?.copyWith(
              color: _remainingSeconds <= 60
                  ? const Color(0xFFFCA5A5)
                  : AppTheme.textPrimary,
            ),
          ),
        ],
      ),
    );
  }
}

class _HintsCard extends StatelessWidget {
  const _HintsCard({required this.hints});

  final List<CurrentGameTaskHint> hints;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Подсказки', style: theme.textTheme.titleLarge),
          const SizedBox(height: 16),
          if (hints.isEmpty)
            Text(
              'Пока подсказок нет. Они появятся здесь, когда станут доступны по времени.',
              style: theme.textTheme.bodyLarge,
            )
          else
            for (var i = 0; i < hints.length; i++) ...[
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: const Color(0x66121B2C),
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: AppTheme.border),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Подсказка ${hints[i].orderIndex ?? i + 1}',
                      style: theme.textTheme.titleMedium,
                    ),
                    const SizedBox(height: 8),
                    Text(hints[i].text, style: theme.textTheme.bodyLarge),
                    const SizedBox(height: 8),
                    Text(
                      'Открыта: ${_formatDate(hints[i].unlockedAt)}',
                      style: theme.textTheme.bodyMedium,
                    ),
                  ],
                ),
              ),
              if (i != hints.length - 1) const SizedBox(height: 12),
            ],
        ],
      ),
    );
  }
}

class _KeySubmitCard extends StatelessWidget {
  const _KeySubmitCard({
    required this.controller,
    required this.onSubmit,
    required this.isSubmitting,
    required this.isEnabled,
  });

  final TextEditingController controller;
  final Future<void> Function() onSubmit;
  final bool isSubmitting;
  final bool isEnabled;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Ввод ключа', style: theme.textTheme.titleLarge),
          const SizedBox(height: 10),
          Text(
            isEnabled
                ? 'Капитан может отправить найденный ключ для перехода на следующий этап.'
                : 'Сейчас отправка ключа недоступна.',
            style: theme.textTheme.bodyMedium,
          ),
          const SizedBox(height: 18),
          TextField(
            controller: controller,
            enabled: isEnabled && !isSubmitting,
            decoration: const InputDecoration(
              labelText: 'Ключ задания',
            ),
          ),
          const SizedBox(height: 16),
          FilledButton(
            onPressed: isEnabled && !isSubmitting ? onSubmit : null,
            child: Text(isSubmitting ? 'Отправляем...' : 'Отправить ключ'),
          ),
        ],
      ),
    );
  }
}

class _ChatBubble extends StatelessWidget {
  const _ChatBubble({
    required this.message,
    required this.isMine,
  });

  final GameChatMessage message;
  final bool isMine;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Align(
      alignment: isMine ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        constraints: const BoxConstraints(maxWidth: 280),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: isMine ? const Color(0x663A2F28) : const Color(0x66121B2C),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: isMine ? const Color(0x80F59E0B) : AppTheme.border,
          ),
        ),
        child: Column(
          crossAxisAlignment:
              isMine ? CrossAxisAlignment.end : CrossAxisAlignment.start,
          children: [
            Text(
              message.senderEmail,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: isMine ? const Color(0xFFFDE68A) : AppTheme.textSecondary,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              message.text,
              style: theme.textTheme.bodyLarge?.copyWith(
                color: AppTheme.textPrimary,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              _formatDate(message.createdAt),
              style: theme.textTheme.bodyMedium,
            ),
          ],
        ),
      ),
    );
  }
}

class _ErrorCard extends StatelessWidget {
  const _ErrorCard({
    required this.title,
    required this.message,
    required this.onRetry,
  });

  final String title;
  final String message;
  final Future<void> Function() onRetry;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: theme.textTheme.titleLarge),
          const SizedBox(height: 12),
          Text(message, style: theme.textTheme.bodyLarge),
          const SizedBox(height: 18),
          OutlinedButton.icon(
            onPressed: onRetry,
            icon: const Icon(Icons.refresh_rounded),
            label: const Text('Повторить'),
          ),
        ],
      ),
    );
  }
}

class _EmptyStateCard extends StatelessWidget {
  const _EmptyStateCard({
    required this.title,
    required this.text,
  });

  final String title;
  final String text;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: theme.textTheme.titleLarge),
          const SizedBox(height: 12),
          Text(text, style: theme.textTheme.bodyLarge),
        ],
      ),
    );
  }
}

class _LoadingCard extends StatelessWidget {
  const _LoadingCard({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      child: Column(
        children: [
          const SizedBox(height: 8),
          const CircularProgressIndicator(),
          const SizedBox(height: 18),
          Text(
            text,
            style: const TextStyle(
              color: AppTheme.textSecondary,
              fontSize: 16,
            ),
          ),
        ],
      ),
    );
  }
}

class _InfoChip extends StatelessWidget {
  const _InfoChip({
    required this.label,
    required this.color,
    required this.textColor,
  });

  final String label;
  final Color color;
  final Color textColor;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: textColor,
          fontSize: 13,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({
    required this.label,
    required this.value,
  });

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Text(
            label,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: const Color(0x99CBD5E1),
            ),
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Text(
            value,
            textAlign: TextAlign.right,
            style: theme.textTheme.bodyLarge?.copyWith(
              color: AppTheme.textPrimary,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ],
    );
  }
}

class _ChipData {
  const _ChipData({
    required this.label,
    required this.color,
    required this.textColor,
  });

  final String label;
  final Color color;
  final Color textColor;
}

String _sessionStatusLabel(String status) {
  switch (status.toUpperCase()) {
    case 'IN_PROGRESS':
      return 'Игра идёт';
    case 'FINISHED':
      return 'Завершена';
    case 'CANCELED':
      return 'Отменена';
    default:
      return status;
  }
}

String _registrationStatusLabel(String status) {
  switch (status.toUpperCase()) {
    case 'PENDING':
      return 'Заявка отправлена';
    case 'APPROVED':
      return 'Заявка подтверждена';
    case 'REJECTED':
      return 'Заявка отклонена';
    case 'CANCELED':
      return 'Заявка отменена';
    default:
      return status;
  }
}

String _gameStatusLabel(String status) {
  switch (status.toUpperCase()) {
    case 'DRAFT':
      return 'Черновик';
    case 'REGISTRATION_OPEN':
      return 'Регистрация открыта';
    case 'REGISTRATION_CLOSED':
      return 'Регистрация закрыта';
    case 'IN_PROGRESS':
      return 'Игра идёт';
    case 'FINISHED':
      return 'Игра завершена';
    case 'CANCELED':
      return 'Игра отменена';
    default:
      return status;
  }
}

String _taskStatusLabel(String status) {
  switch (status.toUpperCase()) {
    case 'ACTIVE':
      return 'Активно';
    case 'COMPLETED':
      return 'Завершено';
    case 'FAILED':
      return 'Провалено';
    default:
      return status;
  }
}

Color _registrationChipColor(String status) {
  switch (status.toUpperCase()) {
    case 'APPROVED':
      return const Color(0x2234D399);
    case 'REJECTED':
      return const Color(0x22EF4444);
    default:
      return const Color(0x22F59E0B);
  }
}

Color _registrationChipTextColor(String status) {
  switch (status.toUpperCase()) {
    case 'APPROVED':
      return const Color(0xFFBBF7D0);
    case 'REJECTED':
      return const Color(0xFFFECACA);
    default:
      return const Color(0xFFFDE68A);
  }
}

String _formatDate(DateTime? value) {
  if (value == null) {
    return 'Нет данных';
  }

  final local = value.toLocal();
  final day = local.day.toString().padLeft(2, '0');
  final month = local.month.toString().padLeft(2, '0');
  final year = local.year.toString();
  final hour = local.hour.toString().padLeft(2, '0');
  final minute = local.minute.toString().padLeft(2, '0');
  return '$day.$month.$year, $hour:$minute';
}

String _formatDuration(int totalSeconds) {
  final safeSeconds = totalSeconds < 0 ? 0 : totalSeconds;
  final hours = safeSeconds ~/ 3600;
  final minutes = (safeSeconds % 3600) ~/ 60;
  final seconds = safeSeconds % 60;

  if (hours > 0) {
    return '${hours.toString().padLeft(2, '0')}:'
        '${minutes.toString().padLeft(2, '0')}:'
        '${seconds.toString().padLeft(2, '0')}';
  }

  return '${minutes.toString().padLeft(2, '0')}:'
      '${seconds.toString().padLeft(2, '0')}';
}
