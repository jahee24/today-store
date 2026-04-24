class AuthResponseModel {
  final String accessToken;
  final String refreshToken;
  final bool isFirstLogin;

  AuthResponseModel({
    required this.accessToken,
    required this.refreshToken,
    required this.isFirstLogin,
  });

  factory AuthResponseModel.fromJson(Map<String, dynamic> json) {
    final user = json['user'] as Map<String, dynamic>?;
    return AuthResponseModel(
      accessToken: (json['accessToken'] ?? '') as String, 
      refreshToken: (json['refreshToken'] ?? '') as String, 
      isFirstLogin: user?['isFirstLogin'] as bool? ?? false,
    );
  }
}