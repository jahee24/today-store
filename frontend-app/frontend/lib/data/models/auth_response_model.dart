import 'user_model.dart';

class AuthResponseModel {
  final String accessToken;
  final String refreshToken;
  final bool isFirstLogin;
  final UserModel? user;

  AuthResponseModel({
    required this.accessToken,
    required this.refreshToken,
    required this.isFirstLogin,
    this.user,
  });

  static bool? _readBool(dynamic v) {
    if (v is bool) return v;
    if (v is String) {
      final lower = v.toLowerCase();
      if (lower == 'true' || lower == '1') return true;
      if (lower == 'false' || lower == '0') return false;
    }
    if (v is num) return v != 0;
    return null;
  }

  factory AuthResponseModel.fromJson(Map<String, dynamic> json) {
    final userJson = json['user'] as Map<String, dynamic>?;

    final topLevelFirstLogin = _readBool(
      json['isFirstLogin'] ?? json['firstLogin'] ?? json['is_first_login']
   );

   final userLevelFirstLogin = _readBool(
        userJson?['isFirstLogin'] ?? userJson?['firstLogin'] ?? userJson?['is_first_login']
  );

    return AuthResponseModel(
      accessToken: (json['accessToken'] ?? '') as String,
      refreshToken: (json['refreshToken'] ?? '') as String,
      isFirstLogin: topLevelFirstLogin ?? userLevelFirstLogin ?? false,
      user: userJson != null ? UserModel.fromJson(userJson) : null,
    );
  }
}
