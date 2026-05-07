import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../../data/providers/auth_provider.dart';
import '../../widgets/navigation/bottom_nav_bar.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final textTheme = Theme.of(context).textTheme;
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);
    final currentUser = ref.watch(currentUserProvider);

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      bottomNavigationBar: const AppBottomNavBar(currentIndex: 3),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: EdgeInsets.fromLTRB(h(18), h(14), h(18), h(24)),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 520),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '설정',
                    style: textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w700,
                      fontSize: f(23),
                      letterSpacing: 0,
                    ),
                  ),
                  SizedBox(height: h(18)),
                  currentUser.when(
                    data: (user) => _ProfileCard(
                      name: user.name.isNotEmpty ? user.name : '이름 없음',
                      email: user.email.isNotEmpty ? user.email : '이메일 없음',
                    ),
                    loading: () => const _ProfileCard(
                      name: '불러오는 중...',
                      email: '사용자 정보를 불러오고 있어요',
                    ),
                    error: (_, __) => const _ProfileCard(
                      name: '사용자 정보 불러오기 실패',
                      email: '다시 시도해 주세요',
                    ),
                  ),
                  SizedBox(height: h(24)),
                  const _SectionLabel('계정'),
                  SizedBox(height: h(8)),
                  _SettingsCard(
                    children: [
                      _SettingsTile(
                        icon: Icons.person_rounded,
                        iconColor: const Color(0xFF6D63F6),
                        iconBg: const Color(0xFFEEEAFE),
                        title: '프로필 수정',
                        onTap: () => context.push('/profile-edit'),
                      ),
                      _SettingsTile(
                        icon: Icons.notifications_rounded,
                        iconColor: const Color(0xFFE5A50A),
                        iconBg: const Color(0xFFFFF8E6),
                        title: '알림 설정',
                        onTap: () {},
                        showDividerAbove: true,
                      ),
                    ],
                  ),
                  SizedBox(height: h(20)),
                  const _SectionLabel('가게'),
                  SizedBox(height: h(8)),
                  _SettingsCard(
                    children: [
                      _SettingsTile(
                        icon: Icons.storefront_rounded,
                        iconColor: const Color(0xFF6B7280),
                        iconBg: AppTheme.fillLight,
                        title: '가게 정보 수정',
                        onTap: () => context.push('/profile-setup?mode=edit'),
                      ),
                      _SettingsTile(
                        icon: Icons.link_rounded,
                        iconColor: const Color(0xFF3B82F6),
                        iconBg: const Color(0xFFEFF6FF),
                        title: 'SNS 계정 관리',
                        onTap: () => context.push('/sns-management'),
                        showDividerAbove: true,
                      ),
                    ],
                  ),
                  SizedBox(height: h(20)),
                  const _SectionLabel('구독'),
                  SizedBox(height: h(8)),
                  _SettingsCard(
                    children: [
                      _SettingsTile(
                        icon: Icons.diamond_rounded,
                        iconColor: const Color(0xFF2563EB),
                        iconBg: const Color(0xFFEFF6FF),
                        title: '구독 플랜',
                        trailingBadge: const _Badge(
                          label: 'Free',
                          textColor: Color(0xFFE8892E),
                          backgroundColor: Color(0xFFFFF4E8),
                        ),
                        onTap: () {},
                      ),
                      _SettingsTile(
                        icon: Icons.credit_card_rounded,
                        iconColor: const Color(0xFF2563EB),
                        iconBg: const Color(0xFFEFF6FF),
                        title: '결제 수단',
                        onTap: () {},
                        showDividerAbove: true,
                      ),
                    ],
                  ),
                  SizedBox(height: h(24)),
                  Center(
                    child: TextButton(
                      onPressed: () async {
                        await ref.read(authProvider.notifier).logout();
                        if (context.mounted) {
                          context.go('/login');
                        }
                      },
                      child: Text(
                        '로그아웃',
                        style: textTheme.titleSmall?.copyWith(
                          color: const Color(0xFFE25555),
                          fontWeight: FontWeight.w600,
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
}

class _SectionLabel extends StatelessWidget {
  const _SectionLabel(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(left: 4),
      child: Text(
        text,
        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: AppTheme.textTertiary,
              fontWeight: FontWeight.w600,
              fontSize: 14,
            ),
      ),
    );
  }
}

class _ProfileCard extends StatelessWidget {
  const _ProfileCard({
    required this.name,
    required this.email,
  });

  final String name;
  final String email;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 18),
      decoration: BoxDecoration(
        color: AppTheme.surfaceColor,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppTheme.borderColor),
        boxShadow: const [
          BoxShadow(
            color: AppTheme.shadowColor,
            blurRadius: 12,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          Container(
            width: 56,
            height: 56,
            decoration: const BoxDecoration(
              color: Color(0xFFEEEAFE),
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.person_rounded,
              size: 30,
              color: Color(0xFF4F46E5),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  name,
                  style: textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  email,
                  style: textTheme.bodyMedium?.copyWith(
                    color: AppTheme.textSecondary,
                    fontSize: 14,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SettingsCard extends StatelessWidget {
  const _SettingsCard({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: AppTheme.surfaceColor,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppTheme.borderColor),
        boxShadow: const [
          BoxShadow(
            color: AppTheme.shadowColor,
            blurRadius: 10,
            offset: Offset(0, 3),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(children: children),
    );
  }
}

class _SettingsTile extends StatelessWidget {
  const _SettingsTile({
    required this.icon,
    required this.iconColor,
    required this.iconBg,
    required this.title,
    required this.onTap,
    this.trailingBadge,
    this.showDividerAbove = false,
  });

  final IconData icon;
  final Color iconColor;
  final Color iconBg;
  final String title;
  final VoidCallback onTap;
  final Widget? trailingBadge;
  final bool showDividerAbove;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        if (showDividerAbove)
          const Divider(height: 1, thickness: 1, color: AppTheme.dividerColor),
        Material(
          color: Colors.transparent,
          child: InkWell(
            onTap: onTap,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
              child: Row(
                children: [
                  Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(
                      color: iconBg,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Icon(icon, color: iconColor, size: 22),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Text(
                      title,
                      style: textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w600,
                        fontSize: 16,
                      ),
                    ),
                  ),
                  if (trailingBadge != null) ...[
                    trailingBadge!,
                    const SizedBox(width: 8),
                  ],
                  Icon(
                    Icons.chevron_right_rounded,
                    color: AppTheme.textTertiary,
                    size: 26,
                  ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _Badge extends StatelessWidget {
  const _Badge({
    required this.label,
    required this.textColor,
    required this.backgroundColor,
  });

  final String label;
  final Color textColor;
  final Color backgroundColor;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        label,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: textColor,
              fontWeight: FontWeight.w700,
              fontSize: 12,
            ),
      ),
    );
  }
}
