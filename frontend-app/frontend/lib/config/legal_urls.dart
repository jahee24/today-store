/// 이용약관 및 개인정보처리방침 통합 공개 URL.
///
/// 기본값은 Notion 공개 페이지입니다. 필요 시 dart-define으로 덮어씁니다.
/// ```bash
/// flutter run --dart-define=LEGAL_URL=https://...
/// ```
class LegalUrls {
  LegalUrls._();

  static const String _notionLegalPage =
      'https://foggy-flax-9ae.notion.site/36701f5581598080b132c19d0f89badb';

  /// 로그인 화면 「이용약관 및 개인정보처리방침」 링크
  static const String agreement = String.fromEnvironment(
    'LEGAL_URL',
    defaultValue: _notionLegalPage,
  );

  @Deprecated('Use LegalUrls.agreement')
  static const String terms = agreement;

  @Deprecated('Use LegalUrls.agreement')
  static const String privacy = agreement;

  static bool get hasAgreement => agreement.trim().isNotEmpty;
}
