import 'package:dio/dio.dart';
import '../../models/auth_response_model.dart';
import '../../models/user_model.dart';

class AuthApi {
  final Dio dio;

  AuthApi(this.dio);

  Future<AuthResponseModel> oauthLogin({
    required String provider,
    required String accessToken,
  }) async {
    final response = await dio.post(
      '/api/v1/auth/oauth/login',
      data: {
        'provider': provider,
        'accessToken': accessToken,
      },
    );

    print('LOGIN RESPONSE: ${response.data}');

  return AuthResponseModel.fromJson(response.data);
  }

  Future<AuthResponseModel> refresh({
    required String refreshToken,
  }) async {
    final response = await dio.post(
      '/api/v1/auth/refresh',
      data: {
        'refreshToken': refreshToken,
      },
    );

    return AuthResponseModel.fromJson(response.data);
  }

  Future<void> logout({
    required String refreshToken,
  }) async {
    await dio.post(
      '/api/v1/auth/logout',
      data: {
        'refreshToken': refreshToken,
      },
    );
  }

  /// DELETE /api/v1/auth/me — 계정 탈퇴 (204 No Content)
  Future<void> deleteAccount() async {
    await dio.delete('/api/v1/auth/me');
  }

  Future<UserModel> getMyProfile() async {
    try {
      final response = await dio.get('/api/v1/users/me');
      print('GET PROFILE RESPONSE: ${response.data}');
      return UserModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      print('GET PROFILE ERROR: $e');
      rethrow;
    }
  }

  /// PATCH /api/v1/users/me 응답에는 이메일 변경 시
  /// 새 accessToken / refreshToken이 함께 내려옴.
  Future<({UserModel user, String? accessToken, String? refreshToken})>
      updateMyProfile({
    String? name,
    String? email,
  }) async {
    final payload = <String, dynamic>{};
    if (name != null && name.trim().isNotEmpty) {
      payload['name'] = name.trim();
    }
    if (email != null && email.trim().isNotEmpty) {
      payload['email'] = email.trim();
    }

    print('UPDATE PROFILE REQUEST: $payload');
    try {
      final response = await dio.patch(
        '/api/v1/users/me',
        data: payload,
      );
      print('UPDATE PROFILE RESPONSE: ${response.data}');
      final data = response.data as Map<String, dynamic>;
      return (
        user: UserModel.fromJson(data),
        accessToken: data['accessToken'] as String?,
        refreshToken: data['refreshToken'] as String?,
      );
    } catch (e) {
      print('UPDATE PROFILE ERROR: $e');
      rethrow;
    }
  }
}