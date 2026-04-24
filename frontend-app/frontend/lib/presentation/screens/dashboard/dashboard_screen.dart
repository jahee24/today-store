import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../config/app_theme.dart';
import '../../widgets/cards/recent_content_card.dart';
import '../../widgets/cards/stat_card.dart';
import '../../widgets/buttons/creation_action_button.dart';
import '../../widgets/navigation/bottom_nav_bar.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final size = MediaQuery.of(context).size;

    final horizontalPadding = math.max(16.0, size.width * 0.05);
    final topPadding = math.max(16.0, size.height * 0.02);
    final bottomPadding = math.max(20.0, size.height * 0.025);

    return Scaffold(
      bottomNavigationBar: AppBottomNavBar(currentIndex: 0),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: EdgeInsets.fromLTRB(
            horizontalPadding,
            topPadding,
            horizontalPadding,
            bottomPadding,
          ),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 520),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '안녕하세요 👋',
                              style: textTheme.bodyLarge?.copyWith(
                                color: AppTheme.textSecondary,
                                fontSize: 18,
                              ),
                            ),
                            Text(
                              '맛있는 카페',
                              style: textTheme.headlineMedium?.copyWith(
                                fontSize: 26,
                              ),
                            ),
                          ],
                        ),
                      ),
                      Material(
                        color: AppTheme.fillLight,
                        shape: const CircleBorder(),
                        child: InkWell(
                          customBorder: const CircleBorder(),
                          onTap: () => context.push('/settings'),
                          child: const Padding(
                            padding: EdgeInsets.all(12),
                            child: Icon(
                              Icons.settings_outlined,
                              color: AppTheme.textSecondary,
                              size: 24,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 22),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(32),
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(
                        colors: [AppTheme.primaryColor, AppTheme.infoText],
                      ),
                      borderRadius: BorderRadius.circular(28),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '새 홍보 콘텐츠 만들기',
                          style: textTheme.titleMedium?.copyWith(
                            color: AppTheme.textOnPrimary,
                            fontWeight: FontWeight.w700,
                            fontSize: 23,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          'AI가 알아서 만들어 드려요',
                          style: textTheme.bodyLarge?.copyWith(
                            color: Colors.white.withOpacity(0.85),
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                        const SizedBox(height: 22),
                        Row(
                          children: [
                            Expanded(
                              child: CreationActionButton(
                                label: '+ 문구 생성',
                                onPressed: () => context.push('/step1'),
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: CreationActionButton(
                                label: '+ 이미지 생성',
                                filled: false,
                                onPressed: () => context.push('/image-step1'),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 20),
                  const Row(
                    children: [
                      Expanded(
                        child: StatCard(
                          value: '12',
                          label: '생성한 콘텐츠',
                          valueColor: AppTheme.primaryColor,
                        ),
                      ),
                      SizedBox(width: 12),
                      Expanded(
                        child: StatCard(
                          value: '5',
                          label: '공유 완료',
                          valueColor: Color(0xFF4F8B41),
                        ),
                      ),
                      SizedBox(width: 12),
                      Expanded(
                        child: StatCard(
                          value: '3',
                          label: '이번 주',
                          valueColor: Color(0xFFD16A31),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        '최근 콘텐츠',
                        style: textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.w700,
                          fontSize: 21,
                        ),
                      ),
                      TextButton(
                        onPressed: () {
                          context.push('/history');
                        },
                        child: const Text(
                          '전체보기',
                          style: TextStyle(
                            fontSize: 17,
                            fontWeight: FontWeight.w500,
                            color: AppTheme.primaryColor,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  RecentContentCard(
                    title: '시즌 딸기 라떼 홍보',
                    subtitle: '2분 전 · 감성적',
                    badgeText: '문구',
                    badgeTextColor: AppTheme.primaryColor,
                    badgeBgColor: const Color(0xFFEEEAFE),
                    thumbnailEmoji: '📸',
                    thumbnailBgColor: AppTheme.fillLight,
                    onTap: () => context.push('/result'),
                  ),
                  const SizedBox(height: 14),
                  RecentContentCard(
                    title: '매장 인테리어 사진',
                    subtitle: '어제 · 8장 생성',
                    badgeText: '이미지',
                    badgeTextColor: const Color(0xFF5B9B4C),
                    badgeBgColor: const Color(0xFFEAF6E5),
                    thumbnailEmoji: '🎨',
                    thumbnailBgColor: const Color(0xFFE6F2F5),
                    onTap: () => context.push('/image-result'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
