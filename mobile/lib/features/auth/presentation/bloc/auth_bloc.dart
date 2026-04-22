import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/models/current_user.dart';
import '../../domain/repository/auth_repository.dart';

sealed class AuthEvent {
  const AuthEvent();
}

class AuthStarted extends AuthEvent {
  const AuthStarted();
}

class AuthLoginRequested extends AuthEvent {
  const AuthLoginRequested({
    required this.email,
    required this.password,
  });

  final String email;
  final String password;
}

class AuthLogoutRequested extends AuthEvent {
  const AuthLogoutRequested();
}

sealed class AuthState {
  const AuthState();
}

class AuthUnknown extends AuthState {
  const AuthUnknown();
}

class Unauthenticated extends AuthState {
  const Unauthenticated();
}

class Authenticated extends AuthState {
  const Authenticated(this.user);

  final CurrentUser user;
}

class AuthLoading extends AuthState {
  const AuthLoading();
}

class AuthFailure extends AuthState {
  const AuthFailure(this.message);

  final String message;
}

class AuthBloc extends Bloc<AuthEvent, AuthState> {
  AuthBloc(this._authRepository) : super(const AuthUnknown()) {
    on<AuthStarted>(_onStarted);
    on<AuthLoginRequested>(_onLoginRequested);
    on<AuthLogoutRequested>(_onLogoutRequested);
  }

  final AuthRepository _authRepository;

  Future<void> _onStarted(AuthStarted event, Emitter<AuthState> emit) async {
    final token = await _authRepository.getSavedToken();
    if (token == null || token.isEmpty) {
      emit(const Unauthenticated());
      return;
    }

    try {
      final user = await _authRepository.getCurrentUser();
      if (user == null) {
        emit(const Unauthenticated());
        return;
      }

      emit(Authenticated(user));
    } catch (_) {
      await _authRepository.logout();
      emit(const Unauthenticated());
    }
  }

  Future<void> _onLoginRequested(
    AuthLoginRequested event,
    Emitter<AuthState> emit,
  ) async {
    emit(const AuthLoading());
    try {
      await _authRepository.login(
        email: event.email.trim(),
        password: event.password,
      );

      final user = await _authRepository.getCurrentUser();
      if (user == null) {
        throw Exception('Не удалось загрузить профиль пользователя');
      }

      emit(Authenticated(user));
    } catch (error) {
      emit(AuthFailure(error.toString().replaceFirst('Exception: ', '')));
      emit(const Unauthenticated());
    }
  }

  Future<void> _onLogoutRequested(
    AuthLogoutRequested event,
    Emitter<AuthState> emit,
  ) async {
    await _authRepository.logout();
    emit(const Unauthenticated());
  }
}
