import 'package:dio/dio.dart';

import '../models/team_model.dart';

class TeamApi {
  TeamApi(this._dio);

  final Dio _dio;

  Future<TeamModel> getCurrentTeam() async {
    final response = await _dio.get<Map<String, dynamic>>('/teams/me');
    final data = response.data ?? <String, dynamic>{};
    return TeamModel.fromJson(data);
  }
}
