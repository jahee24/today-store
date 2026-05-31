import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../../data/providers/auth_provider.dart';
import '../../widgets/legal_agreement_footer.dart';
import 'login_loading.dart';

class LoginScreen extends ConsumerWidget {
  const LoginScreen({super.key});
  
  static const Color _kakaoBackground = Color(0xFFFEE500);
  static const Color _kakaoForeground = Color(0xFF231815);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final textTheme = Theme.of(context).textTheme;
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
                  const Spacer(flex: 4),

                  Image.asset(
                    'assets/images/app_logo_login.png',
                    width: AppLayout.h(context, 220),
                    fit: BoxFit.contain,
                    filterQuality: FilterQuality.high,
                    isAntiAlias: true,
                  ),
                  const Spacer(flex: 2),

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

                  const LegalAgreementFooter(),

                  const Spacer(flex: 3),
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
