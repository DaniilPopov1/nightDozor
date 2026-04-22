import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'theme/app_theme.dart';
import '../features/auth/presentation/bloc/auth_bloc.dart';
import 'di/service_locator.dart';
import 'router/app_router.dart';

class NightDozorApp extends StatefulWidget {
  const NightDozorApp({super.key});

  @override
  State<NightDozorApp> createState() => _NightDozorAppState();
}

class _NightDozorAppState extends State<NightDozorApp> {
  late final AuthBloc _authBloc;
  late final AppRouter _appRouter;

  @override
  void initState() {
    super.initState();
    _authBloc = getIt<AuthBloc>()..add(const AuthStarted());
    _appRouter = AppRouter(_authBloc);
  }

  @override
  void dispose() {
    _appRouter.dispose();
    _authBloc.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return BlocProvider.value(
      value: _authBloc,
      child: MaterialApp.router(
        title: 'Night Dozor',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.dark(),
        routerConfig: _appRouter.config,
      ),
    );
  }
}
