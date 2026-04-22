import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/models/team.dart';
import '../../domain/repository/team_repository.dart';

sealed class TeamState {
  const TeamState();
}

class TeamInitial extends TeamState {
  const TeamInitial();
}

class TeamLoading extends TeamState {
  const TeamLoading();
}

class TeamEmpty extends TeamState {
  const TeamEmpty();
}

class TeamLoaded extends TeamState {
  const TeamLoaded(this.team);

  final Team team;
}

class TeamError extends TeamState {
  const TeamError(this.message);

  final String message;
}

class TeamCubit extends Cubit<TeamState> {
  TeamCubit(this._teamRepository) : super(const TeamInitial());

  final TeamRepository _teamRepository;

  Future<void> loadTeam() async {
    emit(const TeamLoading());

    try {
      final team = await _teamRepository.getCurrentTeam();
      if (team == null) {
        emit(const TeamEmpty());
        return;
      }

      emit(TeamLoaded(team));
    } catch (error) {
      emit(TeamError(error.toString().replaceFirst('Exception: ', '')));
    }
  }
}
