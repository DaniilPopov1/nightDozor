import 'game_team_progress.dart';
import 'team_game_registration.dart';

class GameOverview {
  const GameOverview._({
    this.progress,
    this.registration,
  });

  const GameOverview.active(GameTeamProgress progress)
      : this._(progress: progress);

  const GameOverview.registration(TeamGameRegistration registration)
      : this._(registration: registration);

  final GameTeamProgress? progress;
  final TeamGameRegistration? registration;

  bool get hasActiveGame => progress != null;

  bool get hasRegistration => registration != null;
}
