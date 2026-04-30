String buildChatWebSocketUrl(String token) {
  const defaultBaseUrl = 'https://nightdozor.ru/api';
  const fromEnv = String.fromEnvironment('API_BASE_URL');
  final baseUrl = fromEnv.isEmpty ? defaultBaseUrl : fromEnv;

  final apiUri = Uri.parse(baseUrl);
  final isSecure = apiUri.scheme == 'https';
  final wsScheme = isSecure ? 'wss' : 'ws';

  return Uri(
    scheme: wsScheme,
    host: apiUri.host,
    port: apiUri.hasPort ? apiUri.port : null,
    path: '/ws/chat',
    queryParameters: {'token': token},
  ).toString();
}
