import '../../domain/models/current_user.dart';

class CurrentUserModel extends CurrentUser {
  const CurrentUserModel({
    required super.id,
    required super.email,
    required super.role,
    required super.enabled,
    required super.accountNonLocked,
    required super.createdAt,
  });

  factory CurrentUserModel.fromJson(Map<String, dynamic> json) {
    return CurrentUserModel(
      id: json['id'] as int? ?? 0,
      email: json['email'] as String? ?? '',
      role: json['role'] as String? ?? '',
      enabled: json['enabled'] as bool? ?? false,
      accountNonLocked: json['accountNonLocked'] as bool? ?? false,
      createdAt: DateTime.tryParse(json['createdAt'] as String? ?? '') ??
          DateTime.fromMillisecondsSinceEpoch(0),
    );
  }
}
