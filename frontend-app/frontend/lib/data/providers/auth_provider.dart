import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:fronted/data/models/user_model.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';

import '../datasources/remote/api_client.dart';
import '../datasources/remote/auth_api.dart' as app;
import '../repositories/auth_repository.dart';
import '../../services/token_service.dart';

enum AuthStatus {
  initial,
  loading,
  authenticated,
  unauthenticated,
}

class AuthState {
  final AuthStatus status;
  final bool isFirstLogin;
  final UserModel? user;
  final String? errorMessage;

  const AuthState({
    required this.status,
    this.isFirstLogin = false,
    this.user,
    this.errorMessage,
  });

  AuthState copyWith({
    AuthStatus? status,
    bool? isFirstLogin,
    UserModel? user,
    String? errorMessage,
    bool cleanUser = false,
  }) {
    return AuthState(
      status: status ?? this.status,
      isFirstLogin: isFirstLogin ?? this.isFirstLogin,
      user: cleanUser ? null : (user ?? this.user),
      errorMessage: errorMessage,
    );
  }
}

final tokenServiceProvider = Provider<TokenService>((ref) => TokenService());

final apiClientProvider = Provider<ApiClient>((ref) {
  final tokenService = ref.read(tokenServiceProvider);
  return ApiClient(tokenService: tokenService);
});

final authApiProvider = Provider<app.AuthApi>((ref) {
  final apiClient = ref.read(apiClientProvider);
  return app.AuthApi(apiClient.dio);
});

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(
    authApi: ref.read(authApiProvider),
    tokenService: ref.read(tokenServiceProvider),
  );
});

final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(
    authRepository: ref.read(authRepositoryProvider),
  );
});

const String _googleServerClientId =
    String.fromEnvironment('GOOGLE_SERVER_CLIENT_ID');

class AuthNotifier extends StateNotifier<AuthState> {
  final AuthRepository authRepository;

  AuthNotifier({
    required this.authRepository,
  }) : super(const AuthState(status: AuthStatus.initial));

  Future<void> initialize() async {
    state = state.copyWith(
      status: AuthStatus.loading,
      errorMessage: null,
    );

    final loggedIn = await authRepository.tryAutoLogin();

    if (!loggedIn) {
      state = const AuthState(status: AuthStatus.unauthenticated);
      return;
    }

    try {
      final me = await authRepository.getMe();

      state = state.copyWith(
        status: AuthStatus.authenticated,
        user: me,
        isFirstLogin: me.isFirstLogin ?? false,
        errorMessage: null,
      );
    } catch (e) {
      state = state.copyWith(
        status: AuthStatus.unauthenticated,
        cleanUser: true,
        errorMessage: e.toString(),
      );
    }
  }

  Future<void> loginWithKakao() async {
    state = state.copyWith(
      status: AuthStatus.loading,
      errorMessage: null,
    );

    try {
      debugPrint('[KAKAO] 로그인 시작');

      OAuthToken token;

      if (await isKakaoTalkInstalled()) {
        debugPrint('[KAKAO] 카카오톡 SDK 로그인 시도');
        token = await UserApi.instance.loginWithKakaoTalk();
      } else {
        debugPrint('[KAKAO] 카카오계정 SDK 로그인 시도');
        token = await UserApi.instance.loginWithKakaoAccount();
      }

      debugPrint('[KAKAO] SDK 로그인 성공');

      final result = await authRepository.oauthLogin(
        provider: 'kakao',
        accessToken: token.accessToken,
      );

      debugPrint('[KAKAO] 백엔드 로그인 성공');
      debugPrint('[KAKAO] isFirstLogin=${result.isFirstLogin}');
      debugPrint('[KAKAO] accessToken empty=${result.accessToken.isEmpty}');
      debugPrint('[KAKAO] refreshToken empty=${result.refreshToken.isEmpty}');

      state = state.copyWith(
        status: AuthStatus.authenticated,
        isFirstLogin: result.isFirstLogin,
        user: result.user,
        errorMessage: null,
      );
    } catch (e, st) {
      if (e is DioException) {
        debugPrint('[KAKAO] 로그인 실패 statusCode=${e.response?.statusCode}');
        debugPrint('[KAKAO] 로그인 실패 response=${e.response?.data}');
        debugPrint('[KAKAO] 로그인 실패 request=${e.requestOptions.path}');
      } else {
        debugPrint('[KAKAO] 로그인 실패: $e');
      }

      debugPrintStack(stackTrace: st);

      state = state.copyWith(
        status: AuthStatus.unauthenticated,
        errorMessage: e.toString(),
      );
    }
  }

  Future<void> loginWithGoogle() async {
    state = state.copyWith(
      status: AuthStatus.loading,
      errorMessage: null,
    );

    try {
      final GoogleSignIn googleSignIn = _googleServerClientId.isNotEmpty
          ? GoogleSignIn(
              scopes: <String>['email', 'profile'],
              serverClientId: _googleServerClientId,
            )
          : GoogleSignIn(
              scopes: ['email', 'profile'],
            );

      final GoogleSignInAccount? account = await googleSignIn.signIn();

      if (account == null) {
        state = state.copyWith(
          status: AuthStatus.unauthenticated,
          errorMessage: 'Google 로그인이 취소되었어요.',
        );
        return;
      }

      final String? code = account.serverAuthCode;

      if (code == null || code.isEmpty) {
        throw Exception('Google 서버 인증 코드를 가져오지 못했어요.');
      }

      final result = await authRepository.oauthLogin(
        provider: 'google',
        accessToken: code,
      );

      state = state.copyWith(
        status: AuthStatus.authenticated,
        isFirstLogin: result.isFirstLogin,
        user: result.user,
        errorMessage: null,
      );
    } catch (e, st) {
      debugPrint('[GOOGLE] 로그인 실패: $e');
      debugPrintStack(stackTrace: st);

      state = state.copyWith(
        status: AuthStatus.unauthenticated,
        errorMessage: e.toString(),
      );
    }
  }

  Future<void> logout() async {
    await authRepository.logout();
    state = const AuthState(status: AuthStatus.unauthenticated);
  }
}