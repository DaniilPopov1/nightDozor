import 'team_member.dart';

class Team {
  const Team({
    required this.id,
    required this.name,
    required this.city,
    required this.inviteCode,
    required this.captainId,
    required this.captainEmail,
    required this.createdAt,
    required this.members,
  });

  final int id;
  final String name;
  final String city;
  final String inviteCode;
  final int captainId;
  final String captainEmail;
  final DateTime? createdAt;
  final List<TeamMember> members;
}
