import 'package:dio/dio.dart';

Dio createDioClient() {
  const defaultBaseUrl = 'https://nightdozor.ru/api';
  const fromEnv = String.fromEnvironment('API_BASE_URL');
  final baseUrl = fromEnv.isEmpty ? defaultBaseUrl : fromEnv;

  return Dio(
    BaseOptions(
      baseUrl: baseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 20),
      sendTimeout: const Duration(seconds: 20),
      headers: const {
        'Content-Type': 'application/json',
      },
    ),
  );
}
