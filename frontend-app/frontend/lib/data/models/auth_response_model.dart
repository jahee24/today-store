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
    return AuthResponseModel(
      accessToken: (json['accessToken'] ?? '') as String, 
      refreshToken: (json['refreshToken'] ?? '') as String, 
      isFirstLogin: (json['isFirstLogin'] ?? '') as bool,
    );
  }
}