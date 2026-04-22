import 'package:shared_preferences/shared_preferences.dart';

class AuthLocalStorage {
  AuthLocalStorage(this._preferences);

  static const _tokenKey = 'nightdozor_access_token';

  final SharedPreferences _preferences;

  Future<void> saveToken(String token) async {
    await _preferences.setString(_tokenKey, token);
  }

  String? getToken() {
    return _preferences.getString(_tokenKey);
  }

  Future<void> clearToken() async {
    await _preferences.remove(_tokenKey);
  }
}
