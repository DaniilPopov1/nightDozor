import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../shared/presentation/widgets/night_dozor_backdrop.dart';
import '../bloc/auth_bloc.dart';

class ParticipantOnlyPage extends StatelessWidget {
  const ParticipantOnlyPage({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      backgroundColor: Colors.transparent,
      body: NightDozorBackdrop(
        child: SafeArea(
          child: Center(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: GlassCard(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const EyebrowText('Night Dozor'),
                    const SizedBox(height: 16),
                    Text(
                      'Мобильное приложение доступно только участникам',
                      style: theme.textTheme.headlineMedium,
                    ),
                    const SizedBox(height: 14),
                    Text(
                      'Для организатора основные рабочие сценарии остаются в '
                      'веб-кабинете. Здесь мы развиваем только мобильную часть '
                      'для участия в игре.',
                      style: theme.textTheme.bodyLarge,
                    ),
                    const SizedBox(height: 24),
                    OutlinedButton.icon(
                      onPressed: () {
                        context.read<AuthBloc>().add(const AuthLogoutRequested());
                      },
                      icon: const Icon(Icons.logout_rounded),
                      label: const Text('Выйти'),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
