class ApiConfig {
  static const String baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'https://dev-backend-service-549695709482.asia-northeast3.run.app',);
}