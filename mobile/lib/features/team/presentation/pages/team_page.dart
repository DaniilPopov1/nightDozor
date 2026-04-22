import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../app/theme/app_theme.dart';
import '../../../../shared/presentation/widgets/night_dozor_backdrop.dart';
import '../../../auth/presentation/bloc/auth_bloc.dart';
import '../../domain/models/team.dart';
import '../../domain/models/team_member.dart';
import '../cubit/team_cubit.dart';

class TeamPage extends StatelessWidget {
  const TeamPage({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final authState = context.watch<AuthBloc>().state;
    final currentUserId = authState is Authenticated ? authState.user.id : null;

    return SafeArea(
      child: BlocBuilder<TeamCubit, TeamState>(
        builder: (context, state) {
          return RefreshIndicator(
            color: AppTheme.accent,
            onRefresh: () => context.read<TeamCubit>().loadTeam(),
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 18, 16, 120),
              children: [
                GlassCard(
                  padding: const EdgeInsets.all(28),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const EyebrowText('Команда'),
                      const SizedBox(height: 16),
                      Text('Моя команда', style: theme.textTheme.headlineMedium),
                    ],
                  ),
                ),
                const SizedBox(height: 18),
                if (state is TeamLoading || state is TeamInitial)
                  const _TeamLoadingView()
                else if (state is TeamEmpty)
                  const _TeamEmptyView()
                else if (state is TeamError)
                  _TeamErrorView(message: state.message)
                else if (state is TeamLoaded)
                  _TeamLoadedView(
                    team: state.team,
                    currentUserId: currentUserId,
                  ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _TeamLoadedView extends StatelessWidget {
  const _TeamLoadedView({
    required this.team,
    required this.currentUserId,
  });

  final Team team;
  final int? currentUserId;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _TeamSummaryCard(team: team),
        const SizedBox(height: 14),
        _TeamMetaCard(team: team, currentUserId: currentUserId),
        const SizedBox(height: 14),
        _TeamMembersCard(team: team, currentUserId: currentUserId),
      ],
    );
  }
}

class _TeamSummaryCard extends StatelessWidget {
  const _TeamSummaryCard({required this.team});

  final Team team;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 58,
                height: 58,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(18),
                  gradient: const LinearGradient(
                    colors: [AppTheme.accent, AppTheme.accentDeep],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                ),
                child: const Icon(
                  Icons.groups_rounded,
                  color: Color(0xFFFFF7ED),
                  size: 30,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(team.name, style: theme.textTheme.titleLarge),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        _InfoChip(
                          label: team.city.isEmpty ? 'Город не указан' : team.city,
                          color: const Color(0x221F9CF0),
                          textColor: const Color(0xFFBFDBFE),
                        ),
                        _InfoChip(
                          label: '${team.members.length} участников',
                          color: const Color(0x22F59E0B),
                          textColor: const Color(0xFFFDE68A),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 24),
          _InfoRow(label: 'Капитан', value: team.captainEmail),
          const SizedBox(height: 12),
          _InfoRow(label: 'Код приглашения', value: team.inviteCode),
          const SizedBox(height: 12),
          _InfoRow(
            label: 'Дата создания',
            value: _formatDate(team.createdAt),
          ),
        ],
      ),
    );
  }
}

class _TeamMetaCard extends StatelessWidget {
  const _TeamMetaCard({
    required this.team,
    required this.currentUserId,
  });

  final Team team;
  final int? currentUserId;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final me = team.members.where((member) => member.userId == currentUserId).firstOrNull;

    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Моё состояние в команде', style: theme.textTheme.titleLarge),
          const SizedBox(height: 18),
          _InfoRow(
            label: 'Роль',
            value: me == null ? 'Не определена' : _roleLabel(me.role),
          ),
          const SizedBox(height: 12),
          _InfoRow(
            label: 'Статус',
            value: me == null ? 'Нет данных' : _statusLabel(me.status),
          ),
          const SizedBox(height: 12),
          _InfoRow(
            label: 'Вступление',
            value: me == null ? 'Нет данных' : _formatDate(me.joinedAt),
          ),
        ],
      ),
    );
  }
}

class _TeamMembersCard extends StatelessWidget {
  const _TeamMembersCard({
    required this.team,
    required this.currentUserId,
  });

  final Team team;
  final int? currentUserId;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Состав команды', style: theme.textTheme.titleLarge),
          const SizedBox(height: 18),
          for (var i = 0; i < team.members.length; i++) ...[
            _MemberTile(
              member: team.members[i],
              isCurrentUser: team.members[i].userId == currentUserId,
              isCaptain: team.members[i].userId == team.captainId,
            ),
            if (i != team.members.length - 1) const SizedBox(height: 12),
          ],
        ],
      ),
    );
  }
}

class _MemberTile extends StatelessWidget {
  const _MemberTile({
    required this.member,
    required this.isCurrentUser,
    required this.isCaptain,
  });

  final TeamMember member;
  final bool isCurrentUser;
  final bool isCaptain;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0x66121B2C),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppTheme.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(member.email, style: theme.textTheme.titleMedium),
              ),
              if (isCurrentUser)
                const _InfoChip(
                  label: 'Вы',
                  color: Color(0x221F9CF0),
                  textColor: Color(0xFFBFDBFE),
                ),
            ],
          ),
          const SizedBox(height: 10),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _InfoChip(
                label: isCaptain ? 'Капитан' : _roleLabel(member.role),
                color: const Color(0x22F59E0B),
                textColor: const Color(0xFFFDE68A),
              ),
              _InfoChip(
                label: _statusLabel(member.status),
                color: const Color(0x2234D399),
                textColor: const Color(0xFFBBF7D0),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _TeamEmptyView extends StatelessWidget {
  const _TeamEmptyView();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Команда не найдена', style: theme.textTheme.titleLarge),
          const SizedBox(height: 12),
          Text(
            'Похоже, сейчас этот аккаунт не состоит в команде. Управление вступлением и созданием команд остаётся в веб-версии сервиса.',
            style: theme.textTheme.bodyLarge,
          ),
        ],
      ),
    );
  }
}

class _TeamErrorView extends StatelessWidget {
  const _TeamErrorView({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Не удалось загрузить команду', style: theme.textTheme.titleLarge),
          const SizedBox(height: 12),
          Text(message, style: theme.textTheme.bodyLarge),
          const SizedBox(height: 18),
          OutlinedButton.icon(
            onPressed: () => context.read<TeamCubit>().loadTeam(),
            icon: const Icon(Icons.refresh_rounded),
            label: const Text('Повторить'),
          ),
        ],
      ),
    );
  }
}

class _TeamLoadingView extends StatelessWidget {
  const _TeamLoadingView();

  @override
  Widget build(BuildContext context) {
    return const GlassCard(
      child: Column(
        children: [
          SizedBox(height: 8),
          CircularProgressIndicator(),
          SizedBox(height: 18),
          Text(
            'Загружаем данные команды...',
            style: TextStyle(
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

String _roleLabel(String role) {
  switch (role.toUpperCase()) {
    case 'CAPTAIN':
      return 'Капитан';
    case 'MEMBER':
      return 'Участник';
    default:
      return role;
  }
}

String _statusLabel(String status) {
  switch (status.toUpperCase()) {
    case 'ACTIVE':
      return 'Активен';
    case 'PENDING':
      return 'Ожидает подтверждения';
    case 'REJECTED':
      return 'Отклонён';
    case 'LEFT':
      return 'Покинул команду';
    default:
      return status;
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
