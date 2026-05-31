/// Google OAuth 클라이언트 ID (Google Cloud → 클라이언트).
///
/// iOS 설정 후 [iosClientId]와 `ios/Runner/Info.plist`의 URL Scheme을
/// 같은 iOS 클라이언트 기준으로 맞춰 주세요.
class GoogleAuthConfig {
  GoogleAuthConfig._();

  /// Web 클라이언트 — `serverClientId` / 백엔드 토큰 검증용
  static const String webClientId =
      '549695709482-asrt15cmpvk0g3e97m2jlfkcagvg5ncl.apps.googleusercontent.com';

  /// iOS 클라이언트 ID (Google Cloud에서 iOS 유형으로 생성)
  ///
  /// 예: `549695709482-xxxxxxxx.apps.googleusercontent.com`
  /// `--dart-define=GOOGLE_IOS_CLIENT_ID=...` 로 실행 시 덮어쓸 수 있음.
  static const String iosClientId = String.fromEnvironment(
    'GOOGLE_IOS_CLIENT_ID',
    defaultValue: _iosClientIdFallback,
  );

  static const String _iosClientIdFallback = '549695709482-5vg3rn07h7s8k4mlbm5fv2nktdqijomp.apps.googleusercontent.com';

  static bool get isIosConfigured =>
      iosClientId.isNotEmpty &&
      iosClientId.endsWith('.apps.googleusercontent.com');

  /// Info.plist `CFBundleURLSchemes`에 넣을 값
  static String get reversedIosClientId {
    final suffix =
        iosClientId.replaceAll('.apps.googleusercontent.com', '');
    return 'com.googleusercontent.apps.$suffix';
  }
}
