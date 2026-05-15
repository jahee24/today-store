import 'dart:io' show Platform;

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart' hide AuthApi;
import 'package:google_sign_in/google_sign_in.dart';

import '../datasources/remote/api_client.dart';
import '../datasources/remote/auth_api.dart';
import '../models/user_model.dart';
import '../repositories/auth_repository.dart';
import '../../services/token_service.dart';
import 'network_provider.dart';

enum AuthStatus {
  initial,
  loading,
  authenticated,
  unauthenticated,
}

class AuthState {
  final AuthStatus status;
  final bool isFirstLogin;
  final String? errorMessage;

  const AuthState({
    required this.status,
    this.isFirstLogin = false,
    this.errorMessage,
  });

  AuthState copyWith({
    AuthStatus? status,
    bool? isFirstLogin,
    String? errorMessage,
  }) {
    return AuthState(
      status: status ?? this.status,
      isFirstLogin: isFirstLogin ?? this.isFirstLogin,
      errorMessage: errorMessage,
    );
  }
}

final authApiProvider = Provider<AuthApi>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return AuthApi(apiClient.dio);
});

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(
    authApi: ref.watch(authApiProvider), 
    tokenService: ref.watch(tokenServiceProvider),
  );
});

final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  final notifier = AuthNotifier(
    authRepository: ref.watch(authRepositoryProvider),
  );

  // 전역 인증 실패 이벤트 감시
  ref.listen(authFailureEventProvider, (previous, next) {
    if (next is AsyncData) {
      notifier.forceLogout('로그인 세션이 만료되었습니다.\n보안을 위해 다시 로그인해 주세요.');
    }
  });

  return notifier;
});

final currentUserProvider = FutureProvider<UserModel>((ref) async {
  final authRepository = ref.read(authRepositoryProvider);
  return authRepository.getMyProfile();
});

class AuthNotifier extends StateNotifier<AuthState> {
  final AuthRepository authRepository;

  AuthNotifier({
    required this.authRepository,
  }) : super(const AuthState(status: AuthStatus.initial));

  String _buildAuthErrorMessage(Object error) {
    if (error is DioException) {
      final statusCode = error.response?.statusCode;
      final data = error.response?.data;
      if (data is Map<String, dynamic>) {
        final message = data['message']?.toString().trim();
        if (message != null && message.isNotEmpty) {
          return statusCode != null ? '[$statusCode] $message' : message;
        }
      }
      if (statusCode == 500) {
        return '서버 내부 오류(500)가 발생했어요. 잠시 후 다시 시도해 주세요.';
      }
      return statusCode != null
          ? '로그인 요청이 실패했어요. (HTTP $statusCode)'
          : '로그인 요청 중 네트워크 오류가 발생했어요.';
    }

    return '로그인 처리 중 오류가 발생했어요.';
  }

  Future<bool> tryAutoLogin() async {
    state = state.copyWith(status: AuthStatus.loading);
    try {
      final success = await authRepository.tryAutoLogin();
      if (success) {
        state = state.copyWith(status: AuthStatus.authenticated);
        return true;
      } else {
        state = state.copyWith(status: AuthStatus.unauthenticated);
        return false;
      }
    } catch (_) {
      state = state.copyWith(status: AuthStatus.unauthenticated);
      return false;
    }
  }

  Future<void> loginWithGoogle() async {
    // iOS는 GoogleService-Info.plist 설정 전까지 네이티브 크래시 방지
    if (Platform.isIOS) {
      state = state.copyWith(
        status: AuthStatus.unauthenticated,
        errorMessage: 'iOS Google 로그인은 준비 중이에요.\n카카오 로그인을 이용해주세요.',
      );
      return;
    }

    state = state.copyWith(
      status: AuthStatus.loading,
      errorMessage: null,
    );

    try {
      final googleSignIn = GoogleSignIn(
        scopes: <String>['email', 'profile'],
        serverClientId: '549695709482-asrt15cmpvk0g3e97m2jlfkcagvg5ncl.apps.googleusercontent.com',
      );

      final GoogleSignInAccount? account = await googleSignIn.signIn();

      if (account == null) {
        state = state.copyWith(
          status: AuthStatus.unauthenticated,
          errorMessage: 'Google 로그인이 취소되었어요.',
        );
        return;
      }

      final GoogleSignInAuthentication auth = await account.authentication;
      final String? accessToken = auth.accessToken;

      if (accessToken == null || accessToken.isEmpty) {
        throw Exception('Google 액세스 토큰을 가져오지 못했어요.');
      }

      final result = await authRepository.oauthLogin(
        provider: 'google',
        accessToken: accessToken,
      );
      state = state.copyWith(
        status: AuthStatus.authenticated,
        isFirstLogin: result.isFirstLogin,
        errorMessage: null,
      );
    } catch (e) {
      state = state.copyWith(
        status: AuthStatus.unauthenticated,
        errorMessage: _buildAuthErrorMessage(e),
      );
    }
  }

  Future<void> loginWithKakao() async {
    state = state.copyWith(status: AuthStatus.loading, errorMessage: null);

    try {
      OAuthToken token;

      if (await isKakaoTalkInstalled()) {
        try {
          token = await UserApi.instance.loginWithKakaoTalk();
        } catch (error) {
            token = await UserApi.instance.loginWithKakaoAccount();
        }
      } else {
        token = await UserApi.instance.loginWithKakaoAccount();
      }

      final result = await authRepository.oauthLogin(
        provider: 'kakao', 
        accessToken: token.accessToken,
      );

      state = state.copyWith(
        status: AuthStatus.authenticated,
        isFirstLogin: result.isFirstLogin,
        errorMessage: null,
      );
    } catch (e) {
      state = state.copyWith(
        status: AuthStatus.unauthenticated,
        errorMessage: _buildAuthErrorMessage(e),
      );
    }
  }

  Future<void> logout() async {
    await authRepository.logout();
    state = const AuthState(status: AuthStatus.unauthenticated);
  }

  void forceLogout(String message) {
    state = AuthState(
      status: AuthStatus.unauthenticated,
      errorMessage: message,
    );
  }
}