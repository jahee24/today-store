import 'package:flutter/material.dart';
import '../../../config/app_theme.dart';
import '../../../config/constants.dart';

class LoginLoadingView extends StatelessWidget {
  const LoginLoadingView({super.key});

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;
    final textTheme = Theme.of(context).textTheme;
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);

    return Positioned.fill(
      child: Container(
        color: AppTheme.backgroundColor,
        child: SafeArea(
          child: Column(
            children: [
              SizedBox(height: size.height * 0.35),
              SizedBox(
                width: h(64),
                height: h(64),
                child: CircularProgressIndicator(
                  strokeWidth: 5,
                  backgroundColor: const Color(0xFFE8EAFE),
                  valueColor: AlwaysStoppedAnimation<Color>(AppTheme.primaryColor),
                ),
              ),
              SizedBox(height: h(30)),
              Text(
                '로그인 중...',
                style: textTheme.headlineSmall?.copyWith(
                  fontSize: f(24),
                  fontWeight: FontWeight.w600,
                  color: AppTheme.textPrimary,
                ),
              ),
              SizedBox(height: h(8)),
              Text(
                '잠시만 기다려 주세요',
                style: textTheme.bodyLarge?.copyWith(
                  fontSize: f(17),
                  fontWeight: FontWeight.w400,
                  color: AppTheme.textTertiary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
