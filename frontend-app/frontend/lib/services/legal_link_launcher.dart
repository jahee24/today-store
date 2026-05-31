import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../config/legal_urls.dart';

class LegalLinkLauncher {
  LegalLinkLauncher._();

  static Future<void> openAgreement(BuildContext context) async {
    await _open(
      context,
      LegalUrls.agreement,
      missingLabel: '이용약관 및 개인정보처리방침',
    );
  }

  static Future<void> _open(
    BuildContext context,
    String rawUrl, {
    required String missingLabel,
  }) async {
    final url = rawUrl.trim();
    if (url.isEmpty) {
      if (!context.mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('$missingLabel 문서 URL이 아직 설정되지 않았어요.')),
      );
      return;
    }

    final uri = Uri.tryParse(url);
    if (uri == null || !(uri.isScheme('https') || uri.isScheme('http'))) {
      if (!context.mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('올바르지 않은 링크예요.')),
      );
      return;
    }

    final launched = await launchUrl(uri, mode: LaunchMode.externalApplication);
    if (!launched && context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('$missingLabel 문서를 열 수 없어요.')),
      );
    }
  }
}
