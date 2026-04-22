import '../../domain/models/current_user.dart';
import '../../domain/repository/auth_repository.dart';
import '../datasource/auth_local_storage.dart';
import '../datasource/auth_api.dart';

class AuthRepositoryImpl implements AuthRepository {
  AuthRepositoryImpl(this._authApi, this._localStorage);

  final AuthApi _authApi;
  final AuthLocalStorage _localStorage;
  String? _token;

  @override
  Future<String?> getSavedToken() async {
    _token ??= _localStorage.getToken();

    if (_token != null && _token!.isNotEmpty) {
      _authApi.setAccessToken(_token!);
    }

    return _token;
  }

  @override
  Future<CurrentUser?> getCurrentUser() async {
    final token = await getSavedToken();
    if (token == null || token.isEmpty) {
      return null;
    }

    return _authApi.getCurrentUser();
  }

  @override
  Future<void> login({
    required String email,
    required String password,
  }) async {
    final payload = await _authApi.login(email: email, password: password);
    final token = payload.accessToken;

    if (token.isEmpty) {
      throw Exception('Сервер не вернул токен');
    }

    _token = token;
    _authApi.setAccessToken(token);
    await _localStorage.saveToken(token);
  }

  @override
  Future<void> logout() async {
    _token = null;
    _authApi.clearAccessToken();
    await _localStorage.clearToken();
  }
}
