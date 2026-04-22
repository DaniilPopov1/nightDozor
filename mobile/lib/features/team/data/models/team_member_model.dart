import '../../domain/models/team_member.dart';

class TeamMemberModel extends TeamMember {
  const TeamMemberModel({
    required super.userId,
    required super.email,
    required super.role,
    required super.status,
    required super.joinedAt,
  });

  factory TeamMemberModel.fromJson(Map<String, dynamic> json) {
    return TeamMemberModel(
      userId: json['userId'] as int? ?? 0,
      email: json['email'] as String? ?? '',
      role: json['role'] as String? ?? '',
      status: json['status'] as String? ?? '',
      joinedAt: DateTime.tryParse(json['joinedAt'] as String? ?? ''),
    );
  }
}
