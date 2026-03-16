import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../config/app_theme.dart';
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
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      switch (status) {
        case SplashStatus.authenticated:
          context.go('/dashboard');
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
    ref.listen<AsyncValue<SplashStatus>>(splashProvider, (_, next) {
      next.whenData(_handleStatus);
    });

    final size = MediaQuery.of(context).size;
    final textTheme = Theme.of(context).textTheme;

    return Scaffold(
      body: Container(
        width: double.infinity,
        height: double.infinity,
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topRight,
            end: Alignment.bottomLeft,
            colors: [AppTheme.primaryColor, Color(0xFF7A3AED)],
          ),
        ),
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
                        width: 126,
                        height: 126,
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.14),
                          borderRadius: BorderRadius.circular(28),
                        ),
                        child: const Center(
                          child: Text('🏪', style: TextStyle(fontSize: 60)),
                        ),
                      ),

                      const SizedBox(height: 30),

                      Text(
                        '오늘의 가게',
                        style: textTheme.headlineLarge?.copyWith(
                          fontSize: 36,
                          fontWeight: FontWeight.w700,
                          color: Colors.white,
                          letterSpacing: -0.8,
                          height: 1.1,
                        ),
                      ),

                      const SizedBox(height: 18),

                      Text(
                        '소상공인 맞춤형 AI 홍보 비서',
                        style: textTheme.bodyLarge?.copyWith(
                          fontSize: 18,
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
                padding: const EdgeInsets.only(bottom: 90),
                child: SizedBox(
                  width: 44,
                  height: 44,
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
