import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../../data/providers/auth_provider.dart';
import 'login_loading.dart';

class LoginScreen extends ConsumerWidget {
  const LoginScreen({super.key});
  
  static const Color _kakaoBackground = Color(0xFFFEE500);
  static const Color _kakaoForeground = Color(0xFF231815);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final textTheme = Theme.of(context).textTheme;
    final size = MediaQuery.of(context).size;
    final scale = AppLayout.scale(context);
    final horizontalPadding = AppLayout.h(context, 32);
    final buttonHeight = AppLayout.h(context, 75);
    final authState = ref.watch(authProvider);

    ref.listen<AuthState>(authProvider, (_, next) {
      if (next.status == AuthStatus.authenticated) {
        if (next.isFirstLogin) {
          context.go('/profile-setup');
        } else {
          context.go('/dashboard');
        }
      }

      if (next.status == AuthStatus.unauthenticated &&
          next.errorMessage != null) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(next.errorMessage!)));
      }
    });

    return Scaffold(
      backgroundColor: AppTheme.surfaceColor,
      body: Stack(
        children: [
          SafeArea(
            child: Padding(
              padding: EdgeInsets.symmetric(horizontal: horizontalPadding),
              child: Column(
                children: [
                  SizedBox(height: size.height * 0.23),

                  SizedBox(
                    width: AppLayout.h(context, 90),
                    height: AppLayout.h(context, 90),
                    child: Center(
                      child: Text('🏪', style: TextStyle(fontSize: AppLayout.f(context, 58))),
                    ),
                  ),
                  SizedBox(height: AppLayout.h(context, 20)),

                  Text(
                    '오늘의 가게',
                    style: textTheme.headlineLarge?.copyWith(
                      fontSize: AppLayout.f(context, 33),
                      fontWeight: FontWeight.w700,
                      color: AppTheme.textPrimary,
                      height: 1.1,
                      letterSpacing: -0.3,
                    ),
                  ),
                  SizedBox(height: AppLayout.h(context, 17)),

                  Text(
                    'AI가 만들어주는\n우리 가게 홍보 콘텐츠',
                    textAlign: TextAlign.center,
                    style: textTheme.bodyLarge?.copyWith(
                      fontSize: AppLayout.f(context, 18),
                      height: 1.5,
                      color: AppTheme.textTertiary,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const Spacer(),

                  SizedBox(
                    width: double.infinity,
                    height: buttonHeight,
                    child: ElevatedButton(
                      onPressed: authState.status == AuthStatus.loading
                          ? null
                          : () => ref
                                .read(authProvider.notifier)
                                .loginWithKakao(),
                      style: ElevatedButton.styleFrom(
                        elevation: 1,
                        backgroundColor: _kakaoBackground,
                        foregroundColor: _kakaoForeground,
                        disabledBackgroundColor: AppTheme.disabledBg,
                        disabledForegroundColor: AppTheme.disabledText,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(14),
                        ),
                        padding: EdgeInsets.zero,
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const Text('💬', style: TextStyle(fontSize: 18)),
                          SizedBox(width: AppLayout.h(context, 13)),
                          Text(
                            '카카오로 시작하기',
                            style: textTheme.titleMedium?.copyWith(
                              fontSize: AppLayout.f(context, 20),
                              fontWeight: FontWeight.w600,
                              color: _kakaoForeground,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  SizedBox(height: AppLayout.h(context, 18)),

                  SizedBox(
                    width: double.infinity,
                    height: buttonHeight,
                    child: OutlinedButton(
                      onPressed: authState.status == AuthStatus.loading
                          ? null
                          : () => ref
                                .read(authProvider.notifier)
                                .loginWithGoogle(),
                      style: OutlinedButton.styleFrom(
                        elevation: 1,
                        backgroundColor: AppTheme.surfaceColor,
                        side: const BorderSide(
                          color: AppTheme.borderColor,
                          width: 1.2,
                        ),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(14),
                        ),
                        padding: EdgeInsets.zero,
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(
                            'G',
                            style: TextStyle(
                              fontSize: AppLayout.f(context, 24),
                              fontWeight: FontWeight.w500,
                              color: AppTheme.textPrimary,
                            ),
                          ),
                          SizedBox(width: AppLayout.h(context, 11)),

                          Text(
                            'Google로 시작하기',
                            style: textTheme.titleMedium?.copyWith(
                              fontSize: AppLayout.f(context, 20),
                              fontWeight: FontWeight.w600,
                              color: AppTheme.textPrimary,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  SizedBox(height: AppLayout.h(context, 28)),

                  Text(
                    '로그인 시 이용약관 및 개인정보처리방침에 동의합니다',
                    textAlign: TextAlign.center,
                    style: textTheme.bodyMedium?.copyWith(
                      fontSize: AppLayout.f(context, 14),
                      color: AppTheme.textHint,
                      fontWeight: FontWeight.w400,
                    ),
                  ),

                  SizedBox(height: size.height * (0.18 * scale.clamp(0.9, 1.0))),
                ],
              ),
            ),
          ),

          if (authState.status == AuthStatus.loading) const LoginLoadingView(),
        ],
      ),
    );
  }
}
