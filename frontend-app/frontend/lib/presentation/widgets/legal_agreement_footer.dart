import 'package:flutter/material.dart';

import '../../config/app_theme.dart';
import '../../config/constants.dart';
import '../../services/legal_link_launcher.dart';

/// 로그인 화면 하단 「이용약관 및 개인정보처리방침」 링크.
class LegalAgreementFooter extends StatelessWidget {
  const LegalAgreementFooter({super.key});

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final baseStyle = textTheme.bodyMedium?.copyWith(
      fontSize: AppLayout.f(context, 12),
      color: AppTheme.textHint,
      fontWeight: FontWeight.w400,
      height: 1.45,
    );
    final linkStyle = baseStyle?.copyWith(
      color: AppTheme.primaryColor,
      decoration: TextDecoration.underline,
      decorationColor: AppTheme.primaryColor,
      fontWeight: FontWeight.w600,
    );

    return Wrap(
      alignment: WrapAlignment.center,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        Text('로그인 시 ', style: baseStyle),
        _LegalLink(
          label: '이용약관 및 개인정보처리방침',
          style: linkStyle,
          onTap: () => LegalLinkLauncher.openAgreement(context),
        ),
        Text('에 동의합니다', style: baseStyle),
      ],
    );
  }
}

class _LegalLink extends StatelessWidget {
  const _LegalLink({
    required this.label,
    required this.style,
    required this.onTap,
  });

  final String label;
  final TextStyle? style;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Text(label, style: style),
    );
  }
}
