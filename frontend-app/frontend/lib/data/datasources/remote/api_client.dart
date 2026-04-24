import 'package:dio/dio.dart';
import '../../../config/api_config.dart';
import '../../../services/token_service.dart';

class ApiClient {
  final TokenService tokenService;

  late final Dio dio;
  bool _isRefreshing = false;

  ApiClient({required this.tokenService}) {
    dio = Dio(
      BaseOptions(
        baseUrl: ApiConfig.baseUrl,
        connectTimeout: const Duration(seconds: 30),
        receiveTimeout: const Duration(seconds: 30),
        headers: {
          'Content-type' : 'application/json',
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
        },onError: (error, handler) async {
          final statusCode = error.response?.statusCode;
          final requestPath = error.requestOptions.path;

          final isRefreshRequest = requestPath == '/api/v1/auth/refresh';
          final isOauthLoginRequest = requestPath == '/api/v1/auth/oauth/login';

          if (statusCode == 401 && !isRefreshRequest && !isOauthLoginRequest) {
            if (_isRefreshing) {
              return handler.reject(error);
            }

            _isRefreshing = true;

            try {
              final refreshToken = await tokenService.getRefreshToken();

              if (refreshToken == null || refreshToken.isEmpty) {
                await tokenService.clearTokens();
                _isRefreshing = false;
                return handler.reject(error);
              }

              final refreshDio = Dio(
                BaseOptions(
                  baseUrl: ApiConfig.baseUrl,
                  headers: {'Content-type' : 'application/json'},
                ),
              );

              final refreshResponse = await refreshDio.post(
                '/api/v1/auth/refresh',
                data: {
                  'refreshToken' : refreshToken,
                },
              );

              final newAccessToken = refreshResponse.data['accessToken'] as String?;

              if (newAccessToken == null || newAccessToken.isEmpty) {
                await tokenService.clearTokens();
                _isRefreshing = false;
                return handler.reject(error);
              }

              await tokenService.saveAccessToken(newAccessToken);

              final requestOptions = error.requestOptions;
              requestOptions.headers['Authorization'] = 'Bearer $newAccessToken';

              final clonedResponse = await dio.fetch(requestOptions);

              _isRefreshing = false;
              return handler.resolve(clonedResponse);
            } catch (_) {
              await tokenService.clearTokens();
              _isRefreshing = false;
              return handler.reject(error);
            }
          }

          handler.next(error);
        },
      ),
    );
  }
}