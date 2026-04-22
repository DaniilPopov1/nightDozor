import '../models/current_user.dart';

abstract class AuthRepository {
  Future<String?> getSavedToken();

  Future<CurrentUser?> getCurrentUser();

  Future<void> login({
    required String email,
    required String password,
  });

  Future<void> logout();
}
