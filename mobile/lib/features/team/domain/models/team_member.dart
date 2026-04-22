class TeamMember {
  const TeamMember({
    required this.userId,
    required this.email,
    required this.role,
    required this.status,
    required this.joinedAt,
  });

  final int userId;
  final String email;
  final String role;
  final String status;
  final DateTime? joinedAt;
}
