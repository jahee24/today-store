import 'package:dio/dio.dart';
import '../../models/auth_response_model.dart';

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
}