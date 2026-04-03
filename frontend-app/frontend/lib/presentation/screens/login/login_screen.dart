import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../config/app_theme.dart';
import '../../../data/providers/auth_provider.dart';
import 'login_loading_view.dart';

class LoginScreen extends ConsumerWidget {
  const LoginScreen({super.key});
  
  static const Color _kakaoBackground = Color(0xFFFEE500);
  static const Color _kakaoForeground = Color(0xFF231815);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final textTheme = Theme.of(context).textTheme;
    final size = MediaQuery.of(context).size;
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
              padding: const EdgeInsets.symmetric(horizontal: 32),
              child: Column(
                children: [
                  SizedBox(height: size.height * 0.23),

                  const SizedBox(
                    width: 90,
                    height: 90,
                    child: Center(
                      child: Text('🏪', style: TextStyle(fontSize: 58)),
                    ),
                  ),
                  const SizedBox(height: 20),

                  Text(
                    '오늘의 가게',
                    style: textTheme.headlineLarge?.copyWith(
                      fontSize: 33,
                      fontWeight: FontWeight.w700,
                      color: AppTheme.textPrimary,
                      height: 1.1,
                      letterSpacing: -0.3,
                    ),
                  ),
                  const SizedBox(height: 17),

                  Text(
                    'AI가 만들어주는\n우리 가게 홍보 콘텐츠',
                    textAlign: TextAlign.center,
                    style: textTheme.bodyLarge?.copyWith(
                      fontSize: 18,
                      height: 1.5,
                      color: AppTheme.textTertiary,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const Spacer(),

                  SizedBox(
                    width: double.infinity,
                    height: 75,
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
                          const SizedBox(width: 13),
                          Text(
                            '카카오로 시작하기',
                            style: textTheme.titleMedium?.copyWith(
                              fontSize: 20,
                              fontWeight: FontWeight.w600,
                              color: _kakaoForeground,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 18),

                  SizedBox(
                    width: double.infinity,
                    height: 75,
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
                          const Text(
                            'G',
                            style: TextStyle(
                              fontSize: 24,
                              fontWeight: FontWeight.w500,
                              color: AppTheme.textPrimary,
                            ),
                          ),
                          const SizedBox(width: 11),

                          Text(
                            'Google로 시작하기',
                            style: textTheme.titleMedium?.copyWith(
                              fontSize: 20,
                              fontWeight: FontWeight.w600,
                              color: AppTheme.textPrimary,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 28),

                  Text(
                    '로그인 시 이용약관 및 개인정보처리방침에 동의합니다',
                    textAlign: TextAlign.center,
                    style: textTheme.bodyMedium?.copyWith(
                      fontSize: 14,
                      color: AppTheme.textHint,
                      fontWeight: FontWeight.w400,
                    ),
                  ),

                  SizedBox(height: size.height * 0.18),
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
