import 'package:flutter/material.dart';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../../services/external_app_launcher.dart';

/// 외부 앱 미설치 시 스토어 설치를 안내하는 시스템 스타일 다이얼로그.
class AppInstallDialog {
  AppInstallDialog._();

  static Future<void> show(
    BuildContext context, {
    required StoreListingApp app,
  }) async {
    final name = app.displayName;

    await showDialog<void>(
      context: context,
      barrierDismissible: true,
      builder: (dialogContext) {
        final f = (double v) => AppLayout.f(dialogContext, v);

        return AlertDialog(
          backgroundColor: AppTheme.surfaceColor,
          surfaceTintColor: Colors.transparent,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(28),
          ),
          titlePadding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
          contentPadding: const EdgeInsets.fromLTRB(24, 16, 24, 8),
          actionsPadding: const EdgeInsets.fromLTRB(8, 0, 8, 8),
          title: Text(
            name,
            style: TextStyle(
              fontSize: f(20),
              fontWeight: FontWeight.w500,
              color: AppTheme.textPrimary,
              height: 1.3,
            ),
          ),
          content: Text(
            '기기에 $name이(가) 설치되어 있지 않아요. '
            'App Store 또는 Play Store에서 설치한 뒤 다시 시도해 주세요.',
            style: TextStyle(
              fontSize: f(15),
              fontWeight: FontWeight.w400,
              color: AppTheme.textSecondary,
              height: 1.45,
            ),
          ),
          actionsAlignment: MainAxisAlignment.end,
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(),
              child: Text(
                '취소',
                style: TextStyle(
                  fontSize: f(15),
                  fontWeight: FontWeight.w500,
                  color: AppTheme.primaryColor,
                ),
              ),
            ),
            TextButton(
              onPressed: () async {
                Navigator.of(dialogContext).pop();
                final ok = await ExternalAppLauncher.openStore(app);
                if (!context.mounted) {
                  return;
                }
                if (!ok) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('스토어를 열 수 없어요.')),
                  );
                }
              },
              child: Text(
                '설치하기',
                style: TextStyle(
                  fontSize: f(15),
                  fontWeight: FontWeight.w500,
                  color: AppTheme.primaryColor,
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}
