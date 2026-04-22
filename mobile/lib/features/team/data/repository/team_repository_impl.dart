import 'package:dio/dio.dart';

import '../../domain/models/team.dart';
import '../../domain/repository/team_repository.dart';
import '../datasource/team_api.dart';

class TeamRepositoryImpl implements TeamRepository {
  TeamRepositoryImpl(this._teamApi);

  final TeamApi _teamApi;

  @override
  Future<Team?> getCurrentTeam() async {
    try {
      return await _teamApi.getCurrentTeam();
    } on DioException catch (error) {
      if (error.response?.statusCode == 404) {
        return null;
      }

      final message =
          error.response?.data is Map<String, dynamic>
              ? (error.response?.data as Map<String, dynamic>)['error']
                  as String?
              : null;

      throw Exception(message ?? 'Не удалось загрузить данные команды');
    } catch (_) {
      throw Exception('Не удалось загрузить данные команды');
    }
  }
}
