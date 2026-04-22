class CurrentUser {
  const CurrentUser({
    required this.id,
    required this.email,
    required this.role,
    required this.enabled,
    required this.accountNonLocked,
    required this.createdAt,
  });

  final int id;
  final String email;
  final String role;
  final bool enabled;
  final bool accountNonLocked;
  final DateTime createdAt;
}
