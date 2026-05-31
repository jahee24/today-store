import '../datasources/remote/auth_api.dart';
import '../models/auth_response_model.dart';
import '../models/user_model.dart';
import '../../services/token_service.dart';

class AuthRepository {
  final AuthApi authApi;
  final TokenService tokenService;

  AuthRepository({
    required this.authApi,
    required this.tokenService,
  });

  Future<AuthResponseModel> oauthLogin({
    required String provider,
    required String accessToken,
  }) async {
    final result = await authApi.oauthLogin(
      provider: provider, 
      accessToken: accessToken
    );

    await tokenService.saveTokens(
      accessToken: result.accessToken, 
      refreshToken: result.refreshToken
    );

    return result;
  }

  Future<bool> tryAutoLogin() async {
    final refreshToken = await tokenService.getRefreshToken();

    if (refreshToken == null || refreshToken.isEmpty) {
      return false;
    }

    try {
      final result = await authApi.refresh(refreshToken: refreshToken);

      await tokenService.saveAccessToken(result.accessToken);

      if (result.refreshToken.isNotEmpty) {
        await tokenService.saveRefreshToken(result.refreshToken);
      }

      return true;
    } catch (_) {
      await tokenService.clearTokens();
      return false;
    }
  }

  Future<void> logout() async {
    try {
      final refreshToken = await tokenService.getRefreshToken();

      if (refreshToken != null && refreshToken.isNotEmpty) {
        await authApi.logout(refreshToken: refreshToken);
      }
    } catch (_) {
      // 서버 로그아웃 실패여도 로컬 토큰은 삭제
    } finally {
      await tokenService.clearTokens();
    }
  }

  Future<void> withdraw() async {
    await authApi.deleteAccount();
    await tokenService.clearTokens();
  }

  Future<UserModel> getMyProfile() {
    return authApi.getMyProfile();
  }

  Future<UserModel> updateMyProfile({
    String? name,
    String? email,
  }) async {
    final result = await authApi.updateMyProfile(name: name, email: email);

    // 이메일 변경 시 백엔드가 새 JWT를 내려줌 → 저장해야 세션 유지
    if (result.accessToken != null && result.accessToken!.isNotEmpty) {
      await tokenService.saveAccessToken(result.accessToken!);
    }
    if (result.refreshToken != null && result.refreshToken!.isNotEmpty) {
      await tokenService.saveRefreshToken(result.refreshToken!);
    }

    return result.user;
  }
}