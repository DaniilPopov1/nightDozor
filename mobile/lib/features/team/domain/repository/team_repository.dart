import '../models/team.dart';

abstract class TeamRepository {
  Future<Team?> getCurrentTeam();
}
