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

  Future<UserModel> getMyProfile() async {
    final response = await dio.get('/api/v1/users/me');
    return UserModel.fromJson(response.data as Map<String, dynamic>);
  }

  Future<UserModel> updateMyProfile({
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
    final response = await dio.patch(
      '/api/v1/users/me',
      data: payload,
    );
    return UserModel.fromJson(response.data as Map<String, dynamic>);
  }
}