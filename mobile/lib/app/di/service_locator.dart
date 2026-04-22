import 'package:dio/dio.dart';
import 'package:get_it/get_it.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../core/network/dio_client.dart';
import '../../features/auth/data/datasource/auth_api.dart';
import '../../features/auth/data/datasource/auth_local_storage.dart';
import '../../features/auth/data/repository/auth_repository_impl.dart';
import '../../features/auth/domain/repository/auth_repository.dart';
import '../../features/auth/presentation/bloc/auth_bloc.dart';
import '../../features/team/data/datasource/team_api.dart';
import '../../features/team/data/repository/team_repository_impl.dart';
import '../../features/team/domain/repository/team_repository.dart';
import '../../features/team/presentation/cubit/team_cubit.dart';

final getIt = GetIt.instance;

Future<void> configureDependencies() async {
  if (getIt.isRegistered<Dio>()) {
    return;
  }

  final preferences = await SharedPreferences.getInstance();

  getIt.registerLazySingleton<SharedPreferences>(() => preferences);
  getIt.registerLazySingleton<Dio>(createDioClient);
  getIt.registerLazySingleton<AuthApi>(() => AuthApi(getIt<Dio>()));
  getIt.registerLazySingleton<TeamApi>(() => TeamApi(getIt<Dio>()));
  getIt.registerLazySingleton<AuthLocalStorage>(
    () => AuthLocalStorage(getIt<SharedPreferences>()),
  );
  getIt.registerLazySingleton<AuthRepository>(
    () => AuthRepositoryImpl(getIt<AuthApi>(), getIt<AuthLocalStorage>()),
  );
  getIt.registerFactory<AuthBloc>(
    () => AuthBloc(getIt<AuthRepository>()),
  );
  getIt.registerLazySingleton<TeamRepository>(
    () => TeamRepositoryImpl(getIt<TeamApi>()),
  );
  getIt.registerFactory<TeamCubit>(
    () => TeamCubit(getIt<TeamRepository>()),
  );
}
