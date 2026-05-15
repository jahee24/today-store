import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../config/app_theme.dart';
import '../../../services/external_app_launcher.dart';
import '../../widgets/buttons/back_arrow_button.dart';
import '../../widgets/buttons/primary_button.dart';

/// 외부 앱이 없을 때 App Store / Play Store 설치를 안내하는 화면.
class AppInstallPromptScreen extends StatelessWidget {
  const AppInstallPromptScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final key = ExternalAppLauncher.parseAppKey(
      GoRouterState.of(context).uri.queryParameters['target'],
    );

    if (key == null) {
      return Scaffold(
        backgroundColor: AppTheme.backgroundColor,
        body: SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(24, 18, 24, 24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    BackArrowButton(onTap: () => context.pop()),
                    const SizedBox(width: 14),
                    Text(
                      '앱 설치',
                      style: textTheme.headlineSmall?.copyWith(
                        fontWeight: FontWeight.w700,
                        color: AppTheme.textPrimary,
                      ),
                    ),
                  ],
                ),
                const Spacer(),
                Center(
                  child: Text(
                    '유효하지 않은 요청이에요.',
                    style: textTheme.bodyLarge?.copyWith(
                      color: AppTheme.textSecondary,
                    ),
                  ),
                ),
                const Spacer(),
              ],
            ),
          ),
        ),
      );
    }

    final name = key.displayName;

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 18, 24, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  BackArrowButton(onTap: () => context.pop()),
                  const SizedBox(width: 14),
                  Text(
                    '앱 설치',
                    style: textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                ],
              ),
              const Spacer(),
              Text(
                '$name 앱이 필요해요',
                style: textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: AppTheme.textPrimary,
                  height: 1.25,
                ),
              ),
              const SizedBox(height: 12),
              Text(
                '기기에 $name이(가) 설치되어 있지 않아요.\n'
                'App Store 또는 Play Store에서 설치한 뒤 다시 시도해 주세요.',
                style: textTheme.bodyLarge?.copyWith(
                  color: AppTheme.textSecondary,
                  fontWeight: FontWeight.w500,
                  height: 1.55,
                ),
              ),
              const Spacer(),
              PrimaryButton(
                text: '스토어에서 설치하기',
                height: 64,
                onPressed: () async {
                  final ok = await ExternalAppLauncher.openStore(key);
                  if (!context.mounted) {
                    return;
                  }
                  if (!ok) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('스토어를 열 수 없어요.')),
                    );
                  }
                },
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                height: 56,
                child: OutlinedButton(
                  onPressed: () => context.pop(),
                  style: OutlinedButton.styleFrom(
                    side: const BorderSide(color: AppTheme.borderStrongColor),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(18),
                    ),
                  ),
                  child: Text(
                    '돌아가기',
                    style: textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
