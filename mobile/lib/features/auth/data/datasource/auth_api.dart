import 'package:dio/dio.dart';

import '../models/auth_token_payload.dart';
import '../models/current_user_model.dart';

class AuthApi {
  AuthApi(this._dio);

  final Dio _dio;

  void setAccessToken(String token) {
    _dio.options.headers['Authorization'] = 'Bearer $token';
  }

  void clearAccessToken() {
    _dio.options.headers.remove('Authorization');
  }

  Future<AuthTokenPayload> login({
    required String email,
    required String password,
  }) async {
    final response = await _dio.post<Map<String, dynamic>>(
      '/auth/login',
      data: {
        'email': email,
        'password': password,
      },
    );

    final data = response.data ?? <String, dynamic>{};
    return AuthTokenPayload.fromJson(data);
  }

  Future<CurrentUserModel> getCurrentUser() async {
    final response = await _dio.get<Map<String, dynamic>>('/auth/me');
    final data = response.data ?? <String, dynamic>{};
    return CurrentUserModel.fromJson(data);
  }
}
