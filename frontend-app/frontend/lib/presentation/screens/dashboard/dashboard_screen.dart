import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../config/app_theme.dart';
import '../../../data/models/content_model.dart';
import '../../../data/models/store_model.dart';
import '../../../data/providers/dashboard_provider.dart';
import '../../../data/providers/store_provider.dart';
import '../../widgets/cards/recent_content_card.dart';
import '../../widgets/cards/stat_card.dart';
import '../../widgets/buttons/creation_action_button.dart';
import '../../widgets/navigation/bottom_nav_bar.dart';

String _dashboardStoreTitle(
  AsyncValue<StoreProfileModel?> storeProfileAsync,
  AsyncValue<DashboardData> dashboardAsync,
) {
  String fromModel(StoreProfileModel? p) {
    final n = p?.storeName.trim() ?? '';
    return n.isNotEmpty ? n : '';
  }

  final fromStore = storeProfileAsync.maybeWhen(
    data: fromModel,
    orElse: () => '',
  );
  if (fromStore.isNotEmpty) {
    return fromStore;
  }

  return dashboardAsync.maybeWhen(
    data: (d) {
      final n = fromModel(d.storeProfile);
      return n.isNotEmpty ? n : '우리 가게';
    },
    orElse: () => '우리 가게',
  );
}

class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final textTheme = Theme.of(context).textTheme;
    final size = MediaQuery.of(context).size;
    final dashboardAsync = ref.watch(dashboardDataProvider);
    final storeProfileAsync = ref.watch(currentStoreProfileProvider);

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
                              _dashboardStoreTitle(
                                storeProfileAsync,
                                dashboardAsync,
                              ),
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
                  dashboardAsync.when(
                    data: (data) => Row(
                      children: [
                        Expanded(
                          child: StatCard(
                            value: data.stats.totalCreated.toString(),
                            label: '생성한 콘텐츠',
                            valueColor: AppTheme.primaryColor,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: StatCard(
                            value: data.stats.totalShared.toString(),
                            label: '공유 완료',
                            valueColor: const Color(0xFF4F8B41),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: StatCard(
                            value: data.stats.createdThisWeek.toString(),
                            label: '이번 주',
                            valueColor: const Color(0xFFD16A31),
                          ),
                        ),
                      ],
                    ),
                    loading: () => const Row(
                      children: [
                        Expanded(
                          child: StatCard(
                            value: '-',
                            label: '생성한 콘텐츠',
                            valueColor: AppTheme.primaryColor,
                          ),
                        ),
                        SizedBox(width: 12),
                        Expanded(
                          child: StatCard(
                            value: '-',
                            label: '공유 완료',
                            valueColor: Color(0xFF4F8B41),
                          ),
                        ),
                        SizedBox(width: 12),
                        Expanded(
                          child: StatCard(
                            value: '-',
                            label: '이번 주',
                            valueColor: Color(0xFFD16A31),
                          ),
                        ),
                      ],
                    ),
                    error: (_, __) => const Row(
                      children: [
                        Expanded(
                          child: StatCard(
                            value: '0',
                            label: '생성한 콘텐츠',
                            valueColor: AppTheme.primaryColor,
                          ),
                        ),
                        SizedBox(width: 12),
                        Expanded(
                          child: StatCard(
                            value: '0',
                            label: '공유 완료',
                            valueColor: Color(0xFF4F8B41),
                          ),
                        ),
                        SizedBox(width: 12),
                        Expanded(
                          child: StatCard(
                            value: '0',
                            label: '이번 주',
                            valueColor: Color(0xFFD16A31),
                          ),
                        ),
                      ],
                    ),
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
                  dashboardAsync.when(
                    data: (data) {
                      if (data.recentRequests.isEmpty) {
                        return Padding(
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          child: Text(
                            '아직 생성된 콘텐츠가 없어요.',
                            style: textTheme.bodyLarge?.copyWith(
                              color: AppTheme.textTertiary,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        );
                      }
                      return Column(
                        children: data.recentRequests.asMap().entries.map((entry) {
                          final index = entry.key;
                          final item = entry.value;
                          return Padding(
                            padding: EdgeInsets.only(bottom: index == data.recentRequests.length - 1 ? 0 : 14),
                            child: _buildRecentCard(context, item),
                          );
                        }).toList(),
                      );
                    },
                    loading: () => const Padding(
                      padding: EdgeInsets.symmetric(vertical: 12),
                      child: CircularProgressIndicator(),
                    ),
                    error: (_, __) => Padding(
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      child: Text(
                        '최근 콘텐츠를 불러오지 못했어요.',
                        style: textTheme.bodyLarge?.copyWith(
                          color: AppTheme.textTertiary,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildRecentCard(BuildContext context, ContentRequestItem item) {
    final isImage = item.imageCount > 0;
    return RecentContentCard(
      title: item.concept.isNotEmpty ? item.concept : '제목 없음',
      subtitle: '${_formatRelative(item.createdAt)} · ${item.imageCount}장',
      badgeText: isImage ? '이미지' : '문구',
      badgeTextColor: isImage ? const Color(0xFF5B9B4C) : AppTheme.primaryColor,
      badgeBgColor: isImage ? const Color(0xFFEAF6E5) : const Color(0xFFEEEAFE),
      thumbnailEmoji: isImage ? '🎨' : '📸',
      thumbnailBgColor: isImage ? const Color(0xFFE6F2F5) : AppTheme.fillLight,
      onTap: () => context.push('/history'),
    );
  }

  String _formatRelative(DateTime? createdAt) {
    if (createdAt == null) {
      return '날짜 없음';
    }
    final now = DateTime.now();
    final diff = now.difference(createdAt);
    if (diff.inMinutes < 1) {
      return '방금 전';
    }
    if (diff.inHours < 1) {
      return '${diff.inMinutes}분 전';
    }
    if (diff.inDays < 1) {
      return '${diff.inHours}시간 전';
    }
    if (diff.inDays == 1) {
      return '어제';
    }
    return '${createdAt.year}.${createdAt.month.toString().padLeft(2, '0')}.${createdAt.day.toString().padLeft(2, '0')}';
  }
}
