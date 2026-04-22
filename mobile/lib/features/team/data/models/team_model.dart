import '../../domain/models/team.dart';
import 'team_member_model.dart';

class TeamModel extends Team {
  const TeamModel({
    required super.id,
    required super.name,
    required super.city,
    required super.inviteCode,
    required super.captainId,
    required super.captainEmail,
    required super.createdAt,
    required super.members,
  });

  factory TeamModel.fromJson(Map<String, dynamic> json) {
    final membersJson = json['members'] as List<dynamic>? ?? const [];

    return TeamModel(
      id: json['id'] as int? ?? 0,
      name: json['name'] as String? ?? '',
      city: json['city'] as String? ?? '',
      inviteCode: json['inviteCode'] as String? ?? '',
      captainId: json['captainId'] as int? ?? 0,
      captainEmail: json['captainEmail'] as String? ?? '',
      createdAt: DateTime.tryParse(json['createdAt'] as String? ?? ''),
      members: membersJson
          .whereType<Map<String, dynamic>>()
          .map(TeamMemberModel.fromJson)
          .toList(growable: false),
    );
  }
}
