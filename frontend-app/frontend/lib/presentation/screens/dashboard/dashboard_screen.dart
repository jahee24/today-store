import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../../data/models/content_model.dart';
import '../../../data/models/store_model.dart';
import '../../../data/providers/dashboard_provider.dart';
import '../../../data/providers/store_provider.dart';
import '../../widgets/cards/recent_content_card.dart';
import '../../widgets/cards/stat_card.dart';
import '../../widgets/buttons/creation_action_button.dart';
import '../../widgets/dialogs/app_confirm_dialog.dart';
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

  void _handleCreateContentTap(
    BuildContext context,
    AsyncValue<StoreProfileModel?> storeProfileAsync,
    String targetRoute,
  ) {
    final hasStoreProfile = storeProfileAsync.maybeWhen(
      data: (store) => store != null,
      orElse: () => false,
    );

    if (hasStoreProfile) {
      context.push(targetRoute);
      return;
    }

    showDialog<void>(
      context: context,
      builder: (dialogContext) => AppConfirmDialog(
        title: '알림',
        content: '가게 정보를 먼저 입력해주세요.',
        cancelText: '닫기',
        confirmText: '입력하러 가기',
        onConfirm: () => context.push('/profile-setup?mode=edit'),
      ),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final textTheme = Theme.of(context).textTheme;
    final size = MediaQuery.of(context).size;
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);
    final dashboardAsync = ref.watch(dashboardDataProvider);
    final storeProfileAsync = ref.watch(currentStoreProfileProvider);

    final horizontalPadding = math.max(h(14), size.width * 0.045);
    final topPadding = math.max(h(12), size.height * 0.018);
    final bottomPadding = math.max(h(16), size.height * 0.02);

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
                                fontSize: f(18),
                              ),
                            ),
                            Text(
                              _dashboardStoreTitle(
                                storeProfileAsync,
                                dashboardAsync,
                              ),
                              style: textTheme.headlineMedium?.copyWith(
                                fontSize: f(26),
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
                  SizedBox(height: h(18)),
                  Container(
                    width: double.infinity,
                    padding: EdgeInsets.all(h(24)),
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(
                        colors: [AppTheme.primaryColor, AppTheme.infoText],
                      ),
                      borderRadius: BorderRadius.circular(h(24)),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '새 홍보 콘텐츠 만들기',
                          style: textTheme.titleMedium?.copyWith(
                            color: AppTheme.textOnPrimary,
                            fontWeight: FontWeight.w700,
                            fontSize: f(23),
                          ),
                        ),
                        SizedBox(height: h(4)),
                        Text(
                          'AI가 알아서 만들어 드려요',
                          style: textTheme.bodyLarge?.copyWith(
                            color: Colors.white.withOpacity(0.85),
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                        SizedBox(height: h(18)),
                        Row(
                          children: [
                            Expanded(
                              child: CreationActionButton(
                                label: '+ 문구 생성',
                                onPressed: () => _handleCreateContentTap(
                                  context,
                                  storeProfileAsync,
                                  '/step1',
                                ),
                              ),
                            ),
                            SizedBox(width: h(10)),
                            Expanded(
                              child: CreationActionButton(
                                label: '+ 이미지 생성',
                                filled: false,
                                onPressed: () => _handleCreateContentTap(
                                  context,
                                  storeProfileAsync,
                                  '/image-step1',
                                ),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  SizedBox(height: h(18)),
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
                        SizedBox(width: h(10)),
                        Expanded(
                          child: StatCard(
                            value: data.stats.totalShared.toString(),
                            label: '공유 완료',
                            valueColor: const Color(0xFF4F8B41),
                          ),
                        ),
                        SizedBox(width: h(10)),
                        Expanded(
                          child: StatCard(
                            value: data.stats.createdThisWeek.toString(),
                            label: '이번 주',
                            valueColor: const Color(0xFFD16A31),
                          ),
                        ),
                      ],
                    ),
                    loading: () => Row(
                      children: [
                        Expanded(
                          child: StatCard(
                            value: '-',
                            label: '생성한 콘텐츠',
                            valueColor: AppTheme.primaryColor,
                          ),
                        ),
                        SizedBox(width: h(10)),
                        Expanded(
                          child: StatCard(
                            value: '-',
                            label: '공유 완료',
                            valueColor: Color(0xFF4F8B41),
                          ),
                        ),
                        SizedBox(width: h(10)),
                        Expanded(
                          child: StatCard(
                            value: '-',
                            label: '이번 주',
                            valueColor: Color(0xFFD16A31),
                          ),
                        ),
                      ],
                    ),
                    error: (_, __) => Row(
                      children: [
                        Expanded(
                          child: StatCard(
                            value: '0',
                            label: '생성한 콘텐츠',
                            valueColor: AppTheme.primaryColor,
                          ),
                        ),
                        SizedBox(width: h(10)),
                        Expanded(
                          child: StatCard(
                            value: '0',
                            label: '공유 완료',
                            valueColor: Color(0xFF4F8B41),
                          ),
                        ),
                        SizedBox(width: h(10)),
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
                  SizedBox(height: h(20)),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        '최근 콘텐츠',
                        style: textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.w700,
                          fontSize: f(21),
                        ),
                      ),
                      TextButton(
                        onPressed: () {
                          context.push('/history');
                        },
                        child: Text(
                          '전체보기',
                          style: TextStyle(
                            fontSize: f(17),
                            fontWeight: FontWeight.w500,
                            color: AppTheme.primaryColor,
                          ),
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: h(6)),
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
    final isImage = item.concept.trim() == '이미지 베리에이션';
    return RecentContentCard(
      title: item.concept.isNotEmpty ? item.concept : '제목 없음',
      subtitle: '${_formatRelative(item.createdAt)} · ${item.imageCount}장',
      badgeText: isImage ? '이미지' : '문구',
      badgeTextColor: isImage ? const Color(0xFF5B9B4C) : AppTheme.primaryColor,
      badgeBgColor: isImage ? const Color(0xFFEAF6E5) : const Color(0xFFEEEAFE),
      thumbnailEmoji: isImage ? '🎨' : '📸',
      thumbnailUrl: item.thumbnailUrl,
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
