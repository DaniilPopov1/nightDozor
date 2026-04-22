class AuthTokenPayload {
  const AuthTokenPayload({
    required this.accessToken,
    required this.email,
    required this.role,
  });

  final String accessToken;
  final String email;
  final String role;

  factory AuthTokenPayload.fromJson(Map<String, dynamic> json) {
    return AuthTokenPayload(
      accessToken: json['accessToken'] as String? ?? '',
      email: json['email'] as String? ?? '',
      role: json['role'] as String? ?? '',
    );
  }
}
