class UserModel {
  final String id;
  final String email;
  final String name;
  final DateTime? lastLoginAt;
  final bool? isFirstLogin;
  final bool? profileCompleted;

  const UserModel({
    required this.id,
    required this.email,
    required this.name,
    this.lastLoginAt,
    this.isFirstLogin,
    this.profileCompleted,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    final id = json['id'];

    bool? readBool(dynamic value) {
      if (value is bool) return value;
      if (value is String) {
        final lower = value.toLowerCase();
        if (lower == 'true' || lower == '1') return true;
        if (lower == 'false' || lower == '0') return false;
      }
      if (value is num) return value != 0;
      return null;
    }

    final lastLoginRaw = json['lastLoginAt'];

    return UserModel(
      id: id == null ? '' : id.toString(),
      email: (json['email'] as String?) ?? '',
      name: (json['name'] as String?) ?? '',
      lastLoginAt: lastLoginRaw is String ? DateTime.tryParse(lastLoginRaw) : null,
      isFirstLogin: readBool(json['isFirstLogin'] ?? json['firstLogin'] ?? json['is_first_login']),
      profileCompleted: readBool(json['profileCompleted'] ?? json['isProfileCompleted'] ?? json['profile_completed']),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'email': email,
      'name': name,
      'lastLoginAt': lastLoginAt?.toIso8601String(),
      'isFirstLogin': isFirstLogin,
      'profileCompleted': profileCompleted,
    };
  }

  UserModel copyWith({
    String? id,
    String? email,
    String? name,
    DateTime? lastLoginAt,
    bool? isFirstLogin,
    bool? profileCompleted,
  }) {
    return UserModel(
      id: id ?? this.id,
      email: email ?? this.email,
      name: name ?? this.name,
      lastLoginAt: lastLoginAt ?? this.lastLoginAt,
      isFirstLogin: isFirstLogin ?? this.isFirstLogin,
      profileCompleted: profileCompleted ?? this.profileCompleted,
    );
  }
}