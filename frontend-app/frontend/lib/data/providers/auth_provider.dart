import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../datasources/remote/api_client.dart';
import '../datasources/remote/auth_api.dart';
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

final tokenServiceProvider = Provider<TokenService>((ref) {
  return TokenService();
});

final apiClientProvider = Provider<ApiClient>((ref) {
  final tokenService = ref.read(tokenServiceProvider);
  return ApiClient(tokenService: tokenService);
});

final authApiProvider = Provider<AuthApi>((ref) {
  final apiClient = ref.read(apiClientProvider);
  return AuthApi(apiClient.dio);
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

class AuthNotifier extends StateNotifier<AuthState> {
  final AuthRepository authRepository;

  AuthNotifier({
    required this.authRepository,
  }) : super(const AuthState(status: AuthStatus.initial));

  Future<void> loginWithGoogle() async {
    state = state.copyWith(status: AuthStatus.loading, errorMessage: null);

    try {

      await Future.delayed(const Duration(seconds: 20));
      // 실제 Google SDK 로그인 붙이기
      //final socialAccessToken = 'google_social_access_token';

      final result = await authRepository.oauthLogin(
        provider: 'google', 
        accessToken: 'google_social_access_token', // socialAccessToken
      );

      state = state.copyWith(
        status: AuthStatus.authenticated,
        isFirstLogin: result.isFirstLogin,
      );
    } catch (e) {
      state = state.copyWith(
        status: AuthStatus.unauthenticated,
        errorMessage: e.toString(),
      );
    }
  }

  Future<void> loginWithKakao() async {
    state = state.copyWith(status: AuthStatus.loading, errorMessage: null);

    try {
      // 실제 카카오 SDK 로그인 붙이기
      final socialAccessToken = 'kakao_social_access_token';

      final result = await authRepository.oauthLogin(
        provider: 'kakao', 
        accessToken: socialAccessToken,
      );

      state = state.copyWith(
        status: AuthStatus.authenticated,
        isFirstLogin: result.isFirstLogin,
      );
    } catch (e) {
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