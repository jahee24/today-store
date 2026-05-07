import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import 'splash_provider.dart';

class SplashScreen extends ConsumerStatefulWidget {
  const SplashScreen({super.key});

  @override
  ConsumerState<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends ConsumerState<SplashScreen>
    with SingleTickerProviderStateMixin {
  late final AnimationController _animController;
  late final Animation<double> _fadeAnim;
  late final Animation<double> _scaleAnim;
  SplashStatus? _lastHandledStatus;

  @override
  void initState() {
    super.initState();

    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 700),
    );
    _fadeAnim = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(parent: _animController, curve: Curves.easeIn));
    _scaleAnim = Tween<double>(begin: 0.9, end: 1.0).animate(
      CurvedAnimation(parent: _animController, curve: Curves.easeOutBack),
    );

    _animController.forward();
  }

  @override
  void dispose() {
    _animController.dispose();
    super.dispose();
  }

  // Async 상태에 따라 라우팅
  void _handleStatus(SplashStatus status) {
    if (status == SplashStatus.loading) {
      return;
    }
    if (_lastHandledStatus == status) {
      return;
    }
    _lastHandledStatus = status;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      switch (status) {
        case SplashStatus.authenticated:
          context.go('/dashboard');
          break;
        case SplashStatus.needsProfileSetup:
          context.go('/profile-setup');
          break;
        case SplashStatus.unauthenticated:
          context.go('/login');
          break;
        case SplashStatus.loading:
          break;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final splashState = ref.watch(splashProvider);
    splashState.when(
      data: _handleStatus,
      loading: () {},
      error: (_, __) => _handleStatus(SplashStatus.unauthenticated),
    );

    final size = MediaQuery.of(context).size;
    final textTheme = Theme.of(context).textTheme;
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);

    return Scaffold(
      body: Container(
        width: double.infinity,
        height: double.infinity,
        color: AppTheme.primaryColor,
        child: SafeArea(
          child: Column(
            children: [
              SizedBox(height: size.height * 0.3),

              // 로고영역
              FadeTransition(
                opacity: _fadeAnim,
                child: ScaleTransition(
                  scale: _scaleAnim,
                  child: Column(
                    children: [
                      Container(
                        width: h(118),
                        height: h(118),
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.14),
                          borderRadius: BorderRadius.circular(h(24)),
                        ),
                        child: Center(
                          child: Text('🏪', style: TextStyle(fontSize: f(56))),
                        ),
                      ),

                      SizedBox(height: h(24)),

                      Text(
                        '오늘의 가게',
                        style: textTheme.headlineLarge?.copyWith(
                          fontSize: f(36),
                          fontWeight: FontWeight.w700,
                          color: Colors.white,
                          letterSpacing: -0.8,
                          height: 1.1,
                        ),
                      ),

                      SizedBox(height: h(14)),

                      Text(
                        '소상공인 맞춤형 AI 홍보 비서',
                        style: textTheme.bodyLarge?.copyWith(
                          fontSize: f(18),
                          color: Colors.white.withOpacity(0.85),
                          fontWeight: FontWeight.w400,
                          letterSpacing: -0.1,
                          height: 1.2,
                        ),
                      ),
                    ],
                  ),
                ),
              ),

              const Spacer(),

              // 스피너
              Padding(
                padding: EdgeInsets.only(bottom: h(76)),
                child: SizedBox(
                  width: h(40),
                  height: h(40),
                  child: CircularProgressIndicator(
                    strokeWidth: 4,
                    valueColor: AlwaysStoppedAnimation<Color>(
                      Colors.white.withOpacity(0.78),
                    ),
                    backgroundColor: Colors.white.withOpacity(0.18),
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
