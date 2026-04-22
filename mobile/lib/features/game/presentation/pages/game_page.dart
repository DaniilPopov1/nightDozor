import 'package:flutter/material.dart';

import '../../../../app/theme/app_theme.dart';
import '../../../../shared/presentation/widgets/night_dozor_backdrop.dart';

class GamePage extends StatelessWidget {
  const GamePage({super.key});

  @override
  Widget build(BuildContext context) {
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
                Text('Игровой раздел', style: theme.textTheme.headlineMedium),
                const SizedBox(height: 14),
                Text(
                  'Здесь появятся текущее задание, таймеры, подсказки, '
                  'прогресс команды и игровые чаты. Интерфейс уже приведён '
                  'к мобильному формату и готов к следующему этапу разработки.',
                  style: theme.textTheme.bodyLarge,
                ),
              ],
            ),
          ),
          const SizedBox(height: 18),
          const _FeatureCard(
            icon: Icons.route_rounded,
            title: 'Текущее задание',
            text:
                'В этом блоке будет выводиться активная загадка, статус уровня и время на прохождение.',
          ),
          const SizedBox(height: 14),
          const _FeatureCard(
            icon: Icons.timer_outlined,
            title: 'Таймеры и подсказки',
            text:
                'Для участника будут доступны таймер задания и подсказки, которые открываются по времени.',
          ),
          const SizedBox(height: 14),
          const _FeatureCard(
            icon: Icons.forum_rounded,
            title: 'Игровое общение',
            text:
                'Командный чат и канал капитана с организатором будут встроены сюда в следующем шаге.',
          ),
        ],
      ),
    );
  }
}

class _FeatureCard extends StatelessWidget {
  const _FeatureCard({
    required this.icon,
    required this.title,
    required this.text,
  });

  final IconData icon;
  final String title;
  final String text;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return GlassCard(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 52,
            height: 52,
            decoration: BoxDecoration(
              color: const Color(0x22F59E0B),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Icon(icon, color: AppTheme.accent),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: theme.textTheme.titleLarge),
                const SizedBox(height: 8),
                Text(text, style: theme.textTheme.bodyMedium),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
