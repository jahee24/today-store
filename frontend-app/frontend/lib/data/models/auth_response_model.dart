import 'user_model.dart';

class AuthResponseModel {
  final String accessToken;
  final String refreshToken;
  final bool isFirstLogin;
  final UserModel? user;

  const AuthResponseModel({
    required this.accessToken,
    required this.refreshToken,
    required this.isFirstLogin,
    this.user,
  });

  factory AuthResponseModel.fromJson(Map<String, dynamic> json) {
    final user = json['user'] as Map<String, dynamic>?;
    final parsedIsFirstLogin =
        _asBool(user?['isFirstLogin']) ??
        _asBool(user?['firstLogin']) ??
        _asBool(json['isFirstLogin']) ??
        _asBool(json['firstLogin']) ??
        false;

    return AuthResponseModel(
      accessToken: (json['accessToken'] ?? '').toString(),
      refreshToken: (json['refreshToken'] ?? '').toString(),
      isFirstLogin: parsedIsFirstLogin,
      user: user == null ? null : UserModel.fromJson(user),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'accessToken': accessToken,
      'refreshToken': refreshToken,
      if (user != null)
        'user': {
          ...user!.toJson(),
          'firstLogin': isFirstLogin,
        },
    };
  }
}

bool? _asBool(dynamic value) {
  if (value is bool) {
    return value;
  }
  if (value is num) {
    if (value == 1) {
      return true;
    }
    if (value == 0) {
      return false;
    }
  }
  if (value is String) {
    final normalized = value.trim().toLowerCase();
    if (normalized == 'true' || normalized == '1') {
      return true;
    }
    if (normalized == 'false' || normalized == '0') {
      return false;
    }
  }
  return null;
}