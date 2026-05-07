import 'package:flutter/material.dart';
import '../../../../config/app_theme.dart';
import '../../../../config/constants.dart';

// 히스토리 목록 카드
class RecentContentCard extends StatelessWidget {
  final String title;
  final String subtitle;
  final String badgeText;
  final Color badgeTextColor;
  final Color badgeBgColor;
  final String thumbnailEmoji;
  final Color thumbnailBgColor;
  final VoidCallback? onTap;

  const RecentContentCard({
    super.key,
    required this.title,
    required this.subtitle,
    required this.badgeText,
    required this.badgeTextColor,
    required this.badgeBgColor,
    this.thumbnailEmoji = '📸',
    this.thumbnailBgColor = AppTheme.fillLight,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);

    return InkWell(
      borderRadius: BorderRadius.circular(h(22)),
      onTap: onTap,
      child: Container(
        padding: EdgeInsets.all(h(16)),
        decoration: BoxDecoration(
          color: AppTheme.surfaceColor,
          borderRadius: BorderRadius.circular(h(22)),
          border: Border.all(color: AppTheme.borderColor),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.025),
              blurRadius: 8,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Row(
          children: [
            Container(
              width: h(68),
              height: h(68),
              decoration: BoxDecoration(
                color: thumbnailBgColor,
                borderRadius: BorderRadius.circular(h(16)),
              ),
              alignment: Alignment.center,
              child: Text(thumbnailEmoji, style: TextStyle(fontSize: f(30))),
            ),
            SizedBox(width: h(12)),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                  SizedBox(height: h(2)),
                  Text(
                    subtitle,
                    style: textTheme.bodyMedium?.copyWith(
                      fontSize: f(14),
                      color: AppTheme.textTertiary,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
              ),
            ),
            Container(
              padding: EdgeInsets.symmetric(horizontal: h(10), vertical: h(6)),
              decoration: BoxDecoration(
                color: badgeBgColor,
                borderRadius: BorderRadius.circular(h(10)),
              ),
              child: Text(
                badgeText,
                style: TextStyle(
                  fontSize: f(14),
                  fontWeight: FontWeight.w600,
                  color: badgeTextColor,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
