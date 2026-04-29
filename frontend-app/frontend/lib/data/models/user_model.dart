class UserModel {
  final String id;
  final String email;
  final String name;
  final DateTime? lastLoginAt;

  const UserModel({
    required this.id,
    required this.email,
    required this.name,
    this.lastLoginAt,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: (json['id'] ?? '').toString(),
      email: (json['email'] ?? '').toString(),
      name: (json['name'] ?? '').toString(),
      lastLoginAt: json['lastLoginAt'] != null
          ? DateTime.tryParse(json['lastLoginAt'].toString())
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'email': email,
      'name': name,
      if (lastLoginAt != null) 'lastLoginAt': lastLoginAt!.toIso8601String(),
    };
  }

  UserModel copyWith({
    String? id,
    String? email,
    String? name,
    DateTime? lastLoginAt,
    bool clearLastLoginAt = false,
  }) {
    return UserModel(
      id: id ?? this.id,
      email: email ?? this.email,
      name: name ?? this.name,
      lastLoginAt: clearLastLoginAt ? null : (lastLoginAt ?? this.lastLoginAt),
    );
  }
}
