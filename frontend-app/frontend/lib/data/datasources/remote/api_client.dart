import 'package:dio/dio.dart';
import '../../../config/api_config.dart';
import '../../../services/token_service.dart';

class ApiClient {
  final TokenService tokenService;
  final void Function()? onAuthFailure;

  late final Dio dio;
  Future<void>? _refreshFuture;

  ApiClient({
    required this.tokenService,
    this.onAuthFailure,
  }) {
    dio = Dio(
      BaseOptions(
        baseUrl: ApiConfig.baseUrl,
        connectTimeout: const Duration(seconds: 30),
        receiveTimeout: const Duration(seconds: 30),
        headers: {
          'Content-type': 'application/json',
        },
      ),
    );

    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final accessToken = await tokenService.getAccessToken();

          final isRefreshRequest = options.path == '/api/v1/auth/refresh';
          final isOauthLoginRequest = options.path == '/api/v1/auth/oauth/login';

          if (accessToken != null &&
              accessToken.isNotEmpty &&
              !isRefreshRequest &&
              !isOauthLoginRequest) {
            options.headers['Authorization'] = 'Bearer $accessToken';
          }

          handler.next(options);
        },
        onError: (error, handler) async {
          final statusCode = error.response?.statusCode;
          final requestPath = error.requestOptions.path;

          final isRefreshRequest = requestPath == '/api/v1/auth/refresh';
          final isOauthLoginRequest = requestPath == '/api/v1/auth/oauth/login';

          // 401 Unauthorized 에러가 발생했고, 갱신이나 로그인이 아닌 일반 요청인 경우
          if (statusCode == 401 && !isRefreshRequest && !isOauthLoginRequest) {
            try {
              // 이미 토큰 갱신이 진행 중이라면 해당 Future를 기다림
              // 진행 중이지 않다면 새로운 갱신 프로세스 시작
              _refreshFuture ??= _performTokenRefresh();
              await _refreshFuture;

              // 갱신 성공 후, 새 토큰으로 원래 요청 재시도
              final newAccessToken = await tokenService.getAccessToken();
              final requestOptions = error.requestOptions;
              requestOptions.headers['Authorization'] = 'Bearer $newAccessToken';

              final clonedResponse = await dio.fetch(requestOptions);
              return handler.resolve(clonedResponse);
            } catch (e) {
              // 갱신 실패 시 (Refresh Token 만료 등)
              await tokenService.clearTokens();
              onAuthFailure?.call();
              return handler.reject(error);
            }
          }

          handler.next(error);
        },
      ),
    );
  }

  /// 실제 토큰 갱신 로직을 처리하는 내부 메서드
  Future<void> _performTokenRefresh() async {
    try {
      final refreshToken = await tokenService.getRefreshToken();

      if (refreshToken == null || refreshToken.isEmpty) {
        throw Exception('Refresh token is missing');
      }

      final refreshDio = Dio(
        BaseOptions(
          baseUrl: ApiConfig.baseUrl,
          headers: {'Content-type': 'application/json'},
        ),
      );

      final refreshResponse = await refreshDio.post(
        '/api/v1/auth/refresh',
        data: {
          'refreshToken': refreshToken,
        },
      );

      final newAccessToken = refreshResponse.data['accessToken'] as String?;
      final newRefreshToken = refreshResponse.data['refreshToken'] as String?;

      if (newAccessToken == null || newAccessToken.isEmpty) {
        throw Exception('Failed to receive new access token');
      }

      await tokenService.saveAccessToken(newAccessToken);
      if (newRefreshToken != null && newRefreshToken.isNotEmpty) {
        await tokenService.saveRefreshToken(newRefreshToken);
      }
    } finally {
      // 작업 완료 후 Future 초기화 (다음 401 발생 시 다시 갱신할 수 있도록)
      _refreshFuture = null;
    }
  }
}