import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../di/service_locator.dart';
import '../presentation/pages/participant_shell_page.dart';
import '../../features/auth/presentation/bloc/auth_bloc.dart';
import '../../features/auth/presentation/pages/login_page.dart';
import '../../features/auth/presentation/pages/participant_only_page.dart';
import '../../features/auth/presentation/pages/splash_page.dart';
import '../../features/game/presentation/pages/game_page.dart';
import '../../features/profile/presentation/pages/profile_page.dart';
import '../../features/team/presentation/cubit/team_cubit.dart';
import '../../features/team/presentation/pages/team_page.dart';
import 'go_router_refresh_stream.dart';

class AppRouter {
  AppRouter(this._authBloc)
      : _refreshListenable = GoRouterRefreshStream(_authBloc.stream) {
    config = GoRouter(
      initialLocation: '/splash',
      refreshListenable: _refreshListenable,
      redirect: (context, state) {
        final authState = _authBloc.state;
        final isOnLogin = state.matchedLocation == '/login';
        final isOnSplash = state.matchedLocation == '/splash';
        final isOnRestricted = state.matchedLocation == '/restricted';

        if (authState is AuthUnknown) {
          return isOnSplash ? null : '/splash';
        }

        if (authState is! Authenticated) {
          return isOnLogin ? null : '/login';
        }

        final user = authState.user;
        final isParticipant = user.role.toUpperCase() == 'PARTICIPANT';

        if (!isParticipant) {
          return isOnRestricted ? null : '/restricted';
        }

        if (isOnLogin || isOnSplash || isOnRestricted) {
          return '/game';
        }

        return null;
      },
      routes: [
        GoRoute(
          path: '/splash',
          builder: (context, state) => const SplashPage(),
        ),
        GoRoute(
          path: '/login',
          builder: (context, state) => const LoginPage(),
        ),
        GoRoute(
          path: '/restricted',
          builder: (context, state) => const ParticipantOnlyPage(),
        ),
        ShellRoute(
          builder: (context, state, child) => ParticipantShellPage(
            location: state.uri.path,
            child: child,
          ),
          routes: [
            GoRoute(
              path: '/game',
              builder: (context, state) => const GamePage(),
            ),
            GoRoute(
              path: '/team',
              builder: (context, state) => BlocProvider(
                create: (_) => getIt<TeamCubit>()..loadTeam(),
                child: const TeamPage(),
              ),
            ),
            GoRoute(
              path: '/profile',
              builder: (context, state) => const ProfilePage(),
            ),
          ],
        ),
      ],
    );
  }

  final AuthBloc _authBloc;
  final GoRouterRefreshStream _refreshListenable;
  late final GoRouter config;

  void dispose() {
    _refreshListenable.dispose();
  }
}
