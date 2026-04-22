import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../app/theme/app_theme.dart';
import '../../../../shared/presentation/widgets/night_dozor_backdrop.dart';
import '../../../auth/domain/models/current_user.dart';
import '../../../auth/presentation/bloc/auth_bloc.dart';

class ProfilePage extends StatelessWidget {
  const ProfilePage({super.key});

  @override
  Widget build(BuildContext context) {
    final authState = context.watch<AuthBloc>().state;
    final user = authState is Authenticated ? authState.user : null;
    final theme = Theme.of(context);

    return SafeArea(
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
                Text('Профиль', style: theme.textTheme.headlineMedium),
              ],
            ),
          ),
          const SizedBox(height: 18),
          if (user != null) ...[
            _ProfileSummaryCard(user: user),
            const SizedBox(height: 14),
            _StatusCard(user: user),
            const SizedBox(height: 14),
            OutlinedButton.icon(
              onPressed: () {
                context.read<AuthBloc>().add(const AuthLogoutRequested());
              },
              icon: const Icon(Icons.logout_rounded),
              label: const Text('Выйти из аккаунта'),
            ),
          ] else
            GlassCard(
              child: Text(
                'Профиль пользователя пока не загружен.',
                style: theme.textTheme.bodyLarge,
              ),
            ),
        ],
      ),
    );
  }
}

class _ProfileSummaryCard extends StatelessWidget {
  const _ProfileSummaryCard({required this.user});

  final CurrentUser user;

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
                  Icons.person_rounded,
                  color: Color(0xFFFFF7ED),
                  size: 30,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(user.email, style: theme.textTheme.titleLarge),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        _StatusChip(
                          label: _roleLabel(user.role),
                          color: const Color(0x22F59E0B),
                          textColor: const Color(0xFFFDE68A),
                        ),
                        _StatusChip(
                          label: user.enabled
                              ? 'Подтверждён'
                              : 'Ожидает подтверждения',
                          color: user.enabled
                              ? const Color(0x2234D399)
                              : const Color(0x22F59E0B),
                          textColor: user.enabled
                              ? const Color(0xFFBBF7D0)
                              : const Color(0xFFFDE68A),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 24),
          _InfoRow(label: 'Email', value: user.email),
          const SizedBox(height: 12),
          _InfoRow(label: 'Роль', value: _roleLabel(user.role)),
          const SizedBox(height: 12),
          _InfoRow(
            label: 'Дата регистрации',
            value: _formatDate(user.createdAt),
          ),
        ],
      ),
    );
  }
}

class _StatusCard extends StatelessWidget {
  const _StatusCard({required this.user});

  final CurrentUser user;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Статус аккаунта', style: theme.textTheme.titleLarge),
          const SizedBox(height: 18),
          _InfoRow(
            label: 'Подтверждение профиля',
            value: user.enabled ? 'Аккаунт подтверждён' : 'Ожидает подтверждения',
          ),
          const SizedBox(height: 12),
          _InfoRow(
            label: 'Доступ к системе',
            value: user.accountNonLocked ? 'Аккаунт активен' : 'Аккаунт заблокирован',
          ),
        ],
      ),
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({
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
    case 'PARTICIPANT':
      return 'Участник';
    case 'ORGANIZER':
      return 'Организатор';
    default:
      return role;
  }
}

String _formatDate(DateTime value) {
  final local = value.toLocal();
  final day = local.day.toString().padLeft(2, '0');
  final month = local.month.toString().padLeft(2, '0');
  final year = local.year.toString();
  final hour = local.hour.toString().padLeft(2, '0');
  final minute = local.minute.toString().padLeft(2, '0');
  return '$day.$month.$year, $hour:$minute';
}
